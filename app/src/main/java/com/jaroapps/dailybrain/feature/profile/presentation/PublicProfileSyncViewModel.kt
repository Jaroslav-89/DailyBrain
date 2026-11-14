package com.jaroapps.dailybrain.feature.profile.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.common.domain.usecase.InitializeRewardStateUseCase
import com.jaroapps.dailybrain.feature.avatar.data.AvatarJsonCodec
import com.jaroapps.dailybrain.feature.avatar.domain.usecase.ObserveAvatarUseCase
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectibleKind
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.FirebaseAuthenticationRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.ProfileCollectiblesRepository
import com.jaroapps.dailybrain.feature.shop.domain.repository.GiftDeliveryRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveProfileStatisticsUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveSelectedProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Keeps the signed-in user's explicitly public profile projection in sync while the app is open.
 * Only DailyBrain-entered profile details, the custom avatar and aggregate statistics are published.
 */
class PublicProfileSyncViewModel(
    observeSelectedProfile: ObserveSelectedProfileUseCase,
    observeProfileStatistics: ObserveProfileStatisticsUseCase,
    observeAvatar: ObserveAvatarUseCase,
    private val initializeRewardState: InitializeRewardStateUseCase,
    private val firebaseAuthenticationRepository: FirebaseAuthenticationRepository,
    private val userDirectoryRepository: UserDirectoryRepository,
    private val userProfileRepository: UserProfileRepository,
    private val profileCollectiblesRepository: ProfileCollectiblesRepository,
    private val giftDeliveryRepository: GiftDeliveryRepository,
) : ViewModel() {

    private var relationshipsSyncedForUid: String? = null
    private var giftsSyncedForUid: String? = null
    private val syncRequestVersion = MutableStateFlow(0L)

    /** Re-run the foreground sync pipeline after the app returns to the foreground. */
    fun syncNow() {
        syncRequestVersion.update { it + 1L }
    }

    init {
        viewModelScope.launch {
            // A foreground request restarts this pipeline so initialization and cloud sync are
            // retried in order, rather than trying to resume from a potentially stale snapshot.
            combine(observeSelectedProfile(), syncRequestVersion) { profile, _ -> profile }
                .collectLatest { profile ->
                    if (profile == null) return@collectLatest
                    val firebaseUid = profile.firebaseUid ?: return@collectLatest

                    // Reward recalculation can create legacy badges on first launch. Complete it
                    // before observing/publishing collectibles. NonCancellable lets the Room
                    // transaction finish if its profile update emits a new selected-profile value;
                    // collectLatest will then restart with that fresh profile snapshot.
                    try {
                        withContext(NonCancellable) { initializeRewardState() }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w(TAG, "Could not initialize rewards before public profile sync", error)
                        return@collectLatest
                    }

                    // Cloud privacy controls are account-level and remain authoritative across devices.
                    try {
                        val remotePrivacy = userDirectoryRepository.getPrivacySettings(firebaseUid)
                        if (remotePrivacy == null) {
                            // First rollout or a newly linked Google account: keep the local choices,
                            // then seed the account-level document so future devices inherit them.
                            userDirectoryRepository.updatePrivacySettings(
                                firebaseUid = firebaseUid,
                                profileInfoVisible = profile.profileInfoVisible,
                                feedUpdatesEnabled = profile.feedUpdatesEnabled,
                            )
                        } else if (
                            profile.profileInfoVisible != remotePrivacy.profileInfoVisible ||
                            profile.feedUpdatesEnabled != remotePrivacy.feedUpdatesEnabled
                        ) {
                            userProfileRepository.savePrivacySettings(
                                localAccountId = profile.localAccountId,
                                profileInfoVisible = remotePrivacy.profileInfoVisible,
                                feedUpdatesEnabled = remotePrivacy.feedUpdatesEnabled,
                            )
                            return@collectLatest
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        Log.w(TAG, "Could not refresh cloud privacy settings", error)
                    }

                    // Start the dependent active-profile streams only after the selected profile
                    // and its reward state are known. This prevents stale snapshots during switches.
                    combine(
                        observeProfileStatistics(),
                        observeAvatar(),
                        profileCollectiblesRepository.observeOwn(),
                        firebaseAuthenticationRepository.observeCurrentUserId(),
                    ) { statistics, avatar, collectibles, currentFirebaseUid ->
                        ProfileSyncSnapshot(statistics, avatar, collectibles, currentFirebaseUid)
                    }.collect { snapshot ->
                        if (snapshot.currentFirebaseUid != firebaseUid) return@collect

                        if (giftsSyncedForUid != firebaseUid) {
                            runSyncStep("Could not sync incoming gifts") {
                                giftDeliveryRepository.syncIncoming()
                                giftsSyncedForUid = firebaseUid
                            }
                        }

                        // Keep these operations independent: a failed profile-card update must
                        // not prevent the collectible subcollection from being published.
                        runSyncStep("Could not update public profile") {
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
                        }

                        runSyncStep("Could not sync public collectibles") {
                            profileCollectiblesRepository.syncOwnPublic(firebaseUid, snapshot.collectibles)
                            val badgeCount = snapshot.collectibles.count {
                                it.kind == ProfileCollectibleKind.BADGE
                            }
                            Log.d(TAG, "Published $badgeCount active badges for profile $firebaseUid")
                        }

                        if (relationshipsSyncedForUid != firebaseUid) {
                            runSyncStep("Could not sync public profile relationships") {
                                userDirectoryRepository.getFollowing(firebaseUid)
                                userDirectoryRepository.getFollowers(firebaseUid)
                                relationshipsSyncedForUid = firebaseUid
                            }
                        }
                    }
                }
        }
    }

    private suspend fun runSyncStep(
        message: String,
        action: suspend () -> Unit,
    ) {
        try {
            action()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w(TAG, message, error)
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
