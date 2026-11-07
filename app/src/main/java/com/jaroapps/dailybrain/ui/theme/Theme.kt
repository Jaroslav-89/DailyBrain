package com.jaroapps.dailybrain.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import com.jaroapps.dailybrain.R

private val DailyBrainShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun DailyBrainTheme(
    darkTheme: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val useDarkTheme = darkTheme ?: AppThemePreference.isDarkTheme
    val view = LocalView.current

    val colorScheme = if (useDarkTheme) {
        darkColorScheme(
            primary = colorResource(R.color.dark_primary),
            onPrimary = colorResource(R.color.dark_on_primary),
            primaryContainer = colorResource(R.color.dark_primary_container),
            onPrimaryContainer = colorResource(R.color.dark_on_primary_container),
            secondary = colorResource(R.color.dark_secondary),
            onSecondary = colorResource(R.color.dark_on_secondary),
            secondaryContainer = colorResource(R.color.dark_secondary_container),
            onSecondaryContainer = colorResource(R.color.dark_on_secondary_container),
            tertiary = colorResource(R.color.dark_tertiary),
            onTertiary = colorResource(R.color.dark_on_tertiary),
            tertiaryContainer = colorResource(R.color.dark_tertiary_container),
            onTertiaryContainer = colorResource(R.color.dark_on_tertiary_container),
            background = colorResource(R.color.dark_background),
            onBackground = colorResource(R.color.dark_on_background),
            surface = colorResource(R.color.dark_surface),
            onSurface = colorResource(R.color.dark_on_surface),
            surfaceVariant = colorResource(R.color.dark_surface_variant),
            onSurfaceVariant = colorResource(R.color.dark_on_surface_variant),
            outline = colorResource(R.color.dark_outline),
            outlineVariant = colorResource(R.color.dark_outline_variant),
            error = colorResource(R.color.dark_error),
            onError = colorResource(R.color.dark_on_error),
            errorContainer = colorResource(R.color.dark_error_container),
            onErrorContainer = colorResource(R.color.dark_on_error_container),
            inverseSurface = colorResource(R.color.dark_inverse_surface),
            inverseOnSurface = colorResource(R.color.dark_inverse_on_surface),
            inversePrimary = colorResource(R.color.dark_inverse_primary),
            scrim = colorResource(R.color.dark_scrim),
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.light_primary),
            onPrimary = colorResource(R.color.light_on_primary),
            primaryContainer = colorResource(R.color.light_primary_container),
            onPrimaryContainer = colorResource(R.color.light_on_primary_container),
            secondary = colorResource(R.color.light_secondary),
            onSecondary = colorResource(R.color.light_on_secondary),
            secondaryContainer = colorResource(R.color.light_secondary_container),
            onSecondaryContainer = colorResource(R.color.light_on_secondary_container),
            tertiary = colorResource(R.color.light_tertiary),
            onTertiary = colorResource(R.color.light_on_tertiary),
            tertiaryContainer = colorResource(R.color.light_tertiary_container),
            onTertiaryContainer = colorResource(R.color.light_on_tertiary_container),
            background = colorResource(R.color.light_background),
            onBackground = colorResource(R.color.light_on_background),
            surface = colorResource(R.color.light_surface),
            onSurface = colorResource(R.color.light_on_surface),
            surfaceVariant = colorResource(R.color.light_surface_variant),
            onSurfaceVariant = colorResource(R.color.light_on_surface_variant),
            outline = colorResource(R.color.light_outline),
            outlineVariant = colorResource(R.color.light_outline_variant),
            error = colorResource(R.color.light_error),
            onError = colorResource(R.color.light_on_error),
            errorContainer = colorResource(R.color.light_error_container),
            onErrorContainer = colorResource(R.color.light_on_error_container),
            inverseSurface = colorResource(R.color.light_inverse_surface),
            inverseOnSurface = colorResource(R.color.light_inverse_on_surface),
            inversePrimary = colorResource(R.color.light_inverse_primary),
            scrim = colorResource(R.color.light_scrim),
        )
    }

    SideEffect {
        val activity = view.context as? Activity
        val window = activity?.window
        if (window != null) {
            val backgroundArgb = colorScheme.background.toArgb()
            window.statusBarColor = backgroundArgb
            window.navigationBarColor = backgroundArgb
            val insetsController = WindowInsetsControllerCompat(window, view)
            insetsController.isAppearanceLightStatusBars = !useDarkTheme
            insetsController.isAppearanceLightNavigationBars = !useDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = DailyBrainShapes,
        content = content,
    )
}
