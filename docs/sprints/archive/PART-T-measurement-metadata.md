<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## 🔲 PART T — DETAILED MEASUREMENT METADATA ACROSS ALL HEALTH METRICS (in progress)

**Naming note:** "Part S" is already taken by the dark-mode invisible-button-text urgent fix (see that section near the top of this file). This new work is Part T.

Zunaid's request, triggered by the Blood Pressure "Body position"/"Arm location" work: capture the same kind of rich measurement detail — matching what Google Fit/Health Connect's own manual "Add data" screens ask for — across every health metric Amar Fit logs, not just Blood Pressure.

**Research done this session (Android Health Connect Jetpack client library, `androidx.health.connect.client.records`, confirmed against the actual library source and official docs — sources listed at the bottom of this section):** each record type's constructor was checked for what optional structured detail fields it actually supports, cross-referenced against what Amar Fit currently reads/writes for that metric. Findings:

- **Blood Pressure — HAS rich detail fields (bodyPosition, measurementLocation).** Already scoped and in flight — see Phase 0 below.
- **Blood Glucose — HAS rich detail fields (specimenSource, mealType, relationToMeal)**, and matches what Health Connect's own manual Add Data screen for glucose actually asks for. Amar Fit doesn't write `BloodGlucoseRecord` to Health Connect **at all** today — glucose is read-only (`REQUIRED_PERMISSIONS` only has the read permission). See Phase 1 below.
- **Sleep — HAS a `stages` field (Awake/Light/Deep/REM breakdown)**, but this isn't a good candidate for manual entry: it's sensor/wearable-derived data a person can't accurately self-report after waking, and Google Health Connect's own manual Sleep entry doesn't ask for a stage breakdown either — it only takes a time range. Amar Fit's `setSleep()` is a manual "hours slept" quick-add, so there's no realistic detail to capture here beyond what already exists. **No phase planned for Sleep** — flagging this so it isn't silently expected later.
- **Nutrition — real gap found, not a missing-detail issue.** `FoodLog.mealType` is already collected locally (Breakfast/Lunch/Dinner/Snack, used in `MealTypeSelector`) but is silently dropped when writing to Health Connect — `AppRepository`'s `NutritionRecord(...)` construction only sets `energy`/`name`, never `mealType`, even though `MealType.MEAL_TYPE_*` constants exist and match Amar Fit's own categories exactly. See Phase 2 below.
- **Exercise — real gap found, not a missing-detail issue.** Every guided/completed workout is written to Health Connect as the generic `ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT`, even though Amar Fit's own 876-exercise library already categorizes each workout. See Phase 3 below.
- **Weight — missing the write entirely**, not a detail gap. `WeightRecord`'s own API has no extra structured detail fields at all (just weight + time) — nothing to "enrich" — but Amar Fit never writes it to Health Connect today (local-only). See Phase 4 below.
- **Skin Temperature — HAS a `measurementLocation` field (finger/toe/wrist)**, but Amar Fit doesn't write this record type at all today and has no manual-entry surface for skin temperature (read-only, synced from a wearable) — nothing to enrich until/unless a manual entry point exists. Not phased.
- **Steps, Heart Rate, Resting Heart Rate, Oxygen Saturation, Respiratory Rate, Distance, Active Calories Burned, HRV — NO extra structured detail fields exist in Health Connect's data model for any of these** (each is just a value + time, or a list of timestamped samples). There is genuinely nothing to add here to match Blood Pressure's pattern — flagging this so "detail metadata for every metric" isn't read as a gap in these.

**Bug found during this research, being fixed as part of Phase 1 (same file/function already being touched):** `ShasthoViewModel.kt`'s Health Connect sync (`syncWithHealthConnect`, ~line 676) reads a synced blood glucose value with `latest.level.inMilligramsPerDeciliter.toFloat()` and stores it directly into `bloodGlucoseMorning`. But `GlucoseLogScreen.kt`'s manual entry is explicitly labeled "(mmol/L)" and the HbA1c formula in that same screen (`(avgGlucoseMmol * 18 + 46.7f) / 28.7f`) assumes mmol/L input. So a Health-Connect-synced reading is currently stored in the wrong unit relative to manual entries and the HbA1c calculation — a real, pre-existing correctness bug, unrelated to this feature but directly in the code path Phase 1 has to touch anyway to add the Health Connect write correctly (the write needs to know which unit the stored float represents).

**Phasing (one scoped prompt per phase, same discipline as the rest of this sprint):**
- **Phase 0 — Blood Pressure. [x] Verified fixed in commit `2271588`.** Body position + arm location selectors, plus the first-ever Health Connect write for BP. Diffed symbol-by-symbol against the original prompt across all 4 Kotlin files (`Entities.kt`, `AppDatabase.kt` v35→36, `HealthConnectManager.kt`, `HealthScreen.kt`, `ShasthoViewModel.kt`) — every line matches spec exactly, brace/paren/bracket balance and duplicate-import checks clean. **This same commit deleted `debug.keystore` a third time** — see the recurring-hazard note at the top of this file; restored in my local clone only, still needs Zunaid to restore it in the actual repo.
- **Phase 1 — Blood Glucose meal-context tracking + Health Connect write. This phase absorbed and superseded the original, simpler Morning/Night-only plan** once Zunaid asked for full Before/After Breakfast/Lunch/Dinner tracking (6 readings/day) instead. **Prompt sent this session — see Part V below, which now owns this work** (moved out of Part T since it grew into its own sprint per Zunaid's "keep it in a separate sprint" instruction). Part T retroactively closes after Phase 0 — everything glucose/AI-related continues under Part V.
- **Nutrition meal type, Exercise type, Weight write** — the three remaining smaller gaps from the original research (`FoodLog.mealType` not wired into the `NutritionRecord` HC write; workouts hardcoded to `EXERCISE_TYPE_OTHER_WORKOUT` instead of a real category; `WeightRecord` never written to Health Connect at all) are unaffected by the Part V split and remain queued here, in that order, once Part V's active phases are done.

**Part T is now effectively folded into Part V for anything glucose-related — see Part V below for the live plan.**

**Sources consulted for the record-type field research:** [BloodGlucoseRecord.kt](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/records/BloodGlucoseRecord.kt), [SleepSessionRecord.kt](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/records/SleepSessionRecord.kt), [MealType.kt](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/records/MealType.kt), [SkinTemperatureRecord.kt](https://raw.githubusercontent.com/androidx/androidx/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/records/SkinTemperatureRecord.kt), [Android Health Connect data types reference](https://developer.android.com/health-and-fitness/health-connect/data-types), [android/health-samples HealthConnectManager.kt](https://github.com/android/health-samples/blob/main/health-connect/HealthConnectSample/app/src/main/java/com/example/healthconnectsample/data/HealthConnectManager.kt).



---
<!-- Moved verbatim from docs/sprints/ACTIVE.md when Part T closed (2026-09-29). -->

## Part T — Measurement metadata
Remaining gaps (*code-checked* in `presentation/viewmodel/ShasthoViewModel.kt`):
- [ ] `NutritionRecord(...)` write sets only `energy`/`name` — `FoodLog.mealType` not passed.
- [ ] Workout writes use `ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT`.
- [ ] `WeightRecord` is never written to Health Connect.

**Part T closed (2026-09-29).** All three gaps shipped and verified:
- `363de6c` — `FoodLog.mealType` passed into the `NutritionRecord` write.
- `dd2334f` — macro breakdown (carbs, protein, fat in g; sodium in mg; sugar, fiber in g) added to the same write; each field null when 0 (unknown).
- `0a00f9b` — workout exercise type: `WorkoutPlan.workoutType` (nullable, AI-provided via the `generateStructuredWorkout` schema), resolved by `resolveHealthConnectExerciseType` (AI value → title keywords → `EXERCISE_TYPE_OTHER_WORKOUT`; keywords deliberately exclude "run" and "weight").
- `8a79a86` — `WeightRecord` written on Weight Log saves (`setWeightAndHeight`, `setWeight`) via `writeWeightToHealthConnect`; `WRITE_WEIGHT` declared and requested. Profile-form and onboarding weight edits intentionally don't write (would duplicate weigh-ins).
