# Active sprint items

Only open or unverified work lives here. Budget ~12 KB — when a Part closes, move its notes to `archive/` and leave one line in [INDEX.md](INDEX.md).

Current as of `c2b6bf8` (2026-10-05). Statuses marked *code-checked* were confirmed by reading the code at that commit, not from a push-verification.

## debug.keystore
Present (sha256 `4d6742b2...5f0faa`, 2666 B) since the web-upload restore `e85362f`. It has now been deleted **three times in a week by AI Studio pushes**: `3cb859b` ("chore: remove debug keystore", AI Studio's own commit), `65ca328`, and `6684f03`. Cause: AI Studio's working copy lacks the file, so any push deletes it again even after a restore on GitHub (restores: `f864870`, `78c6447`, `e85362f`). Always: restore on GitHub AND sync AI Studio from GitHub; check AI Studio's diff for a deleted `debug.keystore` before every push (every prompt says STOP and restore if it does). A tag build without the keystore fails (the debug signing config needs it) — the new "Pre-flight checks" workflow step catches this. Earlier history: [archive/00-tracker-preamble.md](archive/00-tracker-preamble.md).

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

## Session fixes — 2026-10-05
- [x] **AI chat dead in GitHub-built APKs.** API keys reach the app through a build-time `.env` file (Secrets Gradle plugin, falling back to `.env.example`, whose values are `MY_…` placeholders). The CI build had no `.env` and passed no keys, so releases 4 to 6 shipped placeholder keys and every Gemini call failed; AI Studio's own builds inject real keys. Fix: repository secrets `GEMINI_API_KEY`, `GEMINI_API_KEY_2`, `GEMINI_API_KEY_3`, `YOUTUBE_API_KEY`, passed as `env:` to the "Build debug APK" step (`48b1abc`); AI Studio's `build.gradle.kts` code (`422d39d`) writes `.env` from those variables. Verified: chat works on release 7. Caveat: the keys are embedded in a public APK and can be extracted — set quotas / billing alerts in Google Cloud; restricting by Android app is impractical (the app's raw REST calls don't send the restriction headers); a small proxy is the real fix.
- [x] **AI Studio's chat-code changes** (`6684f03`, `422d39d`), reviewed 2026-10-05, no UI files touched: `resolveApiKeys` no longer treats the YouTube key as a Gemini key; HTTP 400 (invalid key) now falls through to the next key/model; chat history is sanitised before sending (blank messages dropped, a leading AI greeting removed, never empty — Gemini rejects requests that start with an AI turn); model list updated (`gemini-3.5-flash` and `gemini-3.1-pro-preview` added, `gemini-2.5-flash` removed); user-facing errors are now the generic "Sorry, I couldn't process that. Please try again.", which hides the real cause when debugging.
- [x] **Releases 5 to 7.** Tag `5` was cut with `versionCode = 4`, so the installed app kept seeing "update available". Tag `6` was created one commit before the keystore restore (`e85362f`), so it built without `debug.keystore`. Tag `7` (`c2b6bf8`, `versionCode = 7`) built correctly. The workflow now has a "Pre-flight checks" step (`c2b6bf8`) that fails a tag build early if the tag ≠ `versionCode`, `debug.keystore` is missing or has the wrong checksum, or an API-key secret is empty.

## Session fixes — 2026-10-04
- [x] **Release build failing ("Malformed root json")**: the repository secret `GOOGLE_SERVICES_JSON` did not exist, so the workflow wrote an empty `app/google-services.json` (the Gradle plugin tolerates a *missing* file via `missingGoogleServicesStrategy = WARN`, but not an empty one). The "Write google-services.json from secret" step was rewritten (`120ebbf`) to take the secret through an env var, accept raw JSON or base64, and fail with a clear message if it is empty or not a Firebase config. Zunaid created the secret; `versionCode` bumped to 4 (`b72c1e0`); release tag `4` built.
- [x] **KSP shutdown message** in the build log (`NullPointerException … ksp.com.intellij…ApplicationManager.getApplication() is null` on thread `AWT-EventQueue-0`) is harmless noise from KSP shutting down its embedded compiler; judge a build by the job status and the APK on the release, not by that line.
- [x] **Graphite Black Today-background experiment** removed again (`4090c09`, `b328258`) after it rode along in `78783b9`.
- Updater rule: `UpdateChecker` parses the release **tag as an integer** and compares it to the installed `versionCode`; the tag must equal the `versionCode` built into that APK.

## Session fixes — 2026-09-29
- [x] **Undeclared Health Connect permissions** (`48b82a5`): `READ_RESTING_HEART_RATE`, `WRITE_BLOOD_PRESSURE` and `WRITE_BLOOD_GLUCOSE` were requested but never declared in the manifest, so blood pressure/glucose writes and resting-HR reads had silently failed since they were built. Users must tap Settings → Health Connect → Connect once to grant them (and `WRITE_WEIGHT`, below).
- [x] **Markdown renderer** (`48b82a5`): `MarkdownText` now renders tables as one card per row, `---` as dividers, and `YOUTUBE_SEARCH: <query>` lines as a red Watch button. Applies to every screen that uses it.
- [x] **Diet chart video links** (`0851817`): both diet chart prompts emit a `YOUTUBE_SEARCH:` line after each meal's recipe. Charts saved before this have no buttons.
- [x] **Permission labels** (`0805980`): `HealthConnectManager.getPermissionDisplayName` used to check camel-case names like `"HeartRate"` against real strings like `…READ_HEART_RATE`, so most multi-word permissions showed raw names; it now normalises the string (`READ_HEART_RATE` → `heartrate`) and looks up an exact label; all 29 requested permissions resolve. Minor deviation: the fallback for unmapped permissions still returns the raw name instead of a title-cased one — invisible today (nothing unmapped is requested). Add a label whenever a permission is added.
- [x] **Assistant recipe links** (`0805980`): the assistant prompt asks Gemini to keep `YOUTUBE_SEARCH:` lines, and `sendUniversalAssistantMessage` re-appends any `generate_recipe` video line the reply dropped. Edge case: if Gemini rewrites the line (e.g. adds brackets), the exact-match check can show a second Watch button for the same recipe. Harmless.

## Part Z — Universal AI Assistant
Shipped: repository layer, ViewModel wiring, screen, global floating button, tool-calling with plain-chat fallback, persisted history, save generated diet charts/workouts, multi-model/multi-key fallback, chat sessions (`8555d67` … `994c07c`).
- Rule: function-response role is `"function"` (`ed1c511` reverted the `"user"` change).
- AI answer look and structure (card, renderer, style guide, follow-up chips) is Part Y — closed, see [archive/PART-Y-ai-response-overhaul.md](archive/PART-Y-ai-response-overhaul.md).
- [ ] Phase 2 (deferred by Zunaid): consolidate all AI features into the assistant and remove individual entry points.

## Carried over
- [ ] `WorkoutScreen.kt` "AI Workouts" tab label wrap — label is `fontSize = 13.sp` but has no `maxLines`; confirm on a device (left over from Part R, now closed).
- [ ] Part I #7 — real-device heart-rate check (Zunaid only).
- [ ] Part P #6 — multi-day workout programs (deferred, needs its own scoping).
- [ ] Tile customization via DataStore — not built (*code-checked*: no DataStore usage; tile slots persist via `updateTodayTileSlots` on the profile instead). Confirm whether that's sufficient.
- [ ] Repo hygiene — 34 one-off root scripts (`fix_*.py`, `update_*.py`, `rewrite_*.py`, `add_*.py`, `test_*.kt`). Candidate for an AI Studio cleanup prompt.
- Thriva name / T-monogram explored, not implemented; shipping name is KardIQ.
