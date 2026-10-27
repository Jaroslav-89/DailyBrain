package com.jaroapps.dailybrain.feature.home.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.common.domain.model.ExerciseType
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BrainOrange = Color(0xFFFF9800)
private val MissedBlue = Color(0xFF2196F3)
private val UnavailableGray = Color(0xFFBDBDBD)
private val ZeroStreakGray = Color(0xFF9E9E9E)
private val WeekDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

private val WeekdayLabels = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")

@Composable
fun HomeScreen(
    onOpenCalendar: () -> Unit,
    onStartTraining: () -> Unit,
    onStartMemoryTraining: () -> Unit,
    onStartStroopTest: () -> Unit,
    onStartCountingTraining: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dayButtonsDescription = state.currentWeekDates.zip(WeekdayLabels)
    val context = LocalContext.current
    LaunchedEffect(context) {
        HomeAppearancePreference.ensureLoaded(context)
    }
    val homeBackground = if (HomeAppearancePreference.dynamicBackgroundEnabled) {
        dynamicWeekBackground(state)
    } else {
        SolidColor(MaterialTheme.colorScheme.background)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(homeBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .clickable(onClick = onOpenCalendar)
                .semantics {
                    contentDescription =
                        "Серия тренировочных дней: ${state.currentStreakDays}. Открыть календарь"
                }
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.currentStreakDays.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_notification_brain),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(
                    if (state.currentStreakDays > 0) BrainOrange else ZeroStreakGray,
                ),
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Ваше задание на неделю",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "${state.currentWeekDates.first().format(WeekDateFormatter)} - " +
                state.currentWeekDates.last().format(WeekDateFormatter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val arithmeticButtonSize = 56.dp
            val arithmeticRingSize = 88.dp
            val dayItemHeight = 112.dp
            val additionalButtonSize = 72.dp
            val additionalRingSize = 96.dp

            fun x(fraction: Float, size: Dp) = (maxWidth - size) * fraction
            fun y(fraction: Float, size: Dp) = (maxHeight - size) * fraction

            // Additional exercises sit on the first, second and third quarter-lines.
            // The outer buttons share the same side margins; the middle one is on the opposite side
            // to keep the arithmetic path clear.
            RoundExerciseButton(
                label = "M",
                description = "Тренировка памяти",
                size = additionalButtonSize,
                ringSize = additionalRingSize,
                x = 0.dp,
                y = maxHeight * 0.25f - additionalRingSize / 2,
                completed = state.hasCompletedExerciseThisWeek(ExerciseType.MEMORY_WORDS),
                onClick = onStartMemoryTraining,
            )
            RoundExerciseButton(
                label = "STROOP",
                description = "Тест Струпа",
                size = additionalButtonSize,
                ringSize = additionalRingSize,
                x = maxWidth - additionalRingSize,
                y = maxHeight * 0.5f - additionalRingSize / 2,
                completed = state.hasCompletedExerciseThisWeek(ExerciseType.STROOP_TEST),
                onClick = onStartStroopTest,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(
                        Color(0xFFE53935),
                        Color(0xFF1E88E5),
                        Color(0xFFF9A825),
                        Color(0xFF43A047),
                    ).forEach { color ->
                        Box(
                            Modifier
                                .size(width = 6.dp, height = 24.dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }
            }
            RoundExerciseButton(
                label = "0..\n120",
                description = "Устный счёт",
                size = additionalButtonSize,
                ringSize = additionalRingSize,
                x = 0.dp,
                y = maxHeight * 0.75f - additionalRingSize / 2,
                completed = state.hasCompletedExerciseThisWeek(ExerciseType.COUNTING_TRAINING),
                onClick = onStartCountingTraining,
            )

            val horizontalPositions = listOf(
                0.78f,
                0.63f,
                0.49f,
                0.36f,
                0.39f,
                0.54f,
                0.69f,
            )

            state.currentWeekDates.forEachIndexed { index, date ->
                val status = state.arithmeticDayStatus(date)
                val color = arithmeticButtonColor(status)
                val today = date == state.today
                val dateLabel = dayButtonsDescription[index].second

                Box(
                    modifier = Modifier
                        .offset(
                            x = x(horizontalPositions[index], arithmeticRingSize),
                            y = y(index / 6f, dayItemHeight),
                        )
                        .size(width = arithmeticRingSize, height = dayItemHeight),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(arithmeticRingSize),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (today) {
                                PulsingRing(
                                    modifier = Modifier.fillMaxSize(),
                                    initialRadius = 34.dp,
                                    targetRadius = 42.dp,
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .offset(y = 4.dp)
                                    .size(arithmeticButtonSize)
                                    .clip(CircleShape)
                                    .background(depthColor(color)),
                            )
                            Box(
                                modifier = Modifier
                                    .size(arithmeticButtonSize)
                                    .graphicsLayer {
                                        rotationX = 9f
                                        cameraDistance = 12f * density
                                        shadowElevation = 7.dp.toPx()
                                        shape = CircleShape
                                        clip = true
                                    }
                                    .background(color, CircleShape)
                                    .clickable(
                                        enabled = today,
                                        onClick = onStartTraining,
                                    )
                                    .semantics {
                                        contentDescription = when (status) {
                                            ArithmeticDayStatus.FUTURE ->
                                                "$dateLabel: ещё не доступно"
                                            ArithmeticDayStatus.AVAILABLE_TODAY ->
                                                "$dateLabel: начать арифметику"
                                            ArithmeticDayStatus.COMPLETED ->
                                                "$dateLabel: тренировка выполнена"
                                            ArithmeticDayStatus.MISSED ->
                                                "$dateLabel: пропущенный день"
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(Modifier.fillMaxSize()) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 9.dp, top = 7.dp)
                                            .size(14.dp),
                                    )
                                    Text(
                                        text = "/",
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Center),
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Remove,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 9.dp, bottom = 7.dp)
                                            .size(14.dp),
                                    )
                                }
                            }
                        }

                        Text(
                            text = dateLabel,
                            modifier = Modifier.offset(y = (-8).dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (today) BrainOrange else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
        }
    }
}

private fun arithmeticButtonColor(status: ArithmeticDayStatus): Color = when (status) {
    ArithmeticDayStatus.COMPLETED -> BrainOrange
    ArithmeticDayStatus.MISSED -> MissedBlue
    ArithmeticDayStatus.AVAILABLE_TODAY,
    ArithmeticDayStatus.FUTURE -> UnavailableGray
}

private fun depthColor(color: Color): Color = Color(
    red = color.red * 0.72f,
    green = color.green * 0.72f,
    blue = color.blue * 0.72f,
    alpha = color.alpha,
)

private fun dynamicWeekBackground(state: HomeUiState): Brush {
    val colors = state.currentWeekDates.map { date ->
        lightenForBackground(arithmeticButtonColor(state.arithmeticDayStatus(date)))
    }
    val stops = buildList {
        add(0f to colors.first())
        add(0.12f to colors.first())
        colors.forEachIndexed { index, color ->
            add((0.16f + index * 0.13f).coerceAtMost(0.94f) to color)
        }
        add(1f to colors.last())
    }.toTypedArray()
    return Brush.verticalGradient(colorStops = stops)
}

private fun lightenForBackground(color: Color): Color = Color(
    red = color.red + (1f - color.red) * 0.88f,
    green = color.green + (1f - color.green) * 0.88f,
    blue = color.blue + (1f - color.blue) * 0.88f,
    alpha = 1f,
)

@Composable
private fun RoundExerciseButton(
    label: String,
    description: String,
    size: Dp,
    ringSize: Dp,
    x: Dp,
    y: Dp,
    completed: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit = {
        Text(
            text = label,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    },
) {
    val buttonColor = if (completed) BrainOrange else UnavailableGray
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(ringSize),
        contentAlignment = Alignment.Center,
    ) {
        if (!completed) {
            StaticRing(
                modifier = Modifier.fillMaxSize(),
                radius = size / 2 + 6.dp,
            )
        }

        Box(
            modifier = Modifier
                .offset(y = 4.dp)
                .size(size)
                .clip(CircleShape)
                .background(depthColor(buttonColor)),
        )
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    rotationX = 9f
                    cameraDistance = 12f * density
                    shadowElevation = 7.dp.toPx()
                    shape = CircleShape
                    clip = true
                }
                .background(buttonColor, CircleShape)
                .clickable(onClick = onClick)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun StaticRing(
    modifier: Modifier = Modifier,
    radius: Dp,
) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = BrainOrange,
            radius = radius.toPx(),
            style = Stroke(width = 4.dp.toPx()),
        )
    }
}

@Composable
private fun PulsingRing(
    modifier: Modifier = Modifier,
    initialRadius: Dp,
    targetRadius: Dp,
) {
    val transition = rememberInfiniteTransition(label = "pulsingExerciseRing")
    val radiusDp by transition.animateFloat(
        initialValue = initialRadius.value,
        targetValue = targetRadius.value,
        animationSpec = infiniteRepeatable<Float>(
            animation = tween<Float>(
                durationMillis = 1000,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulsingExerciseRingRadius",
    )

    Canvas(modifier = modifier) {
        drawCircle(
            color = BrainOrange,
            radius = radiusDp.dp.toPx(),
            style = Stroke(width = 4.dp.toPx()),
        )
    }
}
