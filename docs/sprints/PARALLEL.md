# Two sprints at once: AI Studio and Claude (rules for working in parallel)

**Why this file exists:** AI Studio pushes its whole working copy, so a push from a stale copy silently reverts files Claude merged meanwhile (this happened before:
`f02e3ee`, `59763d7`, and the deleted `debug.keystore` several times). The rules below keep the two apart.

## Who owns which files (never overlap)
| Owner | Files |
|---|---|
| **AI Studio** | nothing right now: Sprint 23 (Edit tiles and three small screens) was taken over and finished by Claude on 2026-10-15, so its prompt is void. Give AI Studio new files here before starting a new prompt |
| **Claude** (Sprint 24: old colors and light-mode branches removed from the other screens, design guard test) | everything else: all other app code, tests, `.github/`, `docs/`, build files. Claude never edits the AI Studio files above; AI Studio never edits anything else |

Docs under `docs/` (including this file and the sprint board) are Claude's only; AI Studio does not edit them.

## Rules
1. **Before every AI Studio prompt, and again right before its push:** Zunaid syncs AI Studio from GitHub `main` (pull the latest), so its copy contains whatever Claude merged.
2. **AI Studio's diff must list only its own files.** Anything else in the diff (especially a deleted `debug.keystore` or changes to files it was not given) means a stale copy: do not push, sync from GitHub first.
3. **Claude verifies every AI Studio push** (`git diff --stat` against the previous `main`): only its files changed, keystore present, braces and imports clean, then a real build on a branch. If a Claude file was reverted, Claude restores it.
4. **Claude works on `claude/*` branches and merges by pull request**, never touching AI Studio's files, and rebases on the newest `main` before merging.
5. A release is cut by Claude only when both sprints are verified and merged.

**History:** Sprint 19 (Diet Chart, Meal Plan, Recipes; AI Studio) and Sprint 20 (chip rows, Friends, CI; Claude) were verified and merged on 2026-10-15; Sprint 21 and 22 run now.
