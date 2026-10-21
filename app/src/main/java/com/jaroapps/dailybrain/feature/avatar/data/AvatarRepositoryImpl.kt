package com.jaroapps.dailybrain.feature.avatar.data

import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.dao.UserProfileDao
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import com.jaroapps.dailybrain.feature.avatar.domain.repository.AvatarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class AvatarRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val activeUserProvider: ActiveUserProvider,
) : AvatarRepository {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeAvatar(): Flow<AvatarConfiguration> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            userProfileDao.observeAvatarJson(userId).map(AvatarJsonCodec::decode)
        }

    override suspend fun saveAvatar(configuration: AvatarConfiguration) {
        userProfileDao.updateAvatarJson(
            activeUserProvider.currentUserId(),
            AvatarJsonCodec.encode(configuration),
            System.currentTimeMillis(),
        )
    }

    override suspend fun resetAvatar() {
        userProfileDao.updateAvatarJson(activeUserProvider.currentUserId(), null, System.currentTimeMillis())
    }
}