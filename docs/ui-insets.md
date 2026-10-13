# Screens, dialogs and the system bars (status bar, gesture bar, keyboard)

**The bug this prevents:** on some phones a full-screen `Dialog` is a separate window that does NOT receive the bottom (gesture bar) inset, so a
button at the bottom of the dialog (for example Save in Add exercise) ended up under the gesture bar, and headers could slide under the status
bar. The main window always has the right values.

**Rules (enforced by `InsetsGuardTest`, which fails the build otherwise):**
* Never use `WindowInsets.safeDrawing`, `.statusBars`, `.navigationBars`, `navigationBarsPadding()`, `systemBarsPadding()` or
  `statusBarsPadding()` directly. Use `appSafeInsets()`, `Modifier.topSafePadding()` or `Modifier.bottomSafePadding()` from
  `presentation/common/AppInsets.kt`. They use the main window's insets, provided once in `MyApplicationTheme` (`LocalAppInsets`).
* Never build a full-screen `Dialog` yourself. Use `FullScreenDialog(onDismissRequest) { ... }` (it draws the background under the bars and pads
  the content); `safePadding = false` is for edge-to-edge content such as a camera preview, whose own controls then use the helpers above.
* Bottom sheets (`ModalBottomSheet`) are separate windows too: put `Modifier.bottomSafePadding()` on their content column.
* Screens that show the assistant button (every tab and most routes) need at least 72 dp of space at the end of the main scrolling area
  (we use 88 dp) so the last items can scroll clear of the button. Screens that hide it (assistant, food log, scanner, cycle, auth) do not.
* A horizontally scrolling row of chips inside a padded column should use `Modifier.fullBleed(16.dp)` on the scrolling row plus
  `.padding(horizontal = 16.dp)` inside it, so the chips scroll to the screen edge and the last one is not cut at the margin.

**History:** reported 2026-10-13 with a screenshot of Add exercise (Save under the gesture pill); the first fix (Part AQ) applied
`WindowInsets.safeDrawing` inside the dialog, which is exactly the thing that does not work inside a dialog window on those phones.
