<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## ✅ PART L — AUTO-SYNC WITH HEALTH CONNECT — complete

User request: while permission is granted, the app should auto-sync with Health Connect on a 10-second interval instead of requiring a manual sync tap.

- [x] **10-second auto-sync on the Today screen** — verified fixed in commit `8ccbe99`. New `LaunchedEffect(Unit)` loop in `TodayScreen.kt`, scoped to that screen's composition (starts when Today is visible, auto-cancels on navigating away — no background service, no battery-drain risk). Checks `HealthConnectManager.hasAnyPermissions(context)` before each tick; if granted, silently calls the existing `syncWithHealthConnect()` (already silent internally, no Toast spam). If no permission is granted, the loop simply skips forever — no repeated prompts, no crash. Manual sync buttons (Settings, "Sync Device Steps") confirmed untouched. Diff checked directly, only the intended file changed.

