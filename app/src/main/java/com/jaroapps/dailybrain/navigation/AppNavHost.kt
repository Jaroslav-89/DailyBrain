package com.jaroapps.dailybrain.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarEditorScreen
import com.jaroapps.dailybrain.feature.arithmetic.presentation.ArithmeticScreen
import com.jaroapps.dailybrain.feature.counting.presentation.CountingTrainingScreen
import com.jaroapps.dailybrain.feature.history.presentation.CalendarScreen
import com.jaroapps.dailybrain.feature.history.presentation.DayHistoryScreen
import com.jaroapps.dailybrain.feature.home.presentation.HomeScreen
import com.jaroapps.dailybrain.feature.memory.presentation.MemoryTrainingScreen
import com.jaroapps.dailybrain.feature.onboarding.presentation.AppOnboardingScreen
import com.jaroapps.dailybrain.feature.onboarding.presentation.BrainTrainingArticleScreen
import com.jaroapps.dailybrain.feature.onboarding.presentation.ExerciseEntryScreen
import com.jaroapps.dailybrain.feature.onboarding.presentation.ExerciseIntroScreen
import com.jaroapps.dailybrain.feature.onboarding.presentation.ExerciseRulesScreen
import com.jaroapps.dailybrain.feature.onboarding.presentation.StartupScreen
import com.jaroapps.dailybrain.feature.profile.presentation.EditProfileScreen
import com.jaroapps.dailybrain.feature.profile.presentation.ProfileScreen
import com.jaroapps.dailybrain.feature.statistics.presentation.StatisticsScreen
import com.jaroapps.dailybrain.feature.stroop.presentation.StroopScreen
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter

@Composable
fun AppNavHost(startDestination: String = Route.STARTUP) {
    val navController = rememberNavController()
    val onboardingViewModel: com.jaroapps.dailybrain.feature.onboarding.presentation.OnboardingProgressViewModel = koinViewModel()

    val restartStartup: () -> Unit = {
        navController.navigate(Route.STARTUP) {
            popUpTo(Route.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(Route.STARTUP) {
            StartupScreen(
                onDestinationReady = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Route.STARTUP) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onStartupResolved = onStartupResolved,
            )
        }

        composable(Route.APP_ONBOARDING) {
            AppOnboardingScreen(
                onContinue = {
                    onboardingViewModel.markAppIntroComplete {
                        navController.navigate(Route.HOME) {
                            popUpTo(Route.APP_ONBOARDING) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(Route.BRAIN_ARTICLE) {
            BrainTrainingArticleScreen(onClose = { navController.popBackStack() })
        }

        composable(Route.HOME) {
            HomeScreen(
                onOpenCalendar = { navController.navigate(Route.CALENDAR) },
                onStartTraining = { navController.navigate(Route.exerciseEntry("arithmetic")) },
                onOpenProfile = { navController.navigate(Route.PROFILE) },
                onOpenStatistics = { navController.navigate(Route.STATISTICS) },
                onStartMemoryTraining = { navController.navigate(Route.exerciseEntry("memory")) },
                onStartStroopTest = { navController.navigate(Route.exerciseEntry("stroop")) },
                onStartCountingTraining = { navController.navigate(Route.exerciseEntry("counting")) },
                onOpenArticle = { navController.navigate(Route.BRAIN_ARTICLE) },
            )
        }

        composable(
            route = Route.EXERCISE_ENTRY,
            arguments = listOf(navArgument("exercise") { type = NavType.StringType }),
        ) { entry ->
            val exerciseKey = entry.arguments?.getString("exercise") ?: "arithmetic"
            ExerciseEntryScreen(
                exerciseKey = exerciseKey,
                onDestinationReady = { destination ->
                    navController.navigate(destination) {
                        popUpTo(Route.EXERCISE_ENTRY) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = Route.EXERCISE_INTRO,
            arguments = listOf(navArgument("exercise") { type = NavType.StringType }),
        ) { entry ->
            val exerciseKey = entry.arguments?.getString("exercise") ?: "arithmetic"
            ExerciseIntroScreen(
                exerciseKey = exerciseKey,
                onBack = { navController.popBackStack(Route.HOME, false) },
                onContinue = {
                    onboardingViewModel.markExerciseIntroComplete(exerciseKey) {
                        navController.navigate(Route.exerciseRules(exerciseKey)) {
                            popUpTo(Route.EXERCISE_INTRO) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                },
            )
        }

        composable(
            route = Route.EXERCISE_RULES,
            arguments = listOf(navArgument("exercise") { type = NavType.StringType }),
        ) { entry ->
            val exerciseKey = entry.arguments?.getString("exercise") ?: "arithmetic"
            ExerciseRulesScreen(
                exerciseKey = exerciseKey,
                onBack = { navController.popBackStack(Route.HOME, false) },
                onStart = {
                    val destination = when (exerciseKey) {
                        "memory" -> Route.MEMORY_TRAINING
                        "counting" -> Route.COUNTING_TRAINING
                        "stroop" -> Route.STROOP_TEST
                        else -> Route.ARITHMETIC
                    }
                    navController.navigate(destination) {
                        popUpTo(Route.EXERCISE_RULES) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Route.ARITHMETIC) {
            ArithmeticScreen(onExit = { navController.popBackStack() })
        }

        composable(Route.PROFILE) {
            ProfileScreen(
                onBack = restartStartup,
                onEditProfile = { navController.navigate(Route.EDIT_PROFILE) },
                onActiveProfileChanged = restartStartup,
            )
        }

        composable(Route.EDIT_PROFILE) {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onEditAvatar = { navController.navigate(Route.AVATAR_EDITOR) },
            )
        }

        composable(Route.AVATAR_EDITOR) {
            AvatarEditorScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.STATISTICS) {
            StatisticsScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.CALENDAR) {
            CalendarScreen(
                onBack = { navController.popBackStack() },
                onDateSelected = { date ->
                    val dateKey = date.format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
                    navController.navigate(Route.dayHistory(dateKey))
                },
            )
        }

        composable(
            route = Route.DAY_HISTORY,
            arguments = listOf(navArgument("date") { type = NavType.IntType }),
        ) {
            DayHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.MEMORY_TRAINING) {
            MemoryTrainingScreen(onExit = { navController.popBackStack() })
        }

        composable(Route.STROOP_TEST) {
            StroopScreen(onExit = { navController.popBackStack() })
        }

        composable(Route.COUNTING_TRAINING) {
            CountingTrainingScreen(
                onExit = {
                    navController.popBackStack(route = Route.HOME, inclusive = false)
                },
            )
        }
    }
}
