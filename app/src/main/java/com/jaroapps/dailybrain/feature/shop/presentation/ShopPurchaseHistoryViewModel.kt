package com.jaroapps.dailybrain.feature.shop.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopPurchaseHistoryEntry
import com.jaroapps.dailybrain.feature.shop.domain.repository.ShopRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ShopPurchaseHistoryUiState(
    val entries: List<ShopPurchaseHistoryEntry> = emptyList(),
    val isLoading: Boolean = true,
)

class ShopPurchaseHistoryViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    shopRepository: ShopRepository,
    private val userDirectoryRepository: UserDirectoryRepository,
) : ViewModel() {

    val uiState: StateFlow<ShopPurchaseHistoryUiState> = combine(
        observeUserProfile(),
        shopRepository.observePurchaseHistory(),
    ) { profile, entries ->
        profile to entries
    }.map { (profile, entries) ->
        val viewerUid = profile.firebaseUid
        val enrichedEntries = entries.map { entry ->
            val recipientUid = entry.recipientFirebaseUid
            if (!entry.isGift || recipientUid.isNullOrBlank() || viewerUid.isNullOrBlank()) {
                entry
            } else {
                val publicProfile = runCatching {
                    userDirectoryRepository.getPublicUserProfile(viewerUid, recipientUid)
                }.getOrNull()
                val recipientName = publicProfile?.displayName?.trim()?.takeIf(String::isNotEmpty)
                    ?: publicProfile?.username?.takeIf(String::isNotBlank)?.let { "@$it" }
                    ?: "Пользователь (${recipientUid.take(6)})"
                entry.copy(recipientName = recipientName)
            }
        }
        ShopPurchaseHistoryUiState(entries = enrichedEntries, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShopPurchaseHistoryUiState(),
    )
}
