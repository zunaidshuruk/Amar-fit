package com.example.data.model

/**
 * Static legal document content, rendered via MarkdownText on the
 * Privacy Policy / Terms of Service screens and referenced by the
 * mandatory consent checkbox on Sign Up. Keep this in sync with the
 * approved KardIQ_Privacy_Policy.md / KardIQ_Terms_of_Service.md drafts.
 */
object LegalContent {
    const val PRIVACY_POLICY = """# KardIQ Privacy Policy

**Last updated:** September 26, 2026

This Privacy Policy explains how KardIQ ("the App," "we," "us") collects, uses, stores, and protects your information. KardIQ is developed by Zunaid Hossain Shuruk, provided as a health and fitness tracking application designed for users in Bangladesh.

**Please read this policy carefully. By creating an account or using KardIQ, you agree to the practices described here.**

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
- Medical records you manually enter (e.g., conditions, medications) — this is entirely optional and under your control

Most of this data comes from **Google Health Connect**, which syncs data from your phone's sensors and any other health apps you've connected on your device. Some of it (food logs, manual entries, medical records) is entered directly by you.

### 1.3 AI-Processed Content
When you use food-scanning, food-chat, or AI coaching features, the text, photos, or voice input you provide is sent to **Google's Gemini API** for analysis (e.g., to estimate calories from a food description or photo). This content is processed by Google's AI systems according to Google's own data handling terms for the Gemini API.

### 1.4 Social Features
If you use KardIQ's friend/social features, we collect:
- A friend code linked to your account
- Your name, streak, points, and badges (visible to other users on the leaderboard, if you use it)
- Daily step/water/sleep/calorie progress (visible only to accepted friends)
- Messages you send to friends within the app
- Challenge participation and results

### 1.5 Technical Information
- Device information and app version (for support and update purposes)
- Push notification tokens (Firebase Cloud Messaging), used only to deliver in-app notifications (e.g., a friend request or challenge result)

## 2. How We Use Your Information

We use your information to:
- Provide the core functionality of the app (tracking, logging, insights, goal calculations)
- Generate AI-powered food analysis, health insights, and coaching responses
- Sync your data across your own devices via your account
- Enable social features you choose to use (friends, leaderboard, challenges, messaging)
- Send you notifications about app activity you've opted into (meal reminders, friend requests, challenge results)
- Maintain and improve the app

**We do not sell your personal or health data to third parties, and we do not use your health data for advertising.**

## 3. Where Your Data Is Stored

- Your account data and most app data are stored using **Google Firebase** (Firestore database and Firebase Authentication), operated by Google.
- A local copy of your data is also stored on your device (Room database) for offline access; this local copy is cleared when you log out.
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
- **Deletion:** You can delete your account from Settings, which removes your data from our systems. Locally cached data is also cleared on logout.
- **Health Connect permissions:** You can revoke KardIQ's access to specific health data types at any time via your device's Health Connect settings, without affecting other app functionality.
- **Social visibility:** Friend-visible data (streak, points, badges, daily progress) is only shared with users you've mutually accepted as friends, or shown on the public leaderboard if you choose to participate. You can remove a friend at any time, which ends their access to your shared data.
- **Notifications:** You can disable notifications at any time in your device settings.

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

**Last updated:** September 26, 2026

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
- Adding a friend, joining the leaderboard, or starting a challenge means certain data (name, streak, points, badges, and — for accepted friends — daily progress) becomes visible to that friend or, for the leaderboard, to other users generally.
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
