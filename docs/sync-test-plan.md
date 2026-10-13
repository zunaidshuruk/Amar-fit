# Two-phone sync: how it works and how to test it

Built in Sprint 17 (branch `claude/sync-sprint`). Needs the SAME build on both phones: an older build ignores the
"deleted" markers and would bring deleted items back.

## What syncs, and the rule for each thing
| Data | Rule when two phones disagree |
|---|---|
| Steps, distance, active calories, exercise and mindfulness minutes, water, food calories and macros | the HIGHER value wins |
| Weight, blood pressure, the 8 glucose readings, sleep, heart rate, HRV, oxygen, respiratory rate, skin temperature | the NEWEST entry wins; a value always beats empty |
| Minimum / maximum heart rate | lowest non-zero minimum / highest maximum |
| Profile | the most recently saved profile wins as a whole; points only go up, badges are united, the streak comes from the phone that was active last |
| Food logs, diet charts, saved workouts, workout programs, saved chats, medical records | added or changed items arrive; a delete on one phone removes the item on all |
| AI Assistant chats (all of them) | newer chats arrive with their messages; deleting a chat removes it everywhere |
| Workout / activity history events created in the app | arrive on the other phone |

Not synced on purpose: Health Connect exercise sessions and events that came from Health Connect (each phone reads its
own Health Connect; the day totals still sync through the rules above).

## When it runs
* Every time the app is opened (at most every 30 seconds): last 7 days of metrics, profile, items, chats, events.
* Once a day: the last 60 days. On a new phone (first run after signing in or after this update): the WHOLE history.
* Live: a listener on the last 3 days of metrics, so a change on one phone shows on the other within seconds.
* Settings > Cloud sync shows the last time and the result, and "Sync now" runs it immediately.

## Test plan (two phones, same account, both on the new build, both online)
1. **Counters.** Phone A: walk or add steps so it shows more than phone B. Open the app on B (or tap Sync now): B shows
   A's higher number and keeps it (Health Connect no longer pulls it back down).
2. **Readings.** On A log a glucose reading and a weight; on B log a different weight a minute later. After a sync both
   phones show B's weight (newest) and A's glucose reading (a value beats empty).
3. **Delete everywhere.** On A delete a food log, a saved workout (swipe) and a medical record. Open B: all three are gone.
4. **Chats.** On A ask the Assistant something. Open B: the chat is in the history with its messages. Delete it on B: it
   disappears on A.
5. **New phone.** Sign out on B (this clears its local data), sign in again: Today, history, food logs, saved items,
   chats and profile come back (the first sync pulls everything).
6. **Offline.** Put A in airplane mode, log water and a food item, turn the network back on: within a minute (or Sync now)
   B shows them.
7. **Drive restore.** Backup on A, Restore on B: B's higher counters and newer entries are kept, nothing is lowered.

## Known limits
* Water is a counter (higher wins), not a sum: log it on one phone.
* If the same food log or medical record is edited on two phones at once, the cloud copy wins on the next pull.
* A deleted item keeps only a marker (no content) in the cloud for 30 days, then the marker is removed.
* Reads: a normal open costs a few dozen reads; a new phone reads everything once. Firestore's free tier is 50,000 reads a day.
