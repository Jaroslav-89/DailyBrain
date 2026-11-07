package com.jaroapps.dailybrain.feature.profile.presentation

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectible
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectibleKind
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private const val BADGE_CELL_WIDTH = 192
private const val BADGE_CELL_HEIGHT = 228
private val CollectibleDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm", Locale("ru"))

@Composable
fun ProfileCollectibleSection(
    title: String,
    ownerUid: String,
    items: List<ProfileCollectible>,
    kind: ProfileCollectibleKind,
    onOpenCollection: (String, ProfileCollectibleKind) -> Unit,
    onOpenItem: (String, ProfileCollectibleKind, String) -> Unit,
) {
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().clickable { onOpenCollection(ownerUid, kind) }.padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(">", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items, key = { it.id }) { item ->
                Column(
                    modifier = Modifier.width(88.dp).clickable { onOpenItem(ownerUid, kind, item.id) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    ProfileCollectibleArtwork(
                        item = item,
                        modifier = Modifier.size(width = 76.dp, height = 82.dp),
                    )
                    Text(
                        collectibleTitle(item),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCollectiblesScreen(
    onBack: () -> Unit,
    onOpenItem: (String, ProfileCollectibleKind, String) -> Unit,
    viewModel: ProfileCollectiblesViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val title = if (viewModel.kind == ProfileCollectibleKind.GIFT) "Подарки" else "Бейджики"

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
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            state.items.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (viewModel.kind == ProfileCollectibleKind.GIFT) "Подарков пока нет."
                    else "Бейджиков пока нет.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            onOpenItem(viewModel.ownerUid, item.kind, item.id)
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            ProfileCollectibleArtwork(
                                item = item,
                                modifier = Modifier.fillMaxWidth().height(104.dp),
                            )
                            Text(
                                collectibleTitle(item),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                minLines = 2,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                formatCollectibleDate(item.acquiredAtEpochMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectibleDetailScreen(
    onBack: () -> Unit,
    viewModel: CollectibleDetailViewModel = koinViewModel(),
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val item = state.item
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.let(::collectibleTitle) ?: "Предмет коллекции") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(state.error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }

            item == null -> Box(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                contentAlignment = Alignment.Center,
            ) { Text("Этот предмет больше недоступен.") }

            else -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ProfileCollectibleArtwork(
                            item = item,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
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
                        Text(
                            collectibleTitle(item),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Получен: " + formatCollectibleDate(item.acquiredAtEpochMs),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (item.kind == ProfileCollectibleKind.GIFT) {
                            Text(
                                "Отправитель: " + (
                                    item.senderName?.takeIf(String::isNotBlank)
                                        ?: "Пользователь DailyBrain"
                                    ),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            val amount = item.amount
                                ?: EndorphinGiftType.entries.firstOrNull { it.name == item.itemType }?.price
                            if (amount != null) {
                                Text("Начислено эндорфинов: +$amount", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Text("Достижение: " + badgeDescription(item.itemType), style = MaterialTheme.typography.bodyMedium)
                            item.periodKey?.takeIf(String::isNotBlank)?.let {
                                Text("Период: $it", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileCollectibleArtwork(
    item: ProfileCollectible,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    if (item.kind == ProfileCollectibleKind.GIFT) {
        Image(
            painter = painterResource(giftDrawable(item.itemType)),
            contentDescription = collectibleTitle(item),
            modifier = modifier,
            contentScale = contentScale,
        )
    } else {
        val location = badgeSpriteLocation(item.itemType)
        val context = LocalContext.current
        val bitmap = remember(context, location) {
            location?.let { (resourceId, _) ->
                runCatching {
                    BitmapFactory.decodeResource(context.resources, resourceId)?.asImageBitmap()
                }.getOrNull()
            }
        }
        if (location != null && bitmap != null) {
            Box(modifier = modifier, contentAlignment = Alignment.BottomEnd) {
                Image(
                    painter = BitmapPainter(
                        image = bitmap,
                        srcOffset = IntOffset(location.second * BADGE_CELL_WIDTH, 0),
                        srcSize = IntSize(BADGE_CELL_WIDTH, BADGE_CELL_HEIGHT),
                    ),
                    contentDescription = collectibleTitle(item),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                )
                if (item.itemType.startsWith("MONTHLY_CHALLENGE_")) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Месячное задание выполнено",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(2.dp).size(25.dp),
                    )
                }
            }
        } else {
            Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                Text("★", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun giftDrawable(itemType: String): Int = when (itemType) {
    EndorphinGiftType.CANDY.name -> R.drawable.gift_candy
    EndorphinGiftType.COFFEE.name -> R.drawable.gift_coffee
    EndorphinGiftType.CROISSANT.name -> R.drawable.gift_croissant
    EndorphinGiftType.HAMBURGER.name -> R.drawable.gift_hamburger
    EndorphinGiftType.CAKE.name -> R.drawable.gift_cake
    else -> R.drawable.gift_candy
}

private fun badgeSpriteLocation(itemType: String): Pair<Int, Int>? {
    return when {
    itemType == "WEEKLY_STREAK" -> R.drawable.badge_weekly to 0
    itemType == "WEEKLY_CHALLENGE" -> R.drawable.badge_weekly to 1
    itemType.startsWith("MONTH_"),
    itemType.startsWith("MONTHLY_CHALLENGE_") -> {
        val month = when {
            itemType.startsWith("MONTHLY_CHALLENGE_") ->
                itemType.substringAfter("MONTHLY_CHALLENGE_").toIntOrNull()
            else -> itemType.substringAfter("MONTH_").toIntOrNull()
        }?.takeIf { it in 1..12 } ?: return null
        val sheet = when (month) {
            in 1..4 -> R.drawable.badge_months_01_04
            in 5..8 -> R.drawable.badge_months_05_08
            else -> R.drawable.badge_months_09_12
        }
        sheet to ((month - 1) % 4)
    }
    else -> null
    }
}

private fun collectibleTitle(item: ProfileCollectible): String =
    if (item.kind == ProfileCollectibleKind.GIFT) {
        EndorphinGiftType.entries.firstOrNull { it.name == item.itemType }?.displayName ?: "Подарок"
    } else {
        badgeDescription(item.itemType)
    }

private fun badgeDescription(itemType: String): String = when (itemType) {
    "WEEKLY_STREAK" -> "Недельная серия"
    "WEEKLY_CHALLENGE" -> "Задание недели"
    else -> {
        val isMonthlyChallenge = itemType.startsWith("MONTHLY_CHALLENGE_")
        val month = if (isMonthlyChallenge) {
            itemType.substringAfter("MONTHLY_CHALLENGE_").toIntOrNull()
        } else {
            itemType.substringAfter("MONTH_").toIntOrNull()
        }
        if (month != null && month in 1..12) {
            val monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale("ru"))
            if (isMonthlyChallenge) "Все задания месяца — $monthName" else "Месячная серия — $monthName"
        } else {
            "Достижение"
        }
    }
}

private fun formatCollectibleDate(epochMs: Long): String = runCatching {
    CollectibleDateFormatter.format(
        Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()),
    )
}.getOrDefault("")
