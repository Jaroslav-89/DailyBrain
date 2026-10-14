package com.jaroapps.dailybrain.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jaroapps.dailybrain.feature.home.presentation.HomeScreen

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
                    // Подключим экран тренировки позже.
                },
            )
        }
    }
}