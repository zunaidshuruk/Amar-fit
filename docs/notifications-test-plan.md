# Notifications: what exists and how to test it on a phone

Built in Sprint 14 (branch `claude/notifications`). Settings > Notifications is the single place for everything.

## Defaults and migration
* New user: only the three meal reminders are on (Breakfast 8:00, Lunch 1:30 PM, Dinner 8:00 PM); every other reminder and
  alert is off until switched on. Friends' messages, friend requests and challenges are on (they only arrive once the user
  has friends). Quiet hours exist but start with no window.
* Existing user: the old "Push Notifications" switch becomes the master switch and "Daily Reminders" becomes the Meal
  reminders switch, so what they had keeps working; everything else starts off.
* The settings are stored as JSON in the profile, so they sync to the user's other phones with the profile.

## What each notification does
| Notification | Behaviour | Opens |
|---|---|---|
| Meal reminders (3, own times) | skipped when that meal is already logged | AI scanner |
| Calorie limit reached | once a day when eaten calories pass the goal | Today |
| Nutrition gaps alert | the daily check of recent meals, only when switched on | Today |
| Water reminders | every N hours (1 to 6) between a start and an end time; skipped when water is on an even pace to the goal | Today |
| Water goal reached | once a day | Today |
| Step goal reached | once a day | Today |
| Workout reminder | chosen days and time; skipped when exercise is already logged | Fitness |
| Weekly workout goal met | once a week; the goal (workout days per week, default 3) is set in Health Goals | Today |
| Bedtime reminder | chosen time | Sleep |
| Sleep goal reached | once a day when logged sleep reaches the goal | Today |
| Mindful moment | daily time; skipped after a mindfulness session | Mindfulness timer |
| Weekly check-in | chosen day and time (default Sunday 7 PM); text is the week's averages | Health |
| Blood glucose reminders | up to six slots (before and after breakfast, lunch, dinner); a slot is skipped when its reading is logged | Glucose log |
| Weekly weigh-in | chosen day and time (default Monday 8 AM); skipped when weight is logged | Weight log |
| Messages, Friend requests, Challenges | server pushes, each switchable here | Chat / Friends |
Quiet hours silence everything (also server pushes); a goal alert that falls in quiet hours is shown later when numbers change.
Each category has its own Android channel (Meals & nutrition, Hydration, Activity & workouts, Sleep & mindfulness, Health
checks, Goals reached, Messages & Challenges), so Android's own app notification settings mirror the app.

## How reminders are scheduled
One alarm per reminder (`setAndAllowWhileIdle`, no special permission). Each alarm arms the next one when it fires. Everything
is re-armed after a reboot, an app update, a time or time zone change, and when settings change. Reminders arrive within a few
minutes of their time (Android delays alarms in battery saving), not to the second.
Goal alerts are checked whenever today's numbers change, so they appear when the app has processed new data (steps are read from
Health Connect while the app is open).

## Test plan
1. **Permission and test.** Settings > Notifications. If notifications are off for the app a banner offers Allow / Open settings.
   Tap Test: a notification arrives and opens Today.
2. **Meal reminder.** Set Lunch to two minutes from now: it arrives. Repeat after logging lunch first: it does not.
3. **Water.** Switch Water reminders on (every 1 h). One arrives at the next hour. Log enough water for your pace: the next is skipped.
4. **Quiet hours.** Set a window that covers now and a meal reminder two minutes ahead: nothing arrives.
5. **Goal alert.** Set a low step goal in Health Goals, switch Step goal reached on, open the app so steps sync: one alert, not repeated that day.
6. **Weekly workout goal.** Set it to 1 day, log a workout, switch Weekly workout goal met on: one alert this week.
7. **Weekly check-in.** Switch it on for today, two minutes ahead: the text shows steps, sleep, water, workouts and weight change.
8. **Reboot.** Switch a reminder on for five minutes ahead, restart the phone WITHOUT opening the app: it still arrives.
9. **Friends.** Turn Messages off and have a friend message you: no push. Turn it on: push arrives.
10. **Upgrade.** On a phone that had "Daily Reminders" off before the update, Meal reminders should be off after it.

## Known limits
* Server pushes (messages, challenges) come from the GitHub Actions poller, which runs about every 5 minutes.
* The first version's repeating meal alarms are cancelled on the first start or boot after the update; if one fires before that
  it simply runs the new schedule and shows that meal's reminder once.
* The Android 13+ notification permission is asked at app start as before; Settings > Notifications shows the status.
