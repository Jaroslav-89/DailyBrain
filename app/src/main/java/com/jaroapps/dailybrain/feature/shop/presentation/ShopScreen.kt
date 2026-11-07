package com.jaroapps.dailybrain.feature.shop.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticCatalog
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticItem
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarAssetPreview
import com.jaroapps.dailybrain.feature.profile.domain.model.PublicUserProfile
import com.jaroapps.dailybrain.feature.rewards.presentation.EndorphinAmount
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinEconomy
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopCosmeticOwnership
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopReceivedGift
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopScreen(
    modifier: Modifier = Modifier,
    viewModel: ShopViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        var skipInitialResume = true
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (skipInitialResume) skipInitialResume = false else viewModel.refreshGiftData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var selectedGift by remember { mutableStateOf<EndorphinGiftType?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Магазин") },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        EndorphinAmount(
                            text = (state.profile?.endorphinBalance ?: 0).toString(),
                            textStyle = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.gift_endorphin_patch),
                            contentDescription = "Эндорфиновый пластырь",
                            modifier = Modifier.size(84.dp),
                        )
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Пластырь", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Сохраняет серию дней при пропуске тренировки.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "В наличии: " + (state.profile?.patchCount ?: 0) + "/" + EndorphinEconomy.MAX_PATCH_COUNT,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            EndorphinAmount(EndorphinEconomy.PATCH_PRICE.toString(), textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Button(
                                onClick = viewModel::purchasePatch,
                                enabled = !state.isPurchasing &&
                                    (state.profile?.patchCount ?: 0) < EndorphinEconomy.MAX_PATCH_COUNT,
                                colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.color_button_green), contentColor = colorResource(R.color.color_on_accent)),
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
                        GiftProductCard(
                            giftType = gift,
                            enabled = !state.isPurchasing,
                            onGift = { selectedGift = gift; viewModel.refreshRecipients() },
                        )
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
                            owned = state.inventory.owns(item),
                            enabled = !state.isPurchasing,
                            onBuy = { viewModel.purchaseCosmetic(AvatarCosmeticType.COSTUME, item.id) },
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
                            owned = state.inventory.owns(item),
                            enabled = !state.isPurchasing,
                            onBuy = { viewModel.purchaseCosmetic(AvatarCosmeticType.BACKGROUND, item.id) },
                        )
                    }
                }
            }

            if (state.receivedGifts.isNotEmpty()) {
                item { SectionTitle("Полученные подарки") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.receivedGifts, key = { it.giftId }) { gift ->
                            ReceivedGiftCard(gift, onClick = { viewModel.openGift(gift.giftId) })
                        }
                    }
                }
            }

            state.message?.let { message ->
                item {
                    Text(
                        message,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }

    selectedGift?.let { gift ->
        AlertDialog(
            onDismissRequest = { selectedGift = null },
            title = { Text("Кому подарить " + gift.displayName.lowercase() + "?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    when {
                        state.isLoadingRecipients -> Text("Загрузка подписок…")
                        state.followingUsers.isEmpty() -> Text("Нет доступных пользователей. Подпишитесь на пользователя DailyBrain, чтобы отправить ему подарок.")
                        else -> state.followingUsers.forEach { recipient ->
                            RecipientRow(recipient) {
                                viewModel.sendGift(gift, recipient)
                                selectedGift = null
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedGift = null }) { Text("Закрыть") }
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun GiftProductCard(giftType: EndorphinGiftType, enabled: Boolean, onGift: () -> Unit) {
    Card(
        modifier = Modifier.width(148.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(giftIcon(giftType)),
                contentDescription = giftType.displayName,
                modifier = Modifier.size(88.dp),
            )
            Text(giftType.displayName, fontWeight = FontWeight.SemiBold)
            EndorphinAmount(giftType.price.toString(), textStyle = MaterialTheme.typography.bodyMedium)
            Button(onClick = onGift, enabled = enabled, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.color_button_green), contentColor = colorResource(R.color.color_on_accent))) {
                Text("Подарить")
            }
        }
    }
}

@Composable
private fun CosmeticProductCard(
    item: AvatarCosmeticItem,
    owned: Boolean,
    enabled: Boolean,
    onBuy: () -> Unit,
) {
    Card(
        modifier = Modifier.width(154.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                Text("Куплено", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            } else {
                EndorphinAmount(item.price.toString(), textStyle = MaterialTheme.typography.bodyMedium)
                Button(onClick = onBuy, enabled = enabled, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = colorResource(R.color.color_button_green), contentColor = colorResource(R.color.color_on_accent))) {
                    Text("Купить")
                }
            }
        }
    }
}

@Composable
private fun ReceivedGiftCard(gift: ShopReceivedGift, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(138.dp).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(giftIcon(gift.giftType)),
                contentDescription = gift.giftType.displayName,
                modifier = Modifier.size(76.dp),
            )
            Text(gift.giftType.displayName, fontWeight = FontWeight.SemiBold)
            EndorphinAmount("+" + gift.giftType.price)
            Text(if (gift.isOpened) "Получено" else "Новое", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun RecipientRow(user: PublicUserProfile, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(user.displayName?.takeIf(String::isNotBlank) ?: "Пользователь DailyBrain")
            if (user.username.isNotBlank()) {
                Text("@" + user.username, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

private fun giftIcon(type: EndorphinGiftType): Int = when (type) {
    EndorphinGiftType.CANDY -> R.drawable.gift_candy
    EndorphinGiftType.COFFEE -> R.drawable.gift_coffee
    EndorphinGiftType.CROISSANT -> R.drawable.gift_croissant
    EndorphinGiftType.HAMBURGER -> R.drawable.gift_hamburger
    EndorphinGiftType.CAKE -> R.drawable.gift_cake
}

private fun List<ShopCosmeticOwnership>.owns(item: AvatarCosmeticItem): Boolean =
    any { it.itemId == item.id && it.itemType == item.type }
