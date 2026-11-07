package com.jaroapps.dailybrain.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
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
import com.jaroapps.dailybrain.feature.feed.presentation.FeedScreen
import com.jaroapps.dailybrain.feature.rewards.presentation.RewardHistoryScreen
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
import com.jaroapps.dailybrain.feature.profile.presentation.PrivacyPolicyScreen
import com.jaroapps.dailybrain.feature.profile.presentation.ProfileSelectionScreen
import com.jaroapps.dailybrain.feature.profile.domain.model.ProfileCollectibleKind
import com.jaroapps.dailybrain.feature.profile.presentation.ProfileCollectiblesScreen
import com.jaroapps.dailybrain.feature.profile.presentation.CollectibleDetailScreen
import com.jaroapps.dailybrain.feature.profile.presentation.PreferencesScreen
import com.jaroapps.dailybrain.feature.profile.presentation.FriendsScreen
import com.jaroapps.dailybrain.feature.profile.presentation.PublicProfileSyncViewModel
import com.jaroapps.dailybrain.feature.profile.domain.usecase.ObserveUserProfileUseCase
import com.jaroapps.dailybrain.ui.theme.AppThemePreference
import com.jaroapps.dailybrain.feature.home.presentation.HomeAppearancePreference
import com.jaroapps.dailybrain.feature.publicprofile.presentation.PublicUserProfileScreen
import com.jaroapps.dailybrain.feature.publicprofile.presentation.PublicUserConnectionsScreen
import com.jaroapps.dailybrain.feature.statistics.presentation.StatisticsScreen
import com.jaroapps.dailybrain.feature.shop.presentation.ShopScreen
import com.jaroapps.dailybrain.feature.stroop.presentation.StroopScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import java.time.format.DateTimeFormatter

@Composable
fun AppNavHost(startDestination: String = Route.STARTUP) {
    val navController = rememberNavController()
    val observeUserProfile: ObserveUserProfileUseCase = koinInject()
    val activeProfile by observeUserProfile().collectAsState(initial = null)
    val context = LocalContext.current
    LaunchedEffect(activeProfile?.localAccountId) {
        activeProfile?.localAccountId?.let { localAccountId ->
            AppThemePreference.ensureLoaded(context, localAccountId)
            HomeAppearancePreference.ensureLoaded(context, localAccountId)
        }
    }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val showBottomNavigation = currentRoute in setOf(
        Route.HOME,
        Route.STATISTICS,
        Route.FEED,
        Route.SHOP,
        Route.PROFILE,
    )
    val onboardingViewModel: com.jaroapps.dailybrain.feature.onboarding.presentation.OnboardingProgressViewModel = koinViewModel()
    @Suppress("UNUSED_VARIABLE")
    val publicProfileSyncViewModel: PublicProfileSyncViewModel = koinViewModel()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
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
                    onOpenRewardHistory = { currency -> navController.navigate(Route.rewardHistory(currency)) },
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
                    onBack = { navController.popBackStack() },
                    onContinue = {
                        if (navController.previousBackStackEntry?.destination?.route == Route.EXERCISE_RULES) {
                            navController.popBackStack()
                        } else {
                            onboardingViewModel.markExerciseIntroComplete(exerciseKey) {
                                navController.navigate(Route.exerciseRules(exerciseKey)) {
                                    popUpTo(Route.EXERCISE_INTRO) { inclusive = true }
                                    launchSingleTop = true
                                }
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
                    onOpenFullInfo = { navController.navigate(Route.exerciseIntro(exerciseKey)) },
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

            composable(Route.PROFILE_SELECTION) {
                ProfileSelectionScreen(
                    onProfileSelected = {
                        navController.navigate(Route.HOME) {
                            popUpTo(Route.PROFILE_SELECTION) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onProfileCreated = {
                        navController.navigate(Route.APP_ONBOARDING) {
                            popUpTo(Route.PROFILE_SELECTION) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Route.PROFILE) {
                ProfileScreen(
                    onOpenSettings = { navController.navigate(Route.PROFILE_SETTINGS) },
                    onEditAvatar = { navController.navigate(Route.AVATAR_EDITOR) },
                    onOpenFriends = { navController.navigate(Route.FRIENDS) },
                    onOpenUserProfile = { userId -> navController.navigate(Route.userProfile(userId)) },
                    onOpenCollectibles = { ownerUid, kind ->
                        navController.navigate(Route.profileCollectibles(ownerUid, kind.name))
                    },
                    onOpenCollectible = { ownerUid, kind, itemId ->
                        navController.navigate(Route.collectibleDetail(ownerUid, kind.name, itemId))
                    },
                )
            }

            composable(Route.PROFILE_SETTINGS) {
                ProfileSettingsScreen(
                    onBack = { navController.popBackStack() },
                    onEditProfile = { navController.navigate(Route.EDIT_PROFILE) },
                    onOpenParameters = { navController.navigate(Route.PARAMETERS) },
                    onOpenPrivacyPolicy = { navController.navigate(Route.PRIVACY_POLICY) },
                    onExitProfile = {
                        navController.navigate(Route.PROFILE_SELECTION) {
                            popUpTo(Route.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Route.PARAMETERS) {
                PreferencesScreen(onBack = { navController.popBackStack() })
            }

            composable(Route.PRIVACY_POLICY) {
                PrivacyPolicyScreen(onBack = { navController.popBackStack() })
            }

            composable(Route.FRIENDS) {
                FriendsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenUserProfile = { userId -> navController.navigate(Route.userProfile(userId)) },
                )
            }

            composable(
                route = Route.USER_PROFILE,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { entry ->
                val userId = entry.arguments?.getString("userId").orEmpty()
                PublicUserProfileScreen(
                    profileFirebaseUid = userId,
                    onBack = { navController.popBackStack() },
                    onOpenConnections = { profileUid, connectionType ->
                        navController.navigate(Route.userProfileConnections(profileUid, connectionType))
                    },
                    onOpenCollectibles = { ownerUid, kind ->
                        navController.navigate(Route.profileCollectibles(ownerUid, kind.name))
                    },
                    onOpenCollectible = { ownerUid, kind, itemId ->
                        navController.navigate(Route.collectibleDetail(ownerUid, kind.name, itemId))
                    },
                )
            }

            composable(
                route = Route.USER_PROFILE_CONNECTIONS,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("connectionType") { type = NavType.StringType },
                ),
            ) { entry ->
                val userId = entry.arguments?.getString("userId").orEmpty()
                val connectionType = entry.arguments?.getString("connectionType").orEmpty()
                PublicUserConnectionsScreen(
                    profileFirebaseUid = userId,
                    connectionType = connectionType,
                    onBack = { navController.popBackStack() },
                    onOpenUser = { nestedUserId -> navController.navigate(Route.userProfile(nestedUserId)) },
                )
            }

            composable(
                route = Route.PROFILE_COLLECTIBLES,
                arguments = listOf(
                    navArgument("ownerUid") { type = NavType.StringType },
                    navArgument("kind") { type = NavType.StringType },
                ),
            ) { entry ->
                ProfileCollectiblesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenItem = { ownerUid, kind, itemId ->
                        navController.navigate(Route.collectibleDetail(ownerUid, kind.name, itemId))
                    },
                )
            }

            composable(
                route = Route.COLLECTIBLE_DETAIL,
                arguments = listOf(
                    navArgument("ownerUid") { type = NavType.StringType },
                    navArgument("kind") { type = NavType.StringType },
                    navArgument("itemId") { type = NavType.StringType },
                ),
            ) {
                CollectibleDetailScreen(onBack = { navController.popBackStack() })
            }

            composable(Route.EDIT_PROFILE) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onEditAvatar = { navController.navigate(Route.AVATAR_EDITOR) },
                    onProfileDeleted = {
                        navController.navigate(Route.PROFILE_SELECTION) {
                            popUpTo(Route.HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                )
            }

            composable(Route.AVATAR_EDITOR) {
                AvatarEditorScreen(onBack = { navController.popBackStack() })
            }

            composable(
                route = Route.REWARD_HISTORY,
                arguments = listOf(navArgument("currency") { type = NavType.StringType }),
            ) {
                RewardHistoryScreen(onBack = { navController.popBackStack() })
            }

            composable(Route.STATISTICS) {
                StatisticsScreen()
            }

            composable(Route.FEED) {
                FeedScreen(
                    onOpenUserProfile = { userId ->
                        navController.navigate(Route.userProfile(userId))
                    },
                    onOpenCollectible = { ownerUid, kind, itemId ->
                        navController.navigate(Route.collectibleDetail(ownerUid, kind.name, itemId))
                    },
                )
            }

            composable(Route.SHOP) {
                ShopScreen()
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
            route = Route.FEED,
            label = stringResource(R.string.feed_title),
            icon = Icons.Filled.Newspaper,
        ),
        BottomNavigationDestination(
            route = Route.SHOP,
            label = stringResource(R.string.shop_title),
            icon = Icons.Filled.ShoppingBag,
        ),
        BottomNavigationDestination(
            route = Route.PROFILE,
            label = stringResource(R.string.profile_title),
            icon = Icons.Filled.Person,
        ),
    )

    Surface(
        modifier = Modifier.navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                val selected = currentRoute == destination.route
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = tween(
                        durationMillis = 180,
                        easing = FastOutSlowInEasing,
                    ),
                    label = "bottomNavIconScale",
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable {
                            if (!selected) onDestinationSelected(destination.route)
                        }
                        .semantics {
                            role = Role.Tab
                            contentDescription = destination.label
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 40.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.secondaryContainer
                                else Color.Transparent,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = null,
                            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                },
                        )
                    }
                }
            }
        }
    }
}
