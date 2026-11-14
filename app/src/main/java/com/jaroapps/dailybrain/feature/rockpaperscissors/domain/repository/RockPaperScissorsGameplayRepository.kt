package com.jaroapps.dailybrain.feature.rockpaperscissors.domain.repository

import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomSnapshot
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameChatMessage
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameMatchHistoryItem
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsMove
import kotlinx.coroutines.flow.Flow

interface RockPaperScissorsGameplayRepository {
    fun observeRoom(firebaseUid: String, roomId: String): Flow<GameRoomSnapshot>
    suspend fun submitMove(
        firebaseUid: String,
        roomId: String,
        roundId: String,
        opponentUid: String,
        move: RockPaperScissorsMove,
    )
    suspend fun syncPendingMoves(firebaseUid: String, roomId: String? = null)
    suspend fun updateMemberDisplayName(firebaseUid: String, roomId: String, displayName: String)
    suspend fun getRoundHistory(firebaseUid: String, roomId: String): List<GameMatchHistoryItem>
    fun observeChat(firebaseUid: String, roomId: String): Flow<List<GameChatMessage>>
    suspend fun sendChatMessage(firebaseUid: String, roomId: String, senderName: String, text: String)
    suspend fun markChatRead(firebaseUid: String, roomId: String, readAtEpochMs: Long)
    suspend fun acknowledgeRoundAnnouncement(firebaseUid: String, roomId: String, roundId: String)
}
