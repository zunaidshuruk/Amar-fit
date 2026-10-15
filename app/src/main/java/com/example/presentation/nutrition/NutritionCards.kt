package com.example.presentation.nutrition

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.health.HealthGoalCalculator
import com.example.data.nutrition.NutritionView
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Cards of the redesigned Nutrition screen. Colors come from the theme only: carbs = primary, protein = secondary, fat = tertiary. */

private val cardShape = RoundedCornerShape(20.dp)

@Composable
private fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) { content() }
}

/** The hero card: today's intake as a big percentage with a ring. Tapping it opens the food log. */
@Composable
internal fun IntakeHeroCard(consumed: Int, goal: Int, onClick: () -> Unit) {
    val onHero = MaterialTheme.colorScheme.onPrimary
    val percent = NutritionView.intakePercent(consumed, goal)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(onHero.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = onHero, modifier = Modifier.size(18.dp))
                    }
                    Text("Daily intake", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = onHero, modifier = Modifier.padding(start = 8.dp))
                }
                Text("$percent%", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = onHero, modifier = Modifier.padding(top = 12.dp))
                Text(NutritionView.statusText(consumed, goal), fontSize = 13.sp, color = onHero.copy(alpha = 0.85f))
            }
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .semantics { contentDescription = "Daily intake ring: ${NutritionView.format(consumed)} of ${NutritionView.format(goal)} kilocalories" },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(color = onHero.copy(alpha = 0.18f), startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
                    val sweep = (percent.coerceIn(0, 100) / 100f) * 360f
                    if (sweep > 0f) drawArc(color = onHero, startAngle = -90f, sweepAngle = sweep, useCenter = false, style = stroke)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(NutritionView.format(consumed), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = onHero)
                    Text(NutritionView.format(goal), fontSize = 12.sp, color = onHero.copy(alpha = 0.8f))
                }
            }
        }
    }
}

/** Today's calories against the target, with the target range marked on the bar. */
@Composable
internal fun DailyCaloriesCard(consumed: Int, goal: Int) {
    val scheme = MaterialTheme.colorScheme
    val range = NutritionView.targetRange(goal)
    val over = NutritionView.status(consumed, goal) == NutritionView.CalorieStatus.OVER
    val scaleMax = (maxOf((goal * 1.15f).toInt(), consumed, 100) + 99) / 100 * 100
    CardBox {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp))
                Text("Daily calories", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground, modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                "${NutritionView.format(consumed)} kcal",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = if (over) scheme.tertiary else scheme.onBackground,
                modifier = Modifier.padding(top = 10.dp)
            )
            Text("Target: ${NutritionView.format(goal)} kcal", fontSize = 13.sp, color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(14.dp)) {
                val w = maxWidth
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(7.dp)).background(scheme.background))
                if (range.high > 0) {
                    val low = range.low / scaleMax.toFloat()
                    val high = range.high / scaleMax.toFloat()
                    Box(Modifier.offset(x = w * low).width(w * (high - low)).fillMaxHeight().background(scheme.onSurface.copy(alpha = 0.22f)))
                }
                Box(
                    Modifier
                        .width(w * (consumed / scaleMax.toFloat()).coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (over) scheme.tertiary else scheme.primary)
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0", fontSize = 11.sp, color = scheme.onSurfaceVariant)
                Text(NutritionView.format(scaleMax), fontSize = 11.sp, color = scheme.onSurfaceVariant)
            }
            if (range.high > 0) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(scheme.onSurface.copy(alpha = 0.22f)))
                    Text("Target range", fontSize = 13.sp, color = scheme.onSurfaceVariant, modifier = Modifier.padding(start = 8.dp).weight(1f))
                    Text(
                        "${NutritionView.format(range.low)}\u2013${NutritionView.format(range.high)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onBackground
                    )
                }
            }
        }
    }
}

/** The last 7 days as bars; today starts selected, tap a bar to see that day. The dashed line is the calorie target. */
@Composable
internal fun WeeklyCaloriesCard(days: List<Pair<LocalDate, Int>>, goal: Int) {
    val scheme = MaterialTheme.colorScheme
    var selected by remember(days.size) { mutableIntStateOf(days.lastIndex.coerceAtLeast(0)) }
    val chosen = days.getOrNull(selected)
    CardBox {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("This week", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                if (chosen != null) {
                    Text(
                        if (chosen.second > 0) "${chosen.first.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)} \u00B7 ${NutritionView.format(chosen.second)} kcal"
                        else "${chosen.first.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US)} \u00B7 nothing logged",
                        fontSize = 13.sp,
                        color = scheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            val barSelected = scheme.primary
            val barOther = scheme.onSurfaceVariant.copy(alpha = 0.4f)
            val goalLine = scheme.onSurface.copy(alpha = 0.4f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .semantics { contentDescription = "Calories for the last 7 days; tap a bar to see its day" }
                    .pointerInput(days.size) {
                        detectTapGestures { offset ->
                            val slot = size.width / days.size.coerceAtLeast(1)
                            selected = (offset.x / slot).toInt().coerceIn(0, (days.size - 1).coerceAtLeast(0))
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (days.isEmpty()) return@Canvas
                    val maxVal = maxOf(days.maxOf { it.second }, goal, 1).toFloat()
                    val slot = size.width / days.size
                    val barWidth = slot * 0.45f
                    val usable = size.height - 8.dp.toPx()
                    days.forEachIndexed { index, (_, calories) ->
                        val x = index * slot + (slot - barWidth) / 2f
                        val color = if (index == selected) barSelected else barOther
                        if (calories > 0) {
                            val h = ((calories / maxVal) * usable).coerceAtLeast(6.dp.toPx())
                            drawRoundRect(color, Offset(x, size.height - h), Size(barWidth, h), CornerRadius(6.dp.toPx(), 6.dp.toPx()))
                        } else {
                            drawRoundRect(color.copy(alpha = if (index == selected) 0.8f else 0.35f), Offset(x, size.height - 6.dp.toPx()), Size(barWidth, 6.dp.toPx()), CornerRadius(3.dp.toPx(), 3.dp.toPx()))
                        }
                    }
                    if (goal > 0) {
                        val y = size.height - (goal / maxVal) * usable
                        drawLine(goalLine, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f))
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                days.forEachIndexed { index, (date, _) ->
                    Text(
                        date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US),
                        fontSize = 11.sp,
                        fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == selected) scheme.primary else scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun macroColors(): Triple<Color, Color, Color> =
    Triple(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)

/** Carbs, protein and fat today against their daily targets: the percentage, the grams and a bar. */
@Composable
internal fun NutritionsCard(carbsG: Float, proteinG: Float, fatG: Float, goals: HealthGoalCalculator.MacroGoals) {
    val scheme = MaterialTheme.colorScheme
    val (carbsColor, proteinColor, fatColor) = macroColors()
    CardBox {
        Column {
            Text("Nutritions", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
            Spacer(Modifier.height(14.dp))
            MacroRow("Carbs", carbsG, goals.carbsG, carbsColor)
            Spacer(Modifier.height(14.dp))
            MacroRow("Protein", proteinG, goals.proteinG, proteinColor)
            Spacer(Modifier.height(14.dp))
            MacroRow("Fat", fatG, goals.fatG, fatColor)
        }
    }
}

@Composable
private fun MacroRow(label: String, actualG: Float, goalG: Int, color: Color) {
    val scheme = MaterialTheme.colorScheme
    val percent = NutritionView.macroPercentOfGoal(actualG, goalG)
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, color = scheme.onSurfaceVariant)
            Text("${actualG.toInt()} / $goalG g", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = scheme.onBackground)
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("$percent%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground, modifier = Modifier.width(64.dp))
            BoxWithConstraints(modifier = Modifier.weight(1f).height(10.dp)) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(5.dp)).background(scheme.background))
                Box(
                    Modifier
                        .width(maxWidth * (if (goalG > 0) (actualG / goalG).coerceIn(0f, 1f) else 0f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(5.dp))
                        .background(color)
                )
            }
        }
    }
}

/** Two pies side by side: the recommended split for the calorie goal and what was eaten today. */
@Composable
internal fun NutritionInfoCard(goals: HealthGoalCalculator.MacroGoals, carbsG: Float, proteinG: Float, fatG: Float) {
    val scheme = MaterialTheme.colorScheme
    val colors = macroColors()
    val recommended = NutritionView.macroSplit(goals.carbsG.toFloat(), goals.proteinG.toFloat(), goals.fatG.toFloat())
    val actual = NutritionView.macroSplit(carbsG, proteinG, fatG)
    CardBox {
        Column {
            Text("Nutrition info", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Top) {
                PieWithCaption(recommended, colors, "Recommended")
                PieWithCaption(actual, colors, "Eaten today")
            }
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                Text("Recommended", fontSize = 11.sp, color = scheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(96.dp))
                Text("Today", fontSize = 11.sp, color = scheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(56.dp))
            }
            LegendRow("Carbs", colors.first, recommended?.first, actual?.first)
            LegendRow("Protein", colors.second, recommended?.second, actual?.second)
            LegendRow("Fat", colors.third, recommended?.third, actual?.third)
            Text(
                "The recommended split is a general guide for your calorie goal, not medical advice.",
                fontSize = 11.sp,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun PieWithCaption(split: Triple<Int, Int, Int>?, colors: Triple<Color, Color, Color>, caption: String) {
    val scheme = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
            if (split == null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(color = scheme.onSurfaceVariant.copy(alpha = 0.35f), style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)))
                }
                Text("Log a meal", fontSize = 11.sp, color = scheme.onSurfaceVariant, textAlign = TextAlign.Center)
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var start = -90f
                    listOf(split.first to colors.first, split.second to colors.second, split.third to colors.third).forEach { (share, color) ->
                        val sweep = share * 3.6f
                        if (sweep > 0f) drawArc(color = color, startAngle = start, sweepAngle = sweep, useCenter = true)
                        start += sweep
                    }
                }
            }
        }
        Text(caption, fontSize = 12.sp, color = scheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun LegendRow(label: String, color: Color, recommended: Int?, actual: Int?) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(3.dp)).background(color))
        Text(label, fontSize = 13.sp, color = scheme.onSurface, modifier = Modifier.padding(start = 8.dp).weight(1f))
        Text(recommended?.let { "$it%" } ?: "\u2013", fontSize = 13.sp, color = scheme.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.width(96.dp))
        Text(actual?.let { "$it%" } ?: "\u2013", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = scheme.onBackground, textAlign = TextAlign.End, modifier = Modifier.width(56.dp))
    }
}
