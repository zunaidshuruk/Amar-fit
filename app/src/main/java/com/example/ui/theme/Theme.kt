package com.example.ui.theme

import android.app.Activity
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.example.presentation.common.LocalAppInsets
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat

/** The palette the whole app is drawn with. AccentTokens and other color helpers outside composables read it; MyApplicationTheme keeps it in sync. */
object AppPalette {
  var current by mutableStateOf(ThemePalettes.default)
}

/** Remembers the chosen palette on the phone, so the app starts in the right colors before the profile has loaded (no flash of the default palette). */
object ThemeCache {
  private const val PREFS = "ThemeCache"
  private const val KEY = "theme_id"

  fun load(context: Context): String? =
    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

  fun save(context: Context, id: String) {
    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, id).apply()
  }
}

val ThemePalette.accentColor: Color get() = Color(accent)
val ThemePalette.surfaceVariantColor: Color get() = Color(surfaceVariant)

/** The Material color scheme for a palette: every role the app reads (including dialogs, sheets and dividers) comes from the palette. */
private fun colorSchemeFor(p: ThemePalette) = darkColorScheme(
    primary = Color(p.accent),
    onPrimary = Color(p.onAccent),
    secondary = Color(p.secondary),
    onSecondary = Color(p.onAccent),
    tertiary = Color(p.alert),
    onTertiary = Color(p.onAccent),
    background = Color(p.background),
    surface = Color(p.surface),
    surfaceVariant = Color(p.surfaceVariant),
    onBackground = Color(p.text),
    onSurface = Color(p.text),
    onSurfaceVariant = Color(p.secondary),
    outline = Color(p.secondary),
    outlineVariant = lerp(Color(p.surfaceVariant), Color(p.secondary), 0.25f),
    surfaceContainerLowest = Color(p.background),
    surfaceContainerLow = Color(p.surface),
    surfaceContainer = Color(p.surface),
    surfaceContainerHigh = Color(p.surfaceVariant),
    surfaceContainerHighest = Color(p.surfaceVariant)
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
  palette: ThemePalette = AppPalette.current,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> remember(palette) { colorSchemeFor(palette) }
      else -> LightColorScheme
    }
    
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      // keep the shared palette (read by AccentTokens) and the phone's copy in step with what is drawn
      if (AppPalette.current != palette) AppPalette.current = palette
      if (ThemeCache.load(view.context) != palette.id) ThemeCache.save(view.context, palette.id)
      val window = (view.context as Activity).window
      window.statusBarColor = colorScheme.background.toArgb()
      window.navigationBarColor = colorScheme.background.toArgb()
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
    }
  }

  // the main window's insets, for full-screen dialogs (see AppInsets.kt)
  CompositionLocalProvider(LocalAppInsets provides WindowInsets.safeDrawing) {
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
  }
}
