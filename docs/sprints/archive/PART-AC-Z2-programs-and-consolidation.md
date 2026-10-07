<!-- Written 2026-10-06 when the batch closed. -->

## ✅ PARTS AC + Z PHASE 2 + SPRINT-9 LEFTOVERS — multi-day programs, Assistant consolidation (complete)

Zunaid's decisions: programs use numbered days (Day 1, 2, 3...), are created by hand or by AI; every AI chat surface merges into the Universal Assistant.

**Part AC — multi-day workout programs**
1. [x] AC-1 `86ef584` — Room 45 → 46 `workout_programs` (migration SQL identical to Room's generated `46.json`), `WorkoutProgram` / `WorkoutProgramDao`, `ProgramDay` + `ProgramDaysJson` in `WorkoutModels.kt`, repository functions, view-model flow and actions (`completeProgramDay`, `restartWorkoutProgram`).
2. [x] AC-2 `49863b7` — cloud sync (`users/{uid}/workout_programs`), login pull, Drive backup/restore field `workoutPrograms` (defaulted), account-deletion cleanup, cloud-failure message.
3. [x] AC-3 `1d2e631` — `ProgramsSection.kt`; Exercise tab switch Activity | Workouts | Programs; build days from a saved workout / a new workout in the editor / a rest day; edit, reorder, delete; Start runs the guided session; only the scheduled (next) day advances the program (`completeProgramDay(id, dayIndex)`), cyclic.
4. [x] AC-4 `68bec26` — "Generate with AI": `generateWorkoutProgram` (2 to 7 days, optional focus, at most 2 rest days), `GeneratedProgram` model, `generateWorkoutProgramWithAI`, dialog with progress and retry.
5. [x] AC-5 `4c8363d` — "Today's workout" card on the Today screen (most recently used program, only for today's date); Start asks the view model (`requestProgramDayStart`) and the Exercise tab starts the session; rest days have "Mark rest day done".

**Part Z Phase 2 — consolidation into the Assistant**
- [x] Z2-1 `bf699f0` — route `universal_assistant?starter={starter}` (new chat + auto-sent starter, guarded against re-send); Sleep "AI Wellness Coach" and Food Log chat button use it.
- [x] Z2-2 `9566de4` — old `SavedChat`s shown in the Assistant's History tab ("Older saved chats") with a read-only `LegacyChatViewer`; Health "Saved chats" opens that tab.
- [x] Z2-3 `4c8363d` — `ChatScreen.kt`, `CoachScreen.kt`, `FoodChatScreen.kt` and the `chat`, `coach`, `food_chat` routes deleted. The Scanner's chat button now opens the Assistant (`starter=nutrition`, see ACTIVE.md for the follow-up fix).
- [x] Z2-4 `4c8363d` — "Ask AI for Today's Workout" (AI tab) and "Ask AI for a Diet Chart" (Diet Chart screen) open the Assistant with a starter; the Assistant's tools save the result to Saved Workouts / My Diet Charts. The old inline generators remain in the code but nothing calls them.

**Sprint-9 leftovers shipped in the same stretch:** football and other sports labels/icons `35974ef`; AI generator suggests rounds + preview/saved-workout round line `dc05333`; "Add exercise" picker in the workout editor `2d112dc`; account deletion removes social data `824096d`; Firestore rules: owner-delete for friend codes, activity events and own DM messages, token rule enabled (`38d7da3`).

Dead code left on purpose (clean up in a separate, careful pass): view-model/repository functions that only the removed screens or the old generate buttons used (e.g. `sendChatMessage`, `generateCoachAdvice*`, `generateAIStructuredWorkout`, `generateDietChart` in the view model, the food-chat state).
