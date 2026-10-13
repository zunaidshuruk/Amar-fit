package com.example.presentation.cycle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.viewmodel.ShasthoViewModel

@Composable
fun CycleSettingsScreen(
    viewModel: ShasthoViewModel,
    onNavigateBack: () -> Unit,
    onOpenNotifications: () -> Unit,
    onTurnedOff: () -> Unit
) {
    val context = LocalContext.current
    val profile by viewModel.userProfile.collectAsState()
    val p = profile

    var confirmDelete by remember { mutableStateOf(false) }
    var confirmOff by remember { mutableStateOf(false) }

    /** Saves the four settings together; values not being changed are passed through from the saved profile. */
    fun save(ai: Boolean = p?.cycleAiEnabled ?: true, discreet: Boolean = p?.cycleDiscreetNotifications ?: true,
             cycle: Int = p?.cycleDefaultLength ?: 28, period: Int = p?.cyclePeriodDefaultLength ?: 5) {
        viewModel.updateCycleSettings(ai, discreet, cycle, period)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text("Cycle settings", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        if (p != null) {
            SettingsCard("Privacy") {
                SettingSwitch(
                    "AI Assistant may use a short cycle summary",
                    "Sends only your cycle day, phase and next period date to Google's Gemini when you ask health questions. Never your daily logs, symptoms or mood.",
                    p.cycleAiEnabled
                ) { save(ai = it) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingSwitch(
                    "Hide details on the lock screen",
                    "Cycle notifications show only \"You have a reminder\" until the phone is unlocked.",
                    p.cycleDiscreetNotifications
                ) { save(discreet = it) }
            }

            SettingsCard("Predictions") {
                Text(
                    "Used until you have logged a few cycles. After that your own history is used.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StepperRow(
                    label = "Typical cycle length",
                    valueText = "${p.cycleDefaultLength} days",
                    onMinus = { save(cycle = p.cycleDefaultLength - 1) },
                    onPlus = { save(cycle = p.cycleDefaultLength + 1) },
                    canMinus = p.cycleDefaultLength > 21,
                    canPlus = p.cycleDefaultLength < 45
                )
                StepperRow(
                    label = "Typical period length",
                    valueText = "${p.cyclePeriodDefaultLength} days",
                    onMinus = { save(period = p.cyclePeriodDefaultLength - 1) },
                    onPlus = { save(period = p.cyclePeriodDefaultLength + 1) },
                    canMinus = p.cyclePeriodDefaultLength > 2,
                    canPlus = p.cyclePeriodDefaultLength < 10
                )
            }

            SettingsCard("Ramadan") {
                SettingSwitch(
                    "Track Ramadan fasting make-up",
                    "Counts the fasts you missed (for example on period days) and the ones you made up. Optional. It is stored with your cycle data and handled the same way.",
                    p.ramadanTrackingEnabled
                ) { viewModel.setRamadanTracking(it, p.ramadanOffsetDays) }
                if (p.ramadanTrackingEnabled) {
                    val offset = p.ramadanOffsetDays
                    StepperRow(
                        label = "Moon sighting adjustment",
                        valueText = if (offset == 0) "none" else "${if (offset > 0) "+" else ""}$offset day${if (offset == 1 || offset == -1) "" else "s"}",
                        onMinus = { viewModel.setRamadanTracking(true, offset - 1) },
                        onPlus = { viewModel.setRamadanTracking(true, offset + 1) },
                        canMinus = offset > -2,
                        canPlus = offset < 2
                    )
                    val (hijriYear, range) = com.example.data.cycle.Ramadan.currentOrNext(java.time.LocalDate.now(), offset)
                    val fmt = java.time.format.DateTimeFormatter.ofPattern("MMM d", java.util.Locale.US)
                    Text(
                        "Ramadan $hijriYear is estimated as ${range.start.format(fmt)} \u2013 ${range.end.format(fmt)}, ${range.end.year}. If your country starts a day later than the built-in calendar (as Bangladesh often does), add 1 day.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            SettingsCard("Reminders") {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenNotifications() }.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Cycle notifications", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text("Period soon, period late, fertile window, daily log, fasting make-up", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "Tip: to keep the cycle tracker in the bottom bar, choose it in More > Customize navigation bar.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            CycleHealthConnectSection(viewModel = viewModel, enabled = p.cycleHealthConnectSync)

            SettingsCard("Your data") {
                TextButton(onClick = { confirmOff = true }) { Text("Turn off cycle tracking") }
                TextButton(onClick = { confirmDelete = true }) {
                    Text("Delete all cycle data", color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Turning tracking off hides the tracker and stops its reminders but keeps your log. Deleting removes your periods and daily logs from this phone and from the cloud.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(80.dp))
    }

    if (confirmOff) {
        AlertDialog(
            onDismissRequest = { confirmOff = false },
            title = { Text("Turn off cycle tracking?") },
            text = { Text("The tracker and its reminders are hidden. Your log is kept, and you can turn tracking on again later.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmOff = false
                    viewModel.disableCycleTracking()
                    onTurnedOff()
                }) { Text("Turn off") }
            },
            dismissButton = { TextButton(onClick = { confirmOff = false }) { Text("Cancel") } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete all cycle data?") },
            text = { Text("Every period and daily log is removed from this phone and from your account in the cloud. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deleteAllCycleData { ok ->
                        android.widget.Toast.makeText(
                            context,
                            if (ok) "Cycle data deleted" else "Couldn't reach the cloud. Nothing was deleted; try again when you are online.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } }
        )
    }
}

@Composable
internal fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Text(
        title,
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
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) { content() }
    }
}

@Composable
internal fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
