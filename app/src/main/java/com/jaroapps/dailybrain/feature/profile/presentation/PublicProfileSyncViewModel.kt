package com.jaroapps.dailybrain.feature.profile.presentation

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.avatar.data.AvatarJsonCodec
import com.jaroapps.dailybrain.feature.avatar.domain.usecase.ObserveAvatarUseCase
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveProfileStatisticsUseCase
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps the signed-in user's explicitly public profile projection in sync while the app is open.
 * Only DailyBrain-entered profile details, the custom avatar and aggregate statistics are published.
 */
class PublicProfileSyncViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    observeProfileStatistics: ObserveProfileStatisticsUseCase,
    observeAvatar: ObserveAvatarUseCase,
    private val userDirectoryRepository: UserDirectoryRepository,
) : ViewModel() {

    private var relationshipsSyncedForUid: String? = null

    init {
        viewModelScope.launch {
            combine(
                observeUserProfile(),
                observeProfileStatistics(),
                observeAvatar(),
            ) { profile, statistics, avatar ->
                Triple(profile, statistics, avatar)
            }.collect { (profile, statistics, avatar) ->
                val firebaseUid = profile.firebaseUid ?: return@collect
                try {
                    userDirectoryRepository.updatePublicProfile(
                        firebaseUid = firebaseUid,
                        displayName = profile.displayName,
                        country = profile.country,
                        city = profile.city,
                        gender = profile.gender?.name,
                        memberSinceEpochMs = profile.createdAtEpochMs,
                        avatarConfigurationJson = AvatarJsonCodec.encode(avatar),
                        statistics = statistics,
                    )
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

    private companion object {
        const val TAG = "PublicProfileSync"
    }
}
