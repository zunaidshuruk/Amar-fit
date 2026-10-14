package com.example.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.notifications.DoseStatus
import com.example.data.notifications.MedicineHistoryBuilder
import com.example.data.notifications.MedicineReminders
import com.example.data.notifications.formatMinuteOfDay
import com.example.presentation.common.appSafeInsets
import com.example.presentation.viewmodel.ShasthoViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

/** The last 14 days of medicine doses: taken (with the time), skipped, missed, and how many were taken over the last week. */
@Composable
fun MedicineHistoryScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val prefs by viewModel.notificationPrefs.collectAsState()
    val store = remember { MedicineTakenStore(context) }
    var refresh by remember { mutableIntStateOf(0) }
    // the records live on the phone: read them again whenever the screen comes back (for example after tapping Taken in a notification)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val today = LocalDate.now()
    val history = remember(prefs.medicines, prefs.medicineMissedAfterMinutes, refresh) {
        val now = LocalTime.now()
        MedicineHistoryBuilder.build(
            prefs.medicines, store.takenEntries(), store.skippedEntries(), today, now.hour * 60 + now.minute,
            MedicineReminders.missedAfter(prefs)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(appSafeInsets())
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Medicine history", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        if (history.days.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Nothing to show yet. Add a medicine reminder in Settings > Notifications, and mark each dose Taken or Skip when it rings. Your history appears here.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Last 7 days", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val week = history.last7
                    Text(
                        if (week.counted == 0) "No doses yet" else "${week.taken} of ${week.counted} doses taken",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    if (week.counted > 0) {
                        Text("${week.percent}%", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    val fortnight = history.last14
                    if (fortnight.counted > 0) {
                        Text(
                            "Last 14 days: ${fortnight.taken} of ${fortnight.counted} (${fortnight.percent}%)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            history.days.forEach { day ->
                Text(
                    text = when (day.date) {
                        today -> "Today"
                        today.minusDays(1) -> "Yesterday"
                        else -> day.date.format(dayFormat)
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        day.doses.forEachIndexed { index, dose ->
                            if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(dose.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                    val details = listOfNotNull(dose.dose.takeIf { it.isNotBlank() }, formatMinuteOfDay(dose.minuteOfDay)).joinToString(" \u00B7 ")
                                    Text(details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                val (label, color) = statusLook(dose.status, dose.takenMinute)
                                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color, textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }
            Text(
                "Based on your current reminders, from the day each was created. Records stay on this phone for 14 days.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        // keeps the last items clear of the assistant button
        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
private fun statusLook(status: DoseStatus, takenMinute: Int?): Pair<String, Color> = when (status) {
    DoseStatus.TAKEN -> (if (takenMinute != null) "Taken ${formatMinuteOfDay(takenMinute)}" else "Taken") to MaterialTheme.colorScheme.primary
    DoseStatus.SKIPPED -> "Skipped" to MaterialTheme.colorScheme.onSurfaceVariant
    DoseStatus.MISSED -> "Missed" to MaterialTheme.colorScheme.error
    DoseStatus.DUE -> "Due now" to MaterialTheme.colorScheme.tertiary
    DoseStatus.UPCOMING -> "Later today" to MaterialTheme.colorScheme.onSurfaceVariant
}
