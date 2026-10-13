# Sprint 18: Cycle tracker for women (Part AW), planned 2026-10-13, BUILT 2026-10-13 on `claude/cycle-tracker` (see `docs/cycle-tracker.md`)

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
   only for Female profiles that switched the feature on; also choosable in Customize navigation bar and listed in More.

**Left out of v1 on purpose:** pregnancy mode, any contraception or "safe days" claim, sexual activity log, ovulation tests, cervical
mucus, basal body temperature, partner sharing, app lock. Cycle data is never added to `friend_stats`, `public_profiles`, challenges or
the activity feed (a test will guard this).

## Privacy and safety (this is reproductive health data: the most sensitive thing the app would hold)
* **Only for profiles set to Female (Zunaid's decision), and opt-in.** The feature (Settings entry, Today and Health cards, notifications) is
  offered only when `profile.gender` is "Female" (the profile offers Male, Female, Other; compared ignoring case); it is off until the user
  switches it on and agrees on a feature-specific consent screen that says what is stored and where; the time of consent is saved. If the
  gender is later changed away from Female the feature hides, its notifications stop and the data is kept (not deleted) until the gender is
  Female again; account deletion still removes it. Consequence to be aware of: someone who menstruates but chose Other or Male cannot use
  it unless they change that field; the gender field is only a gate in the screens, not a security control.
* **Discreet notifications by default.** The lock screen shows a generic "KardIQ reminder"; the details show after unlocking
  (`VISIBILITY_PRIVATE` with a generic public version). Phones are often shared at home.
* **AI: ON by default (Zunaid's decision; Claude had recommended off).** To keep that decision safe, three design rules: (1) the consent screen
  shows the "Use my cycle in AI answers" switch, already ON and clearly labelled, so the user sees and can turn it off at the moment they agree,
  not buried later; (2) only a short summary is shared (current phase, cycle day, expected period date, typical cycle length), never the
  raw day logs, symptoms or mood; (3) the privacy policy and the consent screen say plainly that this summary goes to Google's Gemini for
  cloud answers, like the other health summaries the Assistant already sends. The on-device AI and the health analyst use the same summary
  and respect the same switch.
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

## Decisions (answered by Zunaid 2026-10-13)
1. **Who sees the feature: only profiles set to Female.** (Claude had recommended a switch for everyone.) Built as described in the privacy section.
2. **Lock screen: details hidden until the phone is unlocked** (as recommended).
3. **AI Assistant may use cycle data: ON by default.** (Claude had recommended off.) Built with the safeguards in the privacy section:
   visible ON switch on the consent screen, summary only, plain disclosure in the policy.
4. Assumed, change if wrong: v1 scope as above; sync like the rest of the data; the policy PR (#6) ships first as 2.9.1 and the cycle
   tracker gets its own consent later, so nobody is asked to re-accept twice.
5. Open question for Zunaid: is Ramadan fasting relevant for users (tracking missed fasting days to make up)? Not in v1 unless he says so.

## As built (differences from the plan above)
* **Changed after Zunaid's review (2026-10-13):** Cycle IS a bottom-bar tab (choosable in Customize navigation bar, Female profiles only, listed in More while not in the bar); the screen has a Calendar view (easy) and a Details view; Ramadan fasting make-up tracking was added (optional, off by default, `docs/cycle-tracker.md`).
* Enabling tracking switches "period expected soon" and "period is late" ON (they can be switched off in Settings > Notifications); the fertile
  window and the daily log reminder stay off.
* Cycle notifications use ONE daily check at a fixed time that decides from the predictions, instead of arming alarms on predicted dates.
* Health Connect permissions are requested only from Cycle settings and are NOT in `REQUIRED_PERMISSIONS` (everyone else would otherwise be prompted);
  periods are written once when finished, later edits are not sent.
* Privacy policy: section 1.6 added and the same release re-asks everyone to accept (policy version 2), so nobody is asked twice.
* Still open for Zunaid: a legal review of the policy (the policy itself says it was not legally reviewed).
