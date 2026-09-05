import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

bad_block = """    if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
        viewModel.addSteps(1)
    }
  }
  }
        .addOnFailureListener { e ->
            e.printStackTrace()
            // If it fails (e.g., emulator lacks Google Play Services connection), fail silently.
        }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
  }
}"""

good_block = """    if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
        viewModel.addSteps(1)
    }
  }

  override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
  }
}"""

if bad_block in content:
    content = content.replace(bad_block, good_block)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
