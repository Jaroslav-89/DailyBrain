package com.jaroapps.dailybrain.feature.feed.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticCatalog
import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarCosmeticType
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarAssetPreview
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FeedItemDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedItemDetailScreen(
    onBack: () -> Unit,
    eventType: String,
    itemType: String,
    itemId: String,
    occurredAtEpochMs: Long,
    valueInt: Int,
    relatedUserName: String?,
) {
    val gift = if (itemType == "GIFT") {
        EndorphinGiftType.entries.firstOrNull { it.name == itemId }
    } else null
    val cosmeticType = AvatarCosmeticType.entries.firstOrNull { it.name == itemType }
    val cosmetic = cosmeticType?.let { AvatarCosmeticCatalog.find(it, itemId) }
    val title = gift?.displayName ?: cosmetic?.displayName ?: "Предмет"
    val received = eventType == "GIFT_RECEIVED" || eventType == "COSMETIC_GIFT_RECEIVED"
    val sent = eventType == "GIFT_SENT" || eventType == "COSMETIC_GIFT_SENT"
    val status = when {
        received -> "Получен"
        sent -> "Отправлен"
        eventType == "COSMETIC_PURCHASED" -> "Куплен"
        else -> "Событие"
    }
    val date = runCatching {
        FeedItemDateFormatter.format(Instant.ofEpochMilli(occurredAtEpochMs).atZone(ZoneId.systemDefault()))
    }.getOrDefault("")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        gift != null -> Image(
                            painter = painterResource(giftDrawable(gift)),
                            contentDescription = gift.displayName,
                            modifier = Modifier.size(240.dp),
                            contentScale = ContentScale.Fit,
                        )
                        cosmetic != null -> AvatarAssetPreview(
                            assetPath = cosmetic.assetPath,
                            contentDescription = cosmetic.displayName,
                            centerCrop = false,
                            modifier = Modifier.fillMaxSize(),
                        )
                        else -> Text(
                            "Предмет недоступен",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(status, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Дата и время: $date", style = MaterialTheme.typography.bodyMedium)
                    when {
                        eventType == "GIFT_RECEIVED" && valueInt >= 0 ->
                            Text("Начислено эндорфинов: +$valueInt", style = MaterialTheme.typography.bodyMedium)
                        (sent || eventType == "COSMETIC_PURCHASED") && valueInt >= 0 ->
                            Text("Стоимость: −$valueInt эндорфинов", style = MaterialTheme.typography.bodyMedium)
                        eventType == "COSMETIC_GIFT_RECEIVED" ->
                            Text("Получен в подарок", style = MaterialTheme.typography.bodyMedium)
                    }
                    relatedUserName?.takeIf(String::isNotBlank)?.let { name ->
                        Text(
                            (if (received) "От кого: " else "Кому: ") + name,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun giftDrawable(gift: EndorphinGiftType): Int = when (gift) {
    EndorphinGiftType.CANDY -> R.drawable.gift_candy
    EndorphinGiftType.COFFEE -> R.drawable.gift_coffee
    EndorphinGiftType.CROISSANT -> R.drawable.gift_croissant
    EndorphinGiftType.HAMBURGER -> R.drawable.gift_hamburger
    EndorphinGiftType.CAKE -> R.drawable.gift_cake
}
