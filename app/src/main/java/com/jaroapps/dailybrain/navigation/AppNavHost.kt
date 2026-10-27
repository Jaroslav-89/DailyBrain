package com.jaroapps.dailybrain.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jaroapps.dailybrain.R
import com.jaroapps.dailybrain.feature.arithmetic.presentation.ArithmeticScreen
import com.jaroapps.dailybrain.feature.avatar.presentation.AvatarEditorScreen
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
import com.jaroapps.dailybrain.feature.profile.presentation.ProfileSettingsScreen
import com.jaroapps.dailybrain.feature.profile.presentation.PreferencesScreen
import com.jaroapps.dailybrain.feature.profile.presentation.FriendsScreen
import com.jaroapps.dailybrain.feature.statistics.presentation.StatisticsScreen
import com.jaroapps.dailybrain.feature.stroop.presentation.StroopScreen
import org.koin.androidx.compose.koinViewModel
import java.time.format.DateTimeFormatter

@Composable
fun AppNavHost(startDestination: String = Route.STARTUP) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomNavigation = currentRoute in setOf(
        Route.HOME,
        Route.STATISTICS,
        Route.PROFILE,
    )
    val onboardingViewModel: com.jaroapps.dailybrain.feature.onboarding.presentation.OnboardingProgressViewModel = koinViewModel()

    val restartStartup: () -> Unit = {
        navController.navigate(Route.STARTUP) {
            popUpTo(Route.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomNavigation) {
                AppBottomNavigation(
                    currentRoute = currentRoute,
                    onDestinationSelected = { route ->
                        navController.navigate(route) {
                            popUpTo(Route.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Route.STARTUP) {
                StartupScreen(
                    onDestinationReady = { destination ->
                        navController.navigate(destination) {
                            popUpTo(Route.STARTUP) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onStartupResolved = {},
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
                    onStartMemoryTraining = { navController.navigate(Route.exerciseEntry("memory")) },
                    onStartStroopTest = { navController.navigate(Route.exerciseEntry("stroop")) },
                    onStartCountingTraining = { navController.navigate(Route.exerciseEntry("counting")) },
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
                    onOpenSettings = { navController.navigate(Route.PROFILE_SETTINGS) },
                    onEditAvatar = { navController.navigate(Route.AVATAR_EDITOR) },
                    onOpenFriends = { navController.navigate(Route.FRIENDS) },
                    onActiveProfileChanged = restartStartup,
                )
            }

            composable(Route.PROFILE_SETTINGS) {
                ProfileSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onEditProfile = { navController.navigate(Route.EDIT_PROFILE) },
                    onOpenParameters = { navController.navigate(Route.PARAMETERS) },
                )
            }

            composable(Route.PARAMETERS) {
                PreferencesScreen(onBack = { navController.popBackStack() })
            }

            composable(Route.FRIENDS) {
                FriendsScreen(onBack = { navController.popBackStack() })
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
                StatisticsScreen()
            }

            composable(Route.CALENDAR) {
                CalendarScreen(
                    onBack = { navController.popBackStack() },
                    onDateSelected = { date ->
                        val dateKey = date.format(DateTimeFormatter.BASIC_ISO_DATE).toInt()
                        navController.navigate(Route.dayHistory(dateKey))
                    },
                    onStartExercise = {
                        navController.navigate(Route.exerciseEntry("arithmetic"))
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
}


private data class BottomNavigationDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
private fun AppBottomNavigation(
    currentRoute: String?,
    onDestinationSelected: (String) -> Unit,
) {
    val destinations = listOf(
        BottomNavigationDestination(
            route = Route.HOME,
            label = stringResource(R.string.action_home),
            icon = Icons.Filled.Home,
        ),
        BottomNavigationDestination(
            route = Route.STATISTICS,
            label = stringResource(R.string.statistics_title),
            icon = Icons.Filled.BarChart,
        ),
        BottomNavigationDestination(
            route = Route.PROFILE,
            label = stringResource(R.string.profile_title),
            icon = Icons.Filled.Person,
        ),
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFFD6D6D6)),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                Color.Transparent
                            },
                        )
                        .clickable {
                            if (!selected) onDestinationSelected(destination.route)
                        }
                        .semantics {
                            contentDescription = destination.label
                            role = Role.Button
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        modifier = Modifier.size(19.dp),
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        }

        // Reserve a small bottom area for system navigation gestures.
        Spacer(Modifier.height(2.dp))
    }
}
