<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## ✅ PART G — AUTO-CALCULATED HEALTH GOALS (complete)

User request: auto-calculate health goals (Steps/Sleep/Water/Calories) from BMI/profile data, with a per-goal manual override toggle, surfaced in Settings.

1. [x] **Schema + auto-calculation engine** — verified fixed in commit `b118393`. `HealthGoalCalculator` object with 4 formulas (step goal by activity-level table, sleep goal by age bracket, water goal, calorie goal via Mifflin-St Jeor). `MIGRATION_29_30` adds 7 columns to `UserProfile`. `ShasthoViewModel.recalculateAutoGoals(profile)` wired into `saveProfile()`. Diff checked directly.
2. [x] **Settings UI: Health Goals screen** — verified fixed in commit `5e696e6` (AI Studio) + `0f6b3dc` (direct fix, Toast feedback on Save). Diff checked directly, byte-for-byte identical.

**Related gap found and fixed while verifying:** Today screen's Daily Steps ring had `10000` hardcoded instead of reading `profile.stepGoal`. Fixed directly, verified landed as `f40fbaf`. Sleep's own goal-display gap closed later — see Part N below.

