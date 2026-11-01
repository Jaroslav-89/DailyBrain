package com.jaroapps.dailybrain.feature.profile.domain.repository

import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectible
import kotlinx.coroutines.flow.Flow

interface ProfileCollectiblesRepository {
    /** The current local account's received gifts and active badges, newest first. */
    fun observeOwn(): Flow<List<ProfileCollectible>>

    /** Reads local items for the active account or the public projection for another user. */
    suspend fun getForOwner(ownerFirebaseUid: String): List<ProfileCollectible>

    suspend fun getItem(
        ownerFirebaseUid: String,
        itemId: String,
        kind: com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectibleKind,
    ): ProfileCollectible?

    /** Publishes the active user's public-safe item metadata to their profile subcollection. */
    suspend fun syncOwnPublic(ownerFirebaseUid: String, items: List<ProfileCollectible>)
}
