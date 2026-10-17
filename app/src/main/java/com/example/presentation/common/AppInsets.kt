package com.example.presentation.common

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.key
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.activity.compose.BackHandler
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

/**
 * The smallest space kept free at the bottom of the screen. Some phones draw the gesture pill but report a navigation bar height of 0, which put
 * buttons under the pill; whatever the phone reports, at least this much is kept clear (a phone that reports more keeps the larger value).
 */
val MinBottomInset: Dp = 20.dp

/** Bottom floor and the system gesture area, on top of what the phone reports as system bars. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun bottomGuard(): WindowInsets =
    WindowInsets.safeGestures.only(WindowInsetsSides.Bottom).union(WindowInsets(left = 0.dp, top = 0.dp, right = 0.dp, bottom = MinBottomInset))

/** The larger of the main window's system bar insets and this window's own (which adds the on-screen keyboard), never less than [MinBottomInset] at the bottom. */
@Composable
fun appSafeInsets(): WindowInsets = LocalAppInsets.current.union(WindowInsets.safeDrawing).union(bottomGuard())

/** For the root Scaffold: the system bars with the same bottom guard, so screens with a pinned bottom bar (chat input, action bars) never sit under the gesture pill. */
@Composable
fun systemBarsWithFloor(): WindowInsets = WindowInsets.systemBars.union(bottomGuard())

/** The full-screen editors that are currently open, drawn by [OverlayLayer] at the root of the app. */
class OverlayHostState {
    class Entry(val key: Any, val content: @Composable () -> Unit)

    private val list = mutableStateListOf<Entry>()
    val entries: List<Entry> get() = list

    fun add(entry: Entry) {
        if (list.none { it.key == entry.key }) list.add(entry)
    }

    fun remove(key: Any) {
        list.removeAll { it.key == key }
    }
}

val LocalOverlayHost = compositionLocalOf<OverlayHostState?> { null }

/** Draws the open full-screen editors on top of everything, inside the main window of the app (the same window and insets as the Scaffold). */
@Composable
fun OverlayLayer(host: OverlayHostState) {
    host.entries.forEach { entry -> key(entry.key) { entry.content() } }
}

/**
 * The one way to show a full-screen editor. It is drawn INSIDE the main window of the app by [OverlayLayer], not in a separate pop-up window: a pop-up window
 * gets its own screen geometry from the system, and on some phones that put the bottom button partly below the screen edge, under the gesture bar. The editor
 * covers everything (top bar, tab bar, assistant button), closes with the back button, and its content is padded by [appSafeInsets]. [safePadding] = false is
 * for edge-to-edge content such as a camera preview, whose own controls then use [topSafePadding] and [bottomSafePadding].
 */
@Composable
fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    safePadding: Boolean = true,
    content: @Composable () -> Unit
) {
    val host = LocalOverlayHost.current
    if (host == null) {
        PlatformFullScreenDialog(onDismissRequest, safePadding, content)
        return
    }
    val latestContent = rememberUpdatedState(content)
    val latestDismiss = rememberUpdatedState(onDismissRequest)
    val latestSafePadding = rememberUpdatedState(safePadding)
    val key = remember { Any() }
    DisposableEffect(host, key) {
        host.add(OverlayHostState.Entry(key) {
            OverlayFrame(
                onDismissRequest = { latestDismiss.value() },
                safePadding = latestSafePadding.value,
                content = { latestContent.value() }
            )
        })
        onDispose { host.remove(key) }
    }
}

@Composable
private fun OverlayFrame(onDismissRequest: () -> Unit, safePadding: Boolean, content: @Composable () -> Unit) {
    BackHandler(onBack = onDismissRequest)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            // anything the editor does not use itself must not reach the screen underneath
            .pointerInput(Unit) { detectTapGestures { } },
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = if (safePadding) Modifier.fillMaxSize().windowInsetsPadding(appSafeInsets()) else Modifier.fillMaxSize()) {
            content()
        }
    }
}

/** Only used where there is no overlay host (previews and tests). */
@Composable
private fun PlatformFullScreenDialog(onDismissRequest: () -> Unit, safePadding: Boolean, content: @Composable () -> Unit) {
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
