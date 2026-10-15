package com.example.presentation.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.notifications.DoseStatus
import com.example.data.notifications.MedicineHistoryBuilder
import com.example.data.notifications.MedicineReminders
import com.example.data.notifications.MedicineToday
import com.example.data.notifications.ReminderCatalog
import com.example.data.notifications.formatMinuteOfDay
import com.example.presentation.notifications.MedicineNotifier
import com.example.presentation.notifications.MedicineTakenStore
import com.example.presentation.viewmodel.ShasthoViewModel
import java.time.LocalDate
import java.time.LocalTime

/**
 * "Today's medicine": today's doses with a Taken button, so a dose can be ticked off without waiting for a notification. Shown only when
 * there is at least one active medicine reminder (and only for today). Same records as the notifications and the medicine history.
 */
@Composable
fun MedicineTodayCard(viewModel: ShasthoViewModel, onOpenHistory: () -> Unit) {
    val context = LocalContext.current
    val prefs by viewModel.notificationPrefs.collectAsState()
    val store = remember { MedicineTakenStore(context) }
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val today = LocalDate.now()
    val summary = remember(prefs.medicines, prefs.medicineMissedAfterMinutes, refresh) {
        val now = LocalTime.now()
        MedicineToday.of(
            MedicineHistoryBuilder.build(
                prefs.medicines, store.takenEntries(), store.skippedEntries(), today, now.hour * 60 + now.minute,
                MedicineReminders.missedAfter(prefs)
            ),
            today
        )
    }
    if (summary.total == 0) return

    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalPharmacy, contentDescription = null, tint = scheme.primary, modifier = Modifier.padding(end = 8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Today's medicine", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                    Text(MedicineToday.headline(summary), fontSize = 13.sp, color = scheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenHistory) { Text("History", fontSize = 13.sp) }
            }
            Column(modifier = Modifier.padding(top = 4.dp)) {
                summary.doses.forEachIndexed { index, dose ->
                    if (index > 0) HorizontalDivider(color = scheme.outlineVariant)
                    val slot = prefs.medicines.indexOfFirst { it.id == dose.reminderId }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(dose.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = scheme.onSurface)
                            val details = listOfNotNull(dose.dose.takeIf { it.isNotBlank() }, formatMinuteOfDay(dose.minuteOfDay)).joinToString(" \u00B7 ")
                            val stateColor: Color = when (dose.status) {
                                DoseStatus.TAKEN -> scheme.primary
                                DoseStatus.MISSED -> scheme.error
                                DoseStatus.DUE -> scheme.tertiary
                                else -> scheme.onSurfaceVariant
                            }
                            Text(details, fontSize = 12.sp, color = scheme.onSurfaceVariant)
                            Text(
                                if (dose.status == DoseStatus.TAKEN && dose.takenMinute != null) "Taken ${formatMinuteOfDay(dose.takenMinute!!)}" else MedicineToday.statusLabel(dose.status),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = stateColor
                            )
                        }
                        if (dose.status == DoseStatus.TAKEN || dose.status == DoseStatus.SKIPPED) {
                            TextButton(onClick = { store.undo(dose.reminderId); refresh++ }) { Text("Undo", fontSize = 13.sp) }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    MedicineNotifier.markTaken(
                                        context, store, dose.reminderId,
                                        if (slot >= 0) ReminderCatalog.medicineNotificationId(slot) else -1
                                    )
                                    refresh++
                                },
                                modifier = Modifier.height(36.dp)
                            ) { Text("Taken", fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
    }
}
