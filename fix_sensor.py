import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

bad_sensor_block = """  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
        // Fallback: If Fit API is not authenticated, we simulate pedometer ticks.
        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)
        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
            viewModel.addSteps(1)
        }
    }
  }"""

good_sensor_block = """  override fun onSensorChanged(event: SensorEvent?) {
    if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
        viewModel.addSteps(1)
    }
  }"""

if bad_sensor_block in content:
    content = content.replace(bad_sensor_block, good_sensor_block)

# Remove imports
imports = [
    "import com.google.android.gms.auth.api.signin.GoogleSignIn\n",
    "import com.google.android.gms.auth.api.signin.GoogleSignInOptions\n",
    "import com.google.android.gms.fitness.Fitness\n",
    "import com.google.android.gms.fitness.FitnessOptions\n",
    "import com.google.android.gms.fitness.data.DataType\n",
    "import com.google.android.gms.fitness.data.Field\n"
]

for imp in imports:
    content = content.replace(imp, "")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
