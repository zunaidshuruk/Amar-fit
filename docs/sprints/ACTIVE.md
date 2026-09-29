# Active sprint items

Only open or unverified work lives here. Budget ~12 KB — when a Part closes, move its notes to `archive/` and leave one line in [INDEX.md](INDEX.md).

Current as of `8a79a86` (2026-09-29). Statuses marked *code-checked* were confirmed by reading the code at that commit, not from a push-verification.

## debug.keystore
Restored 2026-09-29 by web upload (`46dbc23`/`bdb2a43`), checksum verified, and unchanged through `8a79a86`. Keep checking after every push. Incident history: [archive/00-tracker-preamble.md](archive/00-tracker-preamble.md).

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

## Session fixes — 2026-09-29
- [x] **Undeclared Health Connect permissions** (`48b82a5`): `READ_RESTING_HEART_RATE`, `WRITE_BLOOD_PRESSURE` and `WRITE_BLOOD_GLUCOSE` were requested but never declared in the manifest, so blood pressure/glucose writes and resting-HR reads had silently failed since they were built. Users must tap Settings → Health Connect → Connect once to grant them (and `WRITE_WEIGHT`, below).
- [x] **Markdown renderer** (`48b82a5`): `MarkdownText` now renders tables as one card per row, `---` as dividers, and `YOUTUBE_SEARCH: <query>` lines as a red Watch button. Applies to every screen that uses it.
- [x] **Diet chart video links** (`0851817`): both diet chart prompts emit a `YOUTUBE_SEARCH:` line after each meal's recipe. Charts saved before this have no buttons.
- [ ] **Permission labels bug:** `HealthConnectManager.getPermissionDisplayName` checks names like `"HeartRate"`, but the real strings are `…READ_HEART_RATE`, so Heart Rate, Blood Pressure, Blood Glucose, SpO2, HRV, Resting HR, Active/Total Calories, Skin Temp, Respiratory Rate and BMR show raw names in Settings' denied-permissions dialog. Cosmetic.
- [ ] **Assistant recipe links:** the Universal Assistant's `generate_recipe` returns a `YOUTUBE_SEARCH:` line to Gemini, but Gemini's final answer may paraphrase it away. Not yet addressed.

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
