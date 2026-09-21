package com.example.ui.theme

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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

// Redesign Sprint 2 - Robust dark mode mappings
private val DarkColorScheme =  darkColorScheme(
    primary = SignatureAccent,
    onPrimary = GunMetal,
    secondary = SecondaryAccent,
    onSecondary = GunMetal,
    tertiary = AlertAccent,
    onTertiary = GunMetal,
    background = GunMetal,
    surface = Color(0xFF0C3135),
    surfaceVariant = Color(0xFF163A3E),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onSurfaceVariant = SecondaryAccent,
    outline = SecondaryAccent
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Orange500,
    background = Background, // #F8F9FA
    surface = Surface, // #FFFFFF
    surfaceVariant = Color(0xFFF1F5F9), // Slate100 equivalent for generic soft surface
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary, // #1E1E1E
    onSurface = TextPrimary,
    onSurfaceVariant = Slate500,
    outline = Slate200
)

val DefaultCardShape = RoundedCornerShape(20.dp)
val DefaultCardElevation = 2.dp
val DefaultButtonShape = RoundedCornerShape(16.dp)
val DefaultButtonHeight = 56.dp

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
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
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.background.toArgb()
      window.navigationBarColor = colorScheme.background.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
    }
  }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
