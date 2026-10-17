package com.example.data.model

/**
 * Static legal document content, rendered via MarkdownText on the
 * Privacy Policy / Terms of Service screens and referenced by the
 * mandatory consent checkbox on Sign Up. Keep this in sync with the
 * approved KardIQ_Privacy_Policy.md / KardIQ_Terms_of_Service.md drafts.
 */
object LegalContent {
    /**
     * Bump this whenever the privacy policy changes in a way users must accept again.
     * 1 = September 26, 2026; 2 = October 13, 2026 (multi-device sync, notifications, account deletion wording).
     */
    const val POLICY_VERSION = 2

    /** True when the user has never accepted the policy, or accepted an older version of it. */
    fun needsConsent(hasAccepted: Boolean, acceptedVersion: Int): Boolean =
        !hasAccepted || acceptedVersion < POLICY_VERSION

    const val PRIVACY_POLICY = """# KardIQ Privacy Policy

**Last updated:** October 15, 2026

This Privacy Policy explains how KardIQ ("the App," "we," "us") collects, uses, stores, and protects your information. KardIQ is developed by Zunaid Hossain Shuruk, provided as a health and fitness tracking application designed for users in Bangladesh.

**Please read this policy carefully. By creating an account or using KardIQ, you agree to the practices described here.**

---

## What is new in this version (October 13, 2026)
- Your data now stays in sync across every phone you sign in on, including your AI Assistant conversations and your workout history (section 3).
- Items you delete are removed on your other phones too (section 3).
- Every reminder and alert can be switched on or off in Settings > Notifications (section 5).
- Section 5 now explains exactly what deleting your account removes and what can remain.
- An optional cycle tracker for profiles set to Female, with optional Ramadan fasting make-up tracking. It is off until you switch it on and agree, and its data is never shown to friends (section 1.6).

---

## 1. Information We Collect

### 1.1 Account Information
- Name, email address, and phone number (if you sign up with email/phone)
- Google account information (name, email, profile picture) if you sign in with Google
- Profile picture, if you upload one

### 1.2 Health & Fitness Data
Depending on which features you use, we may collect:
- Steps, distance, active calories, exercise sessions
- Heart rate, resting heart rate, heart rate variability
- Blood pressure and blood glucose readings (including meal-context tags you provide)
- Sleep duration and sleep-related data
- Blood oxygen (SpO2), respiratory rate, skin temperature
- Weight, height, BMI, and body measurements you enter (waist, neck, hip)
- Food and nutrition logs, including photos or descriptions of meals you submit for AI analysis
- Water intake, mindfulness/meditation sessions
- Your workout history: the workouts, programs and exercise sessions you complete or log in the app
- Your AI Assistant conversations: what you ask and the replies you receive
- Your notification settings: which reminders and alerts you turned on, and their times
- Medicine reminders you set up (optional, up to 6): the medicine names, doses and times you type in. They are kept in your private area with your other settings, are never shown to friends and are never sent to the AI. If you tap Taken or Skip, which doses you marked as taken or skipped in the last two weeks, and at what time, is kept on your phone only (it feeds the medicine history screen)
- Medical records you manually enter (e.g., conditions, medications) — this is entirely optional and under your control

Most of this data comes from **Google Health Connect**, which syncs data from your phone's sensors and any other health apps you've connected on your device. Some of it (food logs, manual entries, medical records) is entered directly by you.

### 1.3 AI-Processed Content
When you use food-scanning, food-chat, AI coaching, the AI Assistant or Health Review features, the text, photos, or voice input you provide is sent to **Google's Gemini API** for analysis (e.g., to estimate calories from a food description or photo). To give answers based on your own data, the Assistant and Health Review also send Gemini, over a secure connection, a summary computed on your device: your profile and goals, daily metrics (such as steps, sleep, blood glucose, blood pressure, heart rate, weight, water and calories), food logs, exercise sessions and, if you have entered them, your medical records (conditions, medications, allergies). This content is processed by Google's AI systems according to Google's own data handling terms for the Gemini API. AI features that run on your phone (Offline AI) process this data on your phone only. Your Assistant conversations are also saved to your account (see section 3) so that they appear on your other phones.

### 1.4 Social Features
Every account appears on the public leaderboard. The rest of this list applies only if you use KardIQ's friend/social features. We collect:
- A friend code linked to your account
- Your name, streak, points, and badges (visible to ALL other KardIQ users on the public leaderboard: every account appears there, and there is currently no setting to hide it, even if you never use the social features)
- Daily step/water/sleep/calorie progress (visible only to accepted friends)
- Messages you send to friends within the app
- Challenge participation and results

### 1.5 Technical Information
- Device information and app version (for support and update purposes)
- Push notification tokens (Firebase Cloud Messaging), used only to deliver notifications from friends (messages, friend requests and challenge results). Your own reminders and goal alerts are scheduled and checked on your phone and do not use this token

### 1.6 Cycle Tracker (Optional)
The cycle tracker is offered only to profiles set to Female, and only after you switch it on and agree on its own consent screen. If you use it, we collect:
- The dates of your periods and what you log for each day: flow, symptoms and mood
- Your cycle settings (typical cycle and period length, and the choices below) and the time you agreed
- Only if you switch on Ramadan fasting make-up (off by default): the days you mark as missed or made-up fasts, and a small adjustment (up to two days) for local moon sighting. This can reveal religious practice, so it is treated exactly like your cycle data

This is reproductive health information, so we treat it with extra care:
- It is stored on your phone and in your private area of your KardIQ account (see section 3), so it follows you to your other phones. Only your account can read it.
- It is **never** shown to friends, the leaderboard, challenges or the activity feed.
- **AI:** if "AI Assistant may use a short cycle summary" is on (it is shown as on when you agree, and you can switch it off at any time in Cycle settings), only a one-line summary is added to what the AI sees when you ask health questions: your cycle day and phase, the expected date of your next period, and whether your cycles are regular. It is processed by Google's Gemini like the other health summaries in section 1.3. Your daily logs, symptoms and mood are never sent to the AI.
- **Notifications:** cycle reminders are created on your phone. By default the lock screen shows only "You have a reminder" until you unlock your phone (this follows your phone's lock-screen settings).
- **Health Connect:** only if you switch on "Share with Health Connect", the app reads period records from Health Connect and saves the periods and flow you log here. It uses its own permission.
- **Ramadan fasting make-up (optional):** your fasting days are stored, synced, backed up and deleted together with your cycle data, are never shown to friends, and are never sent to the AI.
- Predictions are estimates based on the dates you log. KardIQ is not a medical device and cannot be used as contraception.
- You can turn tracking off (your log is kept) or delete all cycle data at any time in Cycle settings. Deleting your account also deletes it. A backup you saved to your own Google Drive can contain your cycle data while tracking is on.

## 2. How We Use Your Information

We use your information to:
- Provide the core functionality of the app (tracking, logging, insights, goal calculations)
- Generate AI-powered food analysis, health insights, and coaching responses
- Sync your data across your own devices via your account
- Run the public leaderboard (every account appears on it) and enable the social features you choose to use (friends, challenges, messaging)
- Send you the reminders and alerts you switch on (such as meals, water, workouts, sleep, blood glucose checks, weigh-ins, goal alerts and a weekly summary; these run on your phone) and notifications from friends (messages, friend requests, challenge results)
- Maintain and improve the app

**We do not sell your personal or health data to third parties, and we do not use your health data for advertising.**

## 3. Where Your Data Is Stored

- Your account data and most app data are stored using **Google Firebase** (Firestore database and Firebase Authentication), operated by Google. This includes your profile, daily health metrics, food logs, diet charts, saved workouts and workout programs, saved chats, AI Assistant conversations, workout history, notification settings (including any medicine reminders you add), cycle tracker logs including fasting make-up days (if you use them) and any medical records you enter. Each user's data sits in a private area that only that user's account can read.
- **Sync across your phones:** when you sign in on another phone, your history is restored from your account, and your phones keep each other up to date. If two phones change the same item, the app combines them: counters such as steps keep the higher number, for readings the most recent entry wins, and points never go down.
- **Deleting items:** when you delete an item (a food log, saved workout or plan, saved chat, AI Assistant conversation or medical record), its content is removed from the cloud. A small marker without the content is kept for about 30 days so that your other phones also remove the item, and is then cleaned up.
- A local copy of your data is also stored on your device (Room database) for offline access; this local copy is cleared when you log out.
- Your reminders and goal alerts are scheduled and checked on your phone. Only your notification settings are saved with your profile, so that they follow you to your other phones.
- If you use the optional Google Drive backup feature, a backup file is stored in **your own** Google Drive account, not ours.
- Health data synced from Health Connect stays governed by Android's own Health Connect permissions system — you control exactly which data types the app can read or write at any time via your device's Health Connect settings.

## 4. Third-Party Services We Use

| Service | Purpose |
|---|---|
| Google Firebase (Auth, Firestore, Cloud Messaging) | Account management, data storage, sync, notifications |
| Google Gemini API | AI-powered food analysis, chat, and coaching features |
| Google Health Connect | Reading/writing health & fitness data on your device |
| Open Food Facts | Barcode lookup for packaged food nutrition data |
| Google Sign-In | Optional sign-in method |

Each of these providers has its own privacy practices governing how they handle data passed to them. We encourage you to review Google's privacy policy for details on how Firebase and Gemini handle data.

## 5. Your Choices and Rights

- **Access & correction:** You can view and edit most of your logged data directly within the app.
- **Deleting items:** You can delete individual items in the app and clear your AI Assistant history. They are removed from your account and from your other phones (see section 3).
- **Deleting your account:** Deleting your account from Settings removes your profile, health metrics, food logs, diet charts, saved workouts and programs, saved chats, AI Assistant conversations, workout history, medical records, cycle tracker logs, friend code, public profile, friend connections and progress, activity posts and the messages you sent to friends, and then deletes your sign-in account. Some records shared with other people can remain: challenge and kudos records (they contain only user IDs, dates and step counts) and messages in conversations whose friendship had already been removed before you deleted your account. A backup you saved to your own Google Drive is not touched; you can delete it from your Drive. Locally cached data is also cleared on logout.
- **Health Connect permissions:** You can revoke KardIQ's access to specific health data types at any time via your device's Health Connect settings, without affecting other app functionality.
- **Social visibility:** Friend-visible data (streak, points, badges, daily progress) is only shared with users you've mutually accepted as friends, and, for your name, streak, points and badges only, shown to all KardIQ users on the public leaderboard (every account appears there; there is currently no setting to hide it). You can remove a friend at any time, which ends their access to your shared data.
- **Notifications:** In Settings > Notifications you can switch every reminder, goal alert and friend notification on or off, choose their times and set quiet hours. You can also disable notifications at any time in your device settings.

## 6. Data Security

We take reasonable measures to protect your information, including relying on Firebase's built-in security infrastructure and access-control rules that restrict each user's data to that user (and, for social features, to their confirmed friends only). However, no method of electronic storage or transmission is 100% secure, and we cannot guarantee absolute security.

## 7. Children's Privacy

KardIQ is not directed at children under 13, and we do not knowingly collect data from children under 13. If you believe a child has provided us with personal information, please contact us so we can remove it.

## 8. Changes to This Policy

We may update this Privacy Policy from time to time. If we make material changes, we will notify you in-app and, where required, ask you to review and re-accept the updated policy before continuing to use the app.

## 9. Contact

If you have questions about this Privacy Policy or how your data is handled, please contact:

**Zunaid Hossain Shuruk**
zunaid.shuruk@gmail.com

---

*This document is a starting draft tailored to KardIQ's actual features and data practices. It has not been reviewed by a lawyer. Before relying on it for a public release, especially one handling real users' health data at scale, we recommend having it reviewed by a legal professional familiar with applicable Bangladeshi and international data protection requirements.*
"""

    const val TERMS_OF_SERVICE = """# KardIQ Terms of Service

**Last updated:** October 15, 2026

Welcome to KardIQ. These Terms of Service ("Terms") govern your use of the KardIQ mobile application, developed by Zunaid Hossain Shuruk. By creating an account or using KardIQ, you agree to these Terms.

---

## 1. Not Medical Advice

**KardIQ is a wellness and fitness tracking tool. It is not a medical device, and nothing in the app constitutes medical advice, diagnosis, or treatment.**

- AI-generated health insights, food analysis, coaching suggestions, and glucose-guidance features are for general wellness purposes only.
- Estimated calorie counts, macro breakdowns, and health scores are approximations based on AI analysis and self-reported data — they are not clinically validated measurements.
- Heart rate zones, body fat percentage, and similar calculated metrics are estimates based on standard formulas and the information you provide, not medical assessments.
- **Always consult a qualified healthcare professional** before making decisions about your health, diet, exercise, or medication based on anything in this app — especially if you have a pre-existing medical condition such as diabetes, heart disease, or high blood pressure.
- If you experience a medical emergency, contact your local emergency services immediately. Do not rely on this app in an emergency.

## 2. Your Account

- You must provide accurate information when creating an account.
- You are responsible for keeping your login credentials secure.
- You must be at least 13 years old to use KardIQ.
- You may delete your account at any time from Settings.

## 3. Acceptable Use

You agree not to:
- Use the app for any unlawful purpose
- Attempt to access another user's account or data without authorization
- Harass, abuse, or send inappropriate content to other users via the friends/messaging/social features
- Attempt to reverse-engineer, disrupt, or interfere with the app's operation or its backend services
- Use the social/leaderboard features to impersonate another person

## 4. User-Generated Content

- Content you submit (food descriptions, photos, chat messages, medical notes) remains yours. You grant KardIQ a limited license to process and store this content solely to provide the app's functionality (e.g., sending a food photo to Gemini for analysis, storing your logs in the database).
- You are solely responsible for the accuracy of information you manually enter, including medical records and food log entries.
- Messages sent through the in-app friend messaging feature are only visible to you and the recipient. Do not share sensitive personal or medical information with other users through this feature unless you are comfortable with that friend having access to it.

## 5. Social Features

- Friend requests require mutual acceptance before any data is shared.
- Adding a friend or starting a challenge means certain data (daily progress, and your name, streak, points and badges) becomes visible to that friend. Every account's name, streak, points and badges are also visible to other users generally on the public leaderboard (there is currently no setting to hide it).
- You can remove a friend or decline a request at any time to stop sharing this data with them going forward.
- Challenges award in-app points and badges only; they carry no real-world monetary value.

## 6. AI Features

- AI-powered features (food analysis, chat, coaching, glucose guidance) are powered by Google's Gemini API and may occasionally produce inaccurate, incomplete, or nonsensical results. Always review AI-suggested food log entries before confirming them.
- Voice and phonetic-Bangla input, where available, are provided as a convenience and may not always be interpreted correctly.

## 7. Third-Party Services

KardIQ relies on third-party services (Google Firebase, Google Health Connect, Google Gemini API, Open Food Facts, and others) to function. Your use of KardIQ is also subject to the applicable terms of these third-party providers. We are not responsible for outages, data handling practices, or changes made by these third parties.

## 8. Disclaimers and Limitation of Liability

- KardIQ is provided "as is" and "as available," without warranties of any kind, express or implied.
- We do not guarantee the app will be uninterrupted, error-free, or that any calculated health metric is accurate.
- To the fullest extent permitted by law, Zunaid Hossain Shuruk and KardIQ shall not be liable for any indirect, incidental, or consequential damages arising from your use of the app, including any health-related decisions made based on app content.

## 9. Changes to the App and These Terms

- Features may be added, changed, or removed at any time.
- We may update these Terms from time to time. Material changes will be communicated in-app, and continued use of the app after such changes constitutes acceptance of the updated Terms.

## 10. Termination

We reserve the right to suspend or terminate accounts that violate these Terms, including abusive use of social/messaging features.

## 11. Contact

Questions about these Terms can be directed to:

**Zunaid Hossain Shuruk**
zunaid.shuruk@gmail.com

---

*This document is a starting draft tailored to KardIQ's actual features. It has not been reviewed by a lawyer. Before relying on it for a public release, we recommend having it reviewed by a legal professional familiar with applicable Bangladeshi and international requirements.*
"""
}
