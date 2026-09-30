<!-- Archived VERBATIM from the Claude Project copy of SPRINT_TRACKER.md (last verified commit 8037b30) on 2026-09-29. History: check against the code before trusting. -->

## 🔲 PART U — PER-METRIC GRAPH TYPE REDESIGN (Google Fit / Fitbit-Health parity, in progress)

Zunaid's request: too many metrics render through the same generic chart — redesign so each metric's graph matches how Google Fit / Fitbit (now the Google Health app) actually displays that kind of data.

**Audit done this session, before touching any code.** `MetricDetailScreen.kt`'s chart dispatch (`when (metricKey)`, ~line 727) was read in full, plus `WeightLogScreen.kt`, `GlucoseLogScreen.kt`, and `SleepScreen.kt`'s own bespoke charts (Blood Pressure/Weight/Glucose/Sleep don't route through `MetricDetailScreen` at all today — each either has its own screen-level chart or none). Finding, metric by metric:

- **Steps, Active Calories, Calories Consumed, Carbs, Protein, Fat, Water, Exercise Days → `StepsCaloriesBarChart` (one bar per day).** This is already the right chart type — Google Fit/Fitbit show every one of these as daily-total bars. **No change needed.**
- **Heart Rate, Oxygen Saturation → `ZoneBarChart` (min-max range bars per day, HR additionally zone-colored).** Already matches a reference app from Part I's dedicated redesign. **No change needed.**
- **HRV, Skin Temperature, Respiratory Rate → `LineChartMetric` (plain trend line).** Matches convention for slow-moving physiological trend values. **No change needed.**
- **Weight → its own line chart in `WeightLogScreen.kt`** (dot + connecting line, not routed through `MetricDetailScreen`). Already the right chart type for a weight trend. **No change needed.**
- So four of the seven "chart families" already in the app are correctly differentiated — the real gaps are the remaining three:
- **Blood Glucose → `GlucoseLogScreen.kt`'s own dual-line chart (Morning/Night).** A line chart is directionally right, but it's missing the **colored target-range band** that Google Health's own glucose logging has (confirmed it lets the user set/view a target range) — right now there's no visual cue for whether a reading was in range. **Needs a redesign — Phase 3 below, blocked on Zunaid confirming the actual target-range numbers (this borders on a clinical value, not something to invent).**
- **Blood Pressure → no trend chart exists at all today.** It's not wired into `MetricDetailScreen`'s dispatch and has no drill-down entry point from `HealthScreen.kt`. Zunaid's own reference screenshots (from the Part T conversation) show Google Health's Blood Pressure week view as a per-day vertical range bar (systolic top, diastolic bottom) — structurally identical to Amar Fit's existing Heart Rate range-bar chart. **Needs a new addition — Phase 2 below.**
- **Sleep → no trend chart exists at all today.** `SleepScreen.kt` has zero chart code — just today's hours + goal line (Part N). Google Health's own sleep view shows nightly-hours bars for the Week/Month trend (a separate "stages" view breaks each night into Awake/Light/Deep/REM segments, but that needs real per-stage data Amar Fit doesn't capture from manual entry — consistent with the Part T finding that sleep stages aren't a realistic manual-entry field). A nightly-hours bar chart, matching `StepsCaloriesBarChart`'s exact pattern, is the honest, well-scoped target. **Needs a new addition — Phase 1 below, sending now.**

**Phasing:**
- **Phase 1 — Sleep: "Last 7 Nights" bar chart.** Sending now — touches only `SleepScreen.kt`, no overlap with anything currently in flight.
- **Phase 2 — Blood Pressure range-bar trend chart.** Fully scoped (reuses the exact `ZoneBarChart` range-bar pattern already built for Heart Rate). Was held back pending Part T Phase 0's `HealthScreen.kt` changes landing — **that verified in commit `2271588`, so this phase is now unblocked.** Not yet sent (queued behind Part V's active phases, since those are the priority right now); will send once Part V's schema phase verifies, to keep only one migration-chain change in flight at a time.
- **Phase 3 — Blood Glucose target-range band.** Zunaid decided: **user-set range under Health Goals**, not a standard clinical default. This is now the same feature as Part V's Health Goals phase below — merged there so the target-range fields and the chart that shades them get scoped together.

**Sources consulted:** [Google Health sleep tracking](https://support.google.com/fitbit/answer/14236407?hl=en), [Google Health sleep stages](https://support.google.com/googlehealth/answer/14236712?hl=en), [Google Health glucose logging](https://support.google.com/fitbit/answer/14236603?hl=en) (confirms user-adjustable target ranges exist).



---
<!-- Moved verbatim from docs/sprints/ACTIVE.md when Part U closed (2026-09-29). -->

## Part U — Graph types
- [ ] Phase 2 Blood Pressure range-bar chart appears landed (`3524ab4`; `MetricDetailScreen.kt` routes `bloodPressure` through the range-bar branch) — needs a proper diff verification.

**Part U closed (2026-09-29).**
- Phase 2 verified on `main` (landed in `3524ab4`, `MetricDetailScreen.kt` untouched by the two later full-project restores): Health Vitals' Blood Pressure tile opens `metric_detail/bloodPressure`; `ZoneBarChart` draws one systolic→diastolic range bar per day (one reading per day — `DailyMetric` stores a single `"sys/dia"` string), auto-scaled axis, 3 gridlines, grey stub for empty days. Both write paths (`setBloodPressure`, Health Connect sync) store whole-number `"sys/dia"`, which the chart parses.
- `4900cc9` — added the missing "Average: sys/dia mmHg" line (`bloodPressureAverage`, rounded, ignores malformed readings).
- Left for Part R: `ZoneBarChart`'s hardcoded bar colors (shared by heart rate and blood pressure).
