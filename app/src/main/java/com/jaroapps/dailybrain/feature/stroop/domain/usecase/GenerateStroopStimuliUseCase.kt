package com.jaroapps.dailybrain.feature.stroop.domain.usecase

import com.jaroapps.dailybrain.feature.stroop.domain.model.StroopColor
import com.jaroapps.dailybrain.feature.stroop.domain.model.StroopStimulus
import kotlin.math.roundToInt
import kotlin.random.Random

class GenerateStroopStimuliUseCase {

    companion object {
        const val DEFAULT_STIMULI_COUNT = 40
        const val DEFAULT_CONGRUENT_PERCENT = 20
    }

    operator fun invoke(
        count: Int = DEFAULT_STIMULI_COUNT,
        congruentPercent: Int = DEFAULT_CONGRUENT_PERCENT,
    ): List<StroopStimulus> {
        require(count > 0) {
            "Количество стимулов должно быть больше нуля"
        }
        require(congruentPercent in 0..100) {
            "Доля конгруэнтных стимулов должна быть от 0 до 100"
        }

        val colors = StroopColor.entries
        val congruentCount =
            (count * congruentPercent / 100.0).roundToInt()

        // Балансируем цвета шрифта: количество каждого цвета
        // отличается максимум на один стимул.
        val inkColors = buildList {
            repeat(count / colors.size) {
                addAll(colors)
            }
            addAll(colors.shuffled().take(count % colors.size))
        }.shuffled()

        // Выбираем ровно заданное количество конгруэнтных стимулов.
        val congruentIndexes = (0 until count)
            .shuffled()
            .take(congruentCount)
            .toSet()

        val stimuli = inkColors.mapIndexed { index, inkColor ->
            val isCongruent = index in congruentIndexes

            val wordColor = if (isCongruent) {
                inkColor
            } else {
                colors.filterNot { it == inkColor }.random()
            }

            StroopStimulus(
                word = wordColor.displayName,
                inkColor = inkColor,
                correctAnswer = inkColor,
                isCongruent = wordColor == inkColor,
            )
        }

        return shuffleAvoidingLongRuns(stimuli)
    }

    private fun shuffleAvoidingLongRuns(
        stimuli: List<StroopStimulus>,
    ): List<StroopStimulus> {
        // Пытаемся избежать трёх и более одинаковых цветов подряд.
        repeat(200) {
            val shuffled = stimuli.shuffled()

            if (hasNoLongColorRuns(shuffled)) {
                return shuffled
            }
        }

        // Для нестандартных параметров сохраняем корректный набор,
        // даже если ограничение на повторы выполнить не удалось.
        return stimuli.shuffled()
    }

    private fun hasNoLongColorRuns(
        stimuli: List<StroopStimulus>,
    ): Boolean {
        var previousColor: StroopColor? = null
        var consecutiveCount = 0

        for (stimulus in stimuli) {
            if (stimulus.inkColor == previousColor) {
                consecutiveCount++
            } else {
                previousColor = stimulus.inkColor
                consecutiveCount = 1
            }

            if (consecutiveCount > 3) {
                return false
            }
        }

        return true
    }
}