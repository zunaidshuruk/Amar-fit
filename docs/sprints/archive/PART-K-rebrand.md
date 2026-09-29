<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## 🔲 PART K — REBRAND (logo + product name) — ON HOLD, picking back up later

User request: change the app's logo and product name (currently "Amar Fit"/"আমার Fit"). Inventory below was done, then Zunaid decided to hold rebranding and clear the rest of the backlog (Part B's 2 checks, then data export / body measurements / sleep goal display) first. Revisit when Zunaid's ready.

**Codebase inventory done (this session), so a code prompt can go out fast once the two decisions below are made:**
- Display name: `app/src/main/res/values/strings.xml` → `app_name` = "আমার Fit" (launcher label + toolbar title).
- AI coach persona name "Amar-Fit AI" — hardcoded in 4 files / 9 call sites: `CoachScreen.kt` (header), `OnboardingScreen.kt` (welcome text), `ShasthoViewModel.kt` (3 chat-greeting strings), `AppRepository.kt` (6 Gemini system-prompt strings — the actual instructions sent to the AI).
- One cosmetic technical reference: HTTP `User-Agent` header string ("AmarFit - Android - Version 1.0") — no functional dependency.
- App icon: adaptive vector icon (Emerald `#10B981` glyph on white) + 10 pre-rendered raster fallbacks across 5 densities — a new logo needs both the vector source and regenerated raster fallbacks.
- **Deliberately NOT touching**: `applicationId` (`com.aistudio.amarfit.app`) / package namespace — changing that would make Play Store treat it as a new app, break upgrade paths for existing installs, and require reconfiguring Firebase. Flagged as an assumption, not yet explicitly confirmed by Zunaid.

**Candidate name "KardIQ" — availability check done (this session):**
- Trademark (Justia trademark database, US): no exact "KARDIQ" registration found. Closest neighbors on record: KARDI, KARDIA, KARDINI, KARDIAC BRAND. No exact hit is a good sign but not a legal clearance — a proper clearance search (and attorney opinion, if this goes anywhere) would be needed before committing.
- App stores (Google Play + Apple App Store): no exact "KardIQ" listing found on either. Adjacent/similar names in the health space do exist: Kardi, Kardi Ai, Kardz, Kard-App.
- General web/domain: `kardiq.com` — no clear existing commercial site found squatting the name; not a substitute for an actual registrar WHOIS/availability check before committing, which hasn't been run.
- Non-health collision: an existing open-source GitHub project is literally named "KardIQ" — unrelated domain (an AI-agent/knowledge-card reference tool, not health/fitness), so low real-world confusion risk but worth knowing it's not a clean slate on GitHub/npm-style namespaces.
- **Real risk worth flagging**: AliveCor's "**Kardia**" (KardiaMobile ECG monitor, kardia.com) is a well-established, actively-marketed registered trademark in the heart-health-monitoring space — sold via CVS and other retail channels. "KardIQ" is a close phonetic neighbor to "Kardia" and sits in an adjacent health/heart-data category. This is the main collision risk, even though no exact trademark match was found — phonetic/conceptual similarity in the same broad space is exactly what trademark disputes get built on.
- **Verdict**: no exact-match blocker found (trademark, app stores, or obvious domain squat), but not a clean bill of health either — the AliveCor "Kardia" adjacency is a real practical risk, not just a false-positive of the exact-match search. If Zunaid wants to move forward, next steps before committing would be: (1) an actual domain-registrar availability/WHOIS check for kardiq.com and a couple TLD variants, (2) a fuller trademark clearance (ideally with a professional search or attorney, not just a free Justia lookup), and (3) a gut check on whether "KardIQ next to Kardia" feels too close for comfort.

- [ ] New product name decided (not yet chosen) — **blocking, will not invent this**. "KardIQ" is one candidate under consideration; availability findings above, no final decision made yet.
- [ ] New logo/app icon designed (not yet started) — **blocking, will not invent this**
- [ ] Scope + send the actual code prompt once the above two are decided

