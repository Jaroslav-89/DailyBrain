package com.jaroapps.dailybrain.feature.avatar.domain.repository

import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import com.jaroapps.dailybrain.feature.avatar.domain.model.OwnedAvatarCosmetic
import kotlinx.coroutines.flow.Flow

/** Local-only persistence contract for the current user's avatar. */
interface AvatarRepository {
    fun observeAvatar(): Flow<AvatarConfiguration>
    fun observeOwnedCosmetics(): Flow<Set<OwnedAvatarCosmetic>>
    suspend fun saveAvatar(configuration: AvatarConfiguration)
    suspend fun resetAvatar()
}
