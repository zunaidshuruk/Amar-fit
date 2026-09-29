<!-- Archived from claude/HANDOVER.md (Project knowledge) on 2026-09-29, verbatim except one personal-details line removed for the public repo. -->

# Amar Fit — Session Handover

**Purpose of this document:** this is a handover from a previous chat (running under the same "Amar-Fit" Claude project) that got too long/heavy and is being replaced by a fresh chat. Read this whole document before doing anything else. It contains the standing role, the workflow rules, and the exact current state of the sprint. `SPRINT_TRACKER.md` (also saved in this project) is the detailed item-by-item log — **as of this update it is stale, dated at commit `8037b30` (Part R Phase 2b). A lot of untracked work happened after that (see §4/§5 below) and has not yet been folded into it** — treat this document, not the tracker, as the current source of truth until someone does a careful full rewrite of the tracker to fold the gap in.

---

## 1. Who the user is and what this project is

User: **Zunaid Hossain** (sole developer). [personal/employer details removed for the public repo]

**Amar Fit** is an Android health/fitness app — Kotlin, Jetpack Compose (Material 3), MVVM/Coroutines/StateFlow, Room (local DB), Firebase Auth/Firestore (cloud sync), Health Connect Android SDK (reads device health data), Gemini API (AI food-scan/chat/coach features).

Repo: **github.com/zunaidshuruk/Amar-fit**, default branch `main`. Public repo (anonymous `git clone`/`git fetch` works with no auth).

## 2. The standing role — read this carefully, it's the whole working model

Zunaid does not write code himself. He pastes prompts into **Google AI Studio** (a separate tool, not part of this conversation), which generates code and pushes commits directly to the GitHub repo (sometimes labeled "Add files via upload" — that's a GitHub web-UI upload, not necessarily AI Studio; Zunaid also appears to upload files directly via GitHub's web UI himself at times, e.g. to restore a deleted `debug.keystore`). My job in this chat has three parts:

1. **Draft small, precisely-scoped AI Studio prompts**, one per feature/fix. Rules for every prompt:
   - Never combine unrelated changes into one prompt — one scoped thing at a time.
   - State explicit acceptance criteria.
   - State an explicit "DO NOT TOUCH" / "OUT OF SCOPE" section naming exactly what must stay untouched.
   - If a request is ambiguous or bigger than it sounds, **flag the scope/feasibility gap honestly in the same message as the prompt** (or use AskUserQuestion first) rather than silently guessing or silently blocking.
   - Deliver every prompt **directly in the chat message as a fenced code block** — never as a separate .md file/attachment. This is a hard user preference (saved in memory).

2. **Independently verify every push** — never trust AI Studio's self-reported commit message or summary. The standard sequence, every time:
   ```
   cd <local clone> && git fetch origin
   git diff --stat <old_sha> origin/main      # confirm ONLY the intended files changed
   git diff <old_sha> origin/main -- <file>   # full content review of the actual diff
   git status --short && git reset --hard origin/main   # sync local clone (only after confirming no uncommitted local work would be lost)
   ```
   Read every diff line by line against the prompt's spec before saying it's correct. This has repeatedly caught real bugs — file-ordering/stale-snapshot accidents where uploading an old local copy silently reverts newer work (several precedents logged in SPRINT_TRACKER.md), a recurring `debug.keystore` deletion, and (this session) a reintroduced bad import that would break the build — see §5.

3. **Fix small, precise, low-risk bugs directly myself** rather than routing them back through AI Studio, when doing so is clearly lower-risk than another AI Studio round-trip. **No GitHub push access from this session** (confirmed blocked again this session — see §6), so any direct fix is delivered as a full corrected file via SendUserFile, **with the exact repo-relative path stated every time**, for Zunaid to manually place. This is also a hard user preference (saved in memory) — always state the exact repo-relative path when handing over a file to place.

**Standing preference (saved in memory, always follow):** after verifying a pushed commit is correct, immediately draft and send the next sprint item's prompt without waiting to be asked — unless a major issue needs to be flagged first. Don't sit idle waiting for "what's next."

**No Android build/run capability** in this chat — nothing here can compile or run the app. All verification is diff-review against the prompt spec, not a build/test. This makes catching things like a bad import (§5) purely a matter of careful reading, not a compiler catching it for us.

## 3. Environment specifics for the new chat

- This session had a local clone at `/home/claude/amarfit` already present with real commits I'd authored earlier this same session (see §4) — it is **not** guaranteed to persist to a future chat. If it's gone, re-clone: `git clone https://github.com/zunaidshuruk/Amar-fit.git /home/claude/amarfit`, then `git fetch origin` and work from `origin/main` (see §4 for why "work from origin/main," not from any local branch, is now the right default).
- Outbound HTTPS to github.com (the git protocol host, for clone/fetch) works fine. **Authenticated push does not** — see §6, tested again this session, still blocked at the sandbox's own proxy level (403, "not in this session's authorized repository set"), independent of any token.
- `/mnt/user-data/outputs/` in whatever session is running holds a large batch of individual `.kt` files (most main screens) plus two design-reference images and an `amarfit-designrefs.bundle` — these look like staged deliverables from recent sessions, not necessarily current. Don't assume they reflect the live repo; always diff against `origin/main` before trusting a local file's content.

## 4. Important discovery this session — two independent branches converged, then diverged again on one bug

Before this session's compaction, I had apparently done a chunk of work locally (not yet delivered to Zunaid): a badge-gallery redesign (animated medallions, more badges), then a hero-tile "swipeable pager" redesign of the Today screen's large tiles, then — after presumably getting feedback — a revert back to the original vertical-stack layout, keeping the tap/swipe-to-flip detail interaction. These were real local commits (`7eae691`, `fc6ccdf`, `b285e9b`, `a2be2b2`), but **they were never actually pushed** (push is blocked — see §6) and, importantly, **`origin/main` had moved on independently in the meantime**: it looks like Zunaid's own AI Studio/GitHub-upload workflow built the *same* kind of feature (badge graphics-layer animation, a `HorizontalPager` hero-tile redesign, then its own revert back to vertical-stack-with-flip) from an older base, without ever pulling my local commits in. Both paths converged on almost the exact same end state independently — good news, no real functional divergence — **except that the origin/main path's last few uploads reintroduced a bug my local path didn't have.**

**Net effect: `origin/main` is the authoritative live state, not my local branch.** I checked my local clone out to track `origin/main` directly (`git checkout -B main origin/main`) rather than trying to rebase/preserve the orphaned local commits, since their content is already superseded by origin's own equivalent work. If a future session sees local commits that aren't reachable from `origin/main`, that's most likely this same situation recurring (stale local branch vs. a separately-advanced origin) — the fix is the same: diff local vs. origin/main content-wise (not by commit ancestry) to see if they've actually converged, and if so just work from `origin/main`.

## 5. What's in flight RIGHT NOW — the immediate next action

**Bug found and fixed this session, delivered as 2 files, NOT YET PLACED by Zunaid.** While reviewing the current `origin/main` state of the hero-tile/badge work (HEAD `089358b` at session start), found that both `TodayScreen.kt` and `BadgeGalleryScreen.kt` import `graphicsLayer` from the wrong package:

```kotlin
import androidx.compose.ui.graphics.graphicsLayer   // WRONG — this symbol doesn't exist here
```

The `Modifier.graphicsLayer { ... }` extension actually lives in `androidx.compose.ui.draw`, not `androidx.compose.ui.graphics`. Both files use `.graphicsLayer { ... }` in real call sites (the hero-tile flip animation in `TodayScreen.kt`, the badge shine/flip animation in `BadgeGalleryScreen.kt`), so as committed on `origin/main` **the app will not compile.**

This is a regression, not a first-time bug: an earlier commit (`9b92afe`, "refactor: fix graphicsLayer import in TodayScreen") already fixed this exact import once, but a later stale-snapshot upload silently reverted it, and the same wrong import got copied into `BadgeGalleryScreen.kt`'s own graphicsLayer work along the way (commit `fe2b97e`). Same file-ordering/stale-snapshot failure mode logged elsewhere in SPRINT_TRACKER.md (Part C's `f02e3ee`, Part W's `dbf4dca`→`59763d7` interaction) — AI Studio/upload workflow keeps re-uploading from a checkout that's missing a recent fix.

**Fix:** change the import in both files back to `androidx.compose.ui.draw.graphicsLayer`. Committed locally (`77453da`, on top of `origin/main`'s `089358b`) but **could not be pushed** (see §6) — delivered instead as two corrected files via SendUserFile, for Zunaid to place at their exact existing repo paths:
- `app/src/main/java/com/example/presentation/today/TodayScreen.kt`
- `app/src/main/java/com/example/presentation/badges/BadgeGalleryScreen.kt`

Both are minimal, single-line-per-file diffs from what's currently on `origin/main` (just the import line) — verified brace/paren-balanced, no duplicate imports, no other regressions found in either file or in `TodayTiles.kt`/`ShasthoViewModel.kt` (also touched by the same run of uploads; `ShasthoViewModel.kt`'s change — wrapping the login sync in `withTimeoutOrNull` with a try/catch fallback — looked like a legitimate, unrelated defensive improvement and was left alone).

**When Zunaid says "placed" / "pushed" / similar next:** re-fetch `origin/main`, confirm the new HEAD's `TodayScreen.kt`/`BadgeGalleryScreen.kt` now both read `import androidx.compose.ui.draw.graphicsLayer` (not `.graphics.graphicsLayer`), confirm nothing else changed unexpectedly, then resume the sprint per SPRINT_TRACKER.md's last known "Next step" (the pill-row reorg prompt for Part R Phase 2, last noted as "queued next, not yet run" — but check whether Zunaid's own recent uploads already touched that area, since he's clearly been working the hero-tile design directly with AI Studio outside of prompts I drafted).

## 6. Push-access discussion — RE-CONFIRMED BLOCKED this session (not a token problem)

This was investigated and resolved as "blocked at the sandbox/proxy level, not a token problem" in an earlier session (Zunaid had provided a fine-grained PAT; the session's own outbound git proxy rejects pushes to this repo with `access denied by the git proxy: zunaidshuruk/Amar-fit is not in this session's authorized repository set`). **Re-tested this session** (attempted a real push of the graphicsLayer fix, not a dry-run) — **got the exact same 403/proxy rejection.** Nothing has changed. Don't re-attempt without a signal that something changed on the account/environment-config side.

**Net effect, unchanged:** direct fixes are delivered as full corrected files via SendUserFile with the exact repo-relative path stated, for Zunaid to place manually. The AI-Studio-prompt-then-verify workflow for actual features goes through AI Studio's own GitHub connection, not through this session, and is unaffected.

## 7. Where the durable records live

- **This project** (Claude Project "Amar-Fit") has two saved docs:
  - `claude/HANDOVER.md` — this document (just rewritten this session).
  - `claude/SPRINT_TRACKER.md` — **stale**, dated at commit `8037b30`. Everything in §4/§5 above (badge redesign, hero-tile pager experiment + revert, the graphicsLayer regression) happened after that and is NOT yet reflected there. A future session should do a careful, complete rewrite of it to fold this gap in — that's a big, careful edit (the file is ~164KB), not something to rush; until then, treat this handover doc as authoritative for anything after `8037b30`.
- Keep maintaining both docs going forward: update `SPRINT_TRACKER.md` (once caught up) every time something is verified, and keep this `HANDOVER.md` accurate enough that a fresh chat can pick up from just these two docs plus a fresh `git fetch`.

## 8. Full backlog map (compressed — SPRINT_TRACKER.md has full detail, though see the staleness note above)

- **Parts A–J, L** — complete (core app, known-open-items, design/feature backlog, gap review passes D/F/G/H, manual-entry redesign E, heart rate screen I, steps ring J, auto-sync L).
- **Part K** — Rebrand: on hold, blocked on Zunaid's product-name/logo decision.
- **Part M/N/O** — Drive backup/restore, sleep goal display, body fat % — all complete.
- **Part P** — Fresh gap-review pass: complete (all 10 items, #6 multi-day workout programs explicitly deferred).
- **Part Q** — Social/friends/leaderboard: queued, not started, blocked on a Firestore security-rules architecture decision.
- **Part R** — FitPal-inspired dark theme redesign (all 26 screens): Phase 0 (foundation tokens) and Phase 1 (Today screen color cleanup) verified complete. Phase 2 (Today/Home layout restructure, Pinterest reference) is mid-flight: 2a (header re-theme) and 2b (hero card restyle) verified, 2c (calendar date-strip) verified, 2d (meal thumbnails) explicitly skipped, 2e (calorie ring large-tile option) decided/scoped. The pill-row reorg was "queued next, prompt sent, not yet landed" as of the tracker's last update — **but see §4: Zunaid has since been iterating directly with AI Studio on hero-tile swipe/flip/pager variations, so check the actual current `TodayScreen.kt` state before assuming the pill-row work is still pending as originally scoped.** Phase 3 (Nutrition/Diet macro-breakdown) not yet started.
- **Part S** — Urgent dark-mode invisible-button-text fix: complete (3 related urgent fixes landed in that section).
- **Part T** — Measurement metadata across health metrics: Phase 0 (Blood Pressure) complete; everything glucose-related folded into Part V.
- **Part U** — Per-metric graph type redesign: Phase 1 (Sleep) complete; Phase 2 (Blood Pressure range-bar chart) scoped, not sent; Phase 3 merged into Part V.
- **Part V** — Blood glucose meal-context tracking + AI guidance: complete, all 5 phases.
- **Part W** — Backdated historical glucose entry: complete, all 4 phases + a race-condition fix.
- **New, untracked in SPRINT_TRACKER.md (see §4/§5):** badge-gallery animated-medallion redesign, Today-screen hero-tile flip/swipe interaction (several iterations including a HorizontalPager experiment that was tried and reverted), and this session's graphicsLayer import regression fix.

## 9. Things NOT to re-litigate

- Explicitly out of scope, decided long ago: leaderboards/social features (until Part Q), Zone Minutes, Floors, original video/audio content production, Wear OS companion app.
- Language/i18n was deliberately ripped out (non-functional pickers removed) rather than built — English-only until picked back up as its own deliberate project.
- Part K (rebrand) is deliberately sequenced last — don't suggest starting it early even if it sounds quick.
- GitHub push access from this session — see §6. Don't re-attempt without a signal that something changed.
