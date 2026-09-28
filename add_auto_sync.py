import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Make sure DisposableEffect is imported or we can just use the fully qualified name. But wait, `DisposableEffect` requires an import.
# Let's add it if it's not there.
if "import androidx.compose.runtime.DisposableEffect" not in content:
    content = content.replace("import androidx.compose.runtime.LaunchedEffect", "import androidx.compose.runtime.LaunchedEffect\nimport androidx.compose.runtime.DisposableEffect")

auto_sync_block = """
  val context = androidx.compose.ui.platform.LocalContext.current
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
      if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.syncWithHealthConnect(context)
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }
"""

if "val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current" not in content:
    content = content.replace(
        "var showWaterDialog by remember { mutableStateOf(false) }",
        "var showWaterDialog by remember { mutableStateOf(false) }\n" + auto_sync_block
    )

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
