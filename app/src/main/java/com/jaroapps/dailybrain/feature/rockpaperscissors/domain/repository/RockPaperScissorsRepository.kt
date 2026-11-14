package com.jaroapps.dailybrain.feature.rockpaperscissors.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.CreateGameRoomRequest
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoom
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomInvitation
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomSnapshot
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameChatMessage
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameMatchHistoryItem
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.RockPaperScissorsMove
import kotlinx.coroutines.flow.Flow

interface RockPaperScissorsRepository {
    suspend fun getOpenRooms(firebaseUid: String): List<GameRoom>
    suspend fun getMyRooms(firebaseUid: String): List<GameRoom>
    suspend fun createRoom(
        firebaseUid: String,
        ownerName: String,
        request: CreateGameRoomRequest,
    ): GameRoom

    suspend fun joinOpenRoom(firebaseUid: String, memberName: String, roomId: String): GameRoom
    suspend fun joinRoomByCode(firebaseUid: String, memberName: String, inviteCode: String): GameRoom
    suspend fun inviteUser(
        firebaseUid: String,
        inviterName: String,
        roomId: String,
        user: PublicUserProfile,
    ): GameRoomInvitation

    suspend fun getInvitations(firebaseUid: String): List<GameRoomInvitation>
    suspend fun acceptInvitation(firebaseUid: String, invitationId: String): GameRoom
    suspend fun declineInvitation(firebaseUid: String, invitationId: String)
    suspend fun leaveRoom(firebaseUid: String, roomId: String)
    fun observeRoom(firebaseUid: String, roomId: String): Flow<GameRoomSnapshot>
    suspend fun submitMove(
        firebaseUid: String,
        roomId: String,
        roundId: String,
        opponentUid: String,
        move: RockPaperScissorsMove,
    )
    suspend fun syncPendingMoves(firebaseUid: String, roomId: String? = null)
    suspend fun getRoundHistory(firebaseUid: String, roomId: String): List<GameMatchHistoryItem>
    fun observeChat(firebaseUid: String, roomId: String): Flow<List<GameChatMessage>>
    suspend fun sendChatMessage(firebaseUid: String, roomId: String, text: String)
    suspend fun markChatRead(firebaseUid: String, roomId: String)
    suspend fun acknowledgeRoundAnnouncement(firebaseUid: String, roomId: String, roundId: String)
    suspend fun updateRoom(
        firebaseUid: String,
        roomId: String,
        request: CreateGameRoomRequest,
    ): GameRoom
}
