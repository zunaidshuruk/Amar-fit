# Nutrition & Diet screen (redesign, 2026-10-15)

Follows the CaloriCam reference (`design-refs/part-r-nutrition-caloricam-ref.jpg`) on KardIQ's dark theme and palette (theme tokens only). Only the main
"Nutrition & Diet" screen was redesigned; Diet Chart, Meal Plan and Recipes are unchanged and still reached from the list at the bottom.

From the top:
1. **Daily intake** (lime hero card, like Today): today's calories as a big percentage of the calorie goal, a one-line status ("600 kcal to reach your target
   range", "In your target range", "250 kcal over your target") and a ring with consumed / goal inside. Tap: opens the Food Log.
2. **This week**: the last 7 days (ending today) as bars; today starts selected and is lime, tap a bar to see that day (day and kcal at the top right); dashed
   line = the calorie target. Days without records show a small stub.
3. **Daily calories**: today's kcal, the target, a bar with the **target range** marked (the last 10% below the goal, up to the goal, rounded to 10 kcal) and the
   range as text. The bar turns amber when over the target.
4. **Nutritions**: carbs, protein and fat today against their daily targets (`HealthGoalCalculator.calculateMacroGoals`): percentage, grams and a bar.
5. **Nutrition info**: two pies, the recommended split for the calorie goal and what was eaten today, with the percentages in a small table. Shows "Log a meal" until
   something is logged. The recommended split is a general guide, not medical advice (stated on the card).
6. The existing list: Food Log & Scanner, Meal Plan, Diet Chart, Medicinal Recipes. 88 dp of end space keeps the last item clear of the assistant button.

Numbers (intake percent, target range, status text, macro split that always adds to 100, the 7-day series) are in `data/nutrition/NutritionView.kt`, pure and tested.
Colors: carbs = primary, protein = secondary, fat = tertiary (as before on this screen); amber (tertiary) is also used for "over target".
Not built from the reference: the "last synced" line and the weight card (Weight lives on Health).
