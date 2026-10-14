package com.example.presentation.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.notifications.Days
import com.example.data.notifications.MedicineReminder
import com.example.data.notifications.MedicineReminders
import com.example.data.notifications.NotificationPrefs
import com.example.data.notifications.formatMinuteOfDay

/** Settings > Notifications > Medicine reminders: up to 6, each with a name, an optional dose, a time and the days. */
@Composable
internal fun MedicineSection(
    prefs: NotificationPrefs,
    update: ((NotificationPrefs) -> NotificationPrefs) -> Unit,
    onOpenHistory: () -> Unit = {}
) {
    var editing by remember { mutableStateOf<MedicineReminder?>(null) }
    var confirmDelete by remember { mutableStateOf<MedicineReminder?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { MedicineTakenStore(context) }
    var takenToday by remember { mutableStateOf(store.takenOn()) }

    SectionCard(title = "Medicine reminders") {
        Text(
            "Up to ${MedicineReminders.MAX} reminders. They are never silenced by quiet hours, so a dose is not missed.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
        )
        if (prefs.medicines.isEmpty()) {
            Text("No medicine reminders yet.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 8.dp))
        }
        prefs.medicines.forEach { m ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth().clickable { editing = m }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(m.name, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                    val details = listOfNotNull(
                        m.dose.takeIf { it.isNotBlank() },
                        formatMinuteOfDay(m.minuteOfDay),
                        Days.label(m.days)
                    ).joinToString(" \u00B7 ")
                    Text(details, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val isTaken = m.id in takenToday
                    val index = prefs.medicines.indexOfFirst { it.id == m.id }
                    TextButton(
                        onClick = {
                            if (isTaken) {
                                store.undo(m.id)
                            } else {
                                store.markTaken(m.id)
                                // a dose marked taken also stops a pending snoozed repeat and hides the notification
                                if (index >= 0) {
                                    val notificationId = com.example.data.notifications.ReminderCatalog.medicineNotificationId(index)
                                    MedicineNotifier.cancelSnooze(context, notificationId)
                                    androidx.core.app.NotificationManagerCompat.from(context).cancel(notificationId)
                                }
                            }
                            takenToday = store.takenOn()
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                    ) {
                        Text(if (isTaken) "Taken today \u2713 (undo)" else "Mark as taken today", fontSize = 12.sp)
                    }
                }
                Switch(
                    checked = m.enabled,
                    onCheckedChange = { on ->
                        update { p -> p.copy(medicines = p.medicines.map { x -> if (x.id == m.id) x.copy(enabled = on) else x }) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                IconButton(onClick = { confirmDelete = m }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete ${m.name}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val canAdd = MedicineReminders.canAdd(prefs.medicines)
        OutlinedButton(
            onClick = { editing = MedicineReminder(id = java.util.UUID.randomUUID().toString().take(8), createdOn = java.time.LocalDate.now().toString()) },
            enabled = canAdd,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(if (canAdd) "Add medicine reminder (${prefs.medicines.size} of ${MedicineReminders.MAX})" else "All ${MedicineReminders.MAX} reminders added")
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SwitchRow(
            "Missed-dose alert",
            "Asks \"Missed dose?\" when a dose is not marked taken or skipped, with Taken and Skip buttons",
            prefs.medicineMissedAlert
        ) { on -> update { it.copy(medicineMissedAlert = on) } }
        if (prefs.medicineMissedAlert) {
            val minutes = MedicineReminders.missedAfter(prefs)
            com.example.presentation.cycle.StepperRow(
                label = "Alert after",
                valueText = "$minutes min",
                onMinus = { update { it.copy(medicineMissedAfterMinutes = (MedicineReminders.missedAfter(it) - MedicineReminders.MISSED_AFTER_STEP).coerceAtLeast(MedicineReminders.MISSED_AFTER_MIN)) } },
                onPlus = { update { it.copy(medicineMissedAfterMinutes = (MedicineReminders.missedAfter(it) + MedicineReminders.MISSED_AFTER_STEP).coerceAtMost(MedicineReminders.MISSED_AFTER_MAX)) } },
                canMinus = minutes > MedicineReminders.MISSED_AFTER_MIN,
                canPlus = minutes < MedicineReminders.MISSED_AFTER_MAX
            )
        }
        OutlinedButton(onClick = onOpenHistory, modifier = Modifier.padding(top = 4.dp)) { Text("View history") }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 8.dp))
        SwitchRow(
            "Hide the medicine name on the lock screen",
            "Shows only \"You have a reminder\" until the phone is unlocked",
            prefs.medicineDiscreet
        ) { on -> update { it.copy(medicineDiscreet = on) } }
    }

    editing?.let { current ->
        MedicineDialog(
            initial = current,
            isNew = prefs.medicines.none { it.id == current.id },
            onDismiss = { editing = null },
            onSave = { saved ->
                update { p -> p.copy(medicines = MedicineReminders.upsert(p.medicines, saved)) }
                editing = null
            }
        )
    }
    confirmDelete?.let { m ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Delete this reminder?") },
            text = { Text("The reminder for ${m.name} will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    update { p -> p.copy(medicines = MedicineReminders.remove(p.medicines, m.id)) }
                    confirmDelete = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Keep") } }
        )
    }
}

@Composable
private fun MedicineDialog(
    initial: MedicineReminder,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (MedicineReminder) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by remember { mutableStateOf(initial.name) }
    var dose by remember { mutableStateOf(initial.dose) }
    var minute by remember { mutableIntStateOf(initial.minuteOfDay) }
    var days by remember { mutableIntStateOf(initial.days) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add medicine reminder" else "Edit medicine reminder") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(MedicineReminders.NAME_MAX) },
                    label = { Text("Medicine name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it.take(MedicineReminders.DOSE_MAX) },
                    label = { Text("Dose (optional), for example 500 mg") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    TextButton(onClick = { pickTime(context, minute) { minute = it } }) {
                        Text(formatMinuteOfDay(minute), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("Days", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                DayChips(days) { days = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(initial.copy(name = name, dose = dose, minuteOfDay = minute, days = days)) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
