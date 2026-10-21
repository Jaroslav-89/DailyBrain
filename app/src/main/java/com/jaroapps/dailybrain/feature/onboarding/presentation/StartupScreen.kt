package com.jaroapps.dailybrain.feature.onboarding.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.stringResource
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
        when (state) {
            StartupUiState.Loading -> {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.startup_loading))
            }
            StartupUiState.Error -> {
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
