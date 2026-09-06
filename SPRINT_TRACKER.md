# Amar Fit — Sprint Tracker

**How this file works:** This is the single source of truth for what's done and what's pending. Every item is only checked off after being verified against the actual GitHub repo (file contents, not AI Studio's self-reported summaries). When bug fixes or side work interrupt the main sprint sequence, this file is what keeps the overall plan from getting lost — update it, don't rely on memory. Commit this file to the repo root and re-check it at the start of every session.

Last verified: current session, against commit `91425ae`.

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

- [ ] **Chat save feature (SavedChat)** — was scoped in an earlier sprint but never actually built. No `SavedChat` entity exists, DB is still at version 19 with no chat-save migration. Needs to be redone from scratch.
- [ ] **Account deletion re-authentication flow** — still shows the deferred placeholder Toast ("log out, log back in, try again") instead of a real in-app re-auth prompt.
- [ ] **Your own pending check:** confirm `saved_diet_charts` / `saved_workouts` actually appear in Firestore Console under your UID (this was flagged a while back and may still be unconfirmed).
- [ ] Tab navigation fix — live-test after a fresh rebuild (see Part A note above).

## 🔲 PART C — NEW DESIGN & FEATURE BACKLOG (nothing built yet — this is the big one)

Compiled from the Google Health screenshot review. Each will become its own scoped sprint(s) when we get there — do not combine multiple line items into one AI Studio prompt.

1. [ ] **Height/Weight picker redesign** — modal dialogs, unit toggle (kg/lb/st, cm/ft), wheel-scroll selection, replacing current plain text fields (Onboarding, Settings, Weight Log)
2. [ ] **Health tab overhaul** — Key metrics as graphs/bars (Weight, Energy burned, Calories, Carbs/Fat/Protein, Steps, Exercise days), Focus areas category tiles, Health checks alerts, Personal info section
3. [ ] **Today tab redesign** — 3-page swipeable stat carousel, action row (Log/Start/Edit), chronological activity timeline feed, tile customization (scope: fixed curated set, not a full 25+ tile catalog engine)
4. [ ] **Health Connect READ expansion** — Active calories burned, HRV, SpO2, Skin Temperature, Breathing Rate (all real Health Connect record types)
5. [ ] **Per-metric detail drill-down screens** — shared D/W/M/3M/Y template with variants (bar chart, zone bars, streak strip, hourly pills, status-only) for each tracked metric
6. [ ] **Health Connect WRITE support** — Nutrition (food log/scan), Hydration (water log), Sleep (log sleep), Exercise (completed workouts)
7. [ ] **Guided timed workouts** — structured JSON workout generation (exercise/duration/rest, replacing freeform text), interval timer with auto-advance, in-app embedded YouTube video per exercise, MET-based calorie calculation — feeds real session timing into #6's Exercise write
8. [ ] **Mindfulness** — real Health Connect `MindfulnessSessionRecord` read/write (Meditation/Breathing/Movement types), building on existing Breathwork & Meditation category. Feature-gate on `FEATURE_MINDFULNESS_SESSION` availability.
9. [ ] **Resilience** — Amar Fit's own custom recovery score, computed from existing sleep/HRV/activity data (not a Health Connect read — no such record type exists)
10. [ ] **Searchable exercise library** — sourced from **free-exercise-db** (800+ exercises, public domain, image-based demos, no API key), with search + equipment/muscle filters, feeding into #7's structured workout builder
11. [ ] **Manually-entered Medical section** — Allergies, Conditions, Medications, Vaccines, Pregnancy, Social history, Procedures, Visits, Lab results — user-entered and stored like any other Amar Fit data (Room + Firestore), no Health Connect PHR dependency. "Vital signs" excluded (duplicates existing tracked metrics).

**Explicitly out of scope, decided earlier — do not resurrect without a new discussion:**
- Leaderboards / social features (requires Google's own social-graph infrastructure)
- Zone Minutes, Floors (proprietary Google scoring/sensors, no public API)
- Original video/audio content production for workouts or guided meditation (using YouTube embeds and free-exercise-db instead)
- Wear OS companion app (implied by "start workout on your watch," not something to fold into this)

---

## Next step

Pick the first item from Part C to turn into an actual sprint prompt, or close out Part B's open items first — recommend finishing Part B (it's small) before starting the big new backlog, so nothing from the old work stays half-finished underneath the new work.
