package com.jaroapps.dailybrain.feature.profile.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.avatar.data.AvatarJsonCodec
import com.jaroapps.dailybrain.feature.avatar.domain.usecase.ObserveAvatarUseCase
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileCollectiblesRepository
import com.jaroapps.dailybrain.feature.shop.domain.repository.GiftDeliveryRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveProfileStatisticsUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveSelectedProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps the signed-in user's explicitly public profile projection in sync while the app is open.
 * Only DailyBrain-entered profile details, the custom avatar and aggregate statistics are published.
 */
class PublicProfileSyncViewModel(
    observeSelectedProfile: ObserveSelectedProfileUseCase,
    observeProfileStatistics: ObserveProfileStatisticsUseCase,
    observeAvatar: ObserveAvatarUseCase,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val profileCollectiblesRepository: ProfileCollectiblesRepository,
    private val giftDeliveryRepository: GiftDeliveryRepository,
) : ViewModel() {

    private var relationshipsSyncedForUid: String? = null
    private var giftsSyncedForUid: String? = null

    init {
        viewModelScope.launch {
            observeSelectedProfile().collectLatest { profile ->
                if (profile == null) return@collectLatest
                val firebaseUid = profile.firebaseUid ?: return@collectLatest

                // Start the dependent active-profile streams only after the selected profile
                // is known. This prevents combine() from pairing profile B with profile A's
                // last avatar/statistics/inventory emission during a profile switch.
                combine(
                    observeProfileStatistics(),
                    observeAvatar(),
                    profileCollectiblesRepository.observeOwn(),
                    firebaseAuthenticationRepository.observeCurrentUserId(),
                ) { statistics, avatar, collectibles, currentFirebaseUid ->
                    ProfileSyncSnapshot(statistics, avatar, collectibles, currentFirebaseUid)
                }.collect { snapshot ->
                    if (snapshot.currentFirebaseUid != firebaseUid) return@collect
                    try {
                        if (giftsSyncedForUid != firebaseUid) {
                            runCatching { giftDeliveryRepository.syncIncoming() }
                                .onSuccess { giftsSyncedForUid = firebaseUid }
                        }
                        userDirectoryRepository.updatePublicProfile(
                            firebaseUid = firebaseUid,
                            displayName = profile.effectiveDisplayName,
                            country = profile.country,
                            city = profile.city,
                            gender = profile.gender?.name,
                            memberSinceEpochMs = profile.createdAtEpochMs,
                            avatarConfigurationJson = AvatarJsonCodec.encode(snapshot.avatar),
                            statistics = snapshot.statistics,
                        )
                        profileCollectiblesRepository.syncOwnPublic(firebaseUid, snapshot.collectibles)
                        if (relationshipsSyncedForUid != firebaseUid) {
                            userDirectoryRepository.getFollowing(firebaseUid)
                            userDirectoryRepository.getFollowers(firebaseUid)
                            relationshipsSyncedForUid = firebaseUid
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w(TAG, "Could not sync public profile snapshot", error)
                    }
                }
            }
        }
    }

    private data class ProfileSyncSnapshot(
        val statistics: com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileStatistics,
        val avatar: com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration,
        val collectibles: List<com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectible>,
        val currentFirebaseUid: String?,
    )

    private companion object {
        const val TAG = "PublicProfileSync"
    }
}
