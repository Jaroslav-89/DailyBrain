package com.jaroapps.dailybrain.feature.arithmetic.presentation

import com.jaroapps.dailybrain.common.domain.model.TrainingSession
import com.jaroapps.dailybrain.common.domain.repository.CompletedTrainingRepository
import com.jaroapps.dailybrain.common.domain.usecase.SaveCompletedTrainingUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ArithmeticViewModelTest {

    @Test
    fun `failed save can be retried without rebuilding the session`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FailOnceCompletedTrainingRepository()
            val viewModel = ArithmeticViewModel(
                generateProblem = GenerateArithmeticProblemUseCase(),
                checkAnswer = CheckArithmeticAnswerUseCase(),
                saveCompletedTraining = SaveCompletedTrainingUseCase(repository),
            )

            viewModel.startTraining(ArithmeticTrainingConfig(taskCount = 1))
            val correctAnswer = viewModel.uiState.value.currentProblem!!.correctAnswer
            correctAnswer.toString().forEach { digit ->
                viewModel.onDigitPressed(digit.digitToInt())
            }
            viewModel.submitAnswer()

            assertTrue(viewModel.uiState.value.isCompleted)
            assertTrue(viewModel.uiState.value.isSaving)
            runCurrent()

            assertFalse(viewModel.uiState.value.isSaved)
            assertNotNull(viewModel.uiState.value.saveErrorMessage)
            assertEquals(1, repository.calls)
            val firstAttempt = repository.savedSessions.single()

            viewModel.retrySave()
            viewModel.retrySave()
            runCurrent()

            assertTrue(viewModel.uiState.value.isSaved)
            assertFalse(viewModel.uiState.value.isSaving)
            assertEquals(null, viewModel.uiState.value.saveErrorMessage)
            assertEquals(2, repository.calls)
            assertEquals(firstAttempt, repository.savedSessions.last())
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FailOnceCompletedTrainingRepository : CompletedTrainingRepository {
        var calls: Int = 0
            private set
        val savedSessions = mutableListOf<TrainingSession>()

        override suspend fun saveCompletedTraining(session: TrainingSession) {
            calls += 1
            savedSessions += session
            if (calls == 1) throw IOException("Temporary database error")
        }

        override fun observeAllCompletedTrainings(): Flow<List<TrainingSession>> = emptyFlow()

        override fun observeCompletedTrainingsByDate(date: Int): Flow<List<TrainingSession>> = emptyFlow()

        override fun observeActiveTrainingDates(): Flow<Set<Int>> = emptyFlow()
    }
}
