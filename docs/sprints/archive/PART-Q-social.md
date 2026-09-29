<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## 🔲 PART Q — SOCIAL: FRIENDS, CHALLENGES & LEADERBOARD (queued, not started — after Part P)

Zunaid's request (revisits the "leaderboards/social features — out of scope" line from Part C): a social layer reusing the existing streak/badges/points system. Two ways to compete:
- **Global leaderboard** — challenge any other user ranked there.
- **Friends** — each user gets a unique ID; another user adds them by scanning a QR code or entering the ID directly. **Mutual consent required** — both sides must accept before the friendship is active. Once friends, each can see the other's stats, challenge each other on health goals, and earn points/medals from those challenges.

**Explicitly deferred until Part P is done, per Zunaid.** Not yet scoped into prompts — this note is to not lose the requirements before we get to it.

**Early technical grounding (not a full design yet):**
- Firebase Auth's `uid` (already the app's identity for Firestore's `users/{uid}` tree) is the natural "unique ID" — no need to invent a separate ID scheme. A shorter, shareable friend-code could be derived from it if a raw UID is too ugly to type manually.
- QR: ML Kit's barcode-scanning library (`com.google.mlkit:barcode-scanning:17.3.0`) is already a dependency (used for food barcode scans in `ScannerScreen.kt`) and can decode QR format too — reusable for scanning a friend's code. It cannot *generate* a QR image to display, though — showing your own QR code will need one small new dependency (e.g. `com.google.zxing:core`, a lightweight pure-Java encoder, no extra Android surface).
- `UserProfile` already has `currentStreak`, `points`, `badges` — the existing currency this feature reuses, per Zunaid's own framing.
- **Open architectural question to resolve when we scope this** (not decided yet, flagging rather than guessing): today each user's Firestore data lives under `users/{uid}` and (as far as verified this sprint) is only ever read by that same user. Letting a friend — or a public leaderboard — read someone else's stats needs either (a) Firestore security rules that check a `friends` list before allowing a narrower read, or (b) a separate, smaller "public profile" document per user (name, streak, points, badges only — no health data) that's readable more broadly. This is a real security-rules design decision, not just a UI feature, and needs a call before the first prompt goes out.
- Friend requests need a new Firestore shape (e.g. `users/{uid}/friend_requests` incoming/outgoing, `users/{uid}/friends` once accepted) — mutual consent means a request sits pending until the recipient accepts, not an instant add.
- Likely needs phasing similar to Part M (Drive) or Part O (body fat): schema/Firestore-structure first, then friend-add UI (QR + manual ID + consent flow), then stats-viewing, then challenges/points, roughly in that order — to be finalized when we actually start this.

