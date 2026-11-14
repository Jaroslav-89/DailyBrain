package com.jaroapps.dailybrain.feature.rockpaperscissors.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.model.CreateGameRoomRequest
import com.jaroapps.dailybrain.feature.rockpaperscissors.domain.repository.RockPaperScissorsRepository

class ManageGameRoomsUseCase(
    private val repository: RockPaperScissorsRepository,
) {
    suspend fun openRooms(uid: String) = repository.getOpenRooms(uid)
    suspend fun myRooms(uid: String) = repository.getMyRooms(uid)
    suspend fun createRoom(uid: String, ownerName: String, request: CreateGameRoomRequest) =
        repository.createRoom(uid, ownerName, request)
    suspend fun joinOpenRoom(uid: String, memberName: String, roomId: String) =
        repository.joinOpenRoom(uid, memberName, roomId)
    suspend fun joinRoomByCode(uid: String, memberName: String, code: String) =
        repository.joinRoomByCode(uid, memberName, code)
    suspend fun inviteUser(uid: String, inviterName: String, roomId: String, user: PublicUserProfile) =
        repository.inviteUser(uid, inviterName, roomId, user)
    suspend fun invitations(uid: String) = repository.getInvitations(uid)
    suspend fun feedInvitations(uid: String) = repository.getFeedInvitations(uid)
    suspend fun acceptInvitation(uid: String, invitationId: String) =
        repository.acceptInvitation(uid, invitationId)
    suspend fun declineInvitation(uid: String, invitationId: String) =
        repository.declineInvitation(uid, invitationId)
    suspend fun leaveRoom(uid: String, roomId: String) = repository.leaveRoom(uid, roomId)
    suspend fun deleteRoom(uid: String, roomId: String) = repository.deleteRoom(uid, roomId)
    suspend fun updateRoom(uid: String, roomId: String, request: CreateGameRoomRequest) =
        repository.updateRoom(uid, roomId, request)
}
