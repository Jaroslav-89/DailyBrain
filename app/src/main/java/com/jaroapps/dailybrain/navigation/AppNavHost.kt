package com.jaroapps.dailybrain.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jaroapps.dailybrain.feature.arithmetic.presentation.ArithmeticScreen
import com.jaroapps.dailybrain.feature.home.presentation.HomeScreen
import com.jaroapps.dailybrain.feature.statistics.presentation.StatisticsScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.HOME,
    ) {
        composable(Route.HOME) {
            HomeScreen(
                onStartTraining = {
                    navController.navigate(Route.ARITHMETIC)
                },
                onOpenStatistics = {
                    navController.navigate(Route.STATISTICS)
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
    }
}