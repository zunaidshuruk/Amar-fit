# KardIQ — agent guide

KardIQ (formerly Amar Fit) is a native Android health and fitness app for Bangladeshi users. Kotlin, Jetpack Compose Material 3, MVVM + Coroutines + StateFlow, Room, Firebase Auth/Firestore/FCM, Health Connect, Gemini. Single app-wide ViewModel: `ShasthoViewModel`.

## Hard rules
- **Never delete, regenerate or modify `debug.keystore`** at the repo root. Known-good sha256 `4d6742b2662bfcbbb8e0a6f693d3fb939f4d2d10a446336af85a619f275f0faa` (2666 bytes). Don't touch signing config or `google-services.json`.
- **Room:** every schema change bumps the version and appends a new `MIGRATION_X_Y`; never destructive, never drop data.
- **Firestore-mapped data classes:** every field has a default value (otherwise `toObject()` fails silently on login).
- **Gemini function responses use `role: "function"`.** Do not change it to `"user"`.
- **Auth:** never anonymous auth for DB sync.
- **UI:** every `onClick` wires to a ViewModel or permission launcher. Colors come from `MaterialTheme.colorScheme` / `AccentTokens`, never hardcoded literals; never white-on-white text. App is dark-only.
- **AI text:** show AI answers through `AiMessageCard` / `MarkdownText`; chat prompts get the shared `aiResponseStyle` guide. The `YOUTUBE_SEARCH:` and `FOLLOWUPS:` convention lines must each stay on their own line, never inside a table.
- **Health Connect:** every permission needs both a manifest `<uses-permission>` and a `REQUIRED_PERMISSIONS` entry — one without the other silently never works.
- **Imports:** `Modifier.graphicsLayer` is `androidx.compose.ui.graphics.graphicsLayer`.
- **Scope:** change only the files a task names. Don't revert newer work by editing from an old snapshot.

## Where to look
1. [docs/map/PRODUCT.md](docs/map/PRODUCT.md) — which files own a feature. Open only those.
2. [docs/map/HOTSPOTS.md](docs/map/HOTSPOTS.md) — how to read the giant files by anchor.
3. [docs/map/OPERATIONS.md](docs/map/OPERATIONS.md) — build, release, notifications, signing, Firebase, migrations.
4. [docs/sprints/ACTIVE.md](docs/sprints/ACTIVE.md) — open work. History: [docs/sprints/INDEX.md](docs/sprints/INDEX.md).

Root `fix_*.py`, `update_*.py`, `rewrite_*.py`, `add_*.py`, `remove_fit.py` and `test_*.kt` are historical one-off scripts, not part of the build — don't edit or run them.

## Keep the map true
A change that adds, moves or removes a file named in `docs/map/` updates that row in the same change. A closed sprint Part moves verbatim from `ACTIVE.md` to `docs/sprints/archive/`. Keep this file under ~6 KB.
