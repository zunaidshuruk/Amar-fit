# Amar Fit — Sprint Tracker

**How this file works:** This is the single source of truth for what's done and what's pending. Every item is only checked off after being verified against the actual GitHub repo (file contents, not AI Studio's self-reported summaries). When bug fixes or side work interrupt the main sprint sequence, this file is what keeps the overall plan from getting lost — update it, don't rely on memory. Commit this file to the repo root and re-check it at the start of every session.

Last verified: current session, against commit `25c2ceb` (+ one direct fix on top, see item 3 note).

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

- [x] **Chat save feature (SavedChat)** — built with Room (v20 migration), Moshi serialization of ChatMessage list, Firestore sync, and two-tab ChatScreen layout.
- [x] **Account deletion re-authentication flow** — implemented real in-app re-auth prompt for password and Google Sign-In users, automatic retry on success, and missing `saved_chats` subcollection cleanup.
- [x] **Firestore deserialization fix on login for saved items (SavedWorkout, SavedDietChart, SavedChat)** — added default parameter values to all three entity models so Firestore's zero-arg constructor reflection works on restore; isolated collection pulls into individual try-catches in FirebaseManager.pullDataOnLogin so failures in one collection cannot block others.
- [x] **Email verification for email/password accounts** — newly identified and closed gap: requires email verification via Firebase Auth before reaching the app on sign up or unverified password login, provides 30s rate-limited resend, check status button, and account switcher. Google Sign-In and already-verified accounts remain untouched.
- [x] **Welcome/Onboarding screen theme adaptation (Fix A)** — replaced hardcoded text/container colors in AuthScreen.kt and OnboardingScreen.kt with MaterialTheme.colorScheme tokens (background, onBackground, onSurfaceVariant, outlineVariant), eliminating contrast issues in both dark and light modes.
- [x] **Dark mode tile colors matching Google Health reference (Fix B)** — recolored dark-mode bg/onBg pairs in AccentTokens.kt to muted near-black tints with soft light tints (teal #123832/#5FE0C4, purple #332352/#C9B6F5, blue #152A52/#8FB8FF, etc.) and updated Theme.kt DarkColorScheme background to #000000, surface to #141414, surfaceVariant to #1C1C1E.
- [ ] **Your own pending check:** confirm `saved_diet_charts` / `saved_workouts` actually appear in Firestore Console under your UID (this was flagged a while back and may still be unconfirmed).
- [ ] Tab navigation fix — all 10 in-app shortcut call sites (7 in TodayScreen, 3 in HealthScreen) updated to navigateToTab with popUpTo(findStartDestination), singleTop, and state restoration; ready for live confirmation after a fresh rebuild.

## 🔲 PART C — NEW DESIGN & FEATURE BACKLOG (nothing built yet — this is the big one)

Compiled from the Google Health screenshot review. Each will become its own scoped sprint(s) when we get there — do not combine multiple line items into one AI Studio prompt.

1. [x] **Height/Weight picker redesign** — modal dialogs, unit toggle (kg/lb/st, cm/ft), wheel-scroll selection, replacing current plain text fields (Onboarding, Settings, Weight Log)
2. [ ] **Health tab overhaul** — Key metrics as graphs/bars (Weight, Energy burned, Calories, Carbs/Fat/Protein, Steps, Exercise days), Focus areas category tiles, Health checks alerts, Personal info section
   - *(Partial: Focus areas, Health checks, and Personal info sections added; Key metrics graphs pending)*
3. [x] **Today tab redesign** — 3-page swipeable stat carousel, action row (Log/Start/Edit), chronological activity timeline feed, tile customization ("Edit Focus" with 7 curated metrics)
   - *(Rebuilt: top carousel replaced with a fixed Daily Steps ring + pill-tile layout; Edit Focus rebuilt as a full-screen picker (`EditFocusScreen.kt`) supporting unlimited large/small tile add-remove from 18 real, data-backed metrics (2 large ring tiles including a genuine Weekly Cardio %, 18 small tiles), backward-compatible with the old 3-slot storage format. Resilience score extracted from HealthScreen into a shared `ShasthoViewModel.calculateResilienceScore()` used by both screens. Direct fix applied on top: 6 of the new small tiles (Cal burned, Heart rate, HRV, SpO2, Skin temp, Breathing rate) were wired to metric-detail routes using the wrong key format and always opened an empty "Metric Detail" screen — corrected to the camelCase keys MetricDetailScreen actually matches on. Distance has no drill-down screen at all yet, so that tile is display-only for now.)* *(Updated: top carousel replaced with fixed Google-Health-style layout featuring Daily Steps circular ring gauge + 3-row pill stack and 3x2 pill grid; carousel customization removed; rebuilt Edit Focus as full-screen picker with unbounded large and small metric tiles, live data previews, and backward-compatible slot storage)*
4. [x] **Health Connect READ expansion** — Active calories burned, HRV, SpO2, Skin Temperature, Breathing Rate (all 5 verified non-experimental in connect-client 1.1.0-alpha11 and added to DailyMetric + syncWithHealthConnect)
5. [x] **Per-metric detail drill-down screens** — implemented `MetricDetailScreen.kt` featuring a shared range-selector (D/W/M/3M/Y) and custom chart variants (Canvas bar, zone bars, streak strip, and line chart) powered by `ShasthoViewModel.getMetricsHistoryFlow` with zero schema/database modifications
6. [x] **Health Connect WRITE support** — Nutrition, Hydration, Sleep, Mindfulness, and Exercise session write fully implemented.
7. [x] **Guided timed workouts** — structured JSON workout generation (warmup/main/cooldown), interval timer with auto-advance, lightweight animated image demo per exercise (via free-exercise-db), external "Watch on YouTube" resolver button (replacing in-app WebView embed), MET-based live calorie calculation, and Health Connect Exercise write integration.
8. [x] **Mindfulness** — real Health Connect `MindfulnessSessionRecord` read/write (Meditation/Breathing/Movement types) with `MindfulnessTimerScreen.kt`, duration/type pickers, interval countdown timer, local `DailyMetric.mindfulnessMinutes` increment, and write permission/sync.
9. [ ] **Resilience** — Amar Fit's own custom recovery score, computed from existing sleep/HRV/activity data (not a Health Connect read — no such record type exists)
10. [x] **Searchable exercise library** — bundled 876 exercises from free-exercise-db in `assets/exercises.json`, in-memory cached loader `AppRepository.getExerciseLibrary`, `ExerciseLibraryScreen` with case-insensitive search, dynamic equipment/muscle filter chips, multi-select, and structured `WorkoutPlan` generation saving into existing `viewModel.saveWorkout` without schema changes
11. [ ] **Manually-entered Medical section** — Allergies, Conditions, Medications, Vaccines, Pregnancy, Social history, Procedures, Visits, Lab results — user-entered and stored like any other Amar Fit data (Room + Firestore), no Health Connect PHR dependency. "Vital signs" excluded (duplicates existing tracked metrics).

**Explicitly out of scope, decided earlier — do not resurrect without a new discussion:**
- Leaderboards / social features (requires Google's own social-graph infrastructure)
- Zone Minutes, Floors (proprietary Google scoring/sensors, no public API)
- Original video/audio content production for workouts or guided meditation (using YouTube embeds and free-exercise-db instead)
- Wear OS companion app (implied by "start workout on your watch," not something to fold into this)

---

## Next step

Pick the first item from Part C to turn into an actual sprint prompt, or close out Part B's open items first — recommend finishing Part B (it's small) before starting the big new backlog, so nothing from the old work stays half-finished underneath the new work.
