package com.jaroapps.dailybrain.feature.home.presentation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.foundation.LocalIndication
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.colorResource
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

private val ArithmeticExerciseRingMaxRadius = 43.dp
private val AdditionalExerciseRingMaxRadius = 51.dp

private val WeekDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.getDefault())

private val WeekdayLabels = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")

@Composable
fun HomeScreen(
    onOpenCalendar: () -> Unit,
    onOpenRewardHistory: (String) -> Unit,
    onStartTraining: () -> Unit,
    onStartMemoryTraining: () -> Unit,
    onStartStroopTest: () -> Unit,
    onStartCountingTraining: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dayButtonsDescription = state.currentWeekDates.zip(WeekdayLabels)
    val dynamicBackgroundEnabled = HomeAppearancePreference.dynamicBackgroundEnabled
    val homeTextColor = if (dynamicBackgroundEnabled) {
        colorResource(R.color.color_home_dynamic_text)
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val homeSecondaryTextColor = if (dynamicBackgroundEnabled) {
        colorResource(R.color.color_home_dynamic_text_secondary)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val homeBackground = if (dynamicBackgroundEnabled) {
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
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .clickable(onClick = onOpenCalendar)
                    .semantics {
                        contentDescription =
                            "Серия тренировочных дней: ${state.currentStreakDays}. Открыть календарь"
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = state.currentStreakDays.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = homeTextColor,
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_notification_brain),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 5.dp).size(24.dp),
                    colorFilter = ColorFilter.tint(
                        if (state.currentStreakDays > 0) colorResource(R.color.color_training_completed) else colorResource(R.color.color_training_neutral),
                    ),
                )
            }
            Row(
                modifier = Modifier.weight(1f).clip(CircleShape).clickable { onOpenRewardHistory("xp") }.semantics {
                    contentDescription = "Очки опыта: ${state.xpPoints}. Открыть историю"
                },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.xpPoints.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = homeTextColor,
                )
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = colorResource(R.color.color_success),
                    modifier = Modifier.padding(start = 5.dp).size(24.dp),
                )
            }
            Row(
                modifier = Modifier.weight(1f).clip(CircleShape).clickable { onOpenRewardHistory("endorphins") }.semantics {
                    contentDescription = "Эндорфины: ${state.endorphinBalance}. Открыть историю"
                },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.endorphinBalance.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = homeTextColor,
                )
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_endorphin_molecule),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 5.dp).size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Ваше задание на неделю",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = homeTextColor,
        )
        Text(
            text = "${state.currentWeekDates.first().format(WeekDateFormatter)} - " +
                state.currentWeekDates.last().format(WeekDateFormatter),
            style = MaterialTheme.typography.bodyMedium,
            color = homeSecondaryTextColor,
            modifier = Modifier.padding(top = 2.dp),
        )

        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val arithmeticButtonSize = 56.dp
            val arithmeticRingSize = 96.dp
            val dayItemWidth = arithmeticRingSize + 36.dp
            val dayItemHeight = 100.dp
            val additionalButtonSize = 72.dp
            val additionalRingSize = 112.dp

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
                        colorResource(R.color.color_exercise_arithmetic),
                        colorResource(R.color.color_exercise_memory),
                        colorResource(R.color.color_exercise_counting),
                        colorResource(R.color.color_success),
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
                val interactionSource = remember(date) { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                val pressOffset by animateDpAsState(
                    targetValue = if (isPressed) 5.dp else 0.dp,
                    animationSpec = spring(dampingRatio = 0.38f, stiffness = 900f),
                    label = "arithmeticButtonPress",
                )
                val status = state.arithmeticDayStatus(date)
                val color = arithmeticButtonColor(status)
                val today = date == state.today
                val dateLabel = dayButtonsDescription[index].second

                Box(
                    modifier = Modifier
                        .offset(
                            x = x(horizontalPositions[index], dayItemWidth),
                            y = y(index / 6f, dayItemHeight),
                        )
                        .size(width = dayItemWidth, height = dayItemHeight),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(arithmeticRingSize),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (today) {
                                PulsingRing(
                                    modifier = Modifier.fillMaxSize(),
                                    initialRadius = 34.dp,
                                    targetRadius = ArithmeticExerciseRingMaxRadius,
                                )
                            }

                            Box(
                                modifier = Modifier
                                     .offset(y = 4.dp + pressOffset)
                                    .size(arithmeticButtonSize)
                                    .clip(CircleShape)
                                    .background(depthColor(color)),
                            )
                            Box(
                                modifier = Modifier
                                    .size(arithmeticButtonSize)
                                    .graphicsLayer {
                                        translationY = pressOffset.toPx()
                                        rotationX = 9f
                                        cameraDistance = 12f * density
                                        shadowElevation = 7.dp.toPx()
                                        shape = CircleShape
                                        clip = true
                                    }
                                    .background(color, CircleShape)
                                    .clickable(
                                        interactionSource = interactionSource,
                                        indication = LocalIndication.current,
                                        enabled = today,
                                        onClick = onStartTraining,
                                    )
                                    .semantics {
                                        contentDescription = when (status) {
                                            ArithmeticDayStatus.LOADING ->
                                                "$dateLabel: загрузка истории тренировок"
                                            ArithmeticDayStatus.FUTURE ->
                                                "$dateLabel: ещё не доступно"
                                            ArithmeticDayStatus.AVAILABLE_TODAY ->
                                                "$dateLabel: начать арифметику"
                                            ArithmeticDayStatus.COMPLETED ->
                                                "$dateLabel: тренировка выполнена"
                                            ArithmeticDayStatus.BEFORE_FIRST_TRAINING ->
                                                "$dateLabel: тренировки ещё не начались"
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
                                        tint = colorResource(R.color.color_on_accent),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = 14.dp, top = 13.dp)
                                            .size(12.dp),
                                    )
                                    Text(
                                        text = "/",
                                        color = colorResource(R.color.color_on_accent),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.align(Alignment.Center),
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Remove,
                                        contentDescription = null,
                                        tint = colorResource(R.color.color_on_accent),
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(end = 14.dp, bottom = 13.dp)
                                            .size(12.dp),
                                    )
                                }
                            }
                        }

                        Text(
                            text = dateLabel,
                            modifier = Modifier.padding(start = 0.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = homeTextColor,
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

@Composable
private fun arithmeticButtonColor(status: ArithmeticDayStatus): Color = when (status) {
    ArithmeticDayStatus.LOADING,
    ArithmeticDayStatus.BEFORE_FIRST_TRAINING,
    ArithmeticDayStatus.AVAILABLE_TODAY,
    ArithmeticDayStatus.FUTURE -> colorResource(R.color.color_training_disabled)
    ArithmeticDayStatus.COMPLETED -> colorResource(R.color.color_training_completed)
    ArithmeticDayStatus.MISSED -> colorResource(R.color.color_training_missed)
}

private fun depthColor(color: Color): Color = Color(
    red = color.red * 0.72f,
    green = color.green * 0.72f,
    blue = color.blue * 0.72f,
    alpha = color.alpha,
)

@Composable
private fun dynamicWeekBackground(state: HomeUiState): Brush {
    val completedArithmeticDates = state.completedExerciseDatesByType[
        ExerciseType.ARITHMETIC.storageValue
    ].orEmpty()
    if (state.currentWeekDates.none { it in completedArithmeticDates }) {
        return SolidColor(MaterialTheme.colorScheme.background)
    }

    // Only dates with a completed arithmetic workout contribute orange to the gradient.
    val orange = colorResource(R.color.color_training_completed)
    val neutral = MaterialTheme.colorScheme.background
    val tintLevels = listOf(0.54f, 0.62f, 0.70f)
    val colors = state.currentWeekDates.mapIndexed { index, date ->
        if (date in completedArithmeticDates) {
            lightenForBackground(orange, tintLevels[index % tintLevels.size])
        } else {
            neutral
        }
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

private fun lightenForBackground(color: Color, amount: Float = 0.58f): Color = Color(
    red = color.red + (1f - color.red) * amount,
    green = color.green + (1f - color.green) * amount,
    blue = color.blue + (1f - color.blue) * amount,
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
            color = colorResource(R.color.color_on_accent),
        )
    },
) {
    val buttonColor = if (completed) colorResource(R.color.color_training_completed) else colorResource(R.color.color_training_disabled)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) 5.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.38f, stiffness = 900f),
        label = "roundExerciseButtonPress",
    )
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(ringSize),
        contentAlignment = Alignment.Center,
    ) {
        if (!completed) {
            StaticRing(
                modifier = Modifier.fillMaxSize(),
                radius = AdditionalExerciseRingMaxRadius,
            )
        }

        Box(
            modifier = Modifier
                 .offset(y = 4.dp + pressOffset)
                .size(size)
                .clip(CircleShape)
                .background(depthColor(buttonColor)),
        )
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    translationY = pressOffset.toPx()
                    rotationX = 9f
                    cameraDistance = 12f * density
                    shadowElevation = 7.dp.toPx()
                    shape = CircleShape
                    clip = true
                }
                .background(buttonColor, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick,
                )
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
    val innerGlow = colorResource(R.color.color_exercise_glow_inner)
    val middleGlow = colorResource(R.color.color_exercise_glow_middle)
    val outerGlow = colorResource(R.color.color_exercise_glow_outer)
    Canvas(modifier = modifier) {
        val glowRadius = radius.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to innerGlow,
                    0.58f to middleGlow,
                    0.82f to outerGlow,
                    1f to Color.Transparent,
                ),
                center = center,
                radius = glowRadius,
            ),
            radius = glowRadius,
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

    val innerGlow = colorResource(R.color.color_exercise_glow_inner)
    val middleGlow = colorResource(R.color.color_exercise_glow_middle)
    val outerGlow = colorResource(R.color.color_exercise_glow_outer)
    Canvas(modifier = modifier) {
        val glowRadius = radiusDp.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to innerGlow,
                    0.58f to middleGlow,
                    0.82f to outerGlow,
                    1f to Color.Transparent,
                ),
                center = center,
                radius = glowRadius,
            ),
            radius = glowRadius,
        )
    }
}
