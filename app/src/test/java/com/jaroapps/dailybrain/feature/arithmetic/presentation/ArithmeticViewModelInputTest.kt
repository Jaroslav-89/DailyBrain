package com.jaroapps.dailybrain.feature.arithmetic.presentation

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.common.domain.usecase.SaveCompletedTrainingUseCase
import com.jaroapps.dailybrain.core.resources.StringProvider
import com.jaroapps.dailybrain.feature.arithmetic.domain.model.ArithmeticTrainingConfig
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.CheckArithmeticAnswerUseCase
import com.jaroapps.dailybrain.feature.arithmetic.domain.usecase.GenerateArithmeticProblemUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArithmeticViewModelInputTest {

    @Test
    fun `input is ignored before training and invalid digits are rejected`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = createViewModel()
            viewModel.onDigitPressed(7)
            assertEquals("", viewModel.uiState.value.answerInput)

            viewModel.startTraining(ArithmeticTrainingConfig(taskCount = 2))
            viewModel.onDigitPressed(-1)
            viewModel.onDigitPressed(10)

            assertEquals("", viewModel.uiState.value.answerInput)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `answer input is limited to two digits and can be deleted`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = createViewModel()
            viewModel.startTraining(ArithmeticTrainingConfig(taskCount = 2))

            viewModel.onDigitPressed(1)
            viewModel.onDigitPressed(2)
            viewModel.onDigitPressed(3)
            assertEquals("12", viewModel.uiState.value.answerInput)

            viewModel.onDeletePressed()
            assertEquals("1", viewModel.uiState.value.answerInput)
            viewModel.onDeletePressed()
            viewModel.onDeletePressed()
            assertEquals("", viewModel.uiState.value.answerInput)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `submitting empty input does not complete a task`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = createViewModel()
            viewModel.startTraining(ArithmeticTrainingConfig(taskCount = 2))

            viewModel.submitAnswer()

            assertEquals(0, viewModel.uiState.value.completedTaskCount)
            assertTrue(viewModel.uiState.value.taskResults.isEmpty())
            assertFalse(viewModel.uiState.value.isCompleted)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `submitting an answer records result and advances to the next task`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = createViewModel()
            viewModel.startTraining(ArithmeticTrainingConfig(taskCount = 2))
            viewModel.onDigitPressed(4)

            viewModel.submitAnswer()

            val state = viewModel.uiState.value
            assertEquals(1, state.completedTaskCount)
            assertEquals(1, state.taskResults.size)
            assertEquals(0.5f, state.progress, 0f)
            assertEquals("", state.answerInput)
            assertFalse(state.isCompleted)
            assertTrue(state.currentProblem != null)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(): ArithmeticViewModel = ArithmeticViewModel(
        generateProblem = GenerateArithmeticProblemUseCase(),
        checkAnswer = CheckArithmeticAnswerUseCase(),
        saveCompletedTraining = SaveCompletedTrainingUseCase(NoOpCompletedTrainingRepository()),
        strings = StringProvider { resourceId -> "localized:$resourceId" },
    )

    private class NoOpCompletedTrainingRepository : CompletedTrainingRepository {
        override suspend fun saveCompletedTraining(session: TrainingSession) = Unit
        override fun observeAllCompletedTrainings(): Flow<List<TrainingSession>> = emptyFlow()
        override fun observeCompletedTrainingsByDate(date: Int): Flow<List<TrainingSession>> = emptyFlow()
        override fun observeActiveTrainingDates(): Flow<Set<Int>> = emptyFlow()
        override suspend fun deleteCompletedTraining(id: Long): Boolean = false
        override suspend fun recalculateDailyExerciseResult(date: Int) = Unit
        override fun observeTrainingDaysBetween(startDate: Int, endDate: Int): Flow<Set<Int>> = emptyFlow()
        override fun observeTotalTrainingCount(): Flow<Int> = emptyFlow()
    }
}
