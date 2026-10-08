# Active sprint items

Only open or unverified work lives here. Budget ~12 KB — when a Part closes, move its notes to `archive/` and leave one line in [INDEX.md](INDEX.md).

Current as of `76beb27` (2026-10-07). Statuses marked *code-checked* were confirmed by reading the code at that commit, not from a push-verification.

## debug.keystore
Present on `main` (sha256 `4d6742b2...5f0faa`, 2666 B) after the web-upload restore. AI Studio's working copy lacks the file, so its pushes keep deleting it (`3cb859b`, `65ca328`, `6684f03`, `3abfc55`; restores `f864870`, `78c6447`, `e85362f`, `cd3894b`). Since that cannot be fixed from our side, the release workflow now repairs it: a "Restore debug keystore" step writes the known-good key (checksum-verified) when the repo copy is missing or wrong, and "Pre-flight checks" still fail a tag build on a tag/versionCode mismatch or empty key secrets. Tag `6` and tag `9` were cut before the keystore was present and built without it. Still: restore the file on GitHub and check AI Studio's diff for a deleted `debug.keystore` before every push. Earlier history: [archive/00-tracker-preamble.md](archive/00-tracker-preamble.md).

## Part Q — Social
Shipped: friend codes, QR scan, requests, leaderboard, friend stats, challenges, kudos, friend removal, activity feed, DMs (`ed3db2f` … `5b4d21e`). Challenge notifications/browse UI removed (`087a6f3`).
- [ ] **Firestore rules drift (code-checked):** repo `firestore.rules` only allows `users/{uid}/**`, but `FirebaseManager.kt` uses top-level `public_profiles`, `friendships`, `friend_codes`, `friend_stats`, `challenges`, `dm_threads`, `kudos`. Either the console rules differ from the repo file, or social reads are failing. Zunaid: copy the live console rules into the repo so they're reviewable.

## Session fixes — 2026-10-06
- [x] **Account deletion now removes social data** (`824096d`, `deleteSocialData` in `FirebaseManager.kt`, called before the `users/{uid}` delete): own sent messages (in accepted threads), activity events, friend code, friend stats, public profile, friendships. Challenges and kudos are intentionally kept (clients may not delete them: a losing player could otherwise delete an active challenge); they hold only user ids and step counts. Messages in threads whose friendship is already gone cannot be found and remain.
- [x] **Rules published and in the repo (`38d7da3`):** owner may delete their `friend_codes` entry and `activity_events`; a sender may delete their own `dm_threads` messages. Console and repo are identical.
- [x] **Firestore token Step 3 done (2026-10-06):** `public_profiles` writes containing `fcmToken` / `fcmTokenUpdatedAt` are now rejected; console published and the repo `firestore.rules` updated to match.

## Session fixes — 2026-10-05
- [x] **AI chat dead in GitHub-built APKs.** API keys reach the app through a build-time `.env` file (Secrets Gradle plugin, falling back to `.env.example`, whose values are `MY_…` placeholders). The CI build had no `.env` and passed no keys, so releases 4 to 6 shipped placeholder keys and every Gemini call failed; AI Studio's own builds inject real keys. Fix: repository secrets `GEMINI_API_KEY`, `GEMINI_API_KEY_2`, `GEMINI_API_KEY_3`, `YOUTUBE_API_KEY`, passed as `env:` to the "Build debug APK" step (`48b1abc`); AI Studio's `build.gradle.kts` code (`422d39d`) writes `.env` from those variables. Verified: chat works on release 7. Caveat: the keys are embedded in a public APK and can be extracted — set quotas / billing alerts in Google Cloud; restricting by Android app is impractical (the app's raw REST calls don't send the restriction headers); a small proxy is the real fix.
- [x] **AI Studio's chat-code changes** (`6684f03`, `422d39d`), reviewed 2026-10-05, no UI files touched: `resolveApiKeys` no longer treats the YouTube key as a Gemini key; HTTP 400 (invalid key) now falls through to the next key/model; chat history is sanitised before sending (blank messages dropped, a leading AI greeting removed, never empty — Gemini rejects requests that start with an AI turn); model list updated (`gemini-3.5-flash` and `gemini-3.1-pro-preview` added, `gemini-2.5-flash` removed); user-facing errors are now the generic "Sorry, I couldn't process that. Please try again.", which hides the real cause when debugging.
- [x] **Releases 5 to 7.** Tag `5` was cut with `versionCode = 4`, so the installed app kept seeing "update available". Tag `6` was created one commit before the keystore restore (`e85362f`), so it built without `debug.keystore`. Tag `7` (`c2b6bf8`, `versionCode = 7`) built correctly. The workflow now has a "Pre-flight checks" step (`c2b6bf8`) that fails a tag build early if the tag ≠ `versionCode`, `debug.keystore` is missing or has the wrong checksum, or an API-key secret is empty.

- [x] **Firestore rules reviewed and hardened (2026-10-05).** The live console rules had no rules for `activity_events`, `kudos` and `dm_threads` (so activity feed, kudos and direct messages were denied), allowed deleting only *pending* friendships (so removing a friend was denied), and let a participant rewrite anything in a challenge. The repo's `firestore.rules` now matches the console and fixes all of that: friendship create requires the sorted-uid doc id and either party may delete at any status; `activity_events`, `kudos` and `dm_threads/{pair}/messages` have rules (messages only between accepted friends, immutable); challenges: create only between accepted friends at 0/0 with no winner, a participant may change only their own progress (0 to 700000) while active, completion only on/after the end date with the true leader as winner, completed challenges immutable. Limit: steps are self-reported, so someone can still inflate their own number. Not tested on the emulator; check in the Rules Playground and with two accounts.
- [x] **Push-token exposure (2026-10-05).** Tokens were written to the world-readable `public_profiles`. `121368a`: the notification script reads the token from `users/{uid}` (falling back to the public one) and the app deletes the public copy on start. `8a7d7fd` / tag `8` / `versionCode` 8 ships it. Step 3 (reject `fcmToken` writes to `public_profiles`) was enabled on 2026-10-06, see above.

Older notes (untracked work since `8037b30` and the session fixes of 2026-09-29 and 2026-10-04): [archive/00-session-fixes-and-untracked-work.md](archive/00-session-fixes-and-untracked-work.md).

## Sprint 10 — closed
Multi-day workout programs (Part AC), the Assistant consolidation (Part Z Phase 2) and the Sprint-9 leftovers are done: [archive/PART-AC-Z2-programs-and-consolidation.md](archive/PART-AC-Z2-programs-and-consolidation.md). **Release 12 is deliberately held** (Zunaid will cut it): it carries everything after tag `11` (programs, AI programs, Today's workout card, the Assistant now replacing Chat/Coach/Food Chat, Add exercise picker, AI rounds, sports labels, account-deletion cleanup). Checklist: bump `versionCode` and `versionName`, tag = `versionCode`, create the release last; after installing tap Settings > Health Connect > Connect once.

Open follow-ups:
- [ ] **Scanner chat button starter:** `MainActivity.kt` opens `universal_assistant?starter=nutrition`, which sends the bare word "nutrition" as the user's first message. Fixed by AH-1 (Sprint 11).
- [ ] Dead-code cleanup in the view model/repository (see the archive note); AI Studio prompts must keep the keystore line.
- [ ] Optional: more Assistant tools (e.g. `generate_workout_program`) so programs can also be created by chat.

## Sprint 11 — closed (one small fix pending)
Manual exercise logging (AE), Assistant creates programs (AF), Today big-card polish (AG), Assistant food logging with blank start and multi-meal (AH), workout music (AI), blood glucose unit choice (AJ) and Health Connect history for all metrics (AD) are done: [archive/PART-AD-AJ-sprint-11.md](archive/PART-AD-AJ-sprint-11.md). Pending: **AD-4** — only sync `friend_stats` for today's metric (prompt sent). **Release 13** is not cut yet; checklist: bump `versionCode` AND `versionName` (release 12 still says 1.5), tag = `versionCode`, create the release last, title matching `versionName`; after installing, Settings > Health Connect > Connect once (new permissions since release 11: active-calories write) and consider allowing "Older exercise history".

## Sprint 12 — closed
Brainstorm-mode sprint (breathing animation AK, mindful walking and playlists AL, import report AD-5, stale-data fix AM-1, now-playing AI-2/AI-3, Spotify integration AL-2b) is done: [archive/PART-AK-AM-sprint-12.md](archive/PART-AK-AM-sprint-12.md). **Working mode stays brainstorm:** items Zunaid asks for are added to this board; a prompt is written only when he says "give prompt". Open: 
| AM-2b | Finish AM-2: wrap the read-merge-save in the five functions that were not locked: `addSteps`, `logScannedFood`, `logFoodEntriesBatch`, `getHourlyWaterForDate`, `applyDriveBackup` (fresh database reads are already in place, so only a millisecond race with the Health Connect sync remains) | `ShasthoViewModel.kt` | 💡 optional, waiting for "give prompt" |

## Sprint 13 — Navigation redesign (Part AN, planned 2026-10-08, not started)
Requested by Zunaid (screenshot of the Fitness tab): (1) the three practice cards at the bottom of the Fitness tab's AI Workouts view ("Core Protocol Practices": Physical Exercise & HIIT, Hormonal Health & Sun, Breathwork & Meditation) get their OWN tab; (2) the bottom navigation bar becomes dynamic and truly floating: Today is fixed, the user chooses which other screens appear in the bar, everything else goes under "More", so the bar never gets cluttered as screens are added.

**Code facts (checked at `ac29686`):** `MainActivity.kt` defines `sealed class TabScreen` (Today `today`, Fitness `fitness`, Nutrition `nutrition`, Sleep `sleep`, Health `health`) and `val bottomTabs = listOf(...)`; `isMainTab = bottomTabs.any { it.route == currentRoute }`; the bar is the Scaffold's `bottomBar`: a full-width `Box` with `.background(MaterialTheme.colorScheme.background)` and padding, containing a Row with shadow, `RoundedCornerShape(28.dp)` and `surface` colour. That full-width background strip is why it looks floating but is not: the content ends above a solid page-coloured band instead of scrolling behind a pill. The three practice cards live in `WorkoutScreen.kt` under "Core Protocol Practices" (`ProtocolPracticeCard`, the Breathwork card opens `mindfulness_timer`); the "Create Custom Workout" button above them belongs to workouts.

| # | Step | Notes | Status |
|---|---|---|---|
| NV-1 | Truly floating bar | Remove the full-width background strip; draw the bar as an overlay at the bottom of the screen content (rounded pill, 16 dp side margins, elevation + hairline outline, `navigationBarsPadding`), so pages scroll visibly behind and around it; screens get extra bottom space (they already end with a spacer); the assistant floating button moves above the bar. | ⏳ scoped |
| NV-2 | Dynamic tabs: the registry and the chooser | A `NavTab` registry (route, label, icon) for every top-level screen; Today fixed first; the user picks which others show; the rest open from a "More" item; "More" only appears when something is hidden; the selected state follows the current route even for hidden tabs (highlights More). Choice stored in the profile (synced like `todayTileSlots`; Room migration + default = current five tabs). Settings > "Customize navigation bar" with a checklist and reordering. | ⏳ scoped |
| NV-3 | New "Practices" tab (name to confirm) | Moves the three practice cards out of the Fitness tab; keeps "Create Custom Workout" in Fitness; a home for mindfulness (Meditation, Breathing, Mindful walking), sun and HIIT guides and future practices. | ⏳ scoped |
| NV-4 | "More" screen | A tidy grid of the hidden tabs plus useful shortcuts (Friends, Badges, Health goals, Settings...); decision pending. | ⏳ scoped |
| NV-5 | Deep links and helpers | `navigateToTab(navController, route)` and notification `targetRoute` must work for hidden tabs; back-stack behaviour per tab unchanged. | ⏳ scoped |

**Decisions (Zunaid, 2026-10-08):** (1) the new tab is called **Mind & Body**; (2) the bar has 5 slots: **Today (fixed) + 3 chosen tabs + More**; (3) **More opens as a bottom sheet** that slides up; (4) the chosen tabs **can be reordered**.

**Proposed details, to confirm:** candidate tabs = Fitness, Nutrition, Sleep, Health, Mind & Body (five candidates, three fit in the bar, so More always holds at least two); default bar = Today, Fitness, Nutrition, Health, More (Sleep and Mind & Body inside More); the More sheet shows the hidden tabs as large tiles plus a row of shortcuts (Friends, Badges, Health goals, Settings) and a "Customize" button; reordering with up/down arrow buttons in Settings > "Customize navigation bar" (reliable; drag and drop can follow); choice stored as a CSV of route ids in a new `UserProfile.navTabs` field (default empty = default bar; Room migration + synced with the profile, like `todayTileSlots`); the More item is highlighted when the current screen is a hidden tab; the bar is a floating pill over the content with the assistant button above it.

**Proposed order of work (each step a separate prompt, only on "give prompt"):** NV-3 (Mind & Body tab and the move of the three cards; briefly six tabs in the old bar) -> NV-1 (truly floating bar) -> NV-2 + NV-4 (tab registry, dynamic bar, More sheet, Customize screen, profile field + migration) -> NV-5 (links and notifications).

## Part Z — Universal AI Assistant
Shipped: repository layer, ViewModel wiring, screen, global floating button, tool-calling with plain-chat fallback, persisted history, save generated diet charts/workouts, multi-model/multi-key fallback, chat sessions (`8555d67` … `994c07c`).
- Rule: function-response role is `"function"` (`ed1c511` reverted the `"user"` change).
- AI answer look and structure (card, renderer, style guide, follow-up chips) is Part Y — closed, see [archive/PART-Y-ai-response-overhaul.md](archive/PART-Y-ai-response-overhaul.md).
- [x] Phase 2 (consolidation into the Assistant) done 2026-10-06, see Sprint 10 above..

## Carried over
- [ ] `WorkoutScreen.kt` "AI Workouts" tab label wrap — label is `fontSize = 13.sp` but has no `maxLines`; confirm on a device (left over from Part R, now closed).
- [ ] Part I #7 — real-device heart-rate check (Zunaid only).
- [ ] Part P #6 — multi-day workout programs (deferred, needs its own scoping).
- [ ] Tile customization via DataStore — not built (*code-checked*: no DataStore usage; tile slots persist via `updateTodayTileSlots` on the profile instead). Confirm whether that's sufficient.
- [ ] Repo hygiene — 34 one-off root scripts (`fix_*.py`, `update_*.py`, `rewrite_*.py`, `add_*.py`, `test_*.kt`). Candidate for an AI Studio cleanup prompt.
- Thriva name / T-monogram explored, not implemented; shipping name is KardIQ.
