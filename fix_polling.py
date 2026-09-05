import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

bad_block = """
  LaunchedEffect(Unit) {
    while(true) {
      kotlinx.coroutines.delay(15_000) // Poll every 15 seconds while dashboard is visible
      viewModel.syncWithHealthConnect(context)
    }
  }
"""

if bad_block in content:
    content = content.replace(bad_block, "")
    # Add it correctly right after DisposableEffect
    correct_block = """
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }
  
  LaunchedEffect(Unit) {
    while(true) {
      kotlinx.coroutines.delay(15_000) // Poll every 15 seconds while dashboard is visible
      viewModel.syncWithHealthConnect(context)
    }
  }
"""
    content = content.replace("    onDispose {\n      lifecycleOwner.lifecycle.removeObserver(observer)\n    }\n  }", correct_block)
    
with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
