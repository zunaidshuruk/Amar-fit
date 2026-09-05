import re

with open('app/src/main/java/com/example/presentation/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

# I need to add navController to SettingsScreen if it doesn't exist to navigate back to auth
old_fun = "fun SettingsScreen(viewModel: ShasthoViewModel, navController: androidx.navigation.NavController, modifier: Modifier = Modifier) {"
if old_fun not in content:
    # try without NavController
    if "fun SettingsScreen(viewModel: ShasthoViewModel, modifier: Modifier = Modifier) {" in content:
        content = content.replace("fun SettingsScreen(viewModel: ShasthoViewModel, modifier: Modifier = Modifier) {", "fun SettingsScreen(viewModel: ShasthoViewModel, navController: androidx.navigation.NavController, modifier: Modifier = Modifier) {")

logout_button = """                }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                viewModel.logout {
                    navController.navigate("auth") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color(0xFFE53935)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Log Out", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(100.dp))"""

if "Log Out" not in content:
    content = content.replace("                }\n        Spacer(modifier = Modifier.height(100.dp))", logout_button)

with open('app/src/main/java/com/example/presentation/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
