# Security review of KardIQ (sprint BI), status on 2026-10-18

Plan: `docs/sprints/PLAN-BI-security-review.md`. This page is the living report: every finding, its severity and what is done.
Guard tests that keep the fixes in place: `SecurityGuardTest`, `InsetsGuardTest`, `NotificationGateGuardTest`, `CycleIsolationTest`.

## Fixed in this pass (phase 1)
| # | Finding | Severity | Fix |
|---|---|---|---|
| 2 | Android backup copied the whole local database (health, cycle, medicine, messages) to the cloud and to other devices | High for a health app | `allowBackup="false"`; both backup rule files exclude everything as a second layer. Data comes back from the user's own account (Firestore sync) and the in-app Google Drive backup. *Default chosen while the decision is open; easy to change.* |
| 5 | The push token was written to the log | Low | removed; a test fails the build if a token, password or key is logged |
| 6 | `BootReceiver` was exported (any app could trigger it) | Low | `exported="false"` (the system still delivers boot and update broadcasts); a test pins the list of exported components |
| 7 | Any app could open any screen of KardIQ by sending `NAVIGATE_TO` to `MainActivity` | Low | only screens our own notifications use are accepted (`NavTabs.isAllowedNotificationRoute`), including a strict pattern for chat links; unit tested with hostile inputs |
| new | Secrets in the repository | n/a | **checked: clean.** All 90 commits were searched for private keys, GitHub tokens, cloud keys, service accounts and secret files: none. The Firebase client key in `strings.xml` is public by design (see "Your actions"). A test now scans every file on every build |

## Open: needs a decision from Zunaid
| # | Finding | Severity | Options |
|---|---|---|---|
| 1 | **Gemini (3) and YouTube API keys are compiled into the APK** (`BuildConfig`); anyone can extract them and use up the quota or bill | High | (a) restrict the keys in Google Cloud now (see below) and keep them in the app, or (b) move AI calls behind Firebase AI Logic with App Check (needs a plan check; no Cloud Functions on the free plan). Recommended: (a) now, (b) as its own sprint |
| 12 | The local database is not encrypted | Medium | encrypt with SQLCipher (more safety, a one-time migration, a little slower), or accept (Android already encrypts a locked phone). Recommended: accept for now |
| 10 | `MusicNotificationListenerService` reads other apps' notifications (for the workout music controller) | Medium (privacy, and a Play review question) | keep and disclose, or remove the feature, or limit it to media sessions only |

## Open: planned for sprint BJ (store readiness) or later
| # | Finding | Severity | Plan |
|---|---|---|---|
| 3 | The in-app updater installs an APK from GitHub; sideload builds are signed with the **public** `debug.keystore` | High for sideload users | the Play build has no updater at all; for sideload add a SHA-256 check published with each release |
| 4 | Release builds are not shrunk or obfuscated (`isMinifyEnabled = false`) | Low to Medium | turn on R8 with tested rules when the release build is tested on a device |
| 9 | 26 read and 12 write Health Connect permissions | Medium | keep only what visible features use (Play reviews each one) |
| 8 | `FileProvider` shares the whole external files folder | Low | move the update file into its own folder and narrow the path (sideload only) |
| 11 | Friend codes can be probed, DMs can be spam, no block or report | Low to Medium | block and report, rate limits (Part Q review) |

## Your actions (only you can do these; about 20 minutes in Google Cloud Console)
1. **Restrict the Gemini and YouTube keys** (APIs and Services, Credentials, open each key): *Application restrictions*: Android apps, add package `com.aistudio.amarfit.app` with the SHA-1 of your **signing certificate** (and `com.aistudio.amarfit.app.debug` with the debug certificate SHA-1 `E2:C1:39:4D:60:6F:0E:D5:A8:C8:F1:69:98:C2:CD:6E:A5:CC:0D:7B`). *API restrictions*: only the one API each key needs (Generative Language API; YouTube Data API v3). Set a **daily quota** and a **budget alert**. Do this for all four keys.
2. **Restrict the Firebase key** (the `AIza...` key in `strings.xml`, shown in Credentials as "Android key (auto created by Firebase)"): same Android app restriction; API restriction to Firebase Authentication, Cloud Firestore, Firebase Installations, FCM registration and the Identity Toolkit APIs the app uses.
3. **Turn on Firebase App Check** (Firebase Console, App Check, Play Integrity) in *monitor* mode first, then enforce once the Play build is out. This stops other apps from using your Firebase project.
4. **GitHub repository, Settings, Security**: switch on *Dependabot alerts*, *Dependabot security updates* and *Secret scanning with push protection*.
5. **Rotate the Gemini and YouTube keys** after step 1 if you ever shared the APK widely before restricting them, and update the GitHub secrets (`GEMINI_API_KEY`, `GEMINI_API_KEY_2`, `GEMINI_API_KEY_3`, `YOUTUBE_API_KEY`).

## Reviewed and fine
No WebViews, no cleartext HTTP in code (Android blocks it by default), no SQL built from text (Room only), no `debuggable` in release, Firestore rules audited in earlier sessions (`docs/firestore-rules.md`, `docs/social-review-2026-10-15.md`), notification content hidden on the lock screen for cycle and medicine data, account deletion removes the user's data, `.env.example` holds placeholders only.
