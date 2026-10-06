# Hotspots — files too big to read whole

Each of these costs roughly 8k–30k tokens to read in full. Locate the region with `grep -n "<anchor>" <file>`, then read a slice with `sed -n 'START,ENDp' <file>`. Use anchors, never line numbers — they move every push. For verification, read `git diff` of the file, not the file.

| File | Region | Anchor |
|---|---|---|
| `presentation/viewmodel/ShasthoViewModel.kt` | login sync / logout / delete | `fun syncDataOnLogin`, `fun logout`, `fun deleteAccount` |
| 〃 | date-scoped flows | `fun getMetricsHistoryFlowForPeriod`, `fun getMetricsForDateFlow` |
| 〃 | glucose | `fun saveGlucoseReadingsForDate`, `fun importGlucoseCsv`, `fun writeGlucoseToHealthConnect` |
| 〃 | Health Connect sync + intraday | `fun syncWithHealthConnect`, `fun getHourlyStepsForDate` |
| 〃 | food logging (incl. HC NutritionRecord write) | `fun logScannedFood`, `NutritionRecord(` |
| 〃 | food chat | `fun sendFoodChatMessage`, `fun confirmPendingFoodLog` |
| 〃 | Universal Assistant | `fun sendUniversalAssistantMessage`, `fun openAssistantSession` |
| 〃 | workouts (HC exercise write) | `fun saveCompletedWorkoutSession`, `fun resolveHealthConnectExerciseType` |
| 〃 | Health Connect exercise sessions cache + backfill | `suspend fun refreshExerciseSessions`, `private suspend fun readExerciseSessionsWindow`, `fun startExerciseHistoryBackfill` |
| 〃 | day calories + own-workout calories write | `suspend fun getCaloriesBurnedForDate`, `// 3. Active calories for this session` |
| 〃 | workout plan editing | `fun updateStructuredWorkoutPlan`, `fun updateSavedWorkoutPlan` |
| 〃 | weight (HC weight write) | `fun setWeightAndHeight`, `fun writeWeightToHealthConnect` |
| 〃 | social | `fun sendFriendRequestByCode`, `fun fetchLeaderboard`, `fun createChallengeWithFriend`, `fun sendMessage` |
| 〃 | Drive backup | `fun backupToDrive`, `fun applyDriveBackup` |
| `data/repository/AppRepository.kt` | Gemini plumbing + key/model fallback | `fun resolveApiKeys`, `fun executeGeminiCallWithBackoff`, `fun streamGeminiCall` |
| 〃 | assistant tools | `fun buildAssistantToolsJson`, `fun sendUniversalAssistantMessage` |
| 〃 | food analysis / barcode | `fun analyzeFoodText`, `fun analyzeFoodImage`, `fun lookupBarcodeProduct` |
| 〃 | chat answer style guide (shared by 6 prompts) | `fun aiResponseStyle` |
| 〃 | AI insights | `fun generateHealthInsight`, `fun generateGlucoseGuidance`, `fun generateNutritionalInsights` |
| 〃 | badges | `fun checkAndAwardBadges` |
| `data/repository/FirebaseManager.kt` | login pull (silent-failure risk) | `fun pullDataOnLogin` |
| 〃 | social | `fun syncPublicProfile`, `fun getLeaderboard`, `fun createChallenge`, `fun sendDirectMessage` |
| `presentation/health/MetricDetailScreen.kt` | title / value dispatch | `when (metricKey)` (several) |
| 〃 | charts | `fun StepsCaloriesBarChart`, `fun ZoneBarChart`, `fun LineChartMetric`, `fun IntradayRangeBarChart`, `fun HeartRateZonesCard` |
| `presentation/today/TodayScreen.kt` | hero / pills / calendar | `fun FlippableHeroCard`, `fun TodayPillCard`, `fun CalendarStripCard` |
| `presentation/settings/SettingsScreen.kt` (one composable) | sections by label | `"Health Connect"`, `"Google Drive Backup"`, `"Change Password"`, `"Change Email"`, `"Body Measurements (for Body Fat %)"`, `"Delete Account"` |
| `presentation/health/HealthScreen.kt` (one composable) | sections by label | `"Health Vitals"`, `"BMI Calculator"`, `"Body position"`, `"Health Correlations"` |
| `presentation/assistant/UniversalAssistantScreen.kt` | tabs | `fun ChatHistoryTab`, `fun EmptyChatState` |
| `MainActivity.kt` | routes / theme | `composable("`, `MyApplicationTheme(` |

Whole-file sanity checks (brace balance, duplicate imports) should be a script that prints one line, not a read.
