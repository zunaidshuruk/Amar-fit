package com.example.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * The insets (status bar, gesture or navigation bar, display cutout) of the MAIN window, provided once at the root (see MyApplicationTheme).
 *
 * Why: a full-screen Dialog is a separate window, and on some phones it is NOT given the bottom (gesture bar) inset, so a button at the
 * bottom of the dialog ended up under the gesture bar. The main window always has the right values, so every screen and dialog reads them
 * through [appSafeInsets] instead of [WindowInsets.safeDrawing] directly (InsetsGuardTest enforces this).
 */
val LocalAppInsets = compositionLocalOf<WindowInsets> { WindowInsets(0, 0, 0, 0) }

/** The larger of the main window's system bar insets and this window's own (which adds the on-screen keyboard inside dialogs). */
@Composable
fun appSafeInsets(): WindowInsets = LocalAppInsets.current.union(WindowInsets.safeDrawing)

/**
 * The one way to show a full-screen dialog. It draws the background under the system bars and pads the content by [appSafeInsets],
 * so nothing is hidden behind the status bar or the gesture bar. [safePadding] = false is for edge-to-edge content (a camera preview)
 * whose own controls then use [appSafeInsets].
 */
@Composable
fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    safePadding: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(
                modifier = if (safePadding) Modifier.fillMaxSize().windowInsetsPadding(appSafeInsets()) else Modifier.fillMaxSize()
            ) {
                content()
            }
        }
    }
}

/**
 * Lets a horizontally scrolling row (chips) run to the screen edges while its content still starts at the normal [horizontal] margin.
 * Use it on the scrolling row together with `contentPadding` of the same size, so the last chip is never cut off by the margin.
 */
fun Modifier.fullBleed(horizontal: Dp): Modifier = this.layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth
    val placeable = measurable.measure(Constraints(minWidth = 0, maxWidth = width, minHeight = constraints.minHeight, maxHeight = constraints.maxHeight))
    layout(constraints.maxWidth, placeable.height) {
        placeable.placeRelative(-horizontal.roundToPx(), 0)
    }
}

/** Padding for only the bottom system bar (gesture bar), from the main window's insets: use it inside bottom sheets and dialogs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.bottomSafePadding(): Modifier = this.windowInsetsPadding(appSafeInsets().only(WindowInsetsSides.Bottom))

/** Padding for only the status bar and display cutout, from the main window's insets. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.topSafePadding(): Modifier = this.windowInsetsPadding(appSafeInsets().only(WindowInsetsSides.Top))

/**
 * For a bar that is drawn at the very bottom of the screen (the floating tab bar): keeps it clear of the system navigation bar whatever the
 * phone uses: the gesture pill, the three-button bar at the bottom (about 48 dp) or, in landscape, the buttons at the sides.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Modifier.bottomBarSafePadding(): Modifier =
    this.windowInsetsPadding(appSafeInsets().only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))

/** True on short screens (landscape phones, small windows) where bars and paddings should be tighter. */
@Composable
fun isCompactHeight(): Boolean = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 560
