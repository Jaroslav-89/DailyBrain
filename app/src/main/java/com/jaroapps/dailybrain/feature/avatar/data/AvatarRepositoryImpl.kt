package com.jaroapps.dailybrain.feature.avatar.data

import com.jaroapps.dailybrain.core.database.ActiveUserProvider
import com.jaroapps.dailybrain.core.database.dao.UserProfileDao
import com.jaroapps.dailybrain.core.database.dao.UserInventoryDao
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
import com.jaroapps.dailybrain.feature.avatar.domain.model.OwnedAvatarCosmetic
import com.jaroapps.dailybrain.feature.avatar.domain.repository.AvatarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

class AvatarRepositoryImpl(
    private val userProfileDao: UserProfileDao,
    private val userInventoryDao: UserInventoryDao,
    private val activeUserProvider: ActiveUserProvider,
) : AvatarRepository {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeAvatar(): Flow<AvatarConfiguration> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            userProfileDao.observeAvatarJson(userId).map(AvatarJsonCodec::decode)
        }

    override fun observeOwnedCosmetics(): Flow<Set<OwnedAvatarCosmetic>> =
        activeUserProvider.observeUserId().flatMapLatest { userId ->
            userInventoryDao.observeForUser(userId).map { inventory ->
                inventory.mapNotNull { item ->
                    val type = runCatching { AvatarCosmeticType.valueOf(item.itemType) }.getOrNull()
                        ?: return@mapNotNull null
                    OwnedAvatarCosmetic(type = type, itemId = item.itemId)
                }.toSet()
            }
        }

    override suspend fun saveAvatar(configuration: AvatarConfiguration) {
        val userId = activeUserProvider.currentUserId()
        configuration.costumeId?.let { itemId ->
            check(userInventoryDao.ownsItem(userId, itemId, AvatarCosmeticType.COSTUME.name)) {
                "The selected costume is not owned by the active profile."
            }
        }
        configuration.backgroundSceneId?.let { itemId ->
            check(userInventoryDao.ownsItem(userId, itemId, AvatarCosmeticType.BACKGROUND.name)) {
                "The selected background is not owned by the active profile."
            }
        }
        userProfileDao.updateAvatarJson(
            userId,
            AvatarJsonCodec.encode(configuration),
            System.currentTimeMillis(),
        )
    }

    override suspend fun resetAvatar() {
        userProfileDao.updateAvatarJson(activeUserProvider.currentUserId(), null, System.currentTimeMillis())
    }
}