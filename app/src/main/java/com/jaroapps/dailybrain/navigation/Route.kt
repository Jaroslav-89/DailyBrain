package com.jaroapps.dailybrain.navigation

object Route {
    const val HOME = "home"
    const val PROFILE = "profile"
    const val STATISTICS = "statistics"
    const val ARITHMETIC = "arithmetic"
    const val CALENDAR = "calendar"
    const val DAY_HISTORY = "day_history/{date}"

    fun dayHistory(date: Int): String = "day_history/$date"
}