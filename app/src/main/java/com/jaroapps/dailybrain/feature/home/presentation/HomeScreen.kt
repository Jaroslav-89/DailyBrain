package com.jaroapps.dailybrain.feature.home.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    onStartTraining: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Daily Brain",
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Твоя ежедневная тренировка мозга",
            style = MaterialTheme.typography.bodyLarge,
        )

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onStartTraining,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Начать тренировку")
        }
    }
}