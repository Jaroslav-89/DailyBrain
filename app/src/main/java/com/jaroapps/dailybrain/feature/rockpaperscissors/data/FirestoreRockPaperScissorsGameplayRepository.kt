package com.jaroapps.dailybrain.feature.rockpaperscissors.data

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Transaction
import com.google.firebase.firestore.SetOptions
import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.core.database.entity.PendingGameMoveEntity
import com.jaroapps.dailybrain.feature.profile.domain.repository.requireMatchingFirebaseSession
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameChatMessage
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameMatchHistoryItem
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GamePairState
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameParticipant
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoom
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomSnapshot
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomVisibility
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoundAnnouncement
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsMove
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsOutcome
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsRules
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.repository.RockPaperScissorsGameplayRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FirestoreRockPaperScissorsGameplayRepository(
    context: Context,
    private val database: AppDatabase,
    private val activeUserProvider: ActiveUserProvider,
) : RockPaperScissorsGameplayRepository {
    private val appContext = context.applicationContext

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeRoom(firebaseUid: String, roomId: String): Flow<GameRoomSnapshot> {
        val firestore = firestoreFor(firebaseUid)
        val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
        return observeDocument(roomRef).flatMapLatest { roomSnapshot ->
            val room = roomSnapshot.toGameRoom()
                ?: throw IllegalStateException("Игровая комната больше не существует.")
            val roundRef = roomRef.collection(ROUNDS).document(room.currentRoundId)
            combine(
                observeCollection(roomRef.collection(MEMBERS)),
                observeCollection(roundRef.collection(PAIRS)),
                observeDocument(roomRef.collection(CHAT_STATE).document(CHAT_SUMMARY)),
            ) { membersSnapshot, pairsSnapshot, chatSnapshot ->
                val participants = membersSnapshot.documents.mapNotNull { it.toParticipant() }
                val pairs = pairsSnapshot.documents.mapNotNull { it.toPairState(firebaseUid, participants) }
                val currentParticipant = participants.firstOrNull { it.firebaseUid == firebaseUid }
                val latestMessageAt = chatSnapshot.getLong("lastMessageAtEpochMs") ?: 0L
                val finishedRoundId = roomSnapshot.getString("lastFinishedRoundId")
                val announcement = if (
                    !finishedRoundId.isNullOrBlank() &&
                    currentParticipant?.lastSeenFinishedRoundId != finishedRoundId
                ) {
                    GameRoundAnnouncement(
                        roundId = finishedRoundId,
                        winnerUid = roomSnapshot.getString("lastWinnerUid"),
                        winnerName = roomSnapshot.getString("lastWinnerName"),
                        winningScore = roomSnapshot.getLong("lastWinnerScore")?.toInt() ?: 0,
                        finishedAtEpochMs = roomSnapshot.getLong("lastRoundFinishedAtEpochMs") ?: 0L,
                    )
                } else null
                GameRoomSnapshot(
                    room = room,
                    participants = participants.sortedWith(
                        compareByDescending<GameParticipant> { it.score }.thenBy { it.name.lowercase() },
                    ),
                    pairs = pairs,
                    latestMessageAtEpochMs = latestMessageAt,
                    roundAnnouncement = announcement,
                    hasUnreadMessages = latestMessageAt > (currentParticipant?.lastReadMessageAtEpochMs ?: 0L) &&
                        latestMessageAt > 0L,
                )
            }
        }
    }

    override suspend fun submitMove(
        firebaseUid: String,
        roomId: String,
        roundId: String,
        opponentUid: String,
        move: RockPaperScissorsMove,
    ) {
        require(opponentUid.isNotBlank() && opponentUid != firebaseUid)
        val localUserId = activeUserProvider.currentUserId()
        database.pendingGameMoveDao().insert(
            PendingGameMoveEntity(
                ownerUserId = localUserId,
                roomId = roomId,
                roundId = roundId,
                opponentUid = opponentUid,
                move = move.name,
                createdAtEpochMs = System.currentTimeMillis(),
            ),
        )
        try {
            syncPendingMoves(firebaseUid, roomId)
        } catch (error: Exception) {
            if (!isNetworkUnavailable(error)) throw error
        }
    }

    override suspend fun syncPendingMoves(firebaseUid: String, roomId: String?) {
        val localUserId = activeUserProvider.currentUserId()
        val pending = if (roomId == null) {
            database.pendingGameMoveDao().getPendingForUser(localUserId)
        } else {
            database.pendingGameMoveDao().getPendingForRoom(localUserId, roomId)
        }
        val firestore = firestoreFor(firebaseUid)
        val roomsToCheck = roomId?.let(::listOf) ?: pending.map { it.roomId }.distinct()
        for (candidateRoomId in roomsToCheck) {
            val roomRef = firestore.collection(GAME_ROOMS).document(candidateRoomId)
            val roomState = roomRef.get().awaitResult()
            val finishedRoundId = roomState.getString("lastFinishedRoundId")
            val currentRoundId = roomState.getString("currentRoundId")
            if (!finishedRoundId.isNullOrBlank() && finishedRoundId != currentRoundId) {
                cleanupRound(firestore, candidateRoomId, finishedRoundId)
            }
        }

        for (item in pending) {
            if (item.ownerUserId != localUserId) continue
            val move = runCatching { RockPaperScissorsMove.valueOf(item.move) }.getOrNull()
            if (move == null) {
                deletePending(localUserId, item)
                continue
            }
            try {
                val accepted = persistMove(
                    firestore = firestore,
                    firebaseUid = firebaseUid,
                    roomId = item.roomId,
                    roundId = item.roundId,
                    opponentUid = item.opponentUid,
                    move = move,
                )
                // Keep the pending row until result calculation succeeds. If Firestore rejects
                // the resolution transaction, the next screen entry can retry it.
                if (accepted) {
                    resolvePairIfReady(firestore, firebaseUid, item.roomId, item.roundId, item.opponentUid)
                }
                // A move from an older round is deliberately discarded instead of being replayed.
                deletePending(localUserId, item)
            } catch (error: Exception) {
                if (isNetworkUnavailable(error)) return
                throw error
            }
        }

        // A previous attempt may already have persisted both moves and then failed while
        // calculating the result. Such a pair has no local pending move to replay, so scan
        // the active round whenever the room is opened or refreshed.
        for (candidateRoomId in roomsToCheck) {
            val room = firestore.collection(GAME_ROOMS).document(candidateRoomId).get().awaitResult()
            val activeRoundId = room.getString("currentRoundId") ?: continue
            recoverReadyPairs(firestore, firebaseUid, candidateRoomId, activeRoundId)
        }
    }

    private suspend fun recoverReadyPairs(
        firestore: FirebaseFirestore,
        firebaseUid: String,
        roomId: String,
        roundId: String,
    ) {
        val pairSnapshot = firestore.collection(GAME_ROOMS).document(roomId)
            .collection(ROUNDS).document(roundId)
            .collection(PAIRS)
            .get()
            .awaitResult()

        for (pair in pairSnapshot.documents) {
            if (pair.getString("status") != MATCH_WAITING) continue
            val uidA = pair.getString("uidA") ?: continue
            val uidB = pair.getString("uidB") ?: continue
            if (firebaseUid != uidA && firebaseUid != uidB) continue
            val submitted = pair.stringList("submittedUids")
            if (uidA !in submitted || uidB !in submitted) continue
            val opponentUid = if (firebaseUid == uidA) uidB else uidA
            resolvePairIfReady(firestore, firebaseUid, roomId, roundId, opponentUid)
        }
    }

    private suspend fun deletePending(localUserId: String, item: PendingGameMoveEntity) {
        database.pendingGameMoveDao().delete(
            ownerUserId = localUserId,
            roomId = item.roomId,
            roundId = item.roundId,
            opponentUid = item.opponentUid,
        )
    }

    private suspend fun persistMove(
        firestore: FirebaseFirestore,
        firebaseUid: String,
        roomId: String,
        roundId: String,
        opponentUid: String,
        move: RockPaperScissorsMove,
    ): Boolean {
        val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
        val roundRef = roomRef.collection(ROUNDS).document(roundId)
        val pairId = RockPaperScissorsRules.pairId(firebaseUid, opponentUid)
        val pairRef = roundRef.collection(PAIRS).document(pairId)
        val sortedUids = listOf(firebaseUid, opponentUid).sorted()
        val uidA = sortedUids[0]
        val uidB = sortedUids[1]
        val now = System.currentTimeMillis()

        return firestore.runTransaction { transaction: Transaction ->
            val room = transaction.get(roomRef)
            if (!room.exists() || room.getString("currentRoundId") != roundId) {
                return@runTransaction false
            }
            val roomMembers = room.stringList("memberUids")
            if (firebaseUid !in roomMembers || opponentUid !in roomMembers) {
                throw IllegalStateException("Один из участников больше не состоит в комнате.")
            }
            val pair = transaction.get(pairRef)
            val previousStatus = pair.getString("status")
            val previousNumber = pair.getLong("matchNumber")?.toInt() ?: 0
            val nextMatchNumber = if (previousStatus == MATCH_RESOLVED) previousNumber + 1 else maxOf(1, previousNumber)
            val matchId = if (previousStatus == MATCH_WAITING) {
                pair.getString("matchId") ?: matchId(roundId, pairId, nextMatchNumber)
            } else {
                matchId(roundId, pairId, nextMatchNumber)
            }
            val matchRef = roundRef.collection(MATCHES).document(matchId)
            val moveRef = matchRef.collection(MOVES).document(firebaseUid)
            val ownMove = transaction.get(moveRef)
            val match = transaction.get(matchRef)
            if (ownMove.exists()) return@runTransaction true

            val submitted = if (previousStatus == MATCH_WAITING) {
                (pair.stringList("submittedUids") + firebaseUid).distinct()
            } else {
                listOf(firebaseUid)
            }
            transaction.set(moveRef, mapOf(
                "uid" to firebaseUid,
                "choice" to move.name,
                "createdAtEpochMs" to now,
            ))
            if (!match.exists()) {
                transaction.set(matchRef, mapOf(
                    "matchId" to matchId,
                    "roundId" to roundId,
                    "uidA" to uidA,
                    "uidB" to uidB,
                    "nameA" to "",
                    "nameB" to "",
                    "status" to MATCH_WAITING,
                    "createdAtEpochMs" to now,
                ))
            }
            transaction.set(pairRef, mapOf(
                "pairId" to pairId,
                "uidA" to uidA,
                "uidB" to uidB,
                "matchNumber" to nextMatchNumber,
                "matchId" to matchId,
                "status" to MATCH_WAITING,
                "submittedUids" to submitted,
                "updatedAtEpochMs" to now,
            ), SetOptions.merge())
            true
        }.awaitResult()
    }

    private suspend fun resolvePairIfReady(
        firestore: FirebaseFirestore,
        firebaseUid: String,
        roomId: String,
        roundId: String,
        opponentUid: String,
    ) {
        val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
        val roundRef = roomRef.collection(ROUNDS).document(roundId)
        val pairId = RockPaperScissorsRules.pairId(firebaseUid, opponentUid)
        val pairRef = roundRef.collection(PAIRS).document(pairId)
        val initialPair = pairRef.get().awaitResult()
        if (!initialPair.exists() || initialPair.getString("status") != MATCH_WAITING) return
        val uidA = initialPair.getString("uidA") ?: return
        val uidB = initialPair.getString("uidB") ?: return
        val submitted = initialPair.stringList("submittedUids")
        if (uidA !in submitted || uidB !in submitted) return
        val matchId = initialPair.getString("matchId") ?: return
        val matchRef = roundRef.collection(MATCHES).document(matchId)
        val moveARef = matchRef.collection(MOVES).document(uidA)
        val moveBRef = matchRef.collection(MOVES).document(uidB)
        val memberARef = roomRef.collection(MEMBERS).document(uidA)
        val memberBRef = roomRef.collection(MEMBERS).document(uidB)
        val now = System.currentTimeMillis()

        val roundFinished = firestore.runTransaction { transaction: Transaction ->
            val room = transaction.get(roomRef)
            val round = transaction.get(roundRef)
            val pair = transaction.get(pairRef)
            val match = transaction.get(matchRef)
            if (!room.exists() || room.getString("currentRoundId") != roundId ||
                round.getString("status") != ROUND_ACTIVE ||
                pair.getString("status") != MATCH_WAITING ||
                match.getString("status") != MATCH_WAITING
            ) return@runTransaction false

            val moveAData = transaction.get(moveARef)
            val moveBData = transaction.get(moveBRef)
            if (!moveAData.exists() || !moveBData.exists()) return@runTransaction false
            val moveA = enumValueOrNull<RockPaperScissorsMove>(moveAData.getString("choice")) ?: return@runTransaction false
            val moveB = enumValueOrNull<RockPaperScissorsMove>(moveBData.getString("choice")) ?: return@runTransaction false
            val memberA = transaction.get(memberARef)
            val memberB = transaction.get(memberBRef)
            if (!memberA.exists() || !memberB.exists()) return@runTransaction false

            val memberUids = room.stringList("memberUids")
            val extraMemberRefs = memberUids.filterNot { it == uidA || it == uidB }.associateWith {
                roomRef.collection(MEMBERS).document(it)
            }
            val extraMemberSnapshots = extraMemberRefs.mapValues { (_, ref) -> transaction.get(ref) }
            val currentRoundNumber = room.getLong("roundNumber")?.toInt() ?: 1
            val nextRoundId = (currentRoundNumber + 1).toString()
            val nextRoundRef = roomRef.collection(ROUNDS).document(nextRoundId)
            val resultRef = roomRef.collection(ROUND_RESULTS).document(roundId)
            val existingResult = transaction.get(resultRef)
            val existingNextRound = transaction.get(nextRoundRef)

            val outcomeA = RockPaperScissorsRules.outcome(moveA, moveB)
            val outcomeB = when (outcomeA) {
                RockPaperScissorsOutcome.WIN -> RockPaperScissorsOutcome.LOSS
                RockPaperScissorsOutcome.DRAW -> RockPaperScissorsOutcome.DRAW
                RockPaperScissorsOutcome.LOSS -> RockPaperScissorsOutcome.WIN
            }
            val pointsA = RockPaperScissorsRules.points(outcomeA)
            val pointsB = RockPaperScissorsRules.points(outcomeB)
            val scoreA = (memberA.getLong("score")?.toInt() ?: 0) + pointsA
            val scoreB = (memberB.getLong("score")?.toInt() ?: 0) + pointsB
            // Settings changed during an active round take effect on the next round.
            val targetScore = round.getLong("targetScore")?.toInt()
                ?: (room.getLong("targetScore")?.toInt() ?: RockPaperScissorsRules.DEFAULT_TARGET_SCORE)
            val nextRoundTargetScore = room.getLong("targetScore")?.toInt() ?: targetScore
            val hasWinner = scoreA >= targetScore || scoreB >= targetScore
            val winnerUid = when {
                !hasWinner -> null
                scoreA >= scoreB -> uidA
                else -> uidB
            }
            val winnerName = when (winnerUid) {
                uidA -> memberA.getString("name").orEmpty()
                uidB -> memberB.getString("name").orEmpty()
                else -> null
            }
            val winningScore = maxOf(scoreA, scoreB)
            val roundFinishedNow = hasWinner && !existingResult.exists()

            transaction.update(matchRef, mapOf(
                "status" to MATCH_RESOLVED,
                "roundId" to roundId,
                "nameA" to memberA.getString("name").orEmpty(),
                "nameB" to memberB.getString("name").orEmpty(),
                "moveA" to moveA.name,
                "moveB" to moveB.name,
                "winnerUid" to winnerUid,
                "winnerName" to winnerName,
                "pointsA" to pointsA,
                "pointsB" to pointsB,
                "scoreAfterA" to scoreA,
                "scoreAfterB" to scoreB,
                "targetScore" to targetScore,
                "roundFinished" to roundFinishedNow,
                "resolvedAtEpochMs" to now,
            ))
            transaction.update(pairRef, mapOf(
                "status" to MATCH_RESOLVED,
                "moveA" to moveA.name,
                "moveB" to moveB.name,
                "winnerUid" to winnerUid,
                "pointsA" to pointsA,
                "pointsB" to pointsB,
                "matchesPlayed" to ((pair.getLong("matchesPlayed")?.toInt() ?: 0) + 1),
                "winsA" to ((pair.getLong("winsA")?.toInt() ?: 0) + if (outcomeA == RockPaperScissorsOutcome.WIN) 1 else 0),
                "drawsA" to ((pair.getLong("drawsA")?.toInt() ?: 0) + if (outcomeA == RockPaperScissorsOutcome.DRAW) 1 else 0),
                "lossesA" to ((pair.getLong("lossesA")?.toInt() ?: 0) + if (outcomeA == RockPaperScissorsOutcome.LOSS) 1 else 0),
                "winsB" to ((pair.getLong("winsB")?.toInt() ?: 0) + if (outcomeB == RockPaperScissorsOutcome.WIN) 1 else 0),
                "drawsB" to ((pair.getLong("drawsB")?.toInt() ?: 0) + if (outcomeB == RockPaperScissorsOutcome.DRAW) 1 else 0),
                "lossesB" to ((pair.getLong("lossesB")?.toInt() ?: 0) + if (outcomeB == RockPaperScissorsOutcome.LOSS) 1 else 0),
                "rockCountA" to ((pair.getLong("rockCountA")?.toInt() ?: 0) + if (moveA == RockPaperScissorsMove.ROCK) 1 else 0),
                "paperCountA" to ((pair.getLong("paperCountA")?.toInt() ?: 0) + if (moveA == RockPaperScissorsMove.PAPER) 1 else 0),
                "scissorsCountA" to ((pair.getLong("scissorsCountA")?.toInt() ?: 0) + if (moveA == RockPaperScissorsMove.SCISSORS) 1 else 0),
                "rockCountB" to ((pair.getLong("rockCountB")?.toInt() ?: 0) + if (moveB == RockPaperScissorsMove.ROCK) 1 else 0),
                "paperCountB" to ((pair.getLong("paperCountB")?.toInt() ?: 0) + if (moveB == RockPaperScissorsMove.PAPER) 1 else 0),
                "scissorsCountB" to ((pair.getLong("scissorsCountB")?.toInt() ?: 0) + if (moveB == RockPaperScissorsMove.SCISSORS) 1 else 0),
                "resolvedAtEpochMs" to now,
                "updatedAtEpochMs" to now,
            ))
            // Keep move documents until round cleanup. Removing them inside the
            // result transaction adds two more security-rule checks to the critical path.
            val allMemberSnapshots = if (roundFinishedNow) {
                buildMap {
                    put(uidA, memberA)
                    put(uidB, memberB)
                    putAll(extraMemberSnapshots.filterValues { it.exists() })
                }
            } else emptyMap()

            val memberAUpdate = memberResultUpdate(
                memberA, moveA, outcomeA, if (roundFinishedNow) 0 else scoreA, roundId, matchId,
            )
            val memberBUpdate = memberResultUpdate(
                memberB, moveB, outcomeB, if (roundFinishedNow) 0 else scoreB, roundId, matchId,
            )
            transaction.update(memberARef, if (roundFinishedNow) memberAUpdate + roundResetFields() else memberAUpdate)
            transaction.update(memberBRef, if (roundFinishedNow) memberBUpdate + roundResetFields() else memberBUpdate)

            if (roundFinishedNow) {
                transaction.update(roundRef, mapOf(
                    "status" to ROUND_FINISHED,
                    "winnerUid" to winnerUid,
                    "winnerName" to winnerName,
                    "winningScore" to winningScore,
                    "finishedAtEpochMs" to now,
                ))
                transaction.set(resultRef, mapOf(
                    "roundId" to roundId,
                    "winnerUid" to winnerUid,
                    "winnerName" to winnerName,
                    "winningScore" to winningScore,
                    "scoreAfterA" to scoreA,
                    "scoreAfterB" to scoreB,
                    "targetScore" to targetScore,
                    "triggerMatchId" to matchId,
                    "finishedAtEpochMs" to now,
                    "isDraw" to (winnerUid == null),
                ))
                if (!existingNextRound.exists()) {
                    transaction.set(nextRoundRef, mapOf(
                        "roundId" to nextRoundId,
                        "roundNumber" to currentRoundNumber + 1,
                        "status" to ROUND_ACTIVE,
                        "targetScore" to nextRoundTargetScore,
                        "startedAtEpochMs" to now,
                    ))
                }
                transaction.update(roomRef, mapOf(
                    "currentRoundId" to nextRoundId,
                    "roundNumber" to currentRoundNumber + 1,
                    "lastFinishedRoundId" to roundId,
                    "lastWinnerUid" to winnerUid,
                    "lastWinnerName" to winnerName,
                    "lastWinnerScore" to winningScore,
                    "lastRoundFinishedAtEpochMs" to now,
                    "updatedAtEpochMs" to now,
                ))
                for ((memberUid, memberSnapshot) in allMemberSnapshots) {
                    val memberRef = roomRef.collection(MEMBERS).document(memberUid)
                    val reset = mapOf(
                        "score" to 0,
                        "roundWins" to 0,
                        "roundDraws" to 0,
                        "roundLosses" to 0,
                        "roundRockCount" to 0,
                        "roundPaperCount" to 0,
                        "roundScissorsCount" to 0,
                        "roundMatchesPlayed" to 0,
                    )
                    if (memberUid != uidA && memberUid != uidB) {
                        transaction.update(memberRef, reset)
                    }
                }
                true
            } else {
                false
            }
        }.awaitResult()

        if (roundFinished) cleanupRound(firestore, roomId, roundId)
    }

    private fun memberResultUpdate(
        current: DocumentSnapshot,
        move: RockPaperScissorsMove,
        outcome: RockPaperScissorsOutcome,
        newScore: Int,
        roundId: String,
        matchId: String,
    ): Map<String, Any> {
        val win = if (outcome == RockPaperScissorsOutcome.WIN) 1 else 0
        val draw = if (outcome == RockPaperScissorsOutcome.DRAW) 1 else 0
        val loss = if (outcome == RockPaperScissorsOutcome.LOSS) 1 else 0
        val rock = if (move == RockPaperScissorsMove.ROCK) 1 else 0
        val paper = if (move == RockPaperScissorsMove.PAPER) 1 else 0
        val scissors = if (move == RockPaperScissorsMove.SCISSORS) 1 else 0
        return mapOf(
            "score" to newScore,
            "wins" to ((current.getLong("wins")?.toInt() ?: 0) + win),
            "draws" to ((current.getLong("draws")?.toInt() ?: 0) + draw),
            "losses" to ((current.getLong("losses")?.toInt() ?: 0) + loss),
            "rockCount" to ((current.getLong("rockCount")?.toInt() ?: 0) + rock),
            "paperCount" to ((current.getLong("paperCount")?.toInt() ?: 0) + paper),
            "scissorsCount" to ((current.getLong("scissorsCount")?.toInt() ?: 0) + scissors),
            "matchesPlayed" to ((current.getLong("matchesPlayed")?.toInt() ?: 0) + 1),
            "roundWins" to ((current.getLong("roundWins")?.toInt() ?: 0) + win),
            "roundDraws" to ((current.getLong("roundDraws")?.toInt() ?: 0) + draw),
            "roundLosses" to ((current.getLong("roundLosses")?.toInt() ?: 0) + loss),
            "roundRockCount" to ((current.getLong("roundRockCount")?.toInt() ?: 0) + rock),
            "roundPaperCount" to ((current.getLong("roundPaperCount")?.toInt() ?: 0) + paper),
            "roundScissorsCount" to ((current.getLong("roundScissorsCount")?.toInt() ?: 0) + scissors),
            "roundMatchesPlayed" to ((current.getLong("roundMatchesPlayed")?.toInt() ?: 0) + 1),
            "lastPlayedAtEpochMs" to System.currentTimeMillis(),
            "lastResultMatchId" to matchId,
            "lastResultRoundId" to roundId,
        )
    }

    private fun roundResetFields(): Map<String, Any> = mapOf(
        "roundWins" to 0,
        "roundDraws" to 0,
        "roundLosses" to 0,
        "roundRockCount" to 0,
        "roundPaperCount" to 0,
        "roundScissorsCount" to 0,
        "roundMatchesPlayed" to 0,
    )

    private suspend fun cleanupRound(firestore: FirebaseFirestore, roomId: String, roundId: String) {
        val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
        val roundRef = roomRef.collection(ROUNDS).document(roundId)
        val matches = roundRef.collection(MATCHES).get().awaitResult()
        val moveReferences = matches.documents.flatMap { match ->
            listOfNotNull(match.getString("uidA"), match.getString("uidB"))
                .distinct()
                .map { uid -> match.reference.collection(MOVES).document(uid) }
        }
        moveReferences.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach(batch::delete)
            batch.commit().awaitResult()
        }
        deleteQueryInChunks(firestore, roundRef.collection(PAIRS))
        deleteQueryInChunks(firestore, roundRef.collection(MATCHES))
        roundRef.delete().awaitResult()
    }

    private suspend fun deleteQueryInChunks(firestore: FirebaseFirestore, query: Query) {
        var snapshot = query.limit(BATCH_LIMIT.toLong()).get().awaitResult()
        while (snapshot.documents.isNotEmpty()) {
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().awaitResult()
            if (snapshot.documents.size < BATCH_LIMIT) break
            snapshot = query.startAfter(snapshot.documents.last()).limit(BATCH_LIMIT.toLong()).get().awaitResult()
        }
    }

    override suspend fun getRoundHistory(firebaseUid: String, roomId: String): List<GameMatchHistoryItem> {
        val firestore = firestoreFor(firebaseUid)
        val room = firestore.collection(GAME_ROOMS).document(roomId).get().awaitResult()
        if (!room.exists()) return emptyList()
        val roundId = room.getString("currentRoundId") ?: return emptyList()
        return firestore.collection(GAME_ROOMS).document(roomId)
            .collection(ROUNDS).document(roundId).collection(MATCHES)
            // Unresolved matches do not have resolvedAtEpochMs, so orderBy naturally
            // excludes them while relying only on Firestore's built-in single-field index.
            .orderBy("resolvedAtEpochMs", Query.Direction.DESCENDING)
            .limit(HISTORY_LIMIT.toLong())
            .get()
            .awaitResult()
            .documents
            .mapNotNull { document ->
                val uidA = document.getString("uidA") ?: return@mapNotNull null
                val uidB = document.getString("uidB") ?: return@mapNotNull null
                val moveA = enumValueOrNull<RockPaperScissorsMove>(document.getString("moveA")) ?: return@mapNotNull null
                val moveB = enumValueOrNull<RockPaperScissorsMove>(document.getString("moveB")) ?: return@mapNotNull null
                GameMatchHistoryItem(
                    matchId = document.id,
                    firstUid = uidA,
                    firstName = document.getString("nameA").orEmpty(),
                    firstMove = moveA,
                    secondUid = uidB,
                    secondName = document.getString("nameB").orEmpty(),
                    secondMove = moveB,
                    winnerUid = document.getString("winnerUid"),
                    firstPoints = document.getLong("pointsA")?.toInt() ?: 0,
                    secondPoints = document.getLong("pointsB")?.toInt() ?: 0,
                    createdAtEpochMs = document.getLong("resolvedAtEpochMs")
                        ?: document.getLong("createdAtEpochMs") ?: 0L,
                )
            }
    }

    override fun observeChat(firebaseUid: String, roomId: String): Flow<List<GameChatMessage>> {
        val firestore = firestoreFor(firebaseUid)
        val query = firestore.collection(GAME_ROOMS).document(roomId)
            .collection(MESSAGES)
            .orderBy("createdAtEpochMs", Query.Direction.ASCENDING)
            .limitToLast(CHAT_LIMIT.toLong())
        return observeQuery(query).map { snapshot ->
            snapshot.documents.mapNotNull { document ->
                GameChatMessage(
                    messageId = document.id,
                    senderUid = document.getString("senderUid") ?: return@mapNotNull null,
                    senderName = document.getString("senderName").orEmpty(),
                    text = document.getString("text").orEmpty(),
                    createdAtEpochMs = document.getLong("createdAtEpochMs") ?: 0L,
                )
            }
        }
    }

    override suspend fun sendChatMessage(firebaseUid: String, roomId: String, senderName: String, text: String) {
        val normalized = text.trim()
        require(normalized.isNotEmpty()) { "Введите сообщение." }
        require(normalized.length <= MAX_MESSAGE_LENGTH) { "Сообщение слишком длинное." }
        require(senderName.isNotBlank()) { "Не удалось определить имя отправителя." }
        val firestore = firestoreFor(firebaseUid)
        val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
        val now = System.currentTimeMillis()
        val messageRef = roomRef.collection(MESSAGES).document()
        val summaryRef = roomRef.collection(CHAT_STATE).document(CHAT_SUMMARY)
        val batch = firestore.batch()
        batch.set(messageRef, mapOf(
            "senderUid" to firebaseUid,
            "senderName" to senderName.take(80),
            "text" to normalized,
            "createdAtEpochMs" to now,
        ))
        batch.set(summaryRef, mapOf("lastMessageAtEpochMs" to now), SetOptions.merge())
        batch.commit().awaitResult()
    }

    override suspend fun updateMemberDisplayName(firebaseUid: String, roomId: String, displayName: String) {
        val normalizedName = displayName.trim().take(80)
        require(normalizedName.isNotEmpty()) { "Имя пользователя не может быть пустым." }
        val firestore = firestoreFor(firebaseUid)
        firestore.collection(GAME_ROOMS).document(roomId)
            .collection(MEMBERS).document(firebaseUid)
            .update("name", normalizedName)
            .awaitResult()
    }

    override suspend fun markChatRead(firebaseUid: String, roomId: String, readAtEpochMs: Long) {
        if (readAtEpochMs <= 0L) return
        val firestore = firestoreFor(firebaseUid)
        firestore.collection(GAME_ROOMS).document(roomId)
            .collection(MEMBERS).document(firebaseUid)
            .update("lastReadMessageAtEpochMs", readAtEpochMs)
            .awaitResult()
    }

    override suspend fun acknowledgeRoundAnnouncement(firebaseUid: String, roomId: String, roundId: String) {
        val firestore = firestoreFor(firebaseUid)
        firestore.collection(GAME_ROOMS).document(roomId)
            .collection(MEMBERS).document(firebaseUid)
            .update("lastSeenFinishedRoundId", roundId)
            .awaitResult()
    }

    private fun DocumentSnapshot.toGameRoom(): GameRoom? {
        if (!exists()) return null
        val name = getString("name") ?: return null
        val ownerUid = getString("ownerUid") ?: return null
        return GameRoom(
            roomId = id,
            name = name,
            ownerUid = ownerUid,
            ownerName = getString("ownerName").orEmpty(),
            visibility = if (getBoolean("isPublic") == true) GameRoomVisibility.PUBLIC else GameRoomVisibility.PRIVATE,
            inviteCode = getString("inviteCode"),
            maxParticipants = getLong("maxParticipants")?.toInt() ?: 4,
            targetScore = getLong("targetScore")?.toInt() ?: 50,
            memberUids = stringList("memberUids"),
            currentRoundId = getString("currentRoundId") ?: "1",
            createdAtEpochMs = getLong("createdAtEpochMs") ?: 0L,
        )
    }

    private fun DocumentSnapshot.toParticipant(): GameParticipant? {
        val uid = getString("uid") ?: id
        val name = getString("name") ?: return null
        return GameParticipant(
            firebaseUid = uid,
            name = name,
            score = getLong("score")?.toInt() ?: 0,
            wins = getLong("roundWins")?.toInt() ?: getLong("wins")?.toInt() ?: 0,
            draws = getLong("roundDraws")?.toInt() ?: getLong("draws")?.toInt() ?: 0,
            losses = getLong("roundLosses")?.toInt() ?: getLong("losses")?.toInt() ?: 0,
            rockCount = getLong("roundRockCount")?.toInt() ?: getLong("rockCount")?.toInt() ?: 0,
            paperCount = getLong("roundPaperCount")?.toInt() ?: getLong("paperCount")?.toInt() ?: 0,
            scissorsCount = getLong("roundScissorsCount")?.toInt() ?: getLong("scissorsCount")?.toInt() ?: 0,
            matchesPlayed = getLong("roundMatchesPlayed")?.toInt() ?: getLong("matchesPlayed")?.toInt() ?: 0,
            totalWins = getLong("wins")?.toInt() ?: 0,
            totalDraws = getLong("draws")?.toInt() ?: 0,
            totalLosses = getLong("losses")?.toInt() ?: 0,
            totalRockCount = getLong("rockCount")?.toInt() ?: 0,
            totalPaperCount = getLong("paperCount")?.toInt() ?: 0,
            totalScissorsCount = getLong("scissorsCount")?.toInt() ?: 0,
            totalMatchesPlayed = getLong("matchesPlayed")?.toInt() ?: 0,
            lastReadMessageAtEpochMs = getLong("lastReadMessageAtEpochMs") ?: 0L,
            lastSeenFinishedRoundId = getString("lastSeenFinishedRoundId"),
        )
    }

    private fun DocumentSnapshot.toPairState(
        currentUid: String,
        participants: List<GameParticipant>,
    ): GamePairState? {
        val uidA = getString("uidA") ?: return null
        val uidB = getString("uidB") ?: return null
        val opponentUid = when (currentUid) {
            uidA -> uidB
            uidB -> uidA
            else -> return null
        }
        val status = getString("status") ?: MATCH_WAITING
        val resolved = status == MATCH_RESOLVED
        val ownMove = if (!resolved) null else enumValueOrNull<RockPaperScissorsMove>(
            getString(if (currentUid == uidA) "moveA" else "moveB"),
        )
        val opponentMove = if (!resolved) null else enumValueOrNull<RockPaperScissorsMove>(
            getString(if (currentUid == uidA) "moveB" else "moveA"),
        )
        val outcome = if (ownMove != null && opponentMove != null) {
            RockPaperScissorsRules.outcome(ownMove, opponentMove)
        } else null
        return GamePairState(
            opponentUid = opponentUid,
            opponentName = participants.firstOrNull { it.firebaseUid == opponentUid }?.name ?: "Участник",
            matchNumber = getLong("matchNumber")?.toInt() ?: 0,
            status = status,
            submittedUids = stringList("submittedUids"),
            ownMove = ownMove,
            opponentMove = opponentMove,
            outcome = outcome,
            ownPoints = getLong(if (currentUid == uidA) "pointsA" else "pointsB")?.toInt(),
            opponentPoints = getLong(if (currentUid == uidA) "pointsB" else "pointsA")?.toInt(),
            resolvedAtEpochMs = getLong("resolvedAtEpochMs"),
            matchesPlayed = getLong("matchesPlayed")?.toInt() ?: 0,
            ownWins = getLong(if (currentUid == uidA) "winsA" else "winsB")?.toInt() ?: 0,
            ownDraws = getLong(if (currentUid == uidA) "drawsA" else "drawsB")?.toInt() ?: 0,
            ownLosses = getLong(if (currentUid == uidA) "lossesA" else "lossesB")?.toInt() ?: 0,
            ownRockCount = getLong(if (currentUid == uidA) "rockCountA" else "rockCountB")?.toInt() ?: 0,
            ownPaperCount = getLong(if (currentUid == uidA) "paperCountA" else "paperCountB")?.toInt() ?: 0,
            ownScissorsCount = getLong(if (currentUid == uidA) "scissorsCountA" else "scissorsCountB")?.toInt() ?: 0,
        )
    }

    private fun DocumentSnapshot.stringList(field: String): List<String> =
        (get(field) as? List<*>)?.mapNotNull { it as? String }.orEmpty()

    private fun matchId(roundId: String, pairId: String, number: Int): String =
        roundId + "_" + pairId + "_" + number

    private fun firestoreFor(firebaseUid: String): FirebaseFirestore {
        val app = FirebaseApp.getApps(appContext).firstOrNull()
            ?: throw IllegalStateException("Firebase не настроен. Добавьте app/google-services.json.")
        requireMatchingFirebaseSession(firebaseUid, FirebaseAuth.getInstance(app).currentUser?.uid)
        return FirebaseFirestore.getInstance(app)
    }

    private fun observeDocument(reference: DocumentReference): Flow<DocumentSnapshot> =
        kotlinx.coroutines.flow.callbackFlow {
            val registration = reference.addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else if (snapshot != null) trySend(snapshot)
            }
            awaitClose { registration.remove() }
        }

    private fun observeCollection(reference: com.google.firebase.firestore.CollectionReference): Flow<QuerySnapshot> =
        observeQuery(reference)

    private fun observeQuery(query: Query): Flow<QuerySnapshot> =
        kotlinx.coroutines.flow.callbackFlow {
            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else if (snapshot != null) trySend(snapshot)
            }
            awaitClose { registration.remove() }
        }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(task.exception ?: IllegalStateException("Firestore operation failed."))
            }
        }

    private fun isNetworkUnavailable(error: Throwable): Boolean =
        generateSequence(error) { it.cause }.any {
            it is FirebaseNetworkException ||
                (it is FirebaseFirestoreException && it.code == FirebaseFirestoreException.Code.UNAVAILABLE)
        }

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String?): T? =
        value?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

    private companion object {
        const val GAME_ROOMS = "gameRooms"
        const val MEMBERS = "members"
        const val ROUNDS = "rounds"
        const val PAIRS = "pairs"
        const val MATCHES = "matches"
        const val MOVES = "moves"
        const val MESSAGES = "messages"
        const val CHAT_STATE = "chatState"
        const val CHAT_SUMMARY = "summary"
        const val ROUND_RESULTS = "roundResults"
        const val ITEMS = "items"
        const val ROUND_ACTIVE = "ACTIVE"
        const val ROUND_FINISHED = "FINISHED"
        const val MATCH_WAITING = "WAITING"
        const val MATCH_RESOLVED = "RESOLVED"
        const val CHAT_LIMIT = 40
        const val HISTORY_LIMIT = 100
        const val MAX_MESSAGE_LENGTH = 500
        const val BATCH_LIMIT = 400
    }
}
