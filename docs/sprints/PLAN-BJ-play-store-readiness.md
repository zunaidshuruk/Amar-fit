# Sprint BJ: Google Play readiness checklist (planned, 2026-10-17; runs after sprint BI)

**Goal:** everything the app and the listing need so that Google accepts it and users can install it. The step-by-step publishing guide for Zunaid is `docs/play-store-guide.md`.
Requirements below were checked against Google's own pages on 2026-10-17; re-check each before submitting, they change.

## A. The app itself (Claude does these)
- [ ] **Target Android 16 (API 36):** already done (`targetSdk = 36`). New apps and updates must target API 36 since 2026-08-31.
- [ ] **Release build as an Android App Bundle (.aab)**, signed with an **upload key** (a new keystore kept secret, never in the repo) and Play App Signing. Add a GitHub Actions job that builds `bundleRelease` and attaches the file.
- [ ] **`play` and `sideload` build flavors.** The Play build has **no self-updater and no `REQUEST_INSTALL_PACKAGES`** (Google Play does not allow an app to update itself any other way). The sideload build keeps the updater for GitHub releases. Both have separate application ids or the Play build uses `com.aistudio.amarfit.app`.
- [ ] **R8 shrinking on** with tested rules (Moshi, Room, Firebase, Health Connect, ML Kit).
- [ ] **16 KB memory page support** for every native library (ML Kit barcode, on-device AI): check the `.so` alignment of the bundle; required for apps with native code.
- [ ] **Health permissions trimmed** to what visible features use; every remaining one is justified in the Health apps declaration (Play reviews each).
- [ ] **Notification access** justified or removed (`MusicNotificationListenerService`).
- [ ] **In-app report button for AI answers** ("report this response" in the Assistant, Coach, and other generated text). Required for apps whose generative AI chat is a central feature: users must be able to report or flag offensive AI content without leaving the app.
- [ ] **Account deletion**: the in-app deletion exists; Play also wants a **web page** where a person can request deletion (a URL for the Data safety form).
- [ ] **Medical disclaimers** visible where health advice or predictions appear (cycle tracker, AI health summaries, glucose guidance): "not medical advice, not a medical device".
- [ ] **Privacy policy** reachable inside the app (done) **and at a public web address** (host it, for example with GitHub Pages from `docs/`), and the same address in Play Console and in Health Connect.
- [ ] **Crash and ANR visibility** (Play Console vitals; consider Crashlytics) and a Play pre-launch report review.
- [ ] **Remove debug-only things from the Play build** (debug logging, test tags are fine).

## B. Play Console forms (Claude drafts every answer, Zunaid enters and confirms them)
- [ ] App content: privacy policy URL, **App access** (reviewers need a working test login: create a demo account and give its email and password), ads (none), **target audience** (not for children; 18+ is simplest because of health content), **content rating** (IARC questionnaire), **Data safety** (what is collected, shared, encrypted, how to delete), **Health apps declaration** (health features and every Health Connect data type), financial features (none), government app (no), news (no).
- [ ] Store listing: name (30 characters), short description (80), full description (4000), **app icon 512 x 512**, **feature graphic 1024 x 500**, **at least 2 phone screenshots** (more is better; I can prepare a list of the best screens to capture), category **Health & Fitness**, contact email, optional website, countries.
- [ ] Pricing: free (no merchant account needed).

## C. Account and testing path (Zunaid, with Claude's checklist)
- [x] **Decision (Zunaid, 2026-10-17): a personal account.** The store work is parked until after the theme release; the 12 testers for 14 days rule below therefore applies. (Was: decide **personal or organization account**.) A personal account created after 2023-11-13 must run a **closed test with at least 12 testers opted in for 14 continuous days** before it may apply for production. Organization accounts are exempt but need a D-U-N-S number and business verification. (Bangladesh is a supported registration location.)
- [ ] Pay the one-time registration fee, verify identity, set up the payments profile (needed even for free apps), verify with the Play Console app on an Android phone if asked.
- [ ] Internal test (quick, up to 100 testers) then closed test (the 14-day clock) then **production access questionnaire** then production with a staged rollout.

## D. Definition of done
Signed bundle uploaded; every App content item green; listing complete; closed test finished; production access granted; staged rollout started; monitoring in place.

## Risks to know about now
* **The 14-day closed test** is the longest item on a personal account (plan about 3 weeks from first upload).
* **Health Connect review**: broad permission lists are the most common reason health apps are rejected; trimming them first saves weeks.
* **Moving existing users:** the Play version is signed with a different key, so people who installed the GitHub APK cannot update in place to the Play version; they would reinstall and restore from their Drive backup or cloud sync.
