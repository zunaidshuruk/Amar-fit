# Firestore rules: audit of 2026-10-13

Zunaid pasted the rules that are live in the Firebase console. They were compared with `firestore.rules` in the repo and with every
Firestore path the app uses.

## Result
* **Live rules and the repo file behave identically.** The only differences: the console copy still carries a stale comment block
  ("STEP 3 ... enable ONLY after ...") above the `public_profiles` rule although that protection is already switched on (writes
  containing `fcmToken` / `fcmTokenUpdatedAt` are rejected), and its indentation is uneven there. The repo copy states what is
  enabled. Nothing needs publishing. (Optional: paste the repo copy into the console so the two match word for word.)
* **Every collection the app uses is covered.** Firestore denies anything not matched, so this was the risk after the sync sprint:

| Path the app uses | Rule |
|---|---|
| `users/{uid}/**` (profile, `health_metrics`, `diet_logs`, `saved_diet_charts`, `saved_workouts`, `workout_programs`, `saved_chats`, `medical_records`, **`activity_events`**, **`assistant_chats`** and its `messages`, and from the cycle tracker **`period_logs`** and **`cycle_days`**) | `users/{userId}/{document=**}`: owner only. The two new sync collections are under the user's own path, so they are allowed. |
| `public_profiles/{uid}` | any signed-in user reads; the owner writes (no push token fields) |
| `friend_stats/{uid}` | owner, or an accepted friend, reads; the owner writes |
| `friendships/{pairId}` | parties only; sorted-uid document id; only the other party can accept |
| `friend_codes/{code}` | signed-in read; create by the owner; the owner may delete |
| `challenges/{id}` | participants only, progress and completion rules |
| `activity_events/{id}` (top level, the social feed) | signed-in read; posted by its owner; the owner may delete |
| `kudos/{id}` | sender and recipient read; the sender creates |
| `dm_threads/{pair}/messages/{id}` | accepted friends only; immutable; the sender may delete |

* **Push tokens** are written to `users/{uid}` (private); the app removes any old copy from `public_profiles`, which the rules allow
  because the resulting document has no token fields. `scripts/send_push_notifications.js` reads the private document first.

## Optional hardening (not applied, nothing is broken)
* `friendships` update: accepting only checks `status == 'accepted'`. Adding
  `&& request.resource.data.diff(resource.data).affectedKeys().hasOnly(['status'])` would stop a party from also changing
  `uid1`, `uid2` or `requestedBy` while accepting. It would have to be published in the console as well as saved here.
* `activity_events` (top level) and `public_profiles` are readable by every signed-in user by design (no health data); the privacy
  policy says so for the leaderboard.
