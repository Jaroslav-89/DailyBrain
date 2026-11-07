package com.jaroapps.dailybrain.feature.arithmetic.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.foundation.layout.size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.jaroapps.dailybrain.R
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTaskResult
import com.jaroapps.dailybrain.core.presentation.ExerciseExitConfirmationDialog
import org.koin.androidx.compose.koinViewModel

@Composable
fun ArithmeticScreen(
    onExit: () -> Unit,
    viewModel: ArithmeticViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitConfirmation by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppResumed()
                Lifecycle.Event.ON_STOP -> viewModel.onAppPaused()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            viewModel.onAppResumed()
        }
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.onAppPaused()
        }
    }

    LaunchedEffect(viewModel) {
        if (!state.isStarted && !state.isCompleted) {
            viewModel.startTraining()
        }
    }

    BackHandler {
        when {
            state.isReviewingAnswers -> viewModel.closeAnswerReview()
            !state.isCompleted -> showExitConfirmation = true
            state.isSaved -> {
                viewModel.cancelTraining()
                onExit()
            }
        }
    }

    if (state.isCompleted && state.isReviewingAnswers) {
        AnswerReviewContent(
            results = state.taskResults,
            onBack = viewModel::closeAnswerReview,
        )
    } else if (state.isCompleted) {
        ResultContent(
            state = state,
            onOpenAnswerReview = viewModel::openAnswerReview,
            onRetrySave = viewModel::retrySave,
            onFinish = {
                viewModel.cancelTraining()
                onExit()
            },
        )
    } else {
        TrainingContent(
            state = state,
            onExit = { showExitConfirmation = true },
            onDigitPressed = viewModel::onDigitPressed,
            onDeletePressed = viewModel::onDeletePressed,
            onSubmit = viewModel::submitAnswer,
        )
    }

    ExerciseExitConfirmationDialog(
        visible = showExitConfirmation,
        onDismiss = { showExitConfirmation = false },
        onConfirmExit = {
            showExitConfirmation = false
            viewModel.cancelTraining()
            onExit()
        },
    )
}

@Composable
private fun TrainingContent(
    state: ArithmeticUiState,
    onExit: () -> Unit,
    onDigitPressed: (Int) -> Unit,
    onDeletePressed: () -> Unit,
    onSubmit: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    if (isLandscape) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .padding(end = 16.dp),
            ) {
                ArithmeticTrainingHeader(state = state, onExit = onExit)
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth(),
                )
                ArithmeticProblemAndAnswer(
                    state = state,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )
            }
            NumberKeyboard(
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
                onDigitPressed = onDigitPressed,
                onDeletePressed = onDeletePressed,
                onSubmit = onSubmit,
                canSubmit = state.answerInput.isNotBlank(),
            )
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            ArithmeticTrainingHeader(state = state, onExit = onExit)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth(),
            )
            ArithmeticProblemAndAnswer(
                state = state,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            NumberKeyboard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                onDigitPressed = onDigitPressed,
                onDeletePressed = onDeletePressed,
                onSubmit = onSubmit,
                canSubmit = state.answerInput.isNotBlank(),
            )
        }
    }
}

@Composable
private fun ArithmeticTrainingHeader(
    state: ArithmeticUiState,
    onExit: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onExit) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.action_close),
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "${state.completedTaskCount + 1} / ${state.totalTaskCount}",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ArithmeticProblemAndAnswer(
    state: ArithmeticUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        val problem = state.currentProblem
        if (problem != null) {
            Text(
                text = "${problem.firstOperand} ${problem.operation.symbol} ${problem.secondOperand}",
                style = MaterialTheme.typography.displayLarge,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = state.answerInput.ifEmpty { " " },
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun NumberKeyboard(
    modifier: Modifier = Modifier,
    onDigitPressed: (Int) -> Unit,
    onDeletePressed: () -> Unit,
    onSubmit: () -> Unit,
    canSubmit: Boolean,
) {
    // A compact 3-column / 4-row keypad, following the proportions of the supplied
    // stopwatch layout. The confirmation action occupies the otherwise empty bottom-left cell.
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("submit", "0", "delete"),
    )
    Column(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                row.forEach { key ->
                    when (key) {
                        "delete" -> OutlinedButton(
                            onClick = onDeletePressed,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text("⌫", style = MaterialTheme.typography.headlineSmall)
                        }
                        "submit" -> Button(
                            onClick = onSubmit,
                            enabled = canSubmit,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text("✓", style = MaterialTheme.typography.headlineSmall)
                        }
                        else -> Button(
                            onClick = { onDigitPressed(key.toInt()) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(2.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(0.dp),
                        ) {
                            Text(key, style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultContent(
    state: ArithmeticUiState,
    onFinish: () -> Unit,
    onOpenAnswerReview: () -> Unit,
    onRetrySave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.status_training_completed),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.label_time),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = formatDuration(state.durationMs ?: 0L),
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.label_accuracy),
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = stringResource(R.string.format_percent, "%.1f".format(state.accuracyPercent ?: 0.0)),
            style = MaterialTheme.typography.headlineLarge,
        )

        Spacer(Modifier.height(24.dp))

        when {
            state.isSaving -> Text(
                text = stringResource(R.string.status_saving),
                style = MaterialTheme.typography.bodyMedium,
            )
            state.isSaved -> Text(
                text = stringResource(R.string.status_saved),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            state.saveErrorMessage != null -> {
                Text(
                    text = state.saveErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onRetrySave,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_retry_save))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedButton(
            onClick = onOpenAnswerReview,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_view_answers))
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onFinish,
            enabled = state.isSaved,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.action_finish))
        }
    }
}

@Composable
private fun AnswerReviewContent(
    results: List<ArithmeticTaskResult>,
    onBack: () -> Unit,
) {
    val green = colorResource(R.color.color_positive_amount)
    val red = colorResource(R.color.color_danger)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.action_close),
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = stringResource(R.string.label_results),
                style = MaterialTheme.typography.titleLarge,
            )
        }

        Spacer(Modifier.height(16.dp))


        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(
                items = results,
                key = { index, _ -> index },
            ) { _, result ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val problem = result.problem
                        val answerColor = if (result.isCorrect) {
                            green
                        } else {
                            red
                        }

                        Text(
                            text = "${problem.firstOperand} " +
                                    "${problem.operation.symbol} " +
                                    "${problem.secondOperand} = ${result.userAnswer}",
                            style = MaterialTheme.typography.titleMedium,
                            color = answerColor,
                        )

                        if (!result.isCorrect) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.label_correct_answer, result.correctAnswer),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1_000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}