package com.jaroapps.dailybrain

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.jaroapps.dailybrain.feature.brain.BrainStateManager
import com.jaroapps.dailybrain.feature.onboarding.domain.usecase.GetStartupDestinationUseCase
import com.jaroapps.dailybrain.feature.onboarding.domain.usecase.StartupDestination
import com.jaroapps.dailybrain.navigation.AppNavHost
import com.jaroapps.dailybrain.navigation.Route
import com.jaroapps.dailybrain.ui.theme.DailyBrainTheme
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

        window.statusBarColor = AndroidColor.WHITE
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        lifecycleScope.launch {
            val startDestination = try {
                when (GlobalContext.get().get<GetStartupDestinationUseCase>().invoke().first()) {
                    StartupDestination.APP_ONBOARDING -> Route.APP_ONBOARDING
                    StartupDestination.HOME -> Route.HOME
                }
            } catch (_: Exception) {
                // If startup progress cannot be read, show the existing startup recovery screen.
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
}
