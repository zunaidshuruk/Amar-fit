<!-- Moved verbatim from docs/sprints/ACTIVE.md on 2026-10-05 to keep ACTIVE.md under its size budget. The newer 2026-10-05 session fixes stay in ACTIVE.md. -->

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
