package com.jaroapps.dailybrain.feature.shop.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticCatalog
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarAssetPreview
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopPurchaseHistoryEntry
import com.jaroapps.dailybrain.feature.shop.domain.model.ShopPurchaseItemType
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ShopPurchaseHistoryDateFormatter =
    DateTimeFormatter.ofPattern("dd.MM.yyyy · HH:mm", Locale.getDefault())

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopPurchaseHistoryScreen(
    onBack: () -> Unit,
    viewModel: ShopPurchaseHistoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История покупок") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.Close, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = colorResource(R.color.color_success))
            }
            state.entries.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize().padding(24.dp).padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Покупок пока нет",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 12.dp,
                ),
            ) {
                items(state.entries, key = { it.id }) { entry ->
                    ShopPurchaseHistoryCard(entry)
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

@Composable
private fun ShopPurchaseHistoryCard(entry: ShopPurchaseHistoryEntry) {
    val dateText = runCatching {
        Instant.ofEpochMilli(entry.occurredAtEpochMs)
            .atZone(ZoneId.systemDefault())
            .format(ShopPurchaseHistoryDateFormatter)
    }.getOrDefault("")

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PurchaseHistoryImage(entry)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    purchaseItemName(entry),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    if (entry.isGift) {
                        "Подарок для ${entry.recipientName ?: "пользователя"}"
                    } else {
                        "Покупка"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    dateText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "−${entry.price} эндорфинов",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PurchaseHistoryImage(entry: ShopPurchaseHistoryEntry) {
    when (entry.itemType) {
        ShopPurchaseItemType.PATCH -> Image(
            painter = painterResource(R.drawable.gift_endorphin_patch),
            contentDescription = "Эндорфиновый пластырь",
            modifier = Modifier.size(76.dp),
        )
        ShopPurchaseItemType.GIFT -> {
            val gift = EndorphinGiftType.entries.firstOrNull { it.name == entry.itemId }
            if (gift != null) {
                Image(
                    painter = painterResource(giftImage(gift)),
                    contentDescription = gift.displayName,
                    modifier = Modifier.size(76.dp),
                )
            } else {
                PurchasePlaceholder()
            }
        }
        ShopPurchaseItemType.COSTUME,
        ShopPurchaseItemType.BACKGROUND -> {
            val type = if (entry.itemType == ShopPurchaseItemType.COSTUME) {
                AvatarCosmeticType.COSTUME
            } else {
                AvatarCosmeticType.BACKGROUND
            }
            val cosmetic = AvatarCosmeticCatalog.find(type, entry.itemId)
            if (cosmetic != null) {
                AvatarAssetPreview(
                    assetPath = cosmetic.assetPath,
                    contentDescription = cosmetic.displayName,
                    centerCrop = false,
                    modifier = Modifier.size(76.dp),
                )
            } else {
                PurchasePlaceholder()
            }
        }
    }
}

@Composable
private fun PurchasePlaceholder() {
    Box(
        modifier = Modifier.size(76.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("?", style = MaterialTheme.typography.headlineMedium)
    }
}

private fun purchaseItemName(entry: ShopPurchaseHistoryEntry): String = when (entry.itemType) {
    ShopPurchaseItemType.PATCH -> "Эндорфиновый пластырь"
    ShopPurchaseItemType.GIFT ->
        EndorphinGiftType.entries.firstOrNull { it.name == entry.itemId }?.displayName ?: "Подарок"
    ShopPurchaseItemType.COSTUME ->
        AvatarCosmeticCatalog.find(AvatarCosmeticType.COSTUME, entry.itemId)?.displayName ?: "Костюм"
    ShopPurchaseItemType.BACKGROUND ->
        AvatarCosmeticCatalog.find(AvatarCosmeticType.BACKGROUND, entry.itemId)?.displayName ?: "Фон"
}

private fun giftImage(type: EndorphinGiftType): Int = when (type) {
    EndorphinGiftType.CANDY -> R.drawable.gift_candy
    EndorphinGiftType.COFFEE -> R.drawable.gift_coffee
    EndorphinGiftType.CROISSANT -> R.drawable.gift_croissant
    EndorphinGiftType.HAMBURGER -> R.drawable.gift_hamburger
    EndorphinGiftType.CAKE -> R.drawable.gift_cake
}
