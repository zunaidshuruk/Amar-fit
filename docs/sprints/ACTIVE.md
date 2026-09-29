# Active sprint items

Only open or unverified work lives here. Budget ~12 KB — when a Part closes, move its notes to `archive/` and leave one line in [INDEX.md](INDEX.md).

Current as of `994c07c` (2026-09-29). Statuses marked *code-checked* were confirmed by reading the code at that commit, not from a push-verification.

## 🔴 Hazard — debug.keystore
Missing from `main` since `8d06c8b` ("refactor: fix formatting in UniversalAssistantScreen"); last good copy at `0537d66`. `app/build.gradle.kts` still points the debug signing config at `${rootDir}/debug.keystore`. Restore via GitHub web upload, then confirm sha256 `4d6742b2662bfcbbb8e0a6f693d3fb939f4d2d10a446336af85a619f275f0faa`. Full incident history: [archive/00-tracker-preamble.md](archive/00-tracker-preamble.md).

## Untracked work since 8037b30
~175 commits landed after the tracker's last verified commit. The major ones (from `git log`, not item-by-item verified in this tracker):
- Hero-tile flip + badge-gallery medallion redesign; HorizontalPager variant tried and reverted (`2f5b389` → `6f1bd8a`, `df2c75b`); graphicsLayer import settled on `androidx.compose.ui.graphics.graphicsLayer` (`530e101`) — **the correct package**; the 089358b handover's claim that it belongs in `androidx.compose.ui.draw` was wrong, don't reintroduce it.
- Social (Part Q) — see below.
- KardIQ rebrand (`eac71c6`, `57b069f`), launcher icons via uploads.
- FCM + GitHub Actions polling notifications (`992c5cd`, `.github/workflows/push-notifications.yml`).
- Conversational AI food logging with review-before-log, voice input, persisted chat (`873dd3b`, `d8d22fa`, `09b55f8`).
- In-app updater via GitHub Releases (`3a58269`); versionCode bumps (`e91f830`, `0ffe50e`).
- Privacy policy, ToS, consent gate (`9927c66`, `cbc3fbd`).
- Automatic AI features converted to on-demand (`3ede3bd`).
- Part R Phase 3 nutrition dashboard (`91d52c4`); Health Correlations screen (`5c74249`); Today declutter (`6bdded9`); glucose-in-vitals fix (`7521c69`).
- Two accidental full-project deletions, both reverted (`607b062`, `0537d66`).
- Part Z assistant — see below.

## Part Q — Social
Shipped: friend codes, QR scan, requests, leaderboard, friend stats, challenges, kudos, friend removal, activity feed, DMs (`ed3db2f` … `5b4d21e`). Challenge notifications/browse UI removed (`087a6f3`).
- [ ] **Firestore rules drift (code-checked):** repo `firestore.rules` only allows `users/{uid}/**`, but `FirebaseManager.kt` uses top-level `public_profiles`, `friendships`, `friend_codes`, `friend_stats`, `challenges`, `dm_threads`, `kudos`. Either the console rules differ from the repo file, or social reads are failing. Zunaid: copy the live console rules into the repo so they're reviewable.

## Part R — Dark theme redesign
- [x] Pill-row "featured first row" landed (*code-checked*: `TodayPillCard(featured = rowIndex == 0)`).
- [x] Phase 3 nutrition macro dashboard (`91d52c4`).
- [ ] Screen-by-screen rollout to the remaining screens (order not yet agreed).
- [ ] `WorkoutScreen.kt` "AI Workouts" tab wrap — label now `fontSize = 13.sp` but no `maxLines`; confirm on device.
- [x] Phase 2e calorie-ring large tile landed (*code-checked*: `"large_calories"` in `TodayTiles.kt`); whether it's in the default set is unverified.
- Decision still open: none recorded.

## Part T — Measurement metadata
Remaining gaps (*code-checked* in `presentation/viewmodel/ShasthoViewModel.kt`):
- [ ] `NutritionRecord(...)` write sets only `energy`/`name` — `FoodLog.mealType` not passed.
- [ ] Workout writes use `ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT`.
- [ ] `WeightRecord` is never written to Health Connect.

## Part U — Graph types
- [ ] Phase 2 Blood Pressure range-bar chart appears landed (`3524ab4`; `MetricDetailScreen.kt` routes `bloodPressure` through the range-bar branch) — needs a proper diff verification.

## Part Z — Universal AI Assistant
Shipped: repository layer, ViewModel wiring, screen, global floating button, tool-calling with plain-chat fallback, persisted history, save generated diet charts/workouts, multi-model/multi-key fallback, chat sessions (`8555d67` … `994c07c`).
- Rule: function-response role is `"function"` (`ed1c511` reverted the `"user"` change).
- [ ] Phase 2 (deferred by Zunaid): consolidate all AI features into the assistant and remove individual entry points.

## Carried over
- [ ] Part I #7 — real-device heart-rate check (Zunaid only).
- [ ] Part P #6 — multi-day workout programs (deferred, needs its own scoping).
- [ ] Tile customization via DataStore — not built (*code-checked*: no DataStore usage; tile slots persist via `updateTodayTileSlots` on the profile instead). Confirm whether that's sufficient.
- [ ] Repo hygiene — 34 one-off root scripts (`fix_*.py`, `update_*.py`, `rewrite_*.py`, `add_*.py`, `test_*.kt`). Candidate for an AI Studio cleanup prompt.
- Thriva name / T-monogram explored, not implemented; shipping name is KardIQ.
