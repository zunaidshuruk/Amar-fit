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

## Resolved the same day (Zunaid's decisions)
* **Finding 1 (leaderboard):** option B. The policy and terms now say every account appears on the public leaderboard and that there is currently no setting to hide it (no longer "if you choose to participate"). `LegalContentTest` guards the wording. The policy version was NOT bumped, so nobody has to re-accept; bump `LegalContent.POLICY_VERSION` if a re-accept is wanted. The leaderboard switch is still a possible future sprint.
* **Finding 2 (friend-request pushes):** built. `scripts/send_push_notifications.js` has a new step `processNewFriendRequests` (rules in `scripts/friend_request_push.js`): a pending friendship created in the last 7 days is announced to the OTHER person ("X wants to be your friend on KardIQ", opens Friends) and marked `notifiedRequest: true`; older or incomplete pending requests are marked without a push, so nobody gets stale notifications. The step runs last and in its own try/catch, so a failure cannot stop messages or challenges. The app shows it on the social channel and respects Settings > Notifications > Friend requests, quiet hours and the master switch. Old app versions show it as a generic notification that still opens Friends.
* Tests: `scripts/test_friend_request_push.js` (rules) and `scripts/test_poller.js` (the whole poller against a fake Firestore: all three push kinds sent once, nothing twice, missing token, friend-request failure isolated); both run in the compile check.
* Findings 3 to 5 (block/report, a switch for sharing daily progress, social unit tests) remain optional.

## Update 2026-10-18: findings 1 and 4 resolved (Zunaid: "fix more")
* **Show me on the leaderboard** (Settings): off keeps the person out of the leaderboard (`public_profiles.leaderboardVisible`; people without the flag stay visible; the list is read with twice the limit and cut after hiding). Friends and friend-code lookups still see the name. The policy and terms say so (they no longer claim there is no setting).
* **Share my daily progress with friends** (Settings): off deletes `friend_stats/{uid}` at once and stops writing it; on publishes today's progress again; a friend's card says "X is not sharing their daily progress" instead of loading forever.
* Database 53 to 54 (two columns, default on), `SocialPrivacy` (pure, tested), `SocialPrivacyWiringTest`. No Firestore rule change was needed.
* Still open: finding 3 (block and report, part 2 of sprint BM), 5 (social unit tests).

