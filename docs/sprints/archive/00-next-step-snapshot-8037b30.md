<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## Next step

Fully done and verified: Part D (all 4 items), Part G (full stack), Part J, Part L (auto-sync), Part E (all 6 items), Part I #1-6, Part I #8 in full including the Year-view calendar-dropdown date picker (9 items total, verified in `76f81a2`, `06ec69f`, `e606680`, `fdc71c3`, `01b5027`/`4d1e3a7`, `ab92736`, and `a3c4c1b`), Part F (all 5 items, including the Active Calories/Water hourly chart follow-up verified in `6c59304`), and **Part H in full (all 3 items — barcode + photo scan portion-size adjusters, and true manual macro entry, verified in `fdc71c3`, `fecb429`, and `21626ea`)**. Keystore regression resolved and verified twice (see the recurring-hazard note at the top of this file); no recurrence since the H#3 push.

**Part B: CLOSED.** Both remaining checks confirmed fine live (Firestore Console data present, tab-nav fix works after rebuild) — no fix needed, nothing landed as a commit.

**Rebrand (Part K): on hold**, picking back up later — not now, at Zunaid's request. Inventory already done (see Part K section above) so the prompt can go out fast whenever he's ready.

**Part M: CLOSED.** Both Backup (`055fc9f`) and Restore (`c6bff94`) verified — see Part M section above.

**Part N: CLOSED.** Sleep goal-display surface verified in `8131b0a` — see Part N section above.

**Part O: CLOSED.** Schema + calc engine (`8551d45`) and UI (`31f611e`) both verified — see Part O section below.

**Data export (CSV/PDF): DROPPED**, per Zunaid — Drive backup/restore covers "get my data out" well enough for now.

**Queue refilled — see Part P below.** Ran a fresh gap-review pass (same pattern as Part D/Part F) once Part O closed the previous backlog out. Found 9 concrete gaps, 2 of which are real bugs in already-shipped features (not new asks). Working through them in order per Zunaid's "fix all of them."

Also queued (not started): **Part Q** (social/friends/challenges) and **Part R** (FitPal-inspired dark-theme redesign, all 26 screens) — both captured below, sequenced after Part P.

Still separately open:
- **Rebrand (Part K)** — on hold at Zunaid's request, blocked on his product-name/logo decision. Inventory already done, prompt can go out fast whenever he's ready (see Part K section above).
- **Part I #7** (real-device heart rate verification) — user-only, closes out Part I's original 7 items whenever Zunaid checks it on a real device.

