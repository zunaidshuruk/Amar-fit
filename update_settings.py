import re

with open('app/src/main/java/com/example/presentation/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

imports = """
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.PermissionController
import kotlinx.coroutines.launch
import android.widget.Toast
"""

content = content.replace('import com.example.ui.theme.*', 'import com.example.ui.theme.*\n' + imports)

health_logic = """
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isHealthConnectAvailable by remember { mutableStateOf(HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) }
    
    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(BloodPressureRecord::class),
        HealthPermission.getReadPermission(BloodGlucoseRecord::class)
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        if (granted.containsAll(PERMISSIONS)) {
            Toast.makeText(context, "Health Connect connected!", Toast.LENGTH_SHORT).show()
            // Here you would trigger viewmodel to sync
        } else {
            Toast.makeText(context, "Permissions denied", Toast.LENGTH_SHORT).show()
        }
    }
"""

content = content.replace('var profilePictureUri by remember { mutableStateOf<String?>(null) }', 'var profilePictureUri by remember { mutableStateOf<String?>(null) }\n' + health_logic)

button_ui = """
                Divider(color = Slate100)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Health Connect", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Sync steps, sleep & vitals", fontSize = 12.sp, color = Slate500)
                    }
                    Button(
                        onClick = {
                            if (isHealthConnectAvailable) {
                                requestPermissionLauncher.launch(PERMISSIONS)
                            } else {
                                Toast.makeText(context, "Health Connect is not available on this device", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        Text("Connect")
                    }
                }
"""

content = content.replace('Spacer(modifier = Modifier.height(100.dp))', button_ui + '\n        Spacer(modifier = Modifier.height(100.dp))')

with open('app/src/main/java/com/example/presentation/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
