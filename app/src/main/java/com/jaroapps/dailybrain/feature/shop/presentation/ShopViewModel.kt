package com.jaroapps.dailybrain.feature.shop.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticCatalog
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticItem
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.UserProfile
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserDirectoryRepository
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopCosmeticOwnership
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopPurchaseResult
import com.jaroapps.dailybrain.feature.shop.domain.repository.GiftDeliveryRepository
import com.jaroapps.dailybrain.feature.shop.domain.repository.ShopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class ShopGiftPrompt {
    GOOGLE_AUTH_REQUIRED,
    FIND_FRIENDS,
}

data class ShopUiState(
    val profile: UserProfile? = null,
    val inventory: List<ShopCosmeticOwnership> = emptyList(),
    val followingUsers: List<PublicUserProfile> = emptyList(),
    val isLoadingRecipients: Boolean = false,
    val isPurchasing: Boolean = false,
    val message: String? = null,
    val messageTitle: String = "Магазин",
    val giftPrompt: ShopGiftPrompt? = null,
)

private data class ShopActions(
    val followingUsers: List<PublicUserProfile> = emptyList(),
    val isLoadingRecipients: Boolean = false,
    val isPurchasing: Boolean = false,
    val message: String? = null,
    val messageTitle: String = "Магазин",
    val giftPrompt: ShopGiftPrompt? = null,
)

class ShopViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    private val shopRepository: ShopRepository,
    private val giftDeliveryRepository: GiftDeliveryRepository,
    private val userDirectoryRepository: UserDirectoryRepository,
) : ViewModel() {
    private val actions = MutableStateFlow(ShopActions())

    private val baseState = combine(
        observeUserProfile(),
        shopRepository.observeInventory(),
    ) { profile, inventory ->
        ShopUiState(profile = profile, inventory = inventory)
    }

    val uiState: StateFlow<ShopUiState> = combine(baseState, actions) { base, action ->
        base.copy(
            followingUsers = action.followingUsers,
            isLoadingRecipients = action.isLoadingRecipients,
            isPurchasing = action.isPurchasing,
            message = action.message,
            messageTitle = action.messageTitle,
            giftPrompt = action.giftPrompt,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopUiState())

    init {
        refreshGiftData()
    }

    fun refreshGiftData() {
        viewModelScope.launch {
            runCatching { giftDeliveryRepository.syncIncoming() }
            runCatching { giftDeliveryRepository.syncPendingOutgoing() }
        }
    }

    fun refreshRecipients() {
        val firebaseUid = uiState.value.profile?.firebaseUid
        if (firebaseUid.isNullOrBlank()) {
            actions.value = actions.value.copy(
                followingUsers = emptyList(),
                isLoadingRecipients = false,
                message = null,
                giftPrompt = ShopGiftPrompt.GOOGLE_AUTH_REQUIRED,
            )
            return
        }
        actions.value = actions.value.copy(
            followingUsers = emptyList(),
            isLoadingRecipients = true,
            message = null,
            giftPrompt = null,
        )
        viewModelScope.launch {
            try {
                val users = userDirectoryRepository.getFollowing(firebaseUid)
                    .filterNot { it.firebaseUid == firebaseUid }
                actions.value = actions.value.copy(
                    followingUsers = users,
                    giftPrompt = if (users.isEmpty()) ShopGiftPrompt.FIND_FRIENDS else null,
                )
            } catch (_: Exception) {
                showMessage("Не удалось загрузить список подписок.")
            } finally {
                actions.value = actions.value.copy(isLoadingRecipients = false)
            }
        }
    }

    fun dismissGiftPrompt() {
        actions.value = actions.value.copy(giftPrompt = null)
    }

    fun dismissMessage() {
        actions.value = actions.value.copy(message = null, messageTitle = "Магазин")
    }

    fun purchasePatch() {
        val newPatchCount = (uiState.value.profile?.patchCount ?: 0) + 1
        purchase(
            successMessage = "Пластырь куплен! Теперь у вас $newPatchCount из ${com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinEconomy.MAX_PATCH_COUNT}.",
        ) {
            shopRepository.purchasePatch(UUID.randomUUID().toString())
        }
    }

    fun purchaseCosmetic(type: AvatarCosmeticType, itemId: String) {
        val itemName = AvatarCosmeticCatalog.find(type, itemId)?.displayName ?: "Предмет"
        purchase(
            successMessage = "$itemName куплен! Теперь он доступен в редакторе аватара в профиле.",
        ) {
            shopRepository.purchaseCosmetic(type, itemId, UUID.randomUUID().toString())
        }
    }

    fun sendGift(giftType: EndorphinGiftType, recipient: PublicUserProfile) {
        val profile = uiState.value.profile ?: return
        val senderUid = profile.firebaseUid
        if (senderUid.isNullOrBlank()) {
            showMessage("Чтобы дарить подарки, подключите Google-аккаунт в профиле.")
            return
        }
        if (recipient.firebaseUid == senderUid) {
            showMessage("Нельзя подарить подарок самому себе.")
            return
        }

        viewModelScope.launch {
            actions.value = actions.value.copy(isPurchasing = true, message = null)
            try {
                val giftId = System.currentTimeMillis().toString() + "_" + UUID.randomUUID()
                when (val result = shopRepository.purchaseGift(recipient.firebaseUid, giftType, giftId)) {
                    is ShopPurchaseResult.Purchased -> {
                        val sent = runCatching {
                            giftDeliveryRepository.syncPendingOutgoing(giftId)
                        }.getOrDefault(false)
                        val recipientName = recipient.displayName?.trim()?.takeIf(String::isNotEmpty)
                            ?: "@${recipient.username}"
                        showMessage(
                            if (sent) "Подарок «${giftType.displayName}» отправлен пользователю $recipientName."
                            else "Подарок «${giftType.displayName}» сохранён и будет отправлен пользователю $recipientName при восстановлении соединения.",
                            if (sent) "Подарок отправлен" else "Подарок сохранён",
                        )
                    }
                    is ShopPurchaseResult.InsufficientFunds ->
                        showMessage("Недостаточно эндорфинов: у вас " + result.currentBalance +
                            ", требуется " + result.requiredAmount + ".")
                    is ShopPurchaseResult.AlreadyProcessed -> showMessage("Эта отправка подарка уже обработана.")
                    else -> showMessage("Не удалось отправить подарок.")
                }
            } catch (_: Exception) {
                showMessage("Не удалось отправить подарок. Попробуйте ещё раз.")
            } finally {
                actions.value = actions.value.copy(isPurchasing = false)
            }
        }
    }

    fun sendCosmeticGift(item: AvatarCosmeticItem, recipient: PublicUserProfile) {
        val profile = uiState.value.profile ?: return
        val senderUid = profile.firebaseUid
        if (senderUid.isNullOrBlank()) {
            showMessage("Чтобы дарить подарки, подключите Google-аккаунт в профиле.")
            return
        }
        if (recipient.firebaseUid == senderUid) {
            showMessage("Нельзя подарить предмет самому себе.")
            return
        }
        if (actions.value.isPurchasing) return

        viewModelScope.launch {
            actions.value = actions.value.copy(isPurchasing = true, message = null)
            try {
                val giftId = System.currentTimeMillis().toString() + "_" + UUID.randomUUID()
                when (val result = shopRepository.purchaseCosmeticGift(
                    recipientFirebaseUid = recipient.firebaseUid,
                    itemType = item.type,
                    itemId = item.id,
                    giftId = giftId,
                )) {
                    is ShopPurchaseResult.Purchased -> {
                        val sent = runCatching {
                            giftDeliveryRepository.syncPendingOutgoing(giftId)
                        }.getOrDefault(false)
                        val recipientName = recipient.displayName?.trim()?.takeIf(String::isNotEmpty)
                            ?: "@${recipient.username}"
                        showMessage(
                            if (sent) "Предмет «${item.displayName}» отправлен пользователю $recipientName."
                            else "Предмет «${item.displayName}» сохранён и будет отправлен пользователю $recipientName при восстановлении соединения.",
                            if (sent) "Подарок отправлен" else "Подарок сохранён",
                        )
                    }
                    is ShopPurchaseResult.InsufficientFunds ->
                        showMessage("Недостаточно эндорфинов: у вас " + result.currentBalance +
                            ", требуется " + result.requiredAmount + ".")
                    is ShopPurchaseResult.AlreadyProcessed ->
                        showMessage("Эта отправка подарка уже обработана.")
                    ShopPurchaseResult.AccountNotLinked ->
                        showMessage("Чтобы дарить подарки, подключите Google-аккаунт в профиле.")
                    ShopPurchaseResult.InvalidRecipient ->
                        showMessage("Нельзя подарить предмет самому себе.")
                    else -> showMessage("Не удалось отправить подарок.")
                }
            } catch (_: Exception) {
                showMessage("Не удалось отправить подарок. Попробуйте ещё раз.")
            } finally {
                actions.value = actions.value.copy(isPurchasing = false)
            }
        }
    }

    fun openGift(giftId: String) {
        viewModelScope.launch {
            runCatching { shopRepository.markGiftOpened(giftId) }
        }
    }

    private fun purchase(
        successMessage: String,
        operation: suspend () -> ShopPurchaseResult,
    ) {
        if (actions.value.isPurchasing) return
        viewModelScope.launch {
            actions.value = actions.value.copy(isPurchasing = true, message = null)
            try {
                when (val result = operation()) {
                    is ShopPurchaseResult.Purchased -> showMessage(successMessage, "Покупка успешна")
                    is ShopPurchaseResult.InsufficientFunds ->
                        showMessage("Недостаточно эндорфинов: у вас " + result.currentBalance +
                            ", требуется " + result.requiredAmount + ".")
                    is ShopPurchaseResult.AlreadyProcessed -> showMessage("Покупка уже была обработана.")
                    ShopPurchaseResult.AlreadyOwned -> showMessage("Этот предмет уже куплен.")
                    ShopPurchaseResult.PatchLimitReached -> showMessage("У вас уже максимальное количество пластырей.")
                    ShopPurchaseResult.AccountNotLinked -> showMessage("Подключите Google-аккаунт, чтобы дарить подарки.")
                    ShopPurchaseResult.InvalidRecipient -> showMessage("Нельзя подарить подарок самому себе.")
                }
            } catch (_: Exception) {
                showMessage("Не удалось выполнить покупку. Попробуйте ещё раз.")
            } finally {
                actions.value = actions.value.copy(isPurchasing = false)
            }
        }
    }

    private fun showMessage(message: String, title: String = "Магазин") {
        actions.value = actions.value.copy(message = message, messageTitle = title)
    }
}
