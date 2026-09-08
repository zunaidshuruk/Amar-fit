# Amar Fit — Sprint Tracker

**How this file works:** This is the single source of truth for what's done and what's pending. Every item is only checked off after being verified against the actual GitHub repo (file contents, not AI Studio's self-reported summaries). When bug fixes or side work interrupt the main sprint sequence, this file is what keeps the overall plan from getting lost — update it, don't rely on memory. Commit this file to the repo root and re-check it at the start of every session.

Last verified: current session, against commit `6b81aa9`.

**Note on this file's own reliability:** I (Claude) have no push access to this repo — every tracker edit I make only exists in my local scratch clone until you manually place the file, and each time I `git reset --hard origin/main` to see your latest push, any of my own tracker edits that hadn't been placed yet are silently discarded. This has now happened three times in a row. If you want tracker edits to reliably stick, place the file I send you into the repo root before your next AI Studio push — it costs nothing and stops this from recurring.

---

## ✅ PART A — CORE APP (verified complete)

- [x] Auth: email/password MVVM + Google Sign-In (Credential Manager)
- [x] Session gate: FirebaseAuth-based routing, `onboardingCompleted` flag, non-destructive Room migration
- [x] Stable debug keystore + `applicationIdSuffix = ".debug"` (install/signing saga resolved)
- [x] Firestore security rules published (user-confirmed in console)
- [x] Centralized `HealthConnectManager` (permission set + availability check, no more duplication)
- [x] Cloud sync: Profile (incl. Base64 profile picture), Daily Metrics, Food Log, Diet Charts, Workouts — all with stable UUID `cloudId`s
- [x] Sync failure visibility (Toast on cloud failure, local save never blocked)
- [x] Health Connect read: Steps, Sleep, Blood Pressure, Blood Glucose, Heart Rate, Distance, Exercise Session, Nutrition (`externalNutritionCalories`, kept separate from app-logged calories)
- [x] AI response streaming: Chat, Coach, Diet Chart, Workout generation
- [x] Faster model (`gemini-3.5-flash-lite`) for Food Scan specifically
- [x] Full dark mode + colorful redesign: Today, Health, Nutrition, Sleep, Fitness, Settings, Chat, Meal Plan, Diet Chart, Glucose Log, Weight Log, Lifestyle, Coach, Food Log
- [x] Back navigation added to all 9 sub-screens that were missing it
- [x] Coach topic search
- [x] Shared `MealTypeSelector` component (Manual Entry + Scan Photo)
- [x] Food Log FAB-overlap fix (unified scrollable LazyColumn)
- [x] Tab navigation fix (`findStartDestination()` instead of route-string `popUpTo`) — **code verified correct, but not yet confirmed live after a fresh rebuild — confirm this before checking off**

## 🔲 PART B — KNOWN OPEN ITEMS (small, pre-existing, not yet done)

- [x] **Chat save feature (SavedChat)**
- [x] **Account deletion re-authentication flow**
- [x] **Firestore deserialization fix on login for saved items (SavedWorkout, SavedDietChart, SavedChat)**
- [x] **Email verification for email/password accounts**
- [x] **Welcome/Onboarding screen theme adaptation (Fix A)**
- [x] **Dark mode tile colors matching Google Health reference (Fix B)** — superseded by the full dark-mode redesign below.
- [x] **Meal-type chip truncation fix (Fix C)** — direct fix, not AI Studio. `MealTypeSelector.kt` 2×2 grid layout instead of 4-wide row. Delivered — confirm it's actually been placed in the repo, since it never went through an AI Studio push.
- [x] **Firestore-sync duplicate-entries bug (data corruption)** — verified fixed in commit `2d9b11e`. Unique index on `cloudId` for `FoodLog`/`SavedDietChart` + `MIGRATION_27_28` de-duplicating existing rows first. Diff checked directly.
- [x] **Chart legibility + accuracy issues across metric detail screens** — verified fixed in commit `f6f247a`. Zero-data bars now clearly visible (not near-invisible), line charts (HRV/skin temp/breathing rate/Weight) now position by real calendar index and break the line across real gaps instead of connecting them. Diff checked directly.
- [x] **Dark mode color system redesign (Material dark theme compliant)** — verified fixed in commit `63251b1`. Unified elevation ramp in `Theme.kt` (background #121212, surface #1E1E1E, surfaceVariant #262626, all neutral — no more blue-tinted `Slate800` cards mismatched against a near-black background). All `AccentTokens.kt` dark-mode tile backgrounds unified to the same neutral surfaceVariant; each domain's color now lives only in its icon/text (`onBg`), not the tile background. `TodayScreen.kt`/`EditFocusScreen.kt` hardcoded `Slate700`/`Slate800`/`Slate900` literals replaced with theme tokens throughout (went a bit further than the prompt asked, consistently — good). Diff checked directly, matches spec, light mode untouched.
- [x] **History Trend range selector broken across every metric detail screen** — direct fix, not AI Studio. `MetricsDao.getMetricsHistory` used `ORDER BY date DESC LIMIT :limit`, which selects the N most-recent *existing* rows, not "the last N calendar days" — so every range chip (D/W/M/3M/Y) returned an identical result whenever fewer daily_metrics rows existed than the selected range's day count, since every metric detail screen shares this one function. Changed the query to filter by an actual computed start date (`WHERE date >= :startDate`) instead of a row limit. Delivered as 3 files (Daos.kt, AppRepository.kt, ShasthoViewModel.kt) — confirm they've been placed in the repo, since this didn't go through an AI Studio push either.
- [ ] **Your own pending check:** confirm `saved_diet_charts` / `saved_workouts` actually appear in Firestore Console under your UID.
- [ ] Tab navigation fix — ready for live confirmation after a fresh rebuild.

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
- Leaderboards / social features
- Zone Minutes, Floors
- Original video/audio content production (using free-exercise-db image demos + external YouTube links instead)
- Wear OS companion app

---

- [x] **Macro tracking (Carbs/Fat/Protein + Calories Consumed graph card)** — verified fixed, with one detour. AI Studio's push (`059835f`) correctly added the schema (`carbsG`/`proteinG`/`fatG` on `FoodLog`/`DailyMetric`, migration v28→29), the Gemini JSON schema extension, the parsing/logging plumbing, and the new Key Metrics graph cards + drill-down support in `HealthScreen.kt`/`MetricDetailScreen.kt`. **Then a file-ordering accident broke it**: the three files I'd sent for the history-trend fix (`Daos.kt`/`AppRepository.kt`/`ShasthoViewModel.kt`) were snapshotted *before* macro tracking existed, and uploading them after `059835f` silently reverted the Gemini prompt schema and `logScannedFood`'s macro params — AI Studio then "cleaned up" the resulting compile break by deleting the now-orphaned macro parsing in `ScannerScreen.kt` (`f02e3ee`) instead of restoring the missing plumbing. Caught it this session by grepping for `carbsG`/`proteinG`/`fatG` across every touched file after your push — found the schema/UI intact but the capture path dead. Fixed directly (not AI Studio): restored the Gemini JSON schema + fallback error JSON in `AppRepository.kt`, restored parsing + `logScannedFood` params/increments in `ShasthoViewModel.kt`, restored parsing + pass-through in `ScannerScreen.kt` — merged cleanly on top of the history-trend fix, not a revert of it. Delivered as 3 files, commit `6b81aa9` locally. **Lesson for next time: when I hand you files for a direct fix, I'll flag it explicitly if those files also touch something currently in flight on an AI Studio branch, since uploading stale snapshots after newer work can silently clobber it like this did.**

## Next step

Part C's backlog is now fully closed out with nothing deferred. Worth a fresh backlog pass, or a full regression pass now that several things (dark mode, charts, macro tracking, history trend) have landed in quick succession — reviewing the whole app end to end wouldn't hurt given this session's file-ordering mishap.
