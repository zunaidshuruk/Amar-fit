# Sprint 18: Cycle tracker for women (Part AW), planned 2026-10-13, plan mode, not started

Request (Zunaid): "include a cycle tracker for women along with notifications. I have less clue." So this is Claude's proposal with
recommended defaults; the open decisions are at the bottom. Nothing here is built yet.

## What a cycle tracker does (proposed v1)
1. **Log.** Period start and end (one tap: "Period started today" / "Period ended"), flow per day (spotting, light, medium, heavy),
   symptoms (cramps, headache, bloating, back pain, breast tenderness, acne, nausea, fatigue, cravings) and mood. No free text in v1
   (a note box invites anything to be typed into the cloud).
2. **Calendar.** A month view: period days, predicted period days, fertile window, estimated ovulation day, today.
3. **Predictions** (calendar method, pure Kotlin, unit tested): next period, fertile window and ovulation (about 14 days before the next
   period), cycle day and phase (menstrual, follicular, ovulation, luteal). Defaults of 28 days and 5 days until two cycles are
   logged, then the average of the last 3 to 6 cycles; shows how many cycles it is based on. A cycle shorter than 21 or longer than
   35 days, or cycles that differ by more than about 7 days, show a gentle "your cycles vary, predictions are less accurate, consider
   talking to a doctor" note. A late period says "period is N days late" and never guesses at pregnancy.
4. **History.** Past cycles with cycle length and period length, and the averages.
5. **Notifications** (new channel `kardiq_cycle`, built on the Sprint 14 reminder system): period expected soon (default 2 days before),
   period late (after N days), daily "how are you feeling today" log reminder (off by default), fertile window starts (off by default).
   All switchable in Settings > Notifications.
6. **Health Connect.** Read and write `MenstruationPeriodRecord` and `MenstruationFlowRecord` (permissions
   `android.permission.health.READ_MENSTRUATION` and `WRITE_MENSTRUATION`; needs the manifest `<uses-permission>` AND a
   `REQUIRED_PERMISSIONS` entry, see the hard rules) so history from another app can be imported and what is logged here is shared
   back. If the app is ever published on Google Play, the Health Connect data-type declaration in the Play Console is required.
7. **Where it lives.** A dedicated Cycle screen, a card on Today ("Cycle day 12, period expected in 16 days") and a card on Health,
   only when the feature is switched on; also choosable in Customize navigation bar and listed in More.

**Left out of v1 on purpose:** pregnancy mode, any contraception or "safe days" claim, sexual activity log, ovulation tests, cervical
mucus, basal body temperature, partner sharing, app lock. Cycle data is never added to `friend_stats`, `public_profiles`, challenges or
the activity feed (a test will guard this).

## Privacy and safety (this is reproductive health data: the most sensitive thing the app would hold)
* **Opt-in, off by default for everyone.** A feature-specific consent screen explains what is stored and where; the time of consent is
  saved. The app does not decide from the gender field who gets it.
* **Discreet notifications by default.** The lock screen shows a generic "KardIQ reminder"; the details show after unlocking
  (`VISIBILITY_PRIVATE` with a generic public version). Phones are often shared at home.
* **AI.** Cycle data is NOT sent to Gemini unless the user switches on "Use my cycle in AI answers" (default off). The on-device AI can use
  it. The health analyst (dossier, offline review) gets a cycle finding only with the same switch.
* **Cloud.** Stored in the user's private area (`users/{uid}/period_logs`, `users/{uid}/cycle_days`, already allowed by the live rules),
  synced with the same merge and 30-day deletion markers as everything else, removed by account deletion, included in the Drive backup.
* **Privacy policy.** A cycle section is added with the release that ships the feature (policy must describe real behaviour). Because the
  feature is opt-in with its own consent, everyone need not be forced to re-accept; a lawyer should confirm this (the policy itself
  says it was not legally reviewed).
* **Medical.** "Estimates, not a medical device and not contraception" shown where predictions appear.

## Technical design
* Room (next version after the policy PR): `PeriodLog(cloudId, startDate, endDate?)` and `CycleDayLog(date, flow, symptomsMask, mood, cloudId)`,
  both with `updatedAt`. Profile gets `cycleTrackingEnabled`, `cycleConsentAt`, `cycleAiEnabled`, `cycleDiscreetNotifications`.
* `data/cycle/CyclePredictor.kt` (pure, tested: regular, irregular, one cycle only, late period, boundary lengths, time zones).
* `NotificationPrefs` gains the cycle reminders; `ReminderCatalog` / `ReminderRunner` get a `CYCLE` kind (the planner already supports
  dates; the period-soon reminder is armed for the predicted date minus N days and re-armed whenever a period is logged).
* Sync: new collections under the user's own path reconciled by the existing `CollectionReconciler`; small `CycleMerger` (newest per day).

## Phasing (one pull request each, about the size of the notifications sprint in total)
| # | Step |
|---|---|
| CY-1 | Foundation: Settings > Cycle tracking switch, consent screen, data model + migration, `CyclePredictor` with tests |
| CY-2 | Log screen and month calendar |
| CY-3 | Predictions in the UI: Today and Health cards, history, irregular and late notes |
| CY-4 | Notifications: the cycle reminders and discreet lock-screen text |
| CY-5 | Cloud sync, account deletion, Drive backup |
| CY-6 | Health Connect read and write |
| CY-7 | Opt-in AI and analyst integration |
| CY-8 | Privacy policy cycle section, tests, docs |

## Decisions
1. Who sees the feature: recommended = a switch in Settings for everyone, suggested during onboarding to women.
2. Lock screen: recommended = details hidden until unlock.
3. AI use of cycle data: recommended = off until the user switches it on.
4. Assumed, change if wrong: v1 scope as above; sync like the rest of the data; the policy PR ships first as 2.9.1 and the cycle tracker
   gets its own consent later, so nobody is asked to re-accept twice.
5. Open question for Zunaid: is Ramadan fasting relevant for users (tracking missed fasting days to make up)? Not in v1 unless he says so.
