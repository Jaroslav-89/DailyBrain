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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.onboarding.data.OnboardingProgressRepository
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.first

@Composable
fun StartupScreen(
    onboardingProgressRepository: OnboardingProgressRepository,
    userProfileRepository: UserProfileRepository,
    onDestinationReady: (String) -> Unit,
    onStartupResolved: () -> Unit,
) {
    var retryToken by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }

    LaunchedEffect(retryToken) {
        isLoading = true
        hasError = false
        try {
            // Wait until profile initialization has produced a real active profile.
            userProfileRepository.observeProfile().first()
            val progress = onboardingProgressRepository.getActiveProgress()
            onDestinationReady(if (progress.hasSeenAppIntro) "home" else "app_onboarding")
        } catch (_: Exception) {
            hasError = true
        } finally {
            isLoading = false
            onStartupResolved()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.startup_loading))
        } else if (hasError) {
            Text(
                text = stringResource(R.string.startup_error),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = { retryToken++ }) {
                Text(stringResource(R.string.action_retry))
            }
        }
    }
}
