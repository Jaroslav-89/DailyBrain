package com.jaroapps.dailybrain

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.drawable.ColorDrawable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.jaroapps.dailybrain.feature.brain.BrainStateManager
import com.jaroapps.dailybrain.core.database.AppDatabase
import com.jaroapps.dailybrain.feature.profile.data.auth.GoogleSignInClient
import com.jaroapps.dailybrain.feature.onboarding.domain.usecase.GetStartupDestinationUseCase
import com.jaroapps.dailybrain.feature.onboarding.domain.usecase.StartupDestination
import com.jaroapps.dailybrain.navigation.AppNavHost
import com.jaroapps.dailybrain.navigation.Route
import com.jaroapps.dailybrain.ui.theme.DailyBrainTheme
import com.jaroapps.dailybrain.ui.theme.AppThemePreference
import com.jaroapps.dailybrain.feature.home.presentation.HomeAppearancePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {

    @Volatile private var startupResolved = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                GlobalContext.get().get<BrainStateManager>().refresh()
            } catch (_: Exception) {
                // Mood refresh must not prevent the app from opening.
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !startupResolved }
        super.onCreate(savedInstanceState)
        GlobalContext.get().get<GoogleSignInClient>().attachActivity(this)

        lifecycleScope.launch {
            val database = runCatching { GlobalContext.get().get<AppDatabase>() }.getOrNull()
            val selectedLocalAccountId = runCatching {
                database?.appSettingsDao()?.getActiveLocalAccountId()?.takeIf(String::isNotBlank)
            }.getOrNull()
            AppThemePreference.ensureLoaded(applicationContext, selectedLocalAccountId)
            HomeAppearancePreference.ensureLoaded(applicationContext, selectedLocalAccountId)
            val startupBackground = if (AppThemePreference.isDarkTheme) {
                R.color.dark_background
            } else {
                R.color.light_background
            }
            window.setBackgroundDrawable(ColorDrawable(ContextCompat.getColor(this@MainActivity, startupBackground)))

            val startDestination = try {
                val activeDatabase = requireNotNull(database)
                if (activeDatabase.appSettingsDao().isProfileSelectionRequired() == true) {
                    Route.PROFILE_SELECTION
                } else {
                    when (GlobalContext.get().get<GetStartupDestinationUseCase>().invoke().first()) {
                        StartupDestination.APP_ONBOARDING -> Route.APP_ONBOARDING
                        StartupDestination.HOME -> Route.HOME
                        StartupDestination.PROFILE_SELECTION -> Route.PROFILE_SELECTION
                    }
                }
            } catch (_: Exception) {
                // If startup state cannot be read, show the existing startup recovery screen.
                Route.STARTUP
            }

            setContent {
                DailyBrainTheme {
                    AppNavHost(startDestination = startDestination)
                }
            }
            startupResolved = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    override fun onDestroy() {
        GlobalContext.get().get<GoogleSignInClient>().attachActivity(null)
        super.onDestroy()
    }
}
