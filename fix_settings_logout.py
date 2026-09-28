import re

with open('app/src/main/java/com/example/presentation/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

content = content.replace("fun SettingsScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}) {", "fun SettingsScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}, onLogout: () -> Unit = {}) {")
content = content.replace("viewModel.logout {\n                    navController.navigate(\"auth\") {\n                        popUpTo(0) { inclusive = true }\n                    }\n                }", "viewModel.logout {\n                    onLogout()\n                }")

with open('app/src/main/java/com/example/presentation/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
