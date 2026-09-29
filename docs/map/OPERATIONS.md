# Map — operations

Names only; never values. Never open `.env`, keystores or `google-services.json`.

| Task | What to know | Where |
|---|---|---|
| Build check | `./gradlew :app:compileDebugKotlin`. Claude's sandbox can't build; AI Studio's compiler is the only import/compile check. | `README.md` |
| APK release | Workflow runs on a tag push matching `[0-9]+` or manual dispatch; builds and attaches the APK to a GitHub Release. Uses secret `GOOGLE_SERVICES_JSON` (+ `GITHUB_TOKEN`). | `.github/workflows/build-and-release-apk.yml` |
| In-app updater | Compares installed `versionCode` against the latest GitHub Release. Bump `versionCode`/`versionName` before tagging. | `data/repository/UpdateChecker.kt`, `app/build.gradle.kts` |
| Push notifications | Scheduled every 5 min (plus manual dispatch) — GitHub's scheduler may delay. Runs the Node script with secret `FIREBASE_SERVICE_ACCOUNT_KEY`. No Cloud Functions / Blaze plan, by design. | `.github/workflows/push-notifications.yml`, `scripts/send_push_notifications.js` |
| FCM on device | Messaging service + channels | `presentation/notifications/AmarFitMessagingService.kt`, `NotificationHelper.kt` |
| Debug signing | `debug.keystore` at repo root, referenced by `app/build.gradle.kts`. Debug builds use `applicationIdSuffix = ".debug"`, which needs its own Firebase app registration for Google Sign-In. Check the checksum after every push (value in AGENTS.md). | `app/build.gradle.kts` |
| Release signing | Keystore path from env `KEYSTORE_PATH` (fallback file name in build.gradle.kts). | `app/build.gradle.kts` |
| Application id | `com.aistudio.amarfit.app` — deliberately unchanged by the KardIQ rebrand (changing it breaks upgrades and Firebase). | `app/build.gradle.kts` |
| Firestore rules | Repo copy may lag the console — social collections aren't covered by it (see ACTIVE.md, Part Q). Rules are published manually in the console. | `firestore.rules` |
| Room migrations | Bump `version` and append a `MIGRATION_X_Y` to `.addMigrations(...)`; never destructive. Read the current version there, don't copy it into docs. | `data/local/AppDatabase.kt`, `app/schemas/` |
| Gemini keys / models | Multiple keys and candidate models with fallback on failure. Key names from `.env.example` only. | `data/repository/AppRepository.kt` (`resolveApiKeys`), `.env.example` |
| Health Connect permissions | One permission set; adding a record type = add read/write permission here. | `data/health/HealthConnectManager.kt` |
| Rollback | Revert the commit or re-release a previous tag. Deleted keystore → restore via GitHub web upload from the last good commit. | — |
