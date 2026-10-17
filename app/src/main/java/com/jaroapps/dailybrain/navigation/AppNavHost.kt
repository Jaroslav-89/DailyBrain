package com.jaroapps.dailybrain.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jaroapps.dailybrain.feature.arithmetic.presentation.ArithmeticScreen
import com.jaroapps.dailybrain.feature.history.presentation.CalendarScreen
import com.jaroapps.dailybrain.feature.home.presentation.HomeScreen
import com.jaroapps.dailybrain.feature.statistics.presentation.StatisticsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.jaroapps.dailybrain.feature.history.presentation.DayHistoryScreen
import com.jaroapps.dailybrain.feature.memory.presentation.MemoryTrainingScreen
import com.jaroapps.dailybrain.feature.stroop.presentation.StroopScreen
import java.time.format.DateTimeFormatter

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.HOME,
    ) {
        composable(Route.HOME) {
            HomeScreen(
                onOpenCalendar = {
                    navController.navigate(Route.CALENDAR)
                },
                onStartTraining = {
                    navController.navigate(Route.ARITHMETIC)
                },
                onOpenStatistics = {
                    navController.navigate(Route.STATISTICS)
                },
                onStartMemoryTraining = {
                    navController.navigate(Route.MEMORY_TRAINING)
                },
                onStartStroopTest = {
                    navController.navigate(Route.STROOP_TEST)
                },
            )
        }

        composable(Route.ARITHMETIC) {
            ArithmeticScreen(
                onExit = {
                    navController.popBackStack()
                },
            )
        }

        composable(Route.STATISTICS) {
            StatisticsScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Route.CALENDAR) {
            CalendarScreen(
                onBack = {
                    navController.popBackStack()
                },
                onDateSelected = { date ->
                    val dateKey = date
                        .format(DateTimeFormatter.BASIC_ISO_DATE)
                        .toInt()

                    navController.navigate(Route.dayHistory(dateKey))
                },
            )
        }

        composable(
            route = Route.DAY_HISTORY,
            arguments = listOf(
                navArgument("date") {
                    type = NavType.IntType
                }
            ),
        ) {
            DayHistoryScreen(
                onBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(Route.MEMORY_TRAINING) {
            MemoryTrainingScreen(
                onExit = {
                    navController.popBackStack()
                }
            )
        }

        composable(Route.STROOP_TEST) {
            StroopScreen(
                onExit = {
                    navController.popBackStack()
                }
            )
        }
    }
}