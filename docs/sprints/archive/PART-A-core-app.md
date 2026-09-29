<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## ✅ PART A — CORE APP (verified complete)

- [x] Auth: email/password MVVM + Google Sign-In (Credential Manager)
- [x] Session gate: FirebaseAuth-based routing, `onboardingCompleted` flag, non-destructive Room migration
- [x] Stable debug keystore + `applicationIdSuffix = ".debug"` (install/signing saga resolved — **though see the recurring hazard note above, this keeps coming back**)
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

