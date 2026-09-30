# Active sprint items

Only open or unverified work lives here. Budget ~12 KB — when a Part closes, move its notes to `archive/` and leave one line in [INDEX.md](INDEX.md).

Current as of `e54f0c4` (2026-09-29). Statuses marked *code-checked* were confirmed by reading the code at that commit, not from a push-verification.

## debug.keystore
Restored 2026-09-29 by web upload (`46dbc23`/`bdb2a43`), checksum verified, and unchanged through `e54f0c4`. Keep checking after every push. Incident history: [archive/00-tracker-preamble.md](archive/00-tracker-preamble.md).

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
- **Palette as shipped:** background Gun Metal `#00272B`, signature accent **`#BBE800`** (set in `6300106` and confirmed by Zunaid — supersedes the `#E0FF4F` in the archived Part R notes), secondary Iron `#CDD2D7`, alert Amber `#F5A623`. `#BBE800` contrast: 11.1:1 on background, 9.7:1 on cards, 8.6:1 on inner tiles.
- [x] Metric-detail chart tokens: range-bar charts (`78783b9`) and daily bars / trend lines / intraday steps, calories, water (`e54f0c4`) now use `primary` (accent) and `onSurfaceVariant` (Iron) instead of literals. SpO2 bars: Amber below 95%, accent otherwise.
- [ ] Heart-rate zone colors (`IntradayHeartRateChart`, `HeartRateZonesCard`) — still literals; needs a decision, since 4 zones need 4 distinguishable colors vs. the single-accent rule.
- [ ] **Graphite Black experiment still live:** `78783b9` accidentally re-added `GraphiteBlack` (`Color.kt`) and `.background(GraphiteBlack)` on Today's root (`TodayScreen.kt`). Zunaid asked to revert; the revert push didn't land and he chose to skip it for now. Revert = delete those two lines.
- [ ] Screen-by-screen rollout to the remaining screens (order not yet agreed).
- [ ] `WorkoutScreen.kt` "AI Workouts" tab wrap — label now `fontSize = 13.sp` but no `maxLines`; confirm on device.
- [x] Phase 2e calorie-ring large tile landed (*code-checked*: `"large_calories"` in `TodayTiles.kt`); whether it's in the default set is unverified.
- Decision still open: none recorded.

## Session fixes — 2026-09-29
- [x] **Undeclared Health Connect permissions** (`48b82a5`): `READ_RESTING_HEART_RATE`, `WRITE_BLOOD_PRESSURE` and `WRITE_BLOOD_GLUCOSE` were requested but never declared in the manifest, so blood pressure/glucose writes and resting-HR reads had silently failed since they were built. Users must tap Settings → Health Connect → Connect once to grant them (and `WRITE_WEIGHT`, below).
- [x] **Markdown renderer** (`48b82a5`): `MarkdownText` now renders tables as one card per row, `---` as dividers, and `YOUTUBE_SEARCH: <query>` lines as a red Watch button. Applies to every screen that uses it.
- [x] **Diet chart video links** (`0851817`): both diet chart prompts emit a `YOUTUBE_SEARCH:` line after each meal's recipe. Charts saved before this have no buttons.
- [x] **Permission labels** (`0805980`): `HealthConnectManager.getPermissionDisplayName` used to check camel-case names like `"HeartRate"` against real strings like `…READ_HEART_RATE`, so most multi-word permissions showed raw names; it now normalises the string (`READ_HEART_RATE` → `heartrate`) and looks up an exact label; all 29 requested permissions resolve. Minor deviation: the fallback for unmapped permissions still returns the raw name instead of a title-cased one — invisible today (nothing unmapped is requested). Add a label whenever a permission is added.
- [x] **Assistant recipe links** (`0805980`): the assistant prompt asks Gemini to keep `YOUTUBE_SEARCH:` lines, and `sendUniversalAssistantMessage` re-appends any `generate_recipe` video line the reply dropped. Edge case: if Gemini rewrites the line (e.g. adds brackets), the exact-match check can show a second Watch button for the same recipe. Harmless.

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
