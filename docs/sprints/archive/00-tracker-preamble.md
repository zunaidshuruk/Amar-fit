<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

# Amar Fit — Sprint Tracker

**How this file works:** This is the single source of truth for what's done and what's pending. Every item is only checked off after being verified against the actual GitHub repo (file contents, not AI Studio's self-reported summaries). When bug fixes or side work interrupt the main sprint sequence, this file is what keeps the overall plan from getting lost — update it, don't rely on memory. Commit this file to the repo root and re-check it at the start of every session.

Last verified: current session, against commit `8037b30` ("style: adjust padding and text constraints for buttons"). **Log/Start button text-wrap regression (caused by Part R Phase 2b's wider Edit tiles pill, reported by Zunaid from a real device) fixed and verified clean. Phase 2b (hero/dot-ring/Edit-tiles) and the isDark fix + calendar strip (Phase 2c) previously verified clean. Pill-row reorg prompt is queued next — see Part R below for why it's scoped as a size-only change rather than hardcoding specific metrics.**

**Recurring hazard — debug.keystore deletion, now FIVE times, RESOLVED (for now):** commit `06480a6` first deleted `debug.keystore` from the repo root even though `app/build.gradle.kts` (line 23) still points the debug signing config at that exact file; restored directly and verified byte-for-byte in commit `586271c` (SHA256 `4d6742b2...5f0faa`). **It happened again in commit `fecb429`**, restored again, same SHA256 confirmed. **It happened a THIRD time in commit `2271588`** — Zunaid restored it via GitHub's web upload in commit `c27d335`, verified fixed. **It happened a FOURTH time in commit `78caf43`** (Part W Phase 3, CSV export) — restored again via GitHub's web upload in **commit `f13151c`** ("Add files via upload") — verified fixed, checksum confirmed. Three consecutive clean pushes followed (`dbf4dca`, `59763d7`, `3bf4ac2`). **It happened a FIFTH time in commit `481c8f8`** (Part R Phase 0, theme foundation) — restored again via GitHub's web upload in **commit `a747807`** ("Add files via upload") — **verified fixed**: `git diff --stat 481c8f8 a747807` shows only `debug.keystore` changed (binary, 0→2666 bytes), and `git show a747807:debug.keystore | sha256sum` confirms the exact known-good checksum `4d6742b2662bfcbbb8e0a6f693d3fb939f4d2d10a446336af85a619f275f0faa`. Now 5 for 5 — still recurring despite the DO NOT TOUCH warning in every prompt, but Zunaid's GitHub-web-upload restore method is fast and reliable each time.

**Note on this file's own reliability:** I (Claude) have no push access to this repo — every tracker edit I make only exists in my local scratch clone until you manually place the file, and each time I `git reset --hard origin/main` to see your latest push, any of my own tracker edits that hadn't been placed yet are silently discarded. **Please place this exact file at the repo root before your next AI Studio push — it costs nothing and stops this from recurring.**

---



---
<!-- Moved verbatim from docs/sprints/ACTIVE.md when resolved (2026-09-29): restored via web upload in 46dbc23/bdb2a43, checksum verified. -->

## 🔴 Hazard — debug.keystore
Missing from `main` since `8d06c8b` ("refactor: fix formatting in UniversalAssistantScreen"); last good copy at `0537d66`. `app/build.gradle.kts` still points the debug signing config at `${rootDir}/debug.keystore`. Restore via GitHub web upload, then confirm sha256 `4d6742b2662bfcbbb8e0a6f693d3fb939f4d2d10a446336af85a619f275f0faa`. Full incident history: [archive/00-tracker-preamble.md](archive/00-tracker-preamble.md).

