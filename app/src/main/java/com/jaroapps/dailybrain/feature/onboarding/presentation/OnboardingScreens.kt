package com.jaroapps.dailybrain.feature.onboarding.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.androidx.compose.koinViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jaroapps.dailybrain.R

@Composable
fun ExerciseEntryScreen(
    exerciseKey: String,
    onDestinationReady: (String) -> Unit,
    viewModel: ExerciseEntryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(exerciseKey) {
        viewModel.resolve(exerciseKey)
    }

    LaunchedEffect(state) {
        if (state is ExerciseEntryUiState.DestinationReady) {
            onDestinationReady((state as ExerciseEntryUiState.DestinationReady).route)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (state) {
            ExerciseEntryUiState.Loading -> CircularProgressIndicator()
            ExerciseEntryUiState.Error -> {
                Text(stringResource(R.string.startup_error), style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.resolve(exerciseKey) }) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            is ExerciseEntryUiState.DestinationReady -> Unit
        }
    }
}

@Composable
fun AppOnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_app_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.onboarding_app_body),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_get_started))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BrainTrainingArticleScreen(onClose: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.article_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_close),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
            )
        },
        bottomBar = {
            Button(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(stringResource(R.string.action_close))
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            ArticleParagraph(R.string.article_intro)
            ArticleParagraph(R.string.article_paragraph_1)
            ArticleParagraph(R.string.article_paragraph_2)
            ArticleParagraph(R.string.article_paragraph_3)
            ArticleParagraph(R.string.article_paragraph_4)
            ArticleParagraph(R.string.article_paragraph_5)
            ArticleParagraph(R.string.article_paragraph_6)
        }
    }
}

@Composable
private fun ArticleParagraph(textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 16.dp),
    )
}

@Composable
fun ExerciseIntroScreen(
    exerciseKey: String,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val copy = exerciseCopy(exerciseKey)
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(copy.title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            Text(text = stringResource(copy.intro), style = MaterialTheme.typography.bodyLarge)
        }
        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_get_started))
        }
    }
}

@Composable
fun ExerciseRulesScreen(
    exerciseKey: String,
    onStart: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val copy = exerciseCopy(exerciseKey)
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(copy.title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(20.dp))
            Text(text = stringResource(copy.rules), style = MaterialTheme.typography.bodyLarge)
        }
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_start_exercise))
        }
        Spacer(Modifier.height(8.dp))
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_close),
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

private data class ExerciseCopy(val title: Int, val intro: Int, val rules: Int)

private fun exerciseCopy(key: String): ExerciseCopy = when (key) {
    "arithmetic" -> ExerciseCopy(
        R.string.exercise_arithmetic,
        R.string.intro_arithmetic,
        R.string.rules_arithmetic,
    )
    "memory" -> ExerciseCopy(
        R.string.exercise_memory,
        R.string.intro_memory,
        R.string.rules_memory,
    )
    "counting" -> ExerciseCopy(
        R.string.exercise_counting,
        R.string.intro_counting,
        R.string.rules_counting,
    )
    "stroop" -> ExerciseCopy(
        R.string.exercise_stroop,
        R.string.intro_stroop,
        R.string.rules_stroop,
    )
    else -> ExerciseCopy(
        R.string.exercise_arithmetic,
        R.string.intro_arithmetic,
        R.string.rules_arithmetic,
    )
}
