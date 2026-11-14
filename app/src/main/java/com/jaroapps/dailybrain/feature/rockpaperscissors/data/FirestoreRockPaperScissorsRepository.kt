package com.jaroapps.dailybrain.feature.rockpaperscissors.data

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import com.jaroapps.dailybrain.feature.profile.domain.model.InviteCodePolicy
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.requireMatchingFirebaseSession
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.CreateGameRoomRequest
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameInvitationStatus
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoom
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomInvitation
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomVisibility
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsRules
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.repository.RockPaperScissorsRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreRockPaperScissorsRepository(
    context: Context,
) : RockPaperScissorsRepository {
    private val appContext = context.applicationContext

    override suspend fun getOpenRooms(firebaseUid: String): List<GameRoom> {
        val firestore = firestoreFor(firebaseUid)
        return firestore.collection(GAME_ROOMS)
            .whereEqualTo("isPublic", true)
            .whereEqualTo("status", ROOM_OPEN)
            .limit(ROOM_LIST_LIMIT.toLong())
            .get()
            .awaitResult()
            .documents
            .mapNotNull { document: DocumentSnapshot -> document.toGameRoom() }
            .filter { it.hasSpace || firebaseUid in it.memberUids }
    }

    override suspend fun getMyRooms(firebaseUid: String): List<GameRoom> {
        val firestore = firestoreFor(firebaseUid)
        return firestore.collection(GAME_ROOMS)
            .whereArrayContains("memberUids", firebaseUid)
            .whereEqualTo("status", ROOM_OPEN)
            .limit(ROOM_LIST_LIMIT.toLong())
            .get()
            .awaitResult()
            .documents
            .mapNotNull { document: DocumentSnapshot -> document.toGameRoom() }
    }

    override suspend fun createRoom(
        firebaseUid: String,
        ownerName: String,
        request: CreateGameRoomRequest,
    ): GameRoom {
        val normalized = RockPaperScissorsRules.validateRoomRequest(request)
        val firestore = firestoreFor(firebaseUid)
        val roomRef = firestore.collection(GAME_ROOMS).document()
        val roundId = "1"
        val inviteCode = if (normalized.visibility == GameRoomVisibility.PRIVATE) {
            InviteCodePolicy.generate()
        } else null
        val inviteRef = inviteCode?.let { firestore.collection(GAME_ROOM_CODES).document(it) }
        val memberRef = roomRef.collection(MEMBERS).document(firebaseUid)
        val membershipRef = firestore.collection(GAME_ROOM_MEMBERSHIPS)
            .document(firebaseUid).collection(ROOMS).document(roomRef.id)
        val roundRef = roomRef.collection(ROUNDS).document(roundId)
        val now = System.currentTimeMillis()

        firestore.runTransaction { transaction: Transaction ->
            if (inviteRef != null && transaction.get(inviteRef).exists()) {
                throw IllegalStateException("Не удалось создать уникальный код комнаты. Попробуйте ещё раз.")
            }
            transaction.set(roomRef, mapOf(
                "name" to normalized.name,
                "ownerUid" to firebaseUid,
                "ownerName" to ownerName.take(80),
                "isPublic" to (normalized.visibility == GameRoomVisibility.PUBLIC),
                "inviteCode" to inviteCode,
                "maxParticipants" to normalized.maxParticipants,
                "targetScore" to normalized.targetScore,
                "memberUids" to listOf(firebaseUid),
                "status" to ROOM_OPEN,
                "currentRoundId" to roundId,
                "roundNumber" to 1,
                "createdAtEpochMs" to now,
                "updatedAtEpochMs" to now,
            ))
            transaction.set(memberRef, initialMember(firebaseUid, ownerName, now, joinedAsOwner = true))
            transaction.set(membershipRef, mapOf("roomId" to roomRef.id, "joinedAtEpochMs" to now))
            transaction.set(roundRef, mapOf(
                "roundId" to roundId,
                "roundNumber" to 1,
                "status" to ROUND_ACTIVE,
                "targetScore" to normalized.targetScore,
                "startedAtEpochMs" to now,
            ))
            if (inviteRef != null) transaction.set(inviteRef, mapOf("roomId" to roomRef.id))
            true
        }.awaitResult()

        return requireNotNull(roomRef.get().awaitResult().toGameRoom())
    }

    override suspend fun joinOpenRoom(
        firebaseUid: String,
        memberName: String,
        roomId: String,
    ): GameRoom = joinRoom(firebaseUid, memberName, roomId, requirePublic = true, joinedViaCode = null)

    override suspend fun joinRoomByCode(
        firebaseUid: String,
        memberName: String,
        inviteCode: String,
    ): GameRoom {
        val normalizedCode = InviteCodePolicy.normalize(inviteCode)
        require(InviteCodePolicy.isValid(normalizedCode)) { "Введите корректный код приглашения." }
        val firestore = firestoreFor(firebaseUid)
        val reservation = firestore.collection(GAME_ROOM_CODES).document(normalizedCode).get().awaitResult()
        val roomId = reservation.getString("roomId")
            ?: throw IllegalArgumentException("Комната с таким кодом не найдена.")
        return joinRoom(firebaseUid, memberName, roomId, requirePublic = false, joinedViaCode = normalizedCode)
    }

    private suspend fun joinRoom(
        firebaseUid: String,
        memberName: String,
        roomId: String,
        requirePublic: Boolean,
        joinedViaCode: String?,
    ): GameRoom {
        val firestore = firestoreFor(firebaseUid)
        val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
        val memberRef = roomRef.collection(MEMBERS).document(firebaseUid)
        val membershipRef = firestore.collection(GAME_ROOM_MEMBERSHIPS)
            .document(firebaseUid).collection(ROOMS).document(roomId)
        val now = System.currentTimeMillis()

        firestore.runTransaction { transaction: Transaction ->
            val room = transaction.get(roomRef)
            if (!room.exists() || room.getString("status") != ROOM_OPEN) {
                throw IllegalStateException("Комната больше недоступна.")
            }
            val isPublic = room.getBoolean("isPublic") == true
            if (requirePublic && !isPublic) throw IllegalStateException("Эта комната приватная.")
            if (!requirePublic && isPublic) {
                // Invite-code entry is also valid for a public room, but only if the code resolves.
            }
            val member = transaction.get(memberRef)
            if (member.exists()) return@runTransaction true
            val currentMembers = room.getStringList("memberUids")
            val capacity = room.getLong("maxParticipants")?.toInt() ?: RockPaperScissorsRules.DEFAULT_MAX_PARTICIPANTS
            if (currentMembers.size >= capacity) throw IllegalStateException("В комнате больше нет свободных мест.")
            transaction.update(roomRef, mapOf(
                "memberUids" to currentMembers + firebaseUid,
                "updatedAtEpochMs" to now,
            ))
            transaction.set(memberRef, initialMember(firebaseUid, memberName, now, joinedViaCode = joinedViaCode))
            transaction.set(membershipRef, mapOf("roomId" to roomId, "joinedAtEpochMs" to now))
            true
        }.awaitResult()

        return requireNotNull(roomRef.get().awaitResult().toGameRoom())
    }

    override suspend fun inviteUser(
        firebaseUid: String,
        inviterName: String,
        roomId: String,
        user: PublicUserProfile,
    ): GameRoomInvitation {
        require(user.firebaseUid != firebaseUid) { "Нельзя пригласить самого себя." }
        val firestore = firestoreFor(firebaseUid)
        val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
        val invitationId = UUID.randomUUID().toString()
        val inviteRef = firestore.collection(GAME_ROOM_INVITATIONS)
            .document(user.firebaseUid).collection(ITEMS).document(invitationId)
        val now = System.currentTimeMillis()

        firestore.runTransaction { transaction: Transaction ->
            val room = transaction.get(roomRef)
            if (!room.exists() || room.getString("status") != ROOM_OPEN) {
                throw IllegalStateException("Комната больше недоступна.")
            }
            val memberUids = room.getStringList("memberUids")
            if (firebaseUid !in memberUids) throw IllegalStateException("Вы больше не состоите в этой комнате.")
            if (user.firebaseUid in memberUids) throw IllegalStateException("Пользователь уже состоит в комнате.")
            if (memberUids.size >= (room.getLong("maxParticipants")?.toInt() ?: 0)) {
                throw IllegalStateException("Комната заполнена.")
            }
            transaction.set(inviteRef, mapOf(
                "invitationId" to invitationId,
                "roomId" to roomId,
                "roomName" to room.getString("name").orEmpty(),
                "inviterUid" to firebaseUid,
                "inviterName" to inviterName.take(80),
                "createdAtEpochMs" to now,
                "status" to GameInvitationStatus.PENDING.name,
            ))
            true
        }.awaitResult()

        val room = roomRef.get().awaitResult()
        return GameRoomInvitation(
            invitationId = invitationId,
            roomId = roomId,
            roomName = room.getString("name").orEmpty(),
            inviterUid = firebaseUid,
            inviterName = inviterName.take(80),
            createdAtEpochMs = now,
            status = GameInvitationStatus.PENDING,
        )
    }

    override suspend fun getInvitations(firebaseUid: String): List<GameRoomInvitation> =
        getFeedInvitations(firebaseUid).filter { it.status == GameInvitationStatus.PENDING }

    override suspend fun getFeedInvitations(firebaseUid: String): List<GameRoomInvitation> {
        val firestore = firestoreFor(firebaseUid)
        return firestore.collection(GAME_ROOM_INVITATIONS)
            .document(firebaseUid)
            .collection(ITEMS)
            .orderBy("createdAtEpochMs", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(INVITATION_LIST_LIMIT.toLong())
            .get()
            .awaitResult()
            .documents
            .mapNotNull { document ->
                val status = runCatching {
                    GameInvitationStatus.valueOf(document.getString("status") ?: "")
                }.getOrNull() ?: return@mapNotNull null
                if (status != GameInvitationStatus.PENDING && status != GameInvitationStatus.ACCEPTED) {
                    return@mapNotNull null
                }
                GameRoomInvitation(
                    invitationId = document.id,
                    roomId = document.getString("roomId") ?: return@mapNotNull null,
                    roomName = document.getString("roomName").orEmpty(),
                    inviterUid = document.getString("inviterUid").orEmpty(),
                    inviterName = document.getString("inviterName").orEmpty(),
                    createdAtEpochMs = document.getLong("createdAtEpochMs") ?: 0L,
                    status = status,
                )
            }
            .sortedByDescending { it.createdAtEpochMs }
    }

    override suspend fun acceptInvitation(firebaseUid: String, invitationId: String): GameRoom {
        val firestore = firestoreFor(firebaseUid)
        val inviteRef = firestore.collection(GAME_ROOM_INVITATIONS)
            .document(firebaseUid).collection(ITEMS).document(invitationId)
        val now = System.currentTimeMillis()
        var acceptedRoomId: String? = null

        firestore.runTransaction { transaction: Transaction ->
            val invite = transaction.get(inviteRef)
            if (!invite.exists() || invite.getString("status") != GameInvitationStatus.PENDING.name) {
                throw IllegalStateException("Приглашение больше недействительно.")
            }
            val roomId = invite.getString("roomId")
                ?: throw IllegalStateException("В приглашении отсутствует комната.")
            val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
            val memberRef = roomRef.collection(MEMBERS).document(firebaseUid)
            val membershipRef = firestore.collection(GAME_ROOM_MEMBERSHIPS)
                .document(firebaseUid).collection(ROOMS).document(roomId)
            val room = transaction.get(roomRef)
            val member = transaction.get(memberRef)
            if (!room.exists() || room.getString("status") != ROOM_OPEN) {
                throw IllegalStateException("Комната больше недоступна.")
            }
            val members = room.getStringList("memberUids")
            val capacity = room.getLong("maxParticipants")?.toInt() ?: 0
            if (!member.exists() && members.size >= capacity) {
                throw IllegalStateException("Комната уже заполнена.")
            }
            if (!member.exists()) {
                transaction.update(roomRef, mapOf(
                    "memberUids" to members + firebaseUid,
                    "updatedAtEpochMs" to now,
                ))
                transaction.set(memberRef, initialMember(firebaseUid, "Участник", now, joinedViaInvitationId = invitationId))
                transaction.set(membershipRef, mapOf("roomId" to roomId, "joinedAtEpochMs" to now))
            }
            transaction.update(inviteRef, mapOf(
                "status" to GameInvitationStatus.ACCEPTED.name,
                "respondedAtEpochMs" to now,
            ))
            acceptedRoomId = roomId
            true
        }.awaitResult()

        return requireNotNull(acceptedRoomId).let { id ->
            firestore.collection(GAME_ROOMS).document(id).get().awaitResult().toGameRoom()
        } ?: throw IllegalStateException("Не удалось открыть комнату.")
    }

    override suspend fun declineInvitation(firebaseUid: String, invitationId: String) {
        val firestore = firestoreFor(firebaseUid)
        val inviteRef = firestore.collection(GAME_ROOM_INVITATIONS)
            .document(firebaseUid).collection(ITEMS).document(invitationId)
        firestore.runTransaction { transaction ->
            val invite = transaction.get(inviteRef)
            if (!invite.exists()) return@runTransaction true
            if (invite.getString("status") == GameInvitationStatus.PENDING.name) {
                transaction.update(inviteRef, mapOf(
                    "status" to GameInvitationStatus.DECLINED.name,
                    "respondedAtEpochMs" to System.currentTimeMillis(),
                ))
            }
            true
        }.awaitResult()
    }

    override suspend fun leaveRoom(firebaseUid: String, roomId: String) {
        val firestore = firestoreFor(firebaseUid)
        val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
        val memberRef = roomRef.collection(MEMBERS).document(firebaseUid)
        val membershipRef = firestore.collection(GAME_ROOM_MEMBERSHIPS)
            .document(firebaseUid).collection(ROOMS).document(roomId)
        val now = System.currentTimeMillis()

        val shouldDeleteRoom = firestore.runTransaction { transaction ->
            val room = transaction.get(roomRef)
            val member = transaction.get(memberRef)
            if (!room.exists() || !member.exists()) return@runTransaction false

            val currentMembers = room.getStringList("memberUids")
            require(firebaseUid in currentMembers) { "Вы больше не состоите в этой комнате." }
            val remainingMembers = currentMembers.filterNot { it == firebaseUid }

            if (remainingMembers.isNotEmpty()) {
                transaction.delete(memberRef)
                transaction.delete(membershipRef)
                val previousOwner = room.getString("ownerUid")
                val nextOwner = if (previousOwner == firebaseUid || previousOwner !in remainingMembers) {
                    remainingMembers.first()
                } else {
                    previousOwner.orEmpty()
                }
                transaction.update(roomRef, mapOf(
                    "memberUids" to remainingMembers,
                    "ownerUid" to nextOwner,
                    "updatedAtEpochMs" to now,
                ))
                false
            } else {
                // Lock the room against new joins before removing any nested documents.
                if (room.getString("status") != ROOM_CLOSING || room.getString("ownerUid") != firebaseUid) {
                    val inviteCode = room.getString("inviteCode")
                    transaction.update(roomRef, mapOf(
                        "status" to ROOM_CLOSING,
                        "ownerUid" to firebaseUid,
                        "inviteCode" to null,
                        "updatedAtEpochMs" to now,
                    ))
                    if (!inviteCode.isNullOrBlank()) {
                        transaction.delete(firestore.collection(GAME_ROOM_CODES).document(inviteCode))
                    }
                }
                true
            }
        }.awaitResult()

        if (!shouldDeleteRoom) return

        // Firestore does not cascade deletes to subcollections. Remove them while the
        // final member document still exists so each cleanup write can be authorized.
        deleteRoomSubcollections(firestore, roomRef)

        firestore.runTransaction { transaction ->
            val room = transaction.get(roomRef)
            val member = transaction.get(memberRef)
            if (!room.exists()) {
                transaction.delete(membershipRef)
                return@runTransaction true
            }
            check(
                room.getString("status") == ROOM_CLOSING &&
                    room.getString("ownerUid") == firebaseUid &&
                    room.getStringList("memberUids") == listOf(firebaseUid) &&
                    member.exists()
            ) { "Комната изменилась во время удаления. Повторите выход." }

            val inviteCode = room.getString("inviteCode")
            transaction.delete(memberRef)
            transaction.delete(membershipRef)
            if (!inviteCode.isNullOrBlank()) {
                transaction.delete(firestore.collection(GAME_ROOM_CODES).document(inviteCode))
            }
            transaction.delete(roomRef)
            true
        }.awaitResult()
    }

    override suspend fun deleteRoom(firebaseUid: String, roomId: String) {
        val firestore = firestoreFor(firebaseUid)
        val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
        val ownerMemberRef = roomRef.collection(MEMBERS).document(firebaseUid)
        val ownerMembershipRef = firestore.collection(GAME_ROOM_MEMBERSHIPS)
            .document(firebaseUid).collection(ROOMS).document(roomId)
        val now = System.currentTimeMillis()

        val roomWasAvailable = firestore.runTransaction { transaction ->
            val room = transaction.get(roomRef)
            if (!room.exists()) return@runTransaction false
            check(room.getString("ownerUid") == firebaseUid) {
                "Удалить комнату может только её создатель."
            }
            val status = room.getString("status")
            if (status == ROOM_OPEN) {
                check(firebaseUid in room.getStringList("memberUids")) {
                    "Создатель больше не состоит в этой комнате."
                }
                val inviteCode = room.getString("inviteCode")
                transaction.update(roomRef, mapOf(
                    "status" to ROOM_CLOSING,
                    "ownerUid" to firebaseUid,
                    "inviteCode" to null,
                    "updatedAtEpochMs" to now,
                ))
                if (!inviteCode.isNullOrBlank()) {
                    transaction.delete(firestore.collection(GAME_ROOM_CODES).document(inviteCode))
                }
            } else {
                check(status == ROOM_CLOSING) { "Комната уже недоступна для удаления." }
            }
            true
        }.awaitResult()

        if (!roomWasAvailable) return

        // Keep the creator's member document until every nested collection is cleared.
        // This also makes a partially completed deletion safely retryable.
        val room = roomRef.get().awaitResult()
        check(firebaseUid in room.getStringList("memberUids") && ownerMemberRef.get().awaitResult().exists()) {
            "Не удалось подтвердить права создателя комнаты."
        }
        deleteRoomSubcollections(firestore, roomRef)

        val currentRoom = roomRef.get().awaitResult()
        val otherMemberUids = currentRoom.getStringList("memberUids").filterNot { it == firebaseUid }
        if (otherMemberUids.isNotEmpty()) {
            val batch = firestore.batch()
            otherMemberUids.forEach { memberUid ->
                batch.delete(roomRef.collection(MEMBERS).document(memberUid))
                batch.delete(
                    firestore.collection(GAME_ROOM_MEMBERSHIPS)
                        .document(memberUid).collection(ROOMS).document(roomId)
                )
            }
            batch.update(roomRef, mapOf(
                "memberUids" to listOf(firebaseUid),
                "updatedAtEpochMs" to System.currentTimeMillis(),
            ))
            batch.commit().awaitResult()
        }

        firestore.runTransaction { transaction ->
            val latestRoom = transaction.get(roomRef)
            val ownerMember = transaction.get(ownerMemberRef)
            if (!latestRoom.exists()) {
                transaction.delete(ownerMembershipRef)
                return@runTransaction true
            }
            check(
                latestRoom.getString("status") == ROOM_CLOSING &&
                    latestRoom.getString("ownerUid") == firebaseUid &&
                    latestRoom.getStringList("memberUids") == listOf(firebaseUid) &&
                    ownerMember.exists()
            ) { "Комната изменилась во время удаления. Повторите попытку." }

            transaction.delete(ownerMemberRef)
            transaction.delete(ownerMembershipRef)
            transaction.delete(roomRef)
            true
        }.awaitResult()
    }

    private suspend fun deleteRoomSubcollections(
        firestore: FirebaseFirestore,
        roomRef: DocumentReference,
    ) {
        val rounds = roomRef.collection(ROUNDS).get().awaitResult()
        for (round in rounds.documents) {
            val roundRef = round.reference
            val matches = roundRef.collection(MATCHES).get().awaitResult()
            val moveRefs = matches.documents.flatMap { match ->
                listOfNotNull(match.getString("uidA"), match.getString("uidB"))
                    .distinct()
                    .map { uid -> match.reference.collection(MOVES).document(uid) }
            }
            deleteDocumentReferencesInChunks(firestore, moveRefs)
            deleteQueryInChunks(firestore, roundRef.collection(PAIRS))
            deleteQueryInChunks(firestore, roundRef.collection(MATCHES))
            roundRef.delete().awaitResult()
        }

        deleteQueryInChunks(firestore, roomRef.collection(MESSAGES))
        deleteQueryInChunks(firestore, roomRef.collection(CHAT_STATE))
        deleteQueryInChunks(firestore, roomRef.collection(ROUND_RESULTS))
    }

    private suspend fun deleteDocumentReferencesInChunks(
        firestore: FirebaseFirestore,
        references: List<DocumentReference>,
    ) {
        references.chunked(DELETE_BATCH_LIMIT).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach(batch::delete)
            batch.commit().awaitResult()
        }
    }

    private suspend fun deleteQueryInChunks(firestore: FirebaseFirestore, query: Query) {
        var snapshot = query.limit(DELETE_BATCH_LIMIT.toLong()).get().awaitResult()
        while (snapshot.documents.isNotEmpty()) {
            val batch = firestore.batch()
            snapshot.documents.forEach { batch.delete(it.reference) }
            batch.commit().awaitResult()
            if (snapshot.documents.size < DELETE_BATCH_LIMIT) break
            snapshot = query.startAfter(snapshot.documents.last()).limit(DELETE_BATCH_LIMIT.toLong()).get().awaitResult()
        }
    }

    override suspend fun updateRoom(
        firebaseUid: String,
        roomId: String,
        request: CreateGameRoomRequest,
    ): GameRoom {
        val normalized = RockPaperScissorsRules.validateRoomRequest(request)
        val firestore = firestoreFor(firebaseUid)
        val roomRef = firestore.collection(GAME_ROOMS).document(roomId)
        val now = System.currentTimeMillis()
        var updatedRoom: GameRoom? = null

        firestore.runTransaction { transaction ->
            val room = transaction.get(roomRef)
            if (!room.exists()) throw IllegalStateException("Комната не найдена.")
            if (room.getString("ownerUid") != firebaseUid) {
                throw IllegalStateException("Изменять настройки может только создатель комнаты.")
            }
            val members = room.getStringList("memberUids")
            if (normalized.maxParticipants < members.size) {
                throw IllegalArgumentException("Нельзя установить лимит ниже текущего количества участников.")
            }
            val previousCode = room.getString("inviteCode")
            val nextCode = when (normalized.visibility) {
                GameRoomVisibility.PUBLIC -> null
                GameRoomVisibility.PRIVATE -> previousCode ?: InviteCodePolicy.generate()
            }
            val nextCodeRef = nextCode?.let { firestore.collection(GAME_ROOM_CODES).document(it) }
            val previousCodeRef = previousCode?.takeIf { it != nextCode }
                ?.let { firestore.collection(GAME_ROOM_CODES).document(it) }
            val nextCodeSnapshot = nextCodeRef?.let(transaction::get)
            if (nextCodeSnapshot?.exists() == true && nextCodeSnapshot.getString("roomId") != roomId) {
                throw IllegalStateException("Не удалось создать уникальный код комнаты.")
            }
            transaction.update(roomRef, mapOf(
                "name" to normalized.name,
                "isPublic" to (normalized.visibility == GameRoomVisibility.PUBLIC),
                "inviteCode" to nextCode,
                "maxParticipants" to normalized.maxParticipants,
                "targetScore" to normalized.targetScore,
                "updatedAtEpochMs" to now,
            ))
            if (previousCodeRef != null) transaction.delete(previousCodeRef)
            if (nextCodeRef != null && nextCodeSnapshot?.exists() != true) {
                transaction.set(nextCodeRef, mapOf("roomId" to roomId))
            }
            updatedRoom = room.toGameRoom()?.copy(
                name = normalized.name,
                visibility = normalized.visibility,
                inviteCode = nextCode,
                maxParticipants = normalized.maxParticipants,
                targetScore = normalized.targetScore,
            )
            true
        }.awaitResult()

        return requireNotNull(updatedRoom)
    }

    private fun initialMember(
        uid: String,
        name: String,
        now: Long,
        joinedAsOwner: Boolean = false,
        joinedViaCode: String? = null,
        joinedViaInvitationId: String? = null,
    ): Map<String, Any> = buildMap {
        put("uid", uid)
        put("name", name.take(80))
        put("score", 0)
        put("wins", 0)
        put("draws", 0)
        put("losses", 0)
        put("rockCount", 0)
        put("paperCount", 0)
        put("scissorsCount", 0)
        put("matchesPlayed", 0)
        put("roundWins", 0)
        put("roundDraws", 0)
        put("roundLosses", 0)
        put("roundRockCount", 0)
        put("roundPaperCount", 0)
        put("roundScissorsCount", 0)
        put("roundMatchesPlayed", 0)
        put("lastReadMessageAtEpochMs", 0)
        put("lastSeenFinishedRoundId", "")
        put("joinedAtEpochMs", now)
        put("joinedAsOwner", joinedAsOwner)
        joinedViaCode?.let { put("joinedViaCode", it) }
        joinedViaInvitationId?.let { put("joinedViaInvitationId", it) }
    }

    private fun DocumentSnapshot.toGameRoom(): GameRoom? {
        if (!exists()) return null
        val roomId = id
        val name = getString("name") ?: return null
        val ownerUid = getString("ownerUid") ?: return null
        val visibility = if (getBoolean("isPublic") == true) {
            GameRoomVisibility.PUBLIC
        } else GameRoomVisibility.PRIVATE
        return GameRoom(
            roomId = roomId,
            name = name,
            ownerUid = ownerUid,
            ownerName = getString("ownerName").orEmpty(),
            visibility = visibility,
            inviteCode = getString("inviteCode"),
            maxParticipants = getLong("maxParticipants")?.toInt()
                ?: RockPaperScissorsRules.DEFAULT_MAX_PARTICIPANTS,
            targetScore = getLong("targetScore")?.toInt() ?: RockPaperScissorsRules.DEFAULT_TARGET_SCORE,
            memberUids = getStringList("memberUids"),
            currentRoundId = getString("currentRoundId") ?: "1",
            createdAtEpochMs = getLong("createdAtEpochMs") ?: 0L,
        )
    }

    private fun DocumentSnapshot.getStringList(field: String): List<String> =
        (get(field) as? List<*>)?.mapNotNull { it as? String }.orEmpty()

    private fun firestoreFor(firebaseUid: String): FirebaseFirestore {
        val app = FirebaseApp.getApps(appContext).firstOrNull()
            ?: throw IllegalStateException("Firebase не настроен. Добавьте app/google-services.json.")
        requireMatchingFirebaseSession(firebaseUid, FirebaseAuth.getInstance(app).currentUser?.uid)
        return FirebaseFirestore.getInstance(app)
    }

    private suspend fun <T> Task<T>.awaitResult(): T =
        suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                if (task.isSuccessful) continuation.resume(task.result)
                else continuation.resumeWithException(task.exception ?: IllegalStateException("Firestore operation failed."))
            }
        }

    private companion object {
        const val DELETE_BATCH_LIMIT = 400
        const val GAME_ROOMS = "gameRooms"
        const val GAME_ROOM_CODES = "gameRoomCodes"
        const val GAME_ROOM_MEMBERSHIPS = "gameRoomMemberships"
        const val GAME_ROOM_INVITATIONS = "gameRoomInvitations"
        const val MEMBERS = "members"
        const val PAIRS = "pairs"
        const val MATCHES = "matches"
        const val MOVES = "moves"
        const val MESSAGES = "messages"
        const val CHAT_STATE = "chatState"
        const val ROUND_RESULTS = "roundResults"
        const val ROOMS = "rooms"
        const val ROUNDS = "rounds"
        const val ITEMS = "items"
        const val ROOM_OPEN = "OPEN"
        const val ROOM_CLOSING = "CLOSING"
        const val ROUND_ACTIVE = "ACTIVE"
        const val ROOM_LIST_LIMIT = 30
        const val INVITATION_LIST_LIMIT = 30
    }
}
