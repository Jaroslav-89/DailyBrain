package com.jaroapps.dailybrain.feature.arithmetic.domain.model

data class ArithmeticTrainingConfig(
    val taskCount: Int = DEFAULT_TASK_COUNT,
) {
    init {
        require(taskCount > 0) {
            "Task count must be greater than zero"
        }
    }

    companion object {
        const val DEFAULT_TASK_COUNT = 100
    }
}