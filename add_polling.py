import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

polling_block = """
  LaunchedEffect(Unit) {
    while(true) {
      kotlinx.coroutines.delay(15_000) // Poll every 15 seconds while dashboard is visible
      viewModel.syncWithHealthConnect(context)
    }
  }
"""

if "kotlinx.coroutines.delay(15_000)" not in content:
    content = content.replace(
        "viewModel.syncWithHealthConnect(context)\n      }\n    }\n    lifecycleOwner.lifecycle.addObserver(observer)",
        "viewModel.syncWithHealthConnect(context)\n      }\n    }\n    lifecycleOwner.lifecycle.addObserver(observer)\n" + polling_block
    )

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
