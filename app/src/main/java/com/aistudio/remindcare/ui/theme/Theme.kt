package com.aistudio.remindcare.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1E3A8A), // Navy accent
    secondary = Color(0xFF3B82F6),
    background = Color(0xFFFFF9F2), // Warm ivory/cream
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1917), // Warm dark gray
    onSurface = Color(0xFF1C1917),
    onSurfaceVariant = Color(0xFF57534E), // Warm gray
    error = Color(0xFFDC2626),
    outlineVariant = Color(0xFFE7E5E4) // Warm light border
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = Color(0xFF93C5FD),
    background = Color(0xFF1C1917), // Warm dark gray instead of slate
    surface = Color(0xFF292524), // Slightly lighter warm dark
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),
    onBackground = Color(0xFFFAF9F6), // Warm off-white
    onSurface = Color(0xFFFAF9F6),
    onSurfaceVariant = Color(0xFFA8A29E), // Warm gray
    error = Color(0xFFF87171),
    outlineVariant = Color(0xFF44403C)
)

@Composable
fun RemindCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disable dynamic color to maintain our branding
    fontSizeStr: String = "Normal",
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    val fontScale = when (fontSizeStr) {
        "Large" -> 1.25f
        "Very Large" -> 1.5f
        else -> 1.0f
    }
    
    val typography = getDynamicTypography(fontScale)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
