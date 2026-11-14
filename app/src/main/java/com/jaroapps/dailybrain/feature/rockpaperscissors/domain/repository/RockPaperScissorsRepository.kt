package com.jaroapps.dailybrain.feature.rockpaperscissors.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.CreateGameRoomRequest
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoom
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.GameRoomInvitation

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
    suspend fun getFeedInvitations(firebaseUid: String): List<GameRoomInvitation>
    suspend fun acceptInvitation(firebaseUid: String, invitationId: String): GameRoom
    suspend fun declineInvitation(firebaseUid: String, invitationId: String)
    suspend fun leaveRoom(firebaseUid: String, roomId: String)
    suspend fun deleteRoom(firebaseUid: String, roomId: String)
    suspend fun updateRoom(
        firebaseUid: String,
        roomId: String,
        request: CreateGameRoomRequest,
    ): GameRoom
}
