package com.jaroapps.dailybrain.navigation

object Route {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val STATISTICS = "statistics"
    const val ARITHMETIC = "arithmetic"
    const val CALENDAR = "calendar"
    const val DAY_HISTORY = "day_history/{date}"
    const val MEMORY_TRAINING = "memory_training"
    const val STROOP_TEST = "stroop_test"
    const val COUNTING_TRAINING = "counting_training"
    const val COUNTING_STATISTICS = "counting_statistics"

    fun dayHistory(date: Int): String = "day_history/$date"
}