# BMI calculator fix and the sweep for the same kind of bug (2026-10-19)

**Report (Zunaid):** the BMI calculator is not working: changing the numbers has no effect.

## What was wrong
1. **The BMI calculator on the Health tab never saved anything.** It worked on private copies of your weight and height, showed the new BMI inside the dialog, and its only button was **Close**, which threw the change away. The BMI card on the screen reads the saved profile, so it never changed. (The Weight log screen could save, through `setWeightAndHeight`, but nothing told you that.)
2. **The Weight log copied your weight and height once, when it opened.** If the profile arrived a moment later (or changed on another phone) it kept showing the 70 kg and 170 cm placeholders, and **Save & Calculate would have saved those placeholders over your real numbers**.
3. **The weight and height picker nudged your numbers.** It cut values off instead of rounding: 72.5 kg became 72 just by pressing OK, and a height of 170 cm showed 5 ft 6 in (it is 5 ft 7 in). In pounds or feet each visit could drift the stored value by up to about 0.45 kg or 2.5 cm.
4. **The BMI formula was written out three times** (Health tab, Weight log, Profile), and the Health tab card showed a BMI for the 70 kg and 170 cm placeholders while the profile was loading.

## Fixes
* **The Health tab calculator now has Save and Cancel** (Save is enabled only when the numbers differ from the saved ones and are sensible: 20 to 300 kg, 50 to 250 cm). Save calls the same `setWeightAndHeight` as the Weight log, so the profile, today's weight, the weight chart and Health Connect all update.
* **The Weight log follows the real profile** until you change a number yourself, and refuses to save while the profile is still loading or the numbers are out of range.
* **The picker rounds** (`PickerMath`) and **gives back the exact value when nothing was moved**.
* **`BodyMetrics`** is the one place for BMI, the categories, the bar fill, the ideal weight (BMI 22), the limits and the "24.2" text (always with a dot, even on a Bangla or German phone). The three screens use it.

## The sweep for the same kind of bug (automated, then read by hand)
| Pattern | Result |
|---|---|
| A dialog with inputs or pickers whose only button just closes (edits silently discarded) | 1 real (the BMI calculator). Other hits were false alarms: they save through their own buttons or by tapping a choice (Add playlist, Workout title, Log sleep, Water and Steps quick-add, password reset) |
| `remember { mutableStateOf(<profile or metric value>) }` with no key (copied once, never refreshed) | 2 sites: the Weight log (fixed) and the BMI dialog (made when the dialog opens, after the profile exists: fine) |
| `LaunchedEffect(Unit)` that reads a state value captured at the first composition (stale in a long-running loop) | 4 hits, all false alarms: each only **assigns** loaded data to state (exercise library, Health Connect permission) |
| The same formula written in several screens | BMI: fixed. |
| Number formatting that follows the phone's language (`String.format("%.1f")` without a locale; Kotlin `"%.1f".format(x)`) | 5 plus 28 places, handled separately on `main` after this hotfix (a Bangla phone would show Bengali digits in some places and not in others) |

## Tests
`BodyMetricsTest` (limits, zero and negative inputs, categories exactly at 18.5, 25 and 30, bar fill, ideal weight, a dot even on a Bangla phone), `PickerMathTest` (rounding, round trips within half a unit), `BmiWiringTest` (the calculator saves, the formula exists once, the Weight log follows the profile, the picker keeps the exact value).
