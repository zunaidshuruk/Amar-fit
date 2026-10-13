package com.example.presentation.cycle

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import com.example.data.health.CycleHealthConnect
import com.example.presentation.viewmodel.ShasthoViewModel

/** "Share with Health Connect": reads periods other apps recorded and saves the ones logged here. Permissions are asked only here. */
@Composable
internal fun CycleHealthConnectSection(viewModel: ShasthoViewModel, enabled: Boolean) {
    val context = LocalContext.current

    fun importNow() {
        viewModel.importCycleFromHealthConnect { added ->
            val message = when {
                added < 0 -> "Couldn't read Health Connect. Check the permissions."
                added == 0 -> "No new periods found in Health Connect."
                else -> "Added $added period${if (added == 1) "" else "s"} from Health Connect."
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (granted.containsAll(CycleHealthConnect.PERMISSIONS)) {
            viewModel.setCycleHealthConnectSync(true)
            importNow()
        } else {
            viewModel.setCycleHealthConnectSync(false)
            Toast.makeText(context, "Health Connect permission was not granted.", Toast.LENGTH_LONG).show()
        }
    }

    SettingsCard("Health Connect") {
        SettingSwitch(
            title = "Share with Health Connect",
            subtitle = "Reads periods other apps recorded, and saves the periods you finish here. Needs permission.",
            checked = enabled
        ) { on ->
            if (on) permissionLauncher.launch(CycleHealthConnect.PERMISSIONS) else viewModel.setCycleHealthConnectSync(false)
        }
        if (enabled) {
            OutlinedButton(onClick = { importNow() }) { Text("Import periods now") }
        }
    }
}
