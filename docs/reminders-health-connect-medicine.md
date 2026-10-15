# Health Connect reminder and medicine reminders

Settings > Notifications. Both are built on the Sprint 14 reminder system (`ReminderCatalog`, `ReminderRunner`, one alarm per reminder, re-armed
after reboot, app update and time changes).

## Health Connect reminder
* **When:** once on the first login (when the app opens and Health Connect is not connected), then every **3 calendar days** from a daily check
  (10:00) until it is connected; it stops by itself once any Health Connect permission is granted. Rule: `HealthConnectNudgePolicy` (pure, tested).
* **Never nags** when Health Connect cannot work on the phone (`HealthConnectManager.isAvailable` false), when the switch is off, in quiet hours, or
  while notifications are not allowed yet (nothing is remembered then, so it is simply tried again, for example after the Android 13 prompt).
* **Opens** Settings, where the Health Connect row has the Connect button. Last nudge date is kept on the phone (`HealthConnectNudge` preferences).
* **Default ON** for everyone (Zunaid's request); switch: Settings > Notifications > General > Health Connect reminder.

## Medicine reminders
* **Up to 6** (`MedicineReminders.MAX`). Each has a name (40 characters), an optional dose (40), one time of day, the days of the week, and an on/off switch;
  add, edit (tap a row) and delete (asks first). For several doses a day add several reminders.
* **Never silenced by quiet hours** (a missed dose matters more than a quiet night); the master switch still applies. Own channel "Medicine reminders" (high importance).
* **Lock screen:** by default only "You have a reminder" shows until the phone is unlocked (switch: "Hide the medicine name on the lock screen").
* **Stored** in the notification settings JSON of the profile, so it syncs to the user's other phones and is removed with the account. Never shown to friends and
  never sent to the AI (`CycleIsolationTest.medicineRemindersNeverReachFriendsOrTheAi`). Privacy policy 1.2 and 3 mention it.
* **Taken and Snooze buttons** (Zunaid's request): the notification has **Taken** and **Snooze 10 min**. Taken records the dose as taken today (phone only,
  never synced, last 14 days kept; `MedicineTaken`, `MedicineTakenStore`), hides the notification and cancels any snoozed repeat. Snooze hides it and shows it
  again 10 minutes later (`MedicineNotifier`, receiver `MedicineActionReceiver`), unless the dose was marked taken meanwhile; it can be snoozed again. A dose already
  marked taken today (also with "Mark as taken today" under the reminder in Settings, which can be undone) is not reminded again that day. On a locked phone the
  buttons appear only after unlocking when the lock-screen switch is on (the generic lock-screen version has no buttons).
* **Medicine history** (Settings > Notifications > Medicine reminders > View history, route `medicine_history`; also opened by the missed-dose alert): the last 14
  days, newest first, each dose as Taken (with the time), Skipped, Missed, Due now or Later today, and "N of M doses taken" for the last 7 and 14 days
  (doses still to come are not counted). Worked out from the CURRENT reminders from the day each was created (`MedicineReminder.createdOn`; reminders made before
  the history existed start today, so they never show invented missed doses) and the phone-only records (`MedicineHistoryBuilder`, pure and tested). Editing a
  reminder's time or days changes how earlier days look.
* **Today's medicine card** (Today screen, after the workout card; today only, hidden when there are no active medicine reminders): today's doses by time with their state (Taken with the time, Skipped, Missed, Due now, Later today), a **Taken** button per dose and **Undo** for taken or skipped ones, "N of M taken today" and a History link. Uses the same phone-only records as the notifications and the history, so ticking a dose here also cancels its snoozed repeat, missed-dose check and notification (`MedicineNotifier.markTaken`). Numbers: `MedicineToday` (pure, tested).
* **Missed-dose alert** (on by default, Settings > Notifications > Medicine reminders; "Alert after" 15 to 180 minutes, default 60): when a reminder is shown, a check is
  scheduled; if the dose was neither marked Taken nor Skipped by then, a "Missed dose?" notification appears with **Taken** and **Skip today** buttons (Skip is recorded
  and shows as Skipped in the history). Not silenced by quiet hours, discreet on the lock screen like the reminder. Limit: the check is a one-shot alarm, so it is lost if
  the phone restarts between the reminder and the alert.

## The master switch ("Allow KardIQ notifications")
Turning it off switches EVERYTHING off (Zunaid's request, from a screenshot where other switches still looked active):
* **Nothing is shown.** Every notification is posted through `NotificationHelper` or `MedicineNotifier`, and each post first checks `NotificationGate`
  (a flag kept in sync by `ReminderScheduler.rescheduleAll`, on by default). That covers reminders, goal alerts, medicine reminders, their snoozed repeats and
  missed-dose alerts, the Health Connect reminder, nutrition alerts, the Test button and pushes from friends. `NotificationGateGuardTest` fails the build if a
  notification is posted anywhere else or without the check.
* **Pending alerts are cancelled** when it is turned off: all scheduled reminders, snoozed medicine repeats and missed-dose checks, and whatever is still showing.
* **The screen shows it:** a banner says all notifications are off, and every other control is disabled and dimmed (they keep their saved values, so turning the
  master switch back on restores everything). The Test button is disabled too.

## Phone test plan
1. New install and first login with Health Connect not connected: the nudge arrives once when the app opens (after allowing notifications). Tap it: Settings opens.
2. Do not connect: 3 days later at about 10:00 it arrives again; connect Health Connect: no more nudges. Switch it off in Settings > Notifications: none.
3. Settings > Notifications > Medicine reminders: add a reminder two minutes ahead: a heads-up arrives with **Taken** and **Snooze 10 min** (on a locked phone only "You have a reminder" shows). Tap Snooze: it returns 10 minutes later. Tap Taken: it disappears and the row in Settings says "Taken today"; no repeat comes.
4. Open Settings > Notifications > Medicine reminders > View history: today's dose shows Due now, then Taken with its time after you tap Taken; leave one dose alone: after the "Alert after" minutes a "Missed dose?" notification arrives with Taken and Skip today; tap Skip today: the history shows Skipped.
5. Add six reminders: the add button changes to "All 6 reminders added"; delete one: it is enabled again. Edit one (time, days): the new time is used.
5. Set quiet hours around a medicine time: the medicine reminder still arrives, a water reminder does not.
6. Two phones: add a reminder on one, it appears on the other and rings there too.
7. Turn "Allow KardIQ notifications" off: a banner appears and every other switch, time, day chip and button on the screen is dimmed and locked (the Test button too).
   A medicine reminder set for two minutes ahead does not ring; a snoozed or missed-dose alert that was pending does not appear. Turn it on again: the settings are
   unchanged and the reminders work again.
