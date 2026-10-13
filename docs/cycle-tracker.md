# Cycle tracker: what exists and how to test it on a phone

Built in Sprint 18 (Part AW), branch `claude/cycle-tracker`. Plan and decisions: `docs/sprints/PLAN-AW-cycle-tracker.md`.

## Who sees it, and the consent
* Only profiles whose gender is **Female** (`CycleAccess`); it is OFF until the person switches it on and agrees on the consent dialog
  (`cycleTrackingEnabled`, `cycleConsentAt`). The gender field is a gate for the screens, not a security control. If the gender changes
  away from Female the feature hides and its reminders stop; the data is kept until deleted.
* The consent dialog lists what is stored and where, shows "AI Assistant may use a short cycle summary" already ON (Zunaid's decision) and
  "Hide details on the lock screen" ON, and needs "I understand and agree". It points to Privacy Policy section 1.6.

## What it does
* **Cycle screen** (route `cycle`; from More, the Today card and the Health card): summary (day, phase, next period, fertile window), "Period
  started / ended today", "Log today", a month calendar (logged period, expected period, fertile window, ovulation estimate, a dot on days with a
  log), history of the last 6 cycles, how many cycles the numbers rest on, a gentle note for irregular cycles, and the estimate disclaimer.
  Tapping a day logs flow (none, spotting, light, medium, heavy), symptoms and mood, and can start or end a period on that day or remove one.
* **Cycle settings** (route `cycle_settings`): AI switch, lock-screen switch, typical cycle and period length (used until there is history),
  link to cycle notifications, Share with Health Connect, Turn off tracking, Delete all cycle data (cloud first; nothing is deleted if the cloud
  cannot be reached).
* **Predictions** (`CyclePredictor`, calendar method): average of the last 6 plausible cycles (15 to 90 days), ovulation about 14 days before the
  next period, fertile window 5 days before to 1 day after; irregular when cycle lengths differ by more than 7 days; late periods are reported as
  late, never as anything else; predictions are hidden when more than 60 days late.
* **Notifications** (channel `kardiq_cycle`, controls in Settings > Notifications > Cycle tracker): one daily check (default 9:00) that shows
  "period expected soon" (default 2 days before), "period is late" (default 3 days after) or "fertile window starts" (off by default) on exactly
  that day; plus an optional daily log reminder (off by default, skipped when today is logged). Enabling tracking switches "soon" and "late" on.
  By default the lock screen shows only "You have a reminder" (`VISIBILITY_PRIVATE` with a generic public version) until unlock.
* **Cloud**: `users/{uid}/period_logs` (soft deletes, 30 days) and `users/{uid}/cycle_days`; newest `updatedAt` wins (`LwwSyncPlanner`);
  covered by the live Firestore rules (`users/{userId}/{document=**}`); removed by account deletion; included in the Drive backup only while tracking is on.
* **AI**: only while tracking is active AND the AI switch is on, `CycleAiSummary` adds ONE line to the health dossier (`CYCLE: day 13 of a typical
  28-day cycle, fertile window; next period expected about Oct 29 ...`). Never the daily logs, symptoms or mood.
* **Health Connect**: separate permissions (`READ/WRITE_MENSTRUATION`, requested only from Cycle settings, deliberately NOT in
  `HealthConnectManager.REQUIRED_PERMISSIONS`). Import reads periods of the last 400 days (skips any starting within 3 days of a logged one). A
  finished period is written once (remembered by id in `CycleHealthConnect` preferences); light, medium and heavy flow days are written once; later
  edits are not sent. Spotting is not written (Health Connect has no such value).
* **Never shared socially**: `CycleIsolationTest` fails if a friend, leaderboard, challenge or activity-feed writer mentions the cycle.

## Phone test plan
1. Profile gender Female: More shows "Cycle tracker". Change gender to Male: the entry, the cards and the reminders disappear; back to Female: they return.
2. Open the tracker, turn on tracking: the consent dialog shows the AI switch ON and the lock-screen switch ON; "Turn on" works only after the checkbox.
3. Tap "Period started today", log flow and a symptom on today, end the period the next day: the calendar paints the days, history shows a row.
4. Log a period 28 days ago and one 56 days ago: the next-period date and the fertile window appear; the basis line says "Based on 2 logged cycles".
5. Settings > Notifications > Cycle tracker: set "Days before" so a notice fires today, set the daily check to two minutes ahead: with the phone locked
   the notification shows only "You have a reminder"; after unlocking it shows "Period expected soon".
6. Two phones: log on one, open the other: it appears. Delete a period on one: it disappears on the other.
7. Cycle settings > Delete all cycle data: gone on both phones; turn tracking off: the tracker hides and its reminders stop.
8. Share with Health Connect: grant, then "Import periods now"; finish a period here and check it in Health Connect.
9. Ask the Assistant a health question with the AI switch ON (the answer may mention the phase) and again with it OFF (it cannot).
10. Delete the account on a spare test account: the cycle collections are gone in the console.
