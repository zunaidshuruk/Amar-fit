# How to put KardIQ on Google Play: step by step (for Zunaid)

Written 2026-10-17 from Google's own help pages; rules change, so each step says where to double check. **"Claude"** marks what I do in the repo; **"You"** marks what only you can do (accounts, payments, identity, clicking in Play Console).
The plan behind this is in `docs/sprints/PLAN-BJ-play-store-readiness.md`; the security sprint (`PLAN-BI`) should come first.

## Step 0: Decide the account type (You, 15 minutes)
**Decided 2026-10-17: a personal account.** So the 12-testers-for-14-days closed test (Step 7) applies. The rest of the store work is parked for now.

* **Personal account:** quickest to open. A personal account created after 13 November 2023 must run a **closed test with at least 12 testers, opted in for 14 days in a row**, before it can ask for production.
* **Organization account (for example Synesis IT PLC or your own company):** not subject to the 14-day test, but you need a **D-U-N-S number** and business verification (this can take weeks).
* Either way the app's public name and developer name will show on the store page.
* Check: Play Console Help, "App testing requirements for new personal developer accounts".

## Step 1: Create the developer account (You, about 1 hour plus verification)
1. Go to **play.google.com/console** and sign in with the Google account you want to own the app long term (use a dedicated one, not a work account you might lose).
2. Pay the **one-time registration fee (US$25)**.
3. Fill in your details and **verify your identity** (government ID; for organizations also the D-U-N-S number). Google may ask you to confirm with the **Play Console app on an Android phone**.
4. Bangladesh is a supported location for developers. Create a **payments profile** when asked (needed even for free apps; a bank account is only needed if you ever charge).
5. Wait for approval (hours to a few days).

## Step 2: Host the privacy policy and a deletion page (Claude builds, You switch it on, 20 minutes)
* Claude puts the policy and an "Delete my account and data" instructions page into `docs/` as web pages.
* You: GitHub repository, Settings, Pages, Source: "Deploy from a branch", branch `main`, folder `/docs`. GitHub shows the public address (like `https://zunaidshuruk.github.io/Amar-fit/privacy`). Send it to me.
* This one address goes into Play Console **and** is shown by Health Connect.

## Step 3: Make the Play build (Claude, in sprint BJ)
1. A new **upload keystore** is created by you (I give you the exact command and tell you where to store it, for example a password manager plus an offline copy). **Never** put it in the repo. If you lose it you can ask Google to reset the upload key, but guard it anyway.
2. Claude adds the `play` build flavor (no self-updater, no install-packages permission), turns on shrinking, trims the health permissions, adds the "report this AI answer" button, and a GitHub Actions job that builds the signed **.aab** file.
3. You add the keystore as GitHub secrets (I give the secret names and a click-by-click description).
4. You download the `.aab` from the workflow run.

## Step 4: Create the app in Play Console (You, 30 minutes)
1. **Create app**: name "KardIQ", default language, **App** (not game), **Free**, accept the declarations.
2. Use **Play App Signing** (the default): Google keeps the real signing key, your upload key only proves uploads are yours.

## Step 5: Fill in "App content" (Claude drafts every answer, You paste and confirm; about 2 hours)
Play Console, left menu, Policy and programs, **App content**. Claude provides a ready answer sheet for each:
1. **Privacy policy:** the web address from Step 2.
2. **App access:** the app needs a login, so give reviewers a **demo email and password** (create one account just for review).
3. **Ads:** none.
4. **Content rating:** answer the IARC questionnaire honestly (health and fitness, user-to-user messaging, no violence or gambling).
5. **Target audience:** not for children; choose 18+ (simplest with health content).
6. **Data safety:** list what is collected (health and fitness info, personal info like name and email, messages, photos if profile pictures, app activity), that it is encrypted in transit, that users can delete it, and the **deletion web address**. It must match the privacy policy exactly.
7. **Health apps declaration:** say the app has health and fitness features, is **not** a medical device, and justify **each Health Connect data type** (Claude writes one sentence per type; types without a visible feature are removed first). The privacy policy shown here must be the same one Health Connect shows.
8. **Financial features, government app, news app:** none.
9. Check: Play Console Help, "Publish your health app on Google Play" and "AI-Generated Content" (apps with an AI chat need an in-app report option; Claude adds it).

## Step 6: Store listing (Claude drafts text and a screenshot plan, You upload; about 2 hours)
* **Short description** (up to 80 characters), **full description** (up to 4,000), **app name** (up to 30 characters).
* **App icon 512 x 512 PNG**, **feature graphic 1024 x 500**, **at least 2 phone screenshots** (aim for 6 to 8: Today, Nutrition, Sleep, Medicine card, Cycle tracker with private data hidden, Friends, Assistant).
* Category **Health & Fitness**, contact email, countries (start with Bangladesh and any others you want).
* Do not claim to diagnose or treat anything; keep wording to "tracking" and "wellness".

## Step 7: Test it with real people (You, 14+ days)
1. **Internal testing** first (up to 100 testers, instant, no review): upload the `.aab` to Testing, Internal testing, add your own Gmail, install from the link, check everything works.
2. **Closed testing** (the 14-day clock for personal accounts): create a closed track, upload the same bundle, add **at least 12 Gmail addresses** of real people (family, friends, colleagues), send them the opt-in link, and make sure they **stay opted in for 14 days in a row**. Ask them to open the app now and then and send feedback. Do not use paid tester farms; Google looks for them.
3. Fix what they find; I ship new bundles to the same track. The 14 days must be unbroken **at the moment you apply**.

## Step 8: Apply for production (You, 30 minutes)
* After 14 days with 12+ testers, an **Apply for production** button appears. Answer the questionnaire about your testing, feedback you received and the app's readiness (Claude drafts the answers from the tester feedback).
* Google reviews, usually within about a week.

## Step 9: Launch with a staged rollout (You and Claude)
1. Create a **production release**, upload the bundle, write release notes, and start at **20 percent**.
2. Watch Play Console **Android vitals** (crashes, ANRs) and reviews for 2 to 3 days; raise to 50 percent, then 100 percent.
3. Keep the GitHub sideload build for existing users, or tell them to move to the Play version (they reinstall and restore from their Drive backup or cloud sync, because the signing key is different).

## Timeline (personal account)
Account approval (days) + about 1 week of build and form work (Claude and You in parallel) + **14 days of closed testing** + about a week of review = **about 4 weeks**. An organization account skips the 14 days but adds business verification.

## What can make Google reject or suspend the app
* Asking for health permissions the visible features do not use.
* A self-updating app (the in-app updater) in the Play build.
* Missing or mismatching privacy policy, Data safety answers or health declaration.
* No in-app way to report AI content, or no deletion option.
* Medical claims without a disclaimer.

## Before every future update
Raise `versionCode`, upload the new bundle to the same track, and re-check the Data safety and Health declarations if you add a data type or permission.
