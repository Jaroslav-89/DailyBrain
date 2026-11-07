package com.jaroapps.dailybrain.feature.shop.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticCatalog
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticItem
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarAssetPreview
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCloudSessionStatus
import com.jaroapps.dailybrain.feature.profile.presentation.UserDirectoryViewModel
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinEconomy
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.rewards.presentation.EndorphinAmount
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    onOpenPurchaseHistory: () -> Unit,
    onOpenFriendsSearch: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShopViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val directoryViewModel: UserDirectoryViewModel = koinViewModel()
    val directoryState by directoryViewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        var skipInitialResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (skipInitialResume) {
                    skipInitialResume = false
                } else {
                    viewModel.refreshGiftData()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedProduct by remember { mutableStateOf<ShopProductDetails?>(null) }
    var pendingPurchase by remember { mutableStateOf<PendingShopPurchase?>(null) }
    var pendingGift by remember { mutableStateOf<PendingShopGift?>(null) }

    fun requestGift(gift: PendingShopGift) {
        if (directoryState.cloudSessionStatus != ProfileCloudSessionStatus.READY) {
            selectedProduct = null
            onOpenFriendsSearch()
        } else {
            pendingGift = gift
            selectedProduct = null
            viewModel.refreshRecipients()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Магазин") },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp).clickable(onClick = onOpenPurchaseHistory),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        EndorphinAmount(
                            text = (state.profile?.endorphinBalance ?: 0).toString(),
                            textStyle = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            iconSize = 20.dp,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Image(
                            painter = painterResource(R.drawable.gift_endorphin_patch),
                            contentDescription = "Эндорфиновый пластырь",
                            modifier = Modifier.size(84.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Пластырь", fontWeight = FontWeight.Bold)
                            Text(
                                "Сохраняет серию дней при пропуске тренировки.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "В наличии: ${state.profile?.patchCount ?: 0}/${EndorphinEconomy.MAX_PATCH_COUNT}",
                                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            EndorphinAmount(
                                EndorphinEconomy.PATCH_PRICE.toString(),
                                textStyle = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                            Button(
                                onClick = { pendingPurchase = PendingShopPurchase.Patch },
                                enabled = !state.isPurchasing &&
                                    (state.profile?.patchCount ?: 0) < EndorphinEconomy.MAX_PATCH_COUNT,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorResource(R.color.color_button_green),
                                    contentColor = colorResource(R.color.color_on_accent),
                                ),
                            ) { Text("Купить") }
                        }
                    }
                }
            }

            item { SectionTitle("Подарки") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(EndorphinEconomy.gifts) { gift ->
                        GiftProductCard(gift) { selectedProduct = ShopProductDetails.Gift(gift) }
                    }
                }
            }

            item { SectionTitle("Костюмы") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(AvatarCosmeticCatalog.costumes, key = { it.id }) { item ->
                        CosmeticProductCard(
                            item = item,
                            owned = state.inventory.any { it.itemId == item.id && it.itemType == item.type },
                            onClick = { selectedProduct = ShopProductDetails.Cosmetic(item) },
                        )
                    }
                }
            }

            item { SectionTitle("Фоны") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(AvatarCosmeticCatalog.backgrounds, key = { it.id }) { item ->
                        CosmeticProductCard(
                            item = item,
                            owned = state.inventory.any { it.itemId == item.id && it.itemType == item.type },
                            onClick = { selectedProduct = ShopProductDetails.Cosmetic(item) },
                        )
                    }
                }
            }
        }
    }

    selectedProduct?.let { product ->
        val title = when (product) {
            is ShopProductDetails.Gift -> product.item.displayName
            is ShopProductDetails.Cosmetic -> product.item.displayName
        }
        val price = when (product) {
            is ShopProductDetails.Gift -> product.item.price
            is ShopProductDetails.Cosmetic -> product.item.price
        }
        val owned = product is ShopProductDetails.Cosmetic &&
            state.inventory.any { it.itemId == product.item.id && it.itemType == product.item.type }
        AlertDialog(
            onDismissRequest = { selectedProduct = null },
            title = { Text(title) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(230.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (product) {
                            is ShopProductDetails.Gift -> Image(
                                painter = painterResource(giftIcon(product.item)),
                                contentDescription = product.item.displayName,
                                modifier = Modifier.size(210.dp),
                            )
                            is ShopProductDetails.Cosmetic -> AvatarAssetPreview(
                                assetPath = product.item.assetPath,
                                contentDescription = product.item.displayName,
                                centerCrop = false,
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Стоимость: ", fontWeight = FontWeight.SemiBold)
                        EndorphinAmount(price.toString())
                    }
                    if (product is ShopProductDetails.Gift) {
                        Text(
                            "Получатель получит: +${product.item.recipientEndorphins} эндорфинов",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorResource(R.color.color_success),
                        )
                    }
                    if (owned) {
                        Text("Этот предмет уже есть в вашем инвентаре.")
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (product) {
                        is ShopProductDetails.Gift -> Button(
                            onClick = {
                                requestGift(PendingShopGift.Gift(product.item))
                            },
                            enabled = !state.isPurchasing,
                            colors = shopButtonColors(),
                        ) { Text("Подарить") }
                        is ShopProductDetails.Cosmetic -> {
                            if (!owned) {
                                Button(
                                    onClick = {
                                        pendingPurchase = PendingShopPurchase.Cosmetic(product.item)
                                        selectedProduct = null
                                    },
                                    enabled = !state.isPurchasing,
                                    colors = shopButtonColors(),
                                ) { Text("Купить") }
                            }
                            Button(
                                onClick = {
                                    requestGift(PendingShopGift.Cosmetic(product.item))
                                },
                                enabled = !state.isPurchasing,
                                colors = shopButtonColors(),
                            ) { Text("Подарить") }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedProduct = null }) { Text("Закрыть") }
            },
        )
    }

    pendingPurchase?.let { purchase ->
        val productName = when (purchase) {
            PendingShopPurchase.Patch -> "пластырь"
            is PendingShopPurchase.Cosmetic -> purchase.item.displayName
        }
        val price = when (purchase) {
            PendingShopPurchase.Patch -> EndorphinEconomy.PATCH_PRICE
            is PendingShopPurchase.Cosmetic -> purchase.item.price
        }
        AlertDialog(
            onDismissRequest = { if (!state.isPurchasing) pendingPurchase = null },
            title = { Text("Подтверждение покупки") },
            text = { Text("Купить $productName за $price эндорфинов?") },
            confirmButton = {
                Button(
                    onClick = {
                        when (purchase) {
                            PendingShopPurchase.Patch -> viewModel.purchasePatch()
                            is PendingShopPurchase.Cosmetic ->
                                viewModel.purchaseCosmetic(purchase.item.type, purchase.item.id)
                        }
                        pendingPurchase = null
                    },
                    enabled = !state.isPurchasing,
                    colors = shopButtonColors(),
                ) { Text("Купить") }
            },
            dismissButton = {
                TextButton(onClick = { pendingPurchase = null }, enabled = !state.isPurchasing) {
                    Text("Отмена")
                }
            },
        )
    }

    pendingGift?.let { gift ->
        if (state.message == null) {
            when {
                state.isLoadingRecipients -> AlertDialog(
                    onDismissRequest = { pendingGift = null },
                    title = { Text("Загрузка подписок") },
                    text = { CircularProgressIndicator() },
                    confirmButton = {
                        TextButton(onClick = { pendingGift = null }) { Text("Отмена") }
                    },
                )
                state.giftPrompt == ShopGiftPrompt.GOOGLE_AUTH_REQUIRED -> AlertDialog(
                    onDismissRequest = {
                        pendingGift = null
                        viewModel.dismissGiftPrompt()
                    },
                    title = { Text("Подключите Google") },
                    text = {
                        Text("Чтобы отправлять подарки, подключите Google-аккаунт и найдите друзей DailyBrain.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                pendingGift = null
                                viewModel.dismissGiftPrompt()
                                onOpenFriendsSearch()
                            },
                            colors = shopButtonColors(),
                        ) { Text("Продолжить с Google") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                pendingGift = null
                                viewModel.dismissGiftPrompt()
                            },
                        ) { Text("Отмена") }
                    },
                )
                state.giftPrompt == ShopGiftPrompt.FIND_FRIENDS -> AlertDialog(
                    onDismissRequest = {
                        pendingGift = null
                        viewModel.dismissGiftPrompt()
                    },
                    title = { Text("Найдите друзей") },
                    text = { Text("Вы пока ни на кого не подписаны. Найдите пользователя DailyBrain, чтобы отправить подарок.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                pendingGift = null
                                viewModel.dismissGiftPrompt()
                                onOpenFriendsSearch()
                            },
                            colors = shopButtonColors(),
                        ) { Text("Найти друзей") }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                pendingGift = null
                                viewModel.dismissGiftPrompt()
                            },
                        ) { Text("Закрыть") }
                    },
                )
                else -> AlertDialog(
                    onDismissRequest = { pendingGift = null },
                    title = {
                        Text(
                            when (gift) {
                                is PendingShopGift.Gift -> "Кому подарить ${gift.item.displayName.lowercase()}?"
                                is PendingShopGift.Cosmetic -> "Кому подарить «${gift.item.displayName}»?"
                            },
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            if (state.followingUsers.isEmpty()) {
                                Text("Нет доступных пользователей.")
                            } else {
                                state.followingUsers.forEach { recipient ->
                                    RecipientRow(recipient) {
                                        when (gift) {
                                            is PendingShopGift.Gift -> viewModel.sendGift(gift.item, recipient)
                                            is PendingShopGift.Cosmetic -> viewModel.sendCosmeticGift(gift.item, recipient)
                                        }
                                        pendingGift = null
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { pendingGift = null }) { Text("Закрыть") }
                    },
                )
            }
        }
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            title = { Text("Магазин") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = viewModel::dismissMessage, colors = shopButtonColors()) {
                    Text("Понятно")
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun GiftProductCard(giftType: EndorphinGiftType, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(148.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Image(
                painter = painterResource(giftIcon(giftType)),
                contentDescription = giftType.displayName,
                modifier = Modifier.size(88.dp),
            )
            Text(giftType.displayName, fontWeight = FontWeight.SemiBold)
            EndorphinAmount(giftType.price.toString())
            Text(
                "Подробнее",
                color = colorResource(R.color.color_success),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun CosmeticProductCard(
    item: AvatarCosmeticItem,
    owned: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(154.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            AvatarAssetPreview(
                assetPath = item.assetPath,
                contentDescription = item.displayName,
                centerCrop = false,
                modifier = Modifier.size(104.dp),
            )
            Text(item.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (owned) {
                Text("Куплено", color = androidx.compose.material3.MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else {
                EndorphinAmount(item.price.toString())
            }
            Text(
                "Подробнее",
                color = colorResource(R.color.color_success),
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun RecipientRow(user: PublicUserProfile, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(user.displayName?.takeIf(String::isNotBlank) ?: "Пользователь DailyBrain")
            if (user.username.isNotBlank()) {
                Text(
                    "@" + user.username,
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun shopButtonColors() = ButtonDefaults.buttonColors(
    containerColor = colorResource(R.color.color_button_green),
    contentColor = colorResource(R.color.color_on_accent),
)

private fun giftIcon(type: EndorphinGiftType): Int = when (type) {
    EndorphinGiftType.CANDY -> R.drawable.gift_candy
    EndorphinGiftType.COFFEE -> R.drawable.gift_coffee
    EndorphinGiftType.CROISSANT -> R.drawable.gift_croissant
    EndorphinGiftType.HAMBURGER -> R.drawable.gift_hamburger
    EndorphinGiftType.CAKE -> R.drawable.gift_cake
}

private sealed interface ShopProductDetails {
    data class Gift(val item: EndorphinGiftType) : ShopProductDetails
    data class Cosmetic(val item: AvatarCosmeticItem) : ShopProductDetails
}

private sealed interface PendingShopPurchase {
    data object Patch : PendingShopPurchase
    data class Cosmetic(val item: AvatarCosmeticItem) : PendingShopPurchase
}

private sealed interface PendingShopGift {
    data class Gift(val item: EndorphinGiftType) : PendingShopGift
    data class Cosmetic(val item: AvatarCosmeticItem) : PendingShopGift
}
