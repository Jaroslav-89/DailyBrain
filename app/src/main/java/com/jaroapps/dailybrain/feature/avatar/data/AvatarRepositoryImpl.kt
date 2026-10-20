package com.jaroapps.dailybrain.feature.avatar.data

import com.jaroapps.dailybrain.core.database.dao.UserProfileDao
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import com.jaroapps.dailybrain.feature.avatar.domain.repository.AvatarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AvatarRepositoryImpl(
    private val userProfileDao: UserProfileDao,
) : AvatarRepository {
    override fun observeAvatar(): Flow<AvatarConfiguration> =
        userProfileDao.observeAvatarJson().map(AvatarJsonCodec::decode)

    override suspend fun saveAvatar(configuration: AvatarConfiguration) {
        userProfileDao.updateAvatarJson(AvatarJsonCodec.encode(configuration))
    }

    override suspend fun resetAvatar() {
        userProfileDao.updateAvatarJson(null)
    }
}
