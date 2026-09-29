<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## ✅ PART C — NEW DESIGN & FEATURE BACKLOG (complete)

1. [x] Height/Weight picker redesign
2. [x] Health tab overhaul — Focus areas, Health checks, Personal info, and Key metrics graphs (Weight/Calories Burned/Steps/Exercise Days/Calories Consumed/Carbs/Protein/Fat) all built. The Calories Consumed + macro graph cards landed as part of "macro tracking" below.
3. [x] Today tab redesign — fixed Daily Steps ring + pill-tile layout, full-screen Edit Focus picker, shared `ShasthoViewModel.calculateResilienceScore()`.
4. [x] Health Connect READ expansion (Active calories, HRV, SpO2, Skin Temp, Breathing Rate)
5. [x] Per-metric detail drill-down screens (`MetricDetailScreen.kt`)
6. [x] Health Connect WRITE support (Nutrition, Hydration, Sleep, Mindfulness, Exercise)
7. [x] Guided timed workouts (free-exercise-db image demos, external YouTube link, MET calorie calc)
8. [x] Mindfulness (real Health Connect read/write)
9. [x] Resilience score
10. [x] Searchable exercise library (876 exercises)
11. [x] Manually-entered Medical section (full Room + Firestore sync)

**Explicitly out of scope, decided earlier — do not resurrect without a new discussion:**
- ~~Leaderboards / social features~~ — **revisited by Zunaid, now queued as Part Q (see below).**
- Zone Minutes, Floors
- Original video/audio content production (using free-exercise-db image demos + external YouTube links instead)
- Wear OS companion app

---

- [x] **Macro tracking (Carbs/Fat/Protein + Calories Consumed graph card)** — verified fixed, with one detour. AI Studio's push (`059835f`) correctly added the schema (`carbsG`/`proteinG`/`fatG` on `FoodLog`/`DailyMetric`, migration v28→29), the Gemini JSON schema extension, the parsing/logging plumbing, and the new Key Metrics graph cards + drill-down support in `HealthScreen.kt`/`MetricDetailScreen.kt`. **Then a file-ordering accident broke it**: the three files I'd sent for the history-trend fix (`Daos.kt`/`AppRepository.kt`/`ShasthoViewModel.kt`) were snapshotted *before* macro tracking existed, and uploading them after `059835f` silently reverted the Gemini prompt schema and `logScannedFood`'s macro params — AI Studio then "cleaned up" the resulting compile break by deleting the now-orphaned macro parsing in `ScannerScreen.kt` (`f02e3ee`) instead of restoring the missing plumbing. Caught it this session by grepping for `carbsG`/`proteinG`/`fatG` across every touched file after your push — found the schema/UI intact but the capture path dead. Fixed directly (not AI Studio): restored the Gemini JSON schema + fallback error JSON in `AppRepository.kt`, restored parsing + `logScannedFood` params/increments in `ShasthoViewModel.kt`, restored parsing + pass-through in `ScannerScreen.kt` — merged cleanly on top of the history-trend fix, not a revert of it. Delivered as 3 files, commit `6b81aa9` locally. **Lesson for next time: when I hand you files for a direct fix, I'll flag it explicitly if those files also touch something currently in flight on an AI Studio branch, since uploading stale snapshots after newer work can silently clobber it like this did.**

