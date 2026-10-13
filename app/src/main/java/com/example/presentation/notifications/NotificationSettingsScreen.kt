package com.example.presentation.notifications

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.notifications.Days
import com.example.data.notifications.NotificationPrefs
import com.example.data.notifications.ReminderPref
import com.example.data.notifications.formatMinuteOfDay
import com.example.presentation.viewmodel.ShasthoViewModel

private fun pickTime(context: Context, minuteOfDay: Int, onPicked: (Int) -> Unit) {
    TimePickerDialog(
        context,
        { _, hour, minute -> onPicked(hour * 60 + minute) },
        minuteOfDay / 60,
        minuteOfDay % 60,
        false
    ).show()
}

@Composable
fun NotificationSettingsScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit,
    onOpenHealthGoals: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs by viewModel.notificationPrefs.collectAsState()
    val profile by viewModel.userProfile.collectAsState()

    /** Always changes the latest saved preferences, so quick successive taps never overwrite each other. */
    fun update(transform: (NotificationPrefs) -> NotificationPrefs) {
        viewModel.saveNotificationPrefs(transform(viewModel.notificationPrefs.value))
    }

    var systemAllowed by remember { mutableStateOf(NotificationHelper.areNotificationsAllowed(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) systemAllowed = NotificationHelper.areNotificationsAllowed(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        systemAllowed = NotificationHelper.areNotificationsAllowed(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Notifications", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        if (!systemAllowed) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Notifications are turned off for KardIQ", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    Text(
                        "Reminders and alerts below will not show until you allow notifications.",
                        fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= 33) {
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) { Text("Allow") }
                        }
                        OutlinedButton(onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }) { Text("Open settings") }
                    }
                }
            }
        }

        SectionCard(title = "General") {
            SwitchRow("Allow KardIQ notifications", "Master switch for every reminder, alert and push", prefs.masterEnabled) { on ->
                update { it.copy(masterEnabled = on) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val quietOn = prefs.quietStartMinute >= 0 && prefs.quietEndMinute >= 0
            SwitchRow(
                "Quiet hours",
                if (quietOn) "No notifications from ${formatMinuteOfDay(prefs.quietStartMinute)} to ${formatMinuteOfDay(prefs.quietEndMinute)}"
                else "Choose a time window with no notifications",
                quietOn
            ) { on ->
                update {
                    if (on) it.copy(quietStartMinute = 22 * 60, quietEndMinute = 7 * 60)
                    else it.copy(quietStartMinute = -1, quietEndMinute = -1)
                }
            }
            if (quietOn) {
                TimeRow("From", prefs.quietStartMinute) { m -> update { it.copy(quietStartMinute = m) } }
                TimeRow("Until", prefs.quietEndMinute) { m -> update { it.copy(quietEndMinute = m) } }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Send a test notification", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Check that notifications reach this phone", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = { viewModel.sendTestNotification() }) { Text("Test") }
            }
        }

        SectionCard(title = "Meals and nutrition") {
            SwitchRow("Meal reminders", "Skipped when that meal is already logged", prefs.mealRemindersEnabled) { on ->
                update { it.copy(mealRemindersEnabled = on) }
            }
            if (prefs.mealRemindersEnabled) {
                ReminderTimeRow("Breakfast", prefs.breakfast, { r -> update { it.copy(breakfast = r) } })
                ReminderTimeRow("Lunch", prefs.lunch, { r -> update { it.copy(lunch = r) } })
                ReminderTimeRow("Dinner", prefs.dinner, { r -> update { it.copy(dinner = r) } })
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow("Calorie limit reached", "Once a day when you pass your calorie goal", prefs.calorieLimitReached) { on ->
                update { it.copy(calorieLimitReached = on) }
            }
            SwitchRow("Nutrition gaps alert", "A daily check of your recent meals for missing nutrients", prefs.nutritionGaps) { on ->
                update { it.copy(nutritionGaps = on) }
            }
        }

        SectionCard(title = "Hydration") {
            val water = prefs.waterReminders
            SwitchRow("Water reminders", "Skipped when your water is on track", water.enabled) { on ->
                update { it.copy(waterReminders = it.waterReminders.copy(enabled = on)) }
            }
            if (water.enabled) {
                TimeRow("From", water.startMinute) { m -> update { it.copy(waterReminders = it.waterReminders.copy(startMinute = m)) } }
                TimeRow("Until", water.endMinute) { m -> update { it.copy(waterReminders = it.waterReminders.copy(endMinute = m)) } }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Every", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { update { it.copy(waterReminders = it.waterReminders.copy(everyHours = (it.waterReminders.everyHours - 1).coerceAtLeast(1))) } }) {
                            Icon(Icons.Default.Remove, contentDescription = "Fewer hours")
                        }
                        Text("${water.everyHours} h", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        IconButton(onClick = { update { it.copy(waterReminders = it.waterReminders.copy(everyHours = (it.waterReminders.everyHours + 1).coerceAtMost(6))) } }) {
                            Icon(Icons.Default.Add, contentDescription = "More hours")
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow("Water goal reached", "Once a day when you reach your water goal", prefs.waterGoalReached) { on ->
                update { it.copy(waterGoalReached = on) }
            }
        }

        SectionCard(title = "Activity and workouts") {
            SwitchRow("Step goal reached", "Once a day when you reach your step goal", prefs.stepGoalReached) { on ->
                update { it.copy(stepGoalReached = on) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow("Workout reminder", "Skipped when you already worked out today", prefs.workoutReminder.enabled) { on ->
                update { it.copy(workoutReminder = it.workoutReminder.copy(enabled = on)) }
            }
            if (prefs.workoutReminder.enabled) {
                TimeRow("Time", prefs.workoutReminder.minuteOfDay) { m -> update { it.copy(workoutReminder = it.workoutReminder.copy(minuteOfDay = m)) } }
                DayChips(prefs.workoutReminder.days) { d -> update { it.copy(workoutReminder = it.workoutReminder.copy(days = d)) } }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(
                "Weekly workout goal met",
                "Your goal: ${profile?.weeklyWorkoutGoal ?: 3} workout days a week",
                prefs.weeklyWorkoutGoalMet
            ) { on -> update { it.copy(weeklyWorkoutGoalMet = on) } }
            TextButton(onClick = onOpenHealthGoals) { Text("Change the goal in Health Goals") }
        }

        SectionCard(title = "Sleep and mindfulness") {
            SwitchRow("Bedtime reminder", "A nudge to wind down", prefs.bedtimeReminder.enabled) { on ->
                update { it.copy(bedtimeReminder = it.bedtimeReminder.copy(enabled = on)) }
            }
            if (prefs.bedtimeReminder.enabled) {
                TimeRow("Time", prefs.bedtimeReminder.minuteOfDay) { m -> update { it.copy(bedtimeReminder = it.bedtimeReminder.copy(minuteOfDay = m)) } }
            }
            SwitchRow("Sleep goal reached", "When your logged sleep reaches your sleep goal", prefs.sleepGoalReached) { on ->
                update { it.copy(sleepGoalReached = on) }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow("Mindful moment", "A daily reminder to breathe and reset", prefs.mindfulMoment.enabled) { on ->
                update { it.copy(mindfulMoment = it.mindfulMoment.copy(enabled = on)) }
            }
            if (prefs.mindfulMoment.enabled) {
                TimeRow("Time", prefs.mindfulMoment.minuteOfDay) { m -> update { it.copy(mindfulMoment = it.mindfulMoment.copy(minuteOfDay = m)) } }
            }
        }

        SectionCard(title = "Health checks") {
            SwitchRow("Weekly check-in", "A summary of your week: steps, sleep, water, workouts, weight", prefs.weeklyCheckIn.enabled) { on ->
                update { it.copy(weeklyCheckIn = it.weeklyCheckIn.copy(enabled = on)) }
            }
            if (prefs.weeklyCheckIn.enabled) {
                TimeRow("Time", prefs.weeklyCheckIn.minuteOfDay) { m -> update { it.copy(weeklyCheckIn = it.weeklyCheckIn.copy(minuteOfDay = m)) } }
                DayChips(prefs.weeklyCheckIn.days) { d -> update { it.copy(weeklyCheckIn = it.weeklyCheckIn.copy(days = d)) } }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow("Blood glucose reminders", "For people who log blood sugar. Skipped when a reading is logged", prefs.glucoseRemindersEnabled) { on ->
                update { it.copy(glucoseRemindersEnabled = on) }
            }
            if (prefs.glucoseRemindersEnabled) {
                prefs.glucose.take(6).forEachIndexed { index, slot ->
                    ReminderTimeRow(
                        label = NotificationPrefs.GLUCOSE_SLOT_LABELS[index],
                        pref = slot,
                        onChange = { r ->
                            update { current ->
                                current.copy(glucose = current.glucose.mapIndexed { i, old -> if (i == index) r else old })
                            }
                        }
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow("Weekly weigh-in", "Skipped when you already logged your weight today", prefs.weighIn.enabled) { on ->
                update { it.copy(weighIn = it.weighIn.copy(enabled = on)) }
            }
            if (prefs.weighIn.enabled) {
                TimeRow("Time", prefs.weighIn.minuteOfDay) { m -> update { it.copy(weighIn = it.weighIn.copy(minuteOfDay = m)) } }
                DayChips(prefs.weighIn.days) { d -> update { it.copy(weighIn = it.weighIn.copy(days = d)) } }
            }
        }

        SectionCard(title = "Friends") {
            SwitchRow("Messages", "Direct messages from friends", prefs.socialMessages) { on -> update { it.copy(socialMessages = on) } }
            SwitchRow("Friend requests", "When someone wants to be your friend", prefs.socialFriendRequests) { on -> update { it.copy(socialFriendRequests = on) } }
            SwitchRow("Challenges", "New and finished challenges with friends", prefs.socialChallenges) { on -> update { it.copy(socialChallenges = on) } }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), content = content)
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun TimeRow(label: String, minuteOfDay: Int, onPicked: (Int) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
        TextButton(onClick = { pickTime(context, minuteOfDay, onPicked) }) {
            Text(formatMinuteOfDay(minuteOfDay), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** A labelled reminder with its own on/off switch and time. */
@Composable
private fun ReminderTimeRow(label: String, pref: ReminderPref, onChange: (ReminderPref) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        TextButton(onClick = { pickTime(context, pref.minuteOfDay) { m -> onChange(pref.copy(minuteOfDay = m)) } }) {
            Text(formatMinuteOfDay(pref.minuteOfDay), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
        Switch(
            checked = pref.enabled,
            onCheckedChange = { on -> onChange(pref.copy(enabled = on)) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayChips(mask: Int, onChange: (Int) -> Unit) {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            FilterChip(
                selected = Days.has(mask, index + 1),
                onClick = { onChange(Days.toggle(mask, index + 1)) },
                label = { Text(label, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontSize = 12.sp) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
