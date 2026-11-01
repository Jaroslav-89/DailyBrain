package com.jaroapps.dailybrain.feature.onboarding.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jaroapps.dailybrain.R
import org.koin.androidx.compose.koinViewModel

@Composable
fun StartupScreen(
    onDestinationReady: (String) -> Unit,
    onStartupResolved: () -> Unit,
    viewModel: StartupViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state) {
        when (state) {
            StartupUiState.AppIntroRequired -> {
                onStartupResolved()
                onDestinationReady("app_onboarding")
            }
            StartupUiState.HomeReady -> {
                onStartupResolved()
                onDestinationReady("home")
            }
            StartupUiState.Loading -> Unit
            StartupUiState.Error -> onStartupResolved()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.brain_base),
            contentDescription = null,
            modifier = Modifier.size(112.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1F2937),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.splash_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF64748B),
        )
        when (state) {
            StartupUiState.Loading -> {
                Spacer(Modifier.height(28.dp))
                CircularProgressIndicator()
            }
            StartupUiState.Error -> {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.startup_error),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = viewModel::resolveStartup) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            else -> Unit
        }
    }
}
