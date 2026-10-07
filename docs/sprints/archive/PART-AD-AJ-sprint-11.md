<!-- Moved from docs/sprints/ACTIVE.md on 2026-10-07 when Sprint 11 closed. -->

## Sprint 11 — release 13 batch (Parts AD, AE, AF, AG, AH)
Collected from Zunaid's feedback on 2026-10-06 (release 12 is out, tag `12`). Status: 📤 prompt sent to AI Studio, not yet on GitHub · ⏳ not started · ✅ verified (add the commit).

| # | Step | Files | Status |
|---|---|---|---|
| AE-1 | Manual exercise logging, engine: `ExerciseCalorieEstimator` (MET by library category x weight x time; sets x reps -> duration at 3 s/rep + 60 s rest) + `logManualExercise` (Health Connect session + active calories + Activity cache row + local metrics) | `util/ExerciseCalorieEstimator.kt` (+ unit test), `ShasthoViewModel.kt` | ✅ `f302b51` |
| AE-2 | "Add exercise" button + dialog on the Activity screen (library search or custom name, sets x reps or minutes, start time, live kcal estimate, optional calorie override) | `ActivitySection.kt` (+ new dialog file) | ✅ `a74e33c`, `379361c` |
| AF-1 | Assistant tool `create_workout_program` (days + focus -> AI program saved to the Programs tab) | `AppRepository.kt` | ✅ `1f154b0` |
| AG-1 | Today big cards: aligned flip side (two-column table, no clipping), all big cards in the lime dot-ring style, Calories card = eaten in the ring + Active / Resting burned + Net, default big card | `TodayTiles.kt`, `TodayScreen.kt` | ✅ `8d6c9bb` |
| AH-1 | Food-logging entry points (Food Log chat button, Scanner chat button) open a BLANK new Assistant chat (no auto-sent starter; fixes the Scanner's bare "nutrition" message) via a new `fresh=true` route argument | `MainActivity.kt`, `UniversalAssistantScreen.kt`, `FoodLogScreen.kt` | ✅ `49d0005` |
| AH-2 | Multi-meal food logging: tool `log_foods` takes an `entries` array (one per food, each with meal type and kcal/macros); one review card lists all items (remove one, "Log all"); prompt tells the model to split complex messages by meal instead of logging only the first item | `AppRepository.kt`, `ShasthoViewModel.kt`, `UniversalAssistantScreen.kt` | ✅ `a74e33c`, `379361c` |
| AI-1 | Workout music (Level 1, no accounts): preferred app (Spotify / YouTube Music / YouTube / any), playlist links, play-pause-next-previous buttons in the guided workout via media key events, optional auto-start; settings are stored on the phone only (`ShasthoPrefs`) | `util/WorkoutMusic.kt` (new), `SettingsScreen.kt`, `WorkoutSessionScreen.kt` | ✅ `cd8928b` |
| AJ-1 | Blood glucose unit, foundation: `UserProfile.glucoseUnitMgdl` (Room 46 → 47, `MIGRATION_46_47`), conversion/format helpers in `UnitConverter.kt` (+ unit test), and the Today tile fixed (it labelled a mmol/L value as "mg/dL") | `Entities.kt`, `AppDatabase.kt`, `UnitConverter.kt`, test, `TodayTiles.kt` | ✅ `368580e` |
| AJ-2 | Unit selector (mmol/L / mg/dL chips) on the Glucose log and Health Goals screens; all glucose inputs, charts, HbA1c, target range and tiles shown/entered in the chosen unit, stored canonically in mmol/L | `GlucoseLogScreen.kt`, `GlucoseHistoricalEntryScreen.kt`, `HealthGoalsScreen.kt`, `HealthScreen.kt` | ✅ `4c59e48`, `d68e3b3` |
| AJ-3 | Plumbing: activity-event texts, CSV export/import in the chosen unit, AI guidance prompt shows both units | `ShasthoViewModel.kt`, `AppRepository.kt` | ✅ `a2e2193` |
| AD-1 | Health Connect history reader for ALL synced daily metrics (steps, distance, calories, active calories, sleep, heart rate min/max/avg, resting HR, HRV, SpO2, ...) via per-day aggregates into `DailyMetric`, never overwriting manual entries | `ShasthoViewModel.kt` | ✅ `368580e` |
| AD-2 | Triggers: first 30 days after permission is granted, on-demand when a metric screen opens a range, chunked resumable backfill when full history is granted | view model, metric screens | ✅ `68ae0c0` |
| AD-3 | Metric entries/charts show the daily average + range instead of the latest sample; fix the heart-rate chart label overlap | `MetricDetailScreen.kt` | ✅ `76beb27` |
| AD-4 | Verify cloud sync / Drive backup of the filled-in history | `FirebaseManager.kt` as needed | 📤 friend-stats guard in `AppRepository.saveMetrics` |

**Waves.** Wave 1 (AE-1, AF-1, AG-1, AH-1, AI-1) is DONE and verified. Wave 2 (different files, push together): AE-2 (`ActivitySection.kt` + `LogExerciseDialog.kt`) and AH-2 (`AppRepository.kt`, `ShasthoViewModel.kt`, `UniversalAssistantScreen.kt`). Wave 2: AE-2 and AH-2. Wave 3: AD-1 to AD-4. Release 13 after wave 3 (or after wave 2 if Zunaid wants it earlier). Rule unchanged: never two unpushed prompts touching the same file; every prompt keeps the keystore line.

**Why AJ exists:** Zunaid noticed the Blood Glucose tile on Today says mg/dL while the Glucose screens say mmol/L. Code-checked: values are stored in mmol/L everywhere (Health Connect values are converted to mmol/L on import), but `TodayTiles.kt` printed the stored mmol/L number with the label "mg/dL". Decision: let the user choose the unit; store canonically in mmol/L; convert on input and on display; device (Health Connect) data is converted automatically.

**Why AD exists (code-checked at `4c8363d`):** `syncWithHealthConnect` reads Health Connect only for TODAY's window and writes it into today's `DailyMetric`; there is no vitals backfill (only exercise sessions have one), so a past day has data only if the app ran that day. The same limitation hits every synced metric. Also: the Entries list shows the day's latest sample, which is misleading beside a min-max chart.

Other open items: release 12's `versionName` is still "1.5" (bump it with the next release and match the release title); dead-code cleanup of the removed chat/coach/food-chat view-model code; `read_health_data` (Assistant) will become more useful once AD-1 lands.


**Extra findings and fixes while verifying Sprint 11**
- AJ-2 first landed incomplete (`4c59e48`: Health Goals got only imports, Health screen untouched); completed by AJ-2b `d68e3b3`.
- Checker note: earlier bracket-balance warnings were artifacts of stripping `//` before strings (URLs); corrected order shows every touched file balanced.
- AD-4 finding: `AppRepository.saveMetrics` also calls `FirebaseManager.syncFriendStats(metric, profile)`, which writes the single `friend_stats/{uid}` document (date, steps, water, sleep, calories) for ANY saved metric. History imports (and any backdated entry) therefore overwrite the friends' view of "today" with an old day until the next live sync. Fix: only sync friend stats when `metric.date` is today. Cloud sync of imported days (`syncMetric` per `saveMetrics`) and Drive backup (`gatherBackupPayload` includes all daily metrics) need no change.
