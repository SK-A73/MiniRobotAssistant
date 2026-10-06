package com.minirobot.assistant.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = RobotSkyBlue,
    onPrimary = RobotDarkBg,
    secondary = RobotCyan,
    onSecondary = RobotDarkBg,
    tertiary = RobotAccentPurple,
    background = RobotDarkBg,
    onBackground = RobotTextPrimary,
    surface = RobotSurface,
    onSurface = RobotTextPrimary,
    surfaceVariant = RobotSurfaceVariant,
    onSurfaceVariant = RobotTextSecondary
)

/**
 * Safely resolves an Activity if present in the Context hierarchy.
 * Returns null when Compose is hosted inside a Service (e.g. RobotOverlayService)
 * or non-Activity Context, preventing ClassCastException.
 */
private fun findActivity(context: Context): Activity? {
    var current: Context? = context
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun MiniRobotAssistantTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = findActivity(view.context)
            if (activity != null) {
                val window = activity.window
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
