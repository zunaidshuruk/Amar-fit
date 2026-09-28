import re

with open('app/src/main/java/com/example/presentation/settings/SettingsScreen.kt', 'r') as f:
    content = f.read()

# Make sure rememberCoroutineScope is imported
if "import kotlinx.coroutines.launch" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport kotlinx.coroutines.launch")

# Insert coroutineScope
if "val coroutineScope = rememberCoroutineScope()" not in content:
    content = content.replace("fun SettingsScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}, onLogout: () -> Unit = {}) {", "fun SettingsScreen(viewModel: ShasthoViewModel, onNavigateBack: () -> Unit = {}, onLogout: () -> Unit = {}) {\n    val coroutineScope = rememberCoroutineScope()")

# Wrap saveProfile
content = content.replace("viewModel.saveProfile(updated)\n                            showSavedMessage = true", "coroutineScope.launch {\n                                viewModel.saveProfile(updated)\n                                showSavedMessage = true\n                            }")

with open('app/src/main/java/com/example/presentation/settings/SettingsScreen.kt', 'w') as f:
    f.write(content)
