package com.jaroapps.dailybrain.feature.feed.presentation

import android.graphics.BitmapFactory

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.feed.domain.FeedEvent
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectibleKind
import com.jaroapps.dailybrain.feature.rewards.domain.model.EndorphinGiftType
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val FeedTimeFormatter = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.getDefault())
private const val BADGE_CELL_WIDTH = 192
private const val BADGE_CELL_HEIGHT = 228

@Composable
fun FeedScreen(
    onOpenUserProfile: (String) -> Unit,
    onOpenCollectible: (String, ProfileCollectibleKind, String) -> Unit,
    viewModel: FeedViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    // Decode bitmap resources without Compose's BitmapDrawable cast. A missing or malformed
    // optional badge must never prevent the feed itself from opening.
    val badgeSheets: List<ImageBitmap?> = remember(context) {
        listOf(
            R.drawable.badge_weekly,
            R.drawable.badge_months_01_04,
            R.drawable.badge_months_05_08,
            R.drawable.badge_months_09_12,
        ).map { resourceId ->
            runCatching {
                BitmapFactory.decodeResource(context.resources, resourceId)?.asImageBitmap()
            }.getOrNull()
        }
    }

    LaunchedEffect(listState, state.hasMore, state.isLoadingMore, state.isRefreshing, state.events.size) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: -1
            lastVisible >= layout.totalItemsCount - 4
        }.distinctUntilChanged().collect { nearEnd ->
            if (nearEnd && state.hasMore && !state.isLoadingMore && !state.isRefreshing) {
                viewModel.loadMore()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Лента",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::refresh, enabled = !state.isRefreshing) {
                if (state.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Refresh, contentDescription = "Обновить ленту")
                }
            }
        }
        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        if (state.events.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (state.isRefreshing) "Загружаем новости…" else "Пока нет достижений. Начни тренировку!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.events, key = { it.eventId }) { event ->
                    FeedEventCard(
                        event = event,
                        badgeSheets = badgeSheets,
                        onOpenUserProfile = onOpenUserProfile,
                        onOpenCollectible = onOpenCollectible,
                    )
                }
                if (state.hasMore) {
                    item(key = "feed-loading-more") {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (state.isLoadingMore) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedEventCard(
    event: FeedEvent,
    badgeSheets: List<ImageBitmap?>,
    onOpenUserProfile: (String) -> Unit,
    onOpenCollectible: (String, ProfileCollectibleKind, String) -> Unit,
) {
    val author = if (event.isOwn) "Вы" else event.authorName
    val authorUid = event.authorFirebaseUid?.takeIf(String::isNotBlank)
    val collectibleOwnerUid = authorUid ?: if (event.isOwn) "local" else null
    val username = event.authorUsername?.takeIf(String::isNotBlank)?.let { "@$it" }
    val badgeLocation = event.badgeSpriteLocation()

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .clickable(enabled = authorUid != null) {
                        authorUid?.let(onOpenUserProfile)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = author.firstOrNull()?.uppercase() ?: "?",
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = author,
                        fontWeight = FontWeight.SemiBold,
                        color = if (authorUid != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable(enabled = authorUid != null) {
                            authorUid?.let(onOpenUserProfile)
                        },
                    )
                    if (!username.isNullOrBlank()) {
                        Text(
                            text = "  " + username,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                Text(event.message(), style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = runCatching {
                        FeedTimeFormatter.format(
                            Instant.ofEpochMilli(event.occurredAtEpochMs).atZone(ZoneId.systemDefault()),
                        )
                    }.getOrDefault(""),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val giftType = if (event.eventType == "GIFT_RECEIVED") {
                EndorphinGiftType.entries.firstOrNull { it.name == event.periodKey }
            } else null
            val collectibleId = event.badgeCollectibleId()
            val badgeBitmap = badgeLocation?.let { (sheetIndex, _) ->
                badgeSheets.getOrNull(sheetIndex)
            }
            when {
                giftType != null && collectibleOwnerUid != null -> {
                    Image(
                        painter = androidx.compose.ui.res.painterResource(
                            when (giftType) {
                                EndorphinGiftType.CANDY -> R.drawable.gift_candy
                                EndorphinGiftType.COFFEE -> R.drawable.gift_coffee
                                EndorphinGiftType.CROISSANT -> R.drawable.gift_croissant
                                EndorphinGiftType.HAMBURGER -> R.drawable.gift_hamburger
                                EndorphinGiftType.CAKE -> R.drawable.gift_cake
                            },
                        ),
                        contentDescription = "Подарок: ${giftType.displayName}",
                        modifier = Modifier.size(width = 68.dp, height = 80.dp)
                            .clickable {
                                onOpenCollectible(collectibleOwnerUid, ProfileCollectibleKind.GIFT, event.eventId.removePrefix("gift_received:"))
                            },
                    )
                }
                badgeLocation != null && badgeBitmap != null && collectibleId != null && collectibleOwnerUid != null -> {
                    val (_, cellIndex) = badgeLocation
                    Image(
                        painter = BitmapPainter(
                            image = badgeBitmap,
                            srcOffset = IntOffset(cellIndex * BADGE_CELL_WIDTH, 0),
                            srcSize = IntSize(BADGE_CELL_WIDTH, BADGE_CELL_HEIGHT),
                        ),
                        contentDescription = event.badgeDescription(),
                        modifier = Modifier.size(width = 68.dp, height = 80.dp)
                            .clickable {
                                onOpenCollectible(collectibleOwnerUid, ProfileCollectibleKind.BADGE, collectibleId)
                            },
                    )
                }
            }
        }
    }
}

private fun FeedEvent.badgeSpriteLocation(): Pair<Int, Int>? = when (eventType) {
    "WEEKLY_STREAK" -> 0 to 0
    "WEEKLY_CHALLENGE" -> 0 to 1
    "MONTHLY_STREAK" -> {
        val month = (valueInt ?: periodKey?.substringAfterLast("-")?.toIntOrNull() ?: 1).coerceIn(1, 12)
        (1 + (month - 1) / 4) to ((month - 1) % 4)
    }
    else -> null
}


private fun FeedEvent.badgeCollectibleId(): String? = when (eventType) {
    "WEEKLY_STREAK" -> periodKey?.let { "weekly_streak:$it" }
    "WEEKLY_CHALLENGE" -> periodKey?.let { "weekly_challenge:$it" }
    "MONTHLY_STREAK" -> periodKey?.let { "monthly_streak:$it" }
    else -> null
}

private fun FeedEvent.badgeDescription(): String = when (eventType) {
    "WEEKLY_STREAK" -> "Бейджик: недельная серия"
    "WEEKLY_CHALLENGE" -> "Бейджик: задание недели"
    "MONTHLY_STREAK" -> "Месячный бейджик"
    else -> "Достижение"
}

private fun FeedEvent.message(): String = when (eventType) {
    "WEEKLY_STREAK" -> "Завершил 7 дней непрерывных тренировок и получил бейджик"
    "WEEKLY_CHALLENGE" -> "Выполнил недельное задание и получил бейджик"
    "MONTHLY_STREAK" -> "Завершил месяц ежедневных тренировок и получил месячный бейджик"
    "PERSONAL_RECORD" -> {
        val accuracy = accuracyPercent?.let {
            " · точность " + String.format(Locale.getDefault(), "%.1f%%", it)
        }.orEmpty()
        val time = valueLong?.let { " · " + formatDuration(it) }.orEmpty()
        "Установил новый личный рекорд в арифметике" + time + accuracy
    }
    "WEEKLY_XP_MILESTONE" -> "Набрал " + (valueInt ?: 0) + " очков за текущую неделю"
    "GIFT_RECEIVED" -> {
        val giftName = EndorphinGiftType.entries.firstOrNull { it.name == periodKey }?.displayName ?: "подарок"
        "Получил подарок «$giftName» от " + (relatedUserName?.takeIf(String::isNotBlank) ?: "пользователя DailyBrain")
    }
    else -> "Получил новое достижение"
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1_000L).coerceAtLeast(0L)
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
}
