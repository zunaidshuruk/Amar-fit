<!-- Moved verbatim from docs/sprints/ACTIVE.md when the sprint closed (2026-10-06). -->

## Sprint 9 — Exercise tab (Parts AA + AB)
Two features, built in small prompts, one file-owner at a time. **Part AA:** the Exercise tab shows today's and past exercise sessions pulled from Health Connect (Google Fit, Fitbit, Samsung and others write there) with calories burned. **Part AB:** workouts become editable — sets, reps, time, rest per exercise, plus rounds with a rest between rounds — for custom, saved and AI-generated workouts.

Status: 📤 prompt sent to AI Studio, not yet on GitHub · ⏳ not started · ✅ verified (add the commit).

| # | Step | Files | Status |
|---|---|---|---|
| AA-1 | History permission (`READ_HEALTH_DATA_HISTORY`, Settings row; not added to `REQUIRED_PERMISSIONS`) | `AndroidManifest.xml`, `HealthConnectManager.kt`, `SettingsScreen.kt` | 📤 |
| AA-2 | Data layer: Room cache of sessions (type, time, duration, calories, distance, source app), Health Connect reader with per-session calories (active, else total), ViewModel API; Room 44 → 45 | `Entities.kt`, `Daos.kt`, `AppDatabase.kt`, `ShasthoViewModel.kt` | ⏳ |
| AA-3 | "Activity" section in the Exercise tab: Today summary + today's sessions, history grouped by day (7/30/90 days, load earlier); old History tab merged in | `WorkoutScreen.kt` (+ new activity file) | ⏳ after AB-4 |
| AA-4 | Calories burned: own guided workouts write calories to Health Connect; day active + total calories and trend; link to the metric detail screen | `ShasthoViewModel.kt`, activity UI | ⏳ |
| AA-5 | One-time backfill once full history is granted; fallback when Health Connect is unavailable | view model, activity UI | ⏳ |
| AB-1 | Reusable "Customize workout" editor; "Edit exercises" on saved workouts, saved back and synced | new `WorkoutPlanEditor.kt`, `WorkoutScreen.kt`, `ShasthoViewModel.kt` | 📤 |
| AB-2 | Guided session honors sets; plan-level `rounds` + `roundRestSeconds` (model + session steps + summary) | `WorkoutModels.kt`, `WorkoutSessionScreen.kt` | 📤 |
| AB-3 | Editor controls for rounds and round rest (shows total = sets × rounds) | `WorkoutPlanEditor.kt` | ⏳ after AB-1 and AB-2 |
| AB-4 | "Customize" on a freshly generated AI workout (before Start/Save); Exercise Library opens the editor instead of the fixed 3 × 10 | `WorkoutScreen.kt`, `ShasthoViewModel.kt`, `ExerciseLibraryScreen.kt` | ⏳ after AB-3 |
| AB-5 | Optional: AI generator also returns rounds; add an exercise inside the editor | `AppRepository.kt`, editor | ⏳ |

**Order (waves).** Wave 1 (now, different files, can be pushed together): AA-1, AB-1, AB-2. Wave 2: AB-3 and AA-2. Wave 3: AB-4, then AA-3. Wave 4: AA-4, AA-5 (and AB-5 if wanted). Release 9 after wave 3; release 10 after wave 4.

**Rules for this sprint.** (1) Never two unpushed prompts that touch the same file. (2) `WorkoutScreen.kt` is the hot file: AB-1, then AB-4, then AA-3, strictly in that order. (3) The next prompt for a file is sent only after the previous one is verified on `main`. (4) Every prompt keeps the `debug.keystore` stop-and-restore line.

**Design decisions (defaults assumed unless changed).** Exercise tab splits into "Activity" (Health Connect, shown first) and "Workouts" (AI Workouts + Saved). Sets repeat in the session (strength style) and rounds repeat the main block (circuit style); warmup and cooldown run once; total per exercise = sets × rounds. Calories per session = active calories recorded in the session window, else total calories; the day summary shows both. Sessions are cached locally. Full history is requested but the screen degrades to Health Connect's default 30-day window without it. Google Fit's own APIs are deprecated; Health Connect is the only source.

**Risks.** Full history works only on devices whose Health Connect supports it. AA-2 needs a Room migration. Sessions from other apps may lack calories (shown as "—").


---
**Sprint 9 closed (2026-10-06).** Everything shipped and verified on `main`; only the optional AB-5 is left out.
- AA-1 `5eca37c` — `READ_HEALTH_DATA_HISTORY` declared; `HealthConnectManager.HISTORY_PERMISSION`, `isHistorySupported`, `hasHistoryPermission`; Settings "Older exercise history" row. Deliberately NOT in `REQUIRED_PERMISSIONS`.
- AB-1 + AB-2 `7168d87` — `WorkoutPlanEditor.kt`; "Edit exercises" on saved workouts (`updateSavedWorkoutPlan`); `WorkoutPlan.rounds` / `roundRestSeconds`; guided session honors sets and rounds (steps, "Round Rest", set/round header, "Done" for rep exercises, grouped summary).
- AB-3 + AA-2 `3abfc55` — editor Rounds / Rest-between-rounds card; Room 44 → 45 (`health_exercise_sessions`, migration SQL verified identical to Room's generated `45.json`), `refreshExerciseSessions`, `HealthConnectManager.exerciseTypeLabel`. `FOOTBALL_SOCCER` label left out (constant not accepted), football shows as "Workout".
- AB-4 `4329ce3` — "Customize exercises" on a generated AI workout (`updateStructuredWorkoutPlan` also updates the raw JSON that Save stores); Exercise Library opens the editor instead of saving a fixed 3 x 10.
- AA-3 `728bbd4` — `ActivitySection.kt`; Exercise tab = Activity | Workouts switch; Today card, history by day (7/30/90 days, Load earlier).
- AA-4 `bbd6c1c` — `WRITE_ACTIVE_CALORIES_BURNED` declared and requested; guided workouts write an active-calories record in a separate try block; `getCaloriesBurnedForDate`; "Calories burned today" card with trend link. Fixes calories of own workouts being overwritten by the next Health Connect sync.
- AA-5 `bd467ad` — `readExerciseSessionsWindow` extracted; `startExerciseHistoryBackfill` (90-day chunks from 30 days back to 5 years, stops after 4 empty chunks, per-chunk saves, runs once via pref `exercise_history_backfilled`); app-only fallback list when Health Connect is not connected. The old History tab under Workouts was kept.
- AB-5 (second half) `2d112dc` — "Add exercise" in the editor: picker over the 876-exercise library or a typed custom name, section choice (Warmup / Main / Cooldown), stays open for several adds; `loadLibrary` parameter passed by the three callers. Main defaults 3 x 10, rest 30 s; Warmup/Cooldown default 1 timed set of 30 s, rest 10 s.
- Not built: AB-5 first half (AI generator also returning `rounds`).
- Release 11 (tag `11`, `versionCode` 11, `versionName` "1.5", `9b84622`) shipped AA-3 to AA-5.
