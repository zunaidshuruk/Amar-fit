# Bottom padding audit of every screen (2026-10-17)

**Report (Zunaid):** on phones with the three-button navigation bar turned on, buttons at the bottom get cut off; some screens still break.

## What was wrong
1. **The floating tab bar ignored the system navigation bar.** The bar in `MainActivity` (the Scaffold's `bottomBar`) had only 16 dp and 12 dp of padding. A Scaffold does not inset a custom bottom bar, so with the three-button bar (about 48 dp) the tabs were drawn under the system buttons; even with gestures it sat on the gesture pill.
2. **The assistant button was shown almost everywhere.** It was hidden on only 5 routes, so it also appeared on the login, onboarding and consent screens, on the chat (right over the send button) and on the exercise library (over its bottom action bar).
3. **The About screen did not scroll** (its Privacy and Terms links could be cut off on a small phone, in landscape or with large text); **the Badge gallery's last row sat under the assistant button**; **the legal pages ended flush with the bottom**.

## Fixes
* `Modifier.bottomBarSafePadding()` (`presentation/common/AppInsets.kt`): keeps the tab bar clear of the gesture pill, the three buttons, or the side buttons in landscape. On short screens (`isCompactHeight()`, under 560 dp) the bar's paddings tighten.
* `NavTabs.showsAssistantButton(route)`: the one rule for where the assistant button appears (never before login, never over the chat, the exercise library bar, the scanner or the assistant).
* About scrolls; Badge gallery grid has 88 dp of bottom padding; legal pages end with 32 dp.

## Every routed screen (checked by script)
Every route either scrolls with at least 72 dp of end space, or hides the assistant button, or is a full-screen camera. The earlier insets work (Sprints 19 to 24, `docs/ui-insets.md`) already covered dialogs, bottom sheets and 14 more screens.

## Tests that keep it fixed (`BottomInsetsGuardTest`, `NavTabsAssistantButtonTest`)
* the tab bar uses `bottomBarSafePadding()` and tightens on short screens;
* the assistant button uses the one rule, and a new screen with its own `bottomBar` fails the build until it is listed and hidden;
* every `*Screen.kt` has a scrolling container (only the Fitness wrapper and the QR camera are exempt).

## Not verifiable here
The look on a real phone with each navigation style (gestures, three buttons, landscape, large text). The code follows the rules above; please check the tab bar with three buttons turned on.
