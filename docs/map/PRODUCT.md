# Map — product code

All paths under `app/src/main/java/com/example/` unless noted. Find the area, open only the files listed, then use [HOTSPOTS.md](HOTSPOTS.md) to read big files by anchor. Routes are registered in `MainActivity.kt` (`composable("…")`).

| Area | Files | Calls into | Notes |
|---|---|---|---|
| App shell, nav, top bar, bottom nav, global assistant FAB, notification deep links | `MainActivity.kt`, `presentation/navigation/NavigationExtensions.kt` | ShasthoViewModel | `MyApplicationTheme(darkTheme = true)` — dark-only |
| Today | `presentation/today/TodayScreen.kt`, `TodayTiles.kt`, `EditFocusScreen.kt` | ShasthoViewModel (`updateTodayTileSlots`, date-scoped flows) | flip hero card, calendar strip, tile ids in `TodayTiles.kt` |
| Health tab, BP/BMI dialogs, medical records | `presentation/health/HealthScreen.kt`, `MedicalRecordsScreen.kt` | ShasthoViewModel | |
| Metric drill-downs (charts per metric) | `presentation/health/MetricDetailScreen.kt` | `getMetricsHistoryFlowForPeriod`, `getHourly*ForDate` | chart per `metricKey` |
| Health correlations | `presentation/correlations/HealthCorrelationsScreen.kt` | | histogram + scatter |
| Glucose, weight | `presentation/metrics/GlucoseLogScreen.kt`, `GlucoseHistoricalEntryScreen.kt`, `WeightLogScreen.kt` | `saveGlucoseReadingsForDate`, `importGlucoseCsv`, `fetchGlucoseGuidance` | always save glucose via `saveGlucoseReadingsForDate` (race fix) |
| Nutrition dashboard | `presentation/nutrition/NutritionScreen.kt` | | Part R Phase 3 |
| Food log, scanner | `presentation/foodlog/FoodLogScreen.kt`, `presentation/scanner/ScannerScreen.kt` | `logScannedFood`, AppRepository `analyzeFood*`, `lookupBarcodeProduct` | |
| Diet charts, meal plan, recipes, lifestyle | `presentation/mealplan/DietChartScreen.kt`, `MealPlanScreen.kt`, `presentation/recipe/RecipeScreen.kt`, `presentation/lifestyle/LifestyleScreen.kt` | `generateDietChart`, `generatePremiumRecipe` | |
| Sleep, mindfulness | `presentation/sleep/SleepScreen.kt`, `presentation/mindfulness/MindfulnessTimerScreen.kt` | `setSleep`, `saveCompletedMindfulnessSession` | |
| Exercise tab: Activity (Health Connect) | `presentation/workout/ActivitySection.kt` | `refreshExerciseSessions`, `observeExerciseSessions`, `getCaloriesBurnedForDate`, `startExerciseHistoryBackfill` | Today + history by day, calories burned card; sessions cached in Room `health_exercise_sessions` |
| Workouts (AI / Saved / History), editor, library | `presentation/fitness/FitnessScreen.kt` (wrapper), `presentation/workout/WorkoutScreen.kt`, `WorkoutPlanEditor.kt`, `WorkoutSessionScreen.kt`, `ExerciseLibraryScreen.kt` | `generateAIStructuredWorkout`, `updateStructuredWorkoutPlan`, `updateSavedWorkoutPlan`, `saveCompletedWorkoutSession` | `WorkoutPlan` has `rounds` / `roundRestSeconds`; session runs sets and rounds |
| Universal AI Assistant (Part Z) | `presentation/assistant/UniversalAssistantScreen.kt`, `LegacyChatViewer.kt` | `sendUniversalAssistantMessage`; AppRepository `buildAssistantToolsJson`, `executeRawGeminiCall` | sessions in Room; includes legacy saved chat viewer |
| Social | `presentation/social/FriendsScreen.kt`, `LeaderboardScreen.kt`, `DirectMessageScreen.kt`, `QrScannerScreen.kt` | FirebaseManager social functions | |
| Badges | `presentation/badges/BadgeGalleryScreen.kt`, `data/model/BadgeCatalog.kt` | AppRepository `checkAndAwardBadges` | catalog ids must match stored strings |
| Settings, goals, legal, about | `presentation/settings/SettingsScreen.kt`, `HealthGoalsScreen.kt`, `AboutScreen.kt`, `LegalDocumentScreen.kt`, `data/model/LegalContent.kt`, `PrivacyPolicyActivity.kt` | | |
| Auth, onboarding, consent | `presentation/auth/*`, `data/auth/AuthRepository.kt`, `presentation/onboarding/OnboardingScreen.kt` | | |
| Notifications | `presentation/notifications/*` | | FCM service + meal reminders |
| ViewModel (single, app-wide) | `presentation/viewmodel/ShasthoViewModel.kt` | AppRepository, FirebaseManager, Health Connect | read by anchor |
| Repository / Gemini / sync | `data/repository/AppRepository.kt`, `FirebaseManager.kt`, `GoogleDriveManager.kt`, `UpdateChecker.kt` | `data/remote/*` | |
| Room | `data/local/AppDatabase.kt` (version + migrations), `Entities.kt`, `Daos.kt`, per-entity files | | schema JSON in `app/schemas/` |
| Health Connect | `data/health/HealthConnectManager.kt` (permissions), `HealthGoalCalculator.kt` | | |
| Remote APIs | `data/remote/GeminiApiService.kt`, `RetrofitClient.kt`, `GoogleDrive*`, `YouTube*` | | |
| Theme, shared UI | `ui/theme/Color.kt`, `Theme.kt`, `AccentTokens.kt`, `Type.kt`; `ui/components/*`; `util/UnitConverter.kt` | | tokens, never literals; `MarkdownText.kt` renders all AI text (headings with accent bar, bullets, numbered steps, `> **Label:**` callouts, tables → cards, `---` → divider, `YOUTUBE_SEARCH: <query>` → Watch button); `AiMessageCard.kt` is the "KardIQ AI" answer card (copy button, follow-up chips from a `FOLLOWUPS: q1 \| q2` line) used by the Assistant, Chat (live + saved) and Food Chat |
| Unit tests | `app/src/test/java/com/example/` | | |

Not app code: root `fix_*.py`, `update_*.py`, `rewrite_*.py`, `add_*.py`, `remove_fit.py`, `test_compile.kt`, `test_fit.kt` are historical one-off AI Studio scripts. `design-refs/` holds Part R reference images.
