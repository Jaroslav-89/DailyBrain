package com.jaroapps.dailybrain.feature.onboarding.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun ExerciseCountdownScreen(exerciseName: String, onFinished: () -> Unit) {
    val scale = remember { Animatable(0.65f) }
    var currentNumber by remember { mutableIntStateOf(3) }
    val finish by rememberUpdatedState(onFinished)

    LaunchedEffect(Unit) {
        for (number in 3 downTo 1) {
            currentNumber = number
            scale.snapTo(0.65f)
            scale.animateTo(
                targetValue = 1.3f,
                animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
            )
            delay(160L)
            scale.animateTo(targetValue = 0.65f, animationSpec = tween(durationMillis = 180))
        }
        finish()
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Приготовься!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(exerciseName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = currentNumber.toString(),
                fontSize = 116.sp,
                lineHeight = 124.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
            )
        }
    }
}
