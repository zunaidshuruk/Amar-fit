<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## ✅ PART E — MANUAL-ENTRY / LOGGING SCREEN REDESIGN — complete

Triggered by comparing Amar Fit's entry dialogs against Google Health Connect's own hydration entry screen (quick-add steppers for Glass/Bottle/Large Bottle, date/time fields, "Other amount" custom field). Scope decided: redesign **all loggable metrics'** entry screens, one at a time, same pattern as Part D.

Manual-entry surfaces catalogued in the codebase: Water + Steps quick-add dialogs (`TodayScreen.kt`), Sleep dialog (`SleepScreen.kt`), Blood Pressure dialog (`HealthScreen.kt`), Weight + Glucose (dedicated full screens, currently plain text fields).

1. [x] **Water entry screen redesign** — verified fixed in commit `76f81a2`. Three quick-add options (Glass +0.25L, Bottle +0.5L, Large Bottle +1.0L) each call `addWater()` immediately and close the dialog; "Other amount" section keeps a numeric field with its own dedicated Add button. `addWater()`'s signature, Health Connect write, and sync-failure Toast all confirmed unchanged. Date/time backdating correctly not attempted, matching the scoping note. Diff checked directly — only `TodayScreen.kt`'s water dialog block changed.
2. [x] **Steps entry screen redesign** — verified fixed in commit `3bf33a2`. Mirrors the Water dialog's exact pattern: 3 quick-add chips ("Short Walk" +500, "Walk" +1,000, "Long Walk" +2,000 steps) each call `addSteps()` immediately and close the dialog. `addSteps(steps: Int)`'s signature/additive behavior confirmed untouched. Diff checked directly.
3. [x] **Sleep entry screen redesign** — verified fixed in commit `0aa9026`. Adapted for "set" rather than "add" semantics: 4 quick-select chips (6/7/8/9 Hours) call `setSleep(hours)`. `setSleep` confirmed byte-for-byte untouched. Diff checked directly.
4. [x] **Blood Pressure entry screen redesign** — verified fixed in commit `ae9138b`. Split into two validated Systolic/Diastolic number fields (a single free-text field doesn't fit quick-add presets). Dialog now correctly stays open on invalid input. Output format byte-identical to the existing Health Connect auto-sync format. Diff checked directly.
5. [x] **Weight entry screen redesign** — already done via Part C #1's wheel-picker dialog. Only real gap: zero save feedback. Fixed directly (Toast on Save & Calculate). Verified landed in commit `586271c`, byte-for-byte identical.
6. [x] **Glucose entry screen redesign** — already had clean split numeric fields. Same gap as Weight, fixed directly (Toast on Save Readings). Verified landed in commit `586271c`, byte-for-byte identical.

