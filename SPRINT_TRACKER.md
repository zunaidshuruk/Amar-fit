# Amar Fit — Sprint Tracker

**How this file works:** This is the single source of truth for what's done and what's pending. Every item is only checked off after being verified against the actual GitHub repo (file contents, not AI Studio's self-reported summaries). When bug fixes or side work interrupt the main sprint sequence, this file is what keeps the overall plan from getting lost — update it, don't rely on memory. Commit this file to the repo root and re-check it at the start of every session.

Last verified: current session, against commit `10ab400`.

**Note on this file's own reliability:** I (Claude) have no push access to this repo — every tracker edit I make only exists in my local scratch clone until you manually place the file, and each time I `git reset --hard origin/main` to see your latest push, any of my own tracker edits that hadn't been placed yet are silently discarded. This has now happened again this session (the Part D section and the barcode-scanning entry I'd added previously were both gone after this session's `git reset` — the repo still had the pre-Part-D version). If you want tracker edits to reliably stick, place the file I send you into the repo root before your next AI Studio push — it costs nothing and stops this from recurring.

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
- [ ] **Health Connect permission UX + hydration sync failure (new this session)** — prompt sent, awaiting push. Root cause: the "Connect" button always requests the full permission set with no check for what's already been decided; once Android permanently denies a specific health permission (after repeated denials), it silently stops showing that permission's prompt on future requests — explaining both "permission screen doesn't show up" and "2 permissions stuck denied," since the app never tells the user this happened or how to fix it via Health Connect's own settings. Separately, `ShasthoViewModel.addWater()`'s Health Connect write is wrapped in a try/catch that only does `e.printStackTrace()` — any write failure (e.g. from one of those stuck-denied permissions) is completely invisible, which explains logged hydration not appearing in Google Health Connect. Prompt scopes: (1) SettingsScreen.kt — detect the stuck-denied case and offer a button that opens Health Connect's own permission settings; (2) addWater() — add a success/failure callback instead of swallowing errors; (3) TodayScreen.kt — show a short Toast only on sync failure, reusing the existing sync-failure-visibility pattern. Scoped to hydration only, no other Health Connect write path touched.
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

## 🔲 PART D — NEW BACKLOG PASS (post-launch gap review)

Compiled from a fresh code review of the built app (not a screenshot review) — looking for gaps against a typically "complete" health/fitness/diet app, and for features that are silently half-built. Same rule as Part C: one scoped AI Studio prompt per item, do not combine.

1. [x] **Barcode scanning for packaged food** — verified fixed in commit `10ab400`. Adds a "Scan Meal" / "Scan Barcode" toggle at the top of `ScannerScreen.kt`; barcode mode runs on-device ML Kit detection (`BarcodeScanning.getClient()`) on the captured bitmap, then looks up the result against Open Food Facts (`AppRepository.lookupBarcodeProduct`), reusing the existing OkHttp client (now exposed as public instead of private) rather than a new one. Correctly falls back from per-serving to per-100g nutriment data when serving-size data isn't available, and — the part I'd flagged as easiest to skip — labels which basis was used ("Per serving (X)" vs. "Per 100g — check the package for your actual portion") right in the description shown on the Scan Result card. Feeds into the exact same Scan Result review card, `MealTypeSelector`, and `logScannedFood` call already used by photo scans (carbsG/proteinG/fatG included) — zero changes to the manual entry or photo-scan flows. Also added a sensible safeguard beyond the original spec: the "Log this meal" button is disabled and relabeled "Dismiss" when the result is an error/no-detection case (`parsedCalories == 0`), so a failed scan can't get accidentally logged as a 0-calorie food entry. Diff checked directly across all 4 changed files.
2. [ ] **Water history drill-down chart** — small, consistent fix. `waterLiters` is already logged (Today screen's quick-add dialog) and stored on `DailyMetric`, but unlike every other tracked metric it has no `MetricDetailScreen` entry and no Key Metrics card with a D/W/M/3M/Y trend. Same shape as the Carbs/Protein/Fat work: add a "Water" case to `MetricDetailScreen.kt`'s metricKey dispatch and a matching card in `HealthScreen.kt`. No schema change needed — the data already exists.
3. [ ] **Surface badges in the UI** — dead feature fix. `AppRepository.checkAndAwardBadges` already runs on every log and appends to `UserProfile.badges`; `TodayScreen.kt` already parses that list back out (`val badges = profile?.badges?.split(",")...`) — but nothing anywhere in the app actually renders it. Users are silently earning badges they can never see. Needs a small UI (a badges row/grid on Today or a dedicated screen) surfacing what's already being tracked.
4. [ ] **Fix language setting (real i18n)** — dead feature fix, highest-value of the four. Settings has a working English/Bangla picker that saves to `UserProfile.selectedLanguage`, but the app has only one `strings.xml` (English) and zero locale-switching code anywhere (`AppCompatDelegate.setApplicationLocales` or equivalent doesn't exist in the codebase). Selecting "Bangla" currently changes nothing. For a Bangladeshi-market app this is worth doing properly — either wire real translated strings + locale switching, or pull the picker until it's real. Likely the largest of the four (needs every user-facing string extracted and translated), so scope as its own discussion before drafting a prompt.

**Deferred from this pass, not forgotten:**
- Data export (CSV/PDF of logged health/food history) — real value, but ranked below barcode scanning this round.
- Body measurements beyond weight (waist, body fat %) — lower priority, flagged during the review but not queued yet.

## Next step

Barcode scanning is done and verified. Two things are in flight in parallel: the Health Connect permission/hydration-sync prompt (Part B, above) is with you now, and next up after that lands is the Water drill-down chart (quick), then Badges UI, then a dedicated scoping conversation for real i18n (biggest of the four, deserves its own discussion on translation scope/quality before an AI Studio prompt is drafted).
