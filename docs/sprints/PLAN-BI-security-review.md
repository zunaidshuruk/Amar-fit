# Sprint BI: security review (planned, 2026-10-17)

**Goal:** find and fix security weaknesses before the app goes to the Play Store, and leave tests behind that stop them coming back.
**Assets worth protecting:** health data (steps, sleep, glucose, blood pressure, weight), cycle and fasting logs, medicine names, messages and friend data, the account (email, push token), the AI keys and the Firebase project.
**Who could attack:** another user of the app, a malicious app on the same phone, anyone who downloads the APK, anyone who reads the public GitHub repo, someone on the network, someone who finds a lost phone.

## Already found by a first scan (2026-10-17, from the code; each needs a decision or a fix)
| # | Finding | Why it matters | Likely fix |
|---|---|---|---|
| 1 | **API keys are compiled into the app** (three Gemini keys and the YouTube key, `BuildConfig`, `app/build.gradle.kts`) | anyone can extract them from the APK (shrinking does not hide them) and use up the quota or the bill | restrict each key in Google Cloud to the app's package and signing certificate and set quotas now; then move AI calls behind Firebase AI Logic with App Check (needs a plan check: the free plan has no Cloud Functions) or another server-side proxy; rotate the keys |
| 2 | **Backup rules are empty** (`allowBackup=true` and `data_extraction_rules.xml` is the unedited sample) | the whole local database (health records, profile, medicine data) goes into Android backups and device-to-device transfer, and can be read from a debuggable build | decide what may be backed up; exclude the database and preferences, or keep them only for device transfer |
| 3 | **The in-app updater installs APKs downloaded from GitHub** (`REQUEST_INSTALL_PACKAGES`, `UpdateChecker`) and the shipped debug APK is signed with the **public** `debug.keystore` | anyone can build an APK with the same package name and the public key; a person who installs it would replace the real app and get its data | verify the downloaded file (SHA-256 published in the release) before installing; stop shipping debug-signed builds to users; the Play build has no updater at all (see sprint BJ) |
| 4 | **No code shrinking or obfuscation in release builds** (`isMinifyEnabled = false`) | the code is easy to read and copy | turn on R8 with correct rules (Moshi, Room, Firebase, Health Connect) and test |
| 5 | **The push token is written to the log** (`AmarFitMessagingService`: `Log.d` of the token) | logs can be read over a cable on a debuggable build | remove; add a test that no token or key is logged |
| 6 | **`BootReceiver` is exported** | other apps can trigger it (it re-arms reminders and, after an update, deletes the downloaded installer) | make it not exported (the system still delivers boot and update broadcasts) |
| 7 | **`MainActivity` accepts a navigation target from any app** (`NAVIGATE_TO` extra) | another app can open any screen of KardIQ by name | allow-list the routes it will accept |
| 8 | **`FileProvider` shares the whole external files folder** (`external_files`, path `.`) | a file link could point at any file in that folder | narrow the paths to the one folder that is shared |
| 9 | **Very long health permission list** (26 read, 12 write, plus history, skin temperature, menstruation) | each one is a privacy exposure and a Play review question | keep only what a visible feature uses; drop the rest |
| 10 | **Notification access** (`MusicNotificationListenerService`) reads other apps' notifications | sensitive permission; Play asks for a justification | keep only if the workout music feature needs it; read only media sessions; disclose it |
| 11 | Friend codes can be probed and DMs can be spam (Part Q review, findings 3 to 5) | harassment and enumeration | block and report, rate limits |
| 12 | Room database and preferences are not encrypted | a rooted or lost, unlocked phone shows everything | decide: SQLCipher or accept (Android file encryption already covers a locked phone) |

## Phases
1. **Threat model and inventory** (1 day): list data flows (phone, Firestore, Google Drive backup, Gemini, YouTube, GitHub, FCM) and who can read each.
2. **Code and configuration review**: manifest (exported components, permissions, backup, cleartext, debuggable), intents and deep links, file sharing, logging, WebViews (none found), SQL (Room only), clipboard, screenshots of sensitive screens (consider `FLAG_SECURE` for cycle and medicine screens).
3. **Secrets and supply chain**: search the whole git history for leaked secrets, check `google-services.json` key restrictions, GitHub Actions secrets and who can read them, the service account used by the notification job (least privilege), dependency vulnerabilities (enable Dependabot alerts, run an OWASP dependency check), third-party SDK list.
4. **Backend**: re-audit `firestore.rules` against every collection with a test matrix (owner, friend, stranger, signed out), Storage rules (if used), App Check, abuse limits.
5. **Release hardening**: R8, no debug logging, signing key handling (upload key kept secret, never in the repo), updater verification, `debuggable=false`.
6. **Fix, test, report**: fix findings in small reviewed pull requests; add guard tests (exported components allow-list, no secrets or tokens in code or logs, manifest rules); final report `docs/security-review.md` with every finding, its severity and its status.

## Decisions I need from Zunaid
* AI keys: restrict-only now and server-side later, or go straight to Firebase AI Logic with App Check?
* Backup: should any app data be allowed into Android backups (Drive backup already exists in the app)?
* Encrypt the local database (more safety, a bit more complexity and a one-time migration)?

## Deliverables
`docs/security-review.md`, the fixes, new guard tests, a rotated and restricted set of keys, and a short list of anything left that only Zunaid can do (for example key restrictions in Google Cloud Console).
