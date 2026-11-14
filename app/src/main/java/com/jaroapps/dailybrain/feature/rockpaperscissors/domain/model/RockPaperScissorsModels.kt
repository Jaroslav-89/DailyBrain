package com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model

enum class RockPaperScissorsMove {
    ROCK,
    PAPER,
    SCISSORS,
}

enum class RockPaperScissorsOutcome {
    WIN,
    DRAW,
    LOSS,
}

enum class GameRoomVisibility {
    PUBLIC,
    PRIVATE,
}

data class GameRoom(
    val roomId: String,
    val name: String,
    val ownerUid: String,
    val ownerName: String,
    val visibility: GameRoomVisibility,
    val inviteCode: String?,
    val maxParticipants: Int,
    val targetScore: Int,
    val memberUids: List<String>,
    val currentRoundId: String,
    val createdAtEpochMs: Long,
) {
    val memberCount: Int get() = memberUids.size
    val hasSpace: Boolean get() = memberCount < maxParticipants
    val isPublic: Boolean get() = visibility == GameRoomVisibility.PUBLIC
}

data class GameRoomInvitation(
    val invitationId: String,
    val roomId: String,
    val roomName: String,
    val inviterUid: String,
    val inviterName: String,
    val createdAtEpochMs: Long,
    val status: GameInvitationStatus,
)

enum class GameInvitationStatus {
    PENDING,
    ACCEPTED,
    DECLINED,
    REVOKED,
}

data class CreateGameRoomRequest(
    val name: String,
    val visibility: GameRoomVisibility,
    val maxParticipants: Int,
    val targetScore: Int,
)

object RockPaperScissorsRules {
    const val MIN_PARTICIPANTS = 2
    const val MAX_PARTICIPANTS = 8
    const val DEFAULT_MAX_PARTICIPANTS = 4
    const val DEFAULT_TARGET_SCORE = 50
    const val MIN_TARGET_SCORE = 5
    const val MAX_TARGET_SCORE = 500
    const val MAX_ROOM_NAME_LENGTH = 40

    fun outcome(
        ownMove: RockPaperScissorsMove,
        opponentMove: RockPaperScissorsMove,
    ): RockPaperScissorsOutcome = when {
        ownMove == opponentMove -> RockPaperScissorsOutcome.DRAW
        ownMove == RockPaperScissorsMove.ROCK && opponentMove == RockPaperScissorsMove.SCISSORS -> RockPaperScissorsOutcome.WIN
        ownMove == RockPaperScissorsMove.SCISSORS && opponentMove == RockPaperScissorsMove.PAPER -> RockPaperScissorsOutcome.WIN
        ownMove == RockPaperScissorsMove.PAPER && opponentMove == RockPaperScissorsMove.ROCK -> RockPaperScissorsOutcome.WIN
        else -> RockPaperScissorsOutcome.LOSS
    }

    fun points(outcome: RockPaperScissorsOutcome): Int = when (outcome) {
        RockPaperScissorsOutcome.WIN -> 3
        RockPaperScissorsOutcome.DRAW -> 2
        RockPaperScissorsOutcome.LOSS -> 1
    }

    fun validateRoomRequest(request: CreateGameRoomRequest): CreateGameRoomRequest {
        val normalizedName = request.name.trim()
        require(normalizedName.isNotEmpty()) { "Название комнаты не может быть пустым." }
        require(normalizedName.length <= MAX_ROOM_NAME_LENGTH) {
            "Название комнаты не должно превышать $MAX_ROOM_NAME_LENGTH символов."
        }
        require(request.maxParticipants in MIN_PARTICIPANTS..MAX_PARTICIPANTS) {
            "Количество участников должно быть от $MIN_PARTICIPANTS до $MAX_PARTICIPANTS."
        }
        require(request.targetScore in MIN_TARGET_SCORE..MAX_TARGET_SCORE) {
            "Целевой счёт должен быть от $MIN_TARGET_SCORE до $MAX_TARGET_SCORE."
        }
        return request.copy(name = normalizedName)
    }

    fun pairId(firstUid: String, secondUid: String): String {
        require(firstUid.isNotBlank() && secondUid.isNotBlank())
        require(firstUid != secondUid)
        return listOf(firstUid, secondUid).sorted().joinToString("__")
    }
}
