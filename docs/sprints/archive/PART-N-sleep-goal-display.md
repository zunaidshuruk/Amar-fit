<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## ✅ PART N — SLEEP GOAL-DISPLAY SURFACE — complete

Gap originally noted while verifying Part G (auto-calculated health goals): Sleep had no UI showing `profile.sleepGoalHours` anywhere, unlike Steps/Water/Calories.

- [x] **Sleep Tracking card goal line** — verified fixed in commit `8131b0a`. Adds a new `Text("of %.1f hrs goal")` line directly under the existing "X.X Hours" headline in `SleepScreen.kt`'s Sleep Tracking card, using `profile?.sleepGoalHours?.let { ... }` so it simply doesn't render while profile is still loading (no crash, no "of null"). 14sp/Medium, `sleepAccent.onBg` — proportionally matches the Steps ring's "of 10,000" sub-label styling relative to its headline. The existing "(+ Tap to log manually)" line and its preceding `Spacer(8.dp)` are both untouched, still rendering below the new line exactly as before. Purely additive diff (8 insertions, 0 deletions), only `SleepScreen.kt` touched, `debug.keystore` untouched, brace/paren balance and duplicate-import checks clean. Diff checked directly.

