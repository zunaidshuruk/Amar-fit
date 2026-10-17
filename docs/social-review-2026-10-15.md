# Part Q (social) verified against the code, 2026-10-15

Checked: `FirebaseManager.kt` social functions, `firestore.rules`, the privacy policy, `scripts/send_push_notifications.js`, tests. Earlier sessions had already audited the rules, account
deletion and push-token exposure; this review covers the feature code itself.

## Verified (works as designed)
* **Friend codes:** 5 characters from a 31-character alphabet without look-alike letters, claimed in a transaction, so each code is unique. A code can be released on deletion.
* **Mutual consent:** a request is created as `pending` by the requester only; only the OTHER person can accept it (`status == accepted`); the pair document id must be the two sorted uids, so nobody can squat a pair.
* **Friend stats** (`friend_stats`: date, steps, water, sleep, calories and their goals): readable only by the owner and accepted friends (rule checks the friendship). Cycle and medicine data never reach it (`CycleIsolationTest`).
* **Public profile** (`public_profiles`: name, streak, points, badges, friend code): no health data; push tokens are rejected there (rule) and live in the private `users/{uid}` document.
* **Challenges:** created only between accepted friends at 0/0; a participant may change only their own progress (integer, 0 to 700000, while active); the **winner is computed by the rules** (`expectedWinner`), not trusted from the client; no deletes.
* **Direct messages:** only between accepted friends, 1 to 4000 characters, immutable; a sender may delete their own messages (account deletion). **Kudos:** one per sender, recipient and day.
* **Account deletion** removes the user's social data (messages, activity events, friend code, friend stats, public profile, friendships); challenges and kudos stay because clients may not delete them.

## Findings (not fixed; decisions needed)
1. **The leaderboard is not optional, but the policy says it is.** `syncPublicProfile` runs for every account whenever the profile is saved, and there is no setting to leave the leaderboard. The policy says
   the data is visible "on the leaderboard, if you use it" and "if you choose to participate". Either the wording or the behaviour has to change. (Friends' names also come from this profile, so a plain
   opt-out would need a separate way to show a friend's name.)
2. **Friend-request pushes are promised but not sent.** The policy lists friend requests among the notifications from friends; the server script only sends `direct_message` and `challenge` pushes.
3. **No block or report.** A declined person can send a new request; there is no way to stop requests from one user. Anyone with an account can also read any friend code document (`friend_codes` allows signed-in reads, needed to look codes up), so codes can be probed (about 28.6 million possible codes; the result is only a uid).
4. **No switch to stop sharing daily progress with friends.** Steps, water, sleep and calories go to every accepted friend; the policy discloses it, but the only control is removing the friend.
5. **No unit tests for the social logic** beyond the isolation guards (it is mostly Firestore calls; the winner logic now lives in the rules, which cannot be unit-tested here).

## Verdict
The security design is sound (consent, friends-only data, server-side challenge winner). Part Q is closed as verified; findings 1 and 2 are accuracy problems with the policy text, 3 to 5 are optional hardening.
