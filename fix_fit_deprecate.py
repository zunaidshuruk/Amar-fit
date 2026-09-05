import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# Replace the steps option dialog button
old_button = """        TextButton(onClick = {
            showStepsOptionDialog = false
            val account = GoogleSignIn.getAccountForExtension(navController.context, fitnessOptions)
            if (!GoogleSignIn.hasPermissions(account, fitnessOptions)) {
                try {
                    GoogleSignIn.requestPermissions(
                        navController.context as android.app.Activity,
                        1001,
                        account,
                        fitnessOptions
                    )
                } catch (e: Exception) {
                    showStepsDialog = true
                }
            } else {
                // If already authed, trigger sync directly
                android.widget.Toast.makeText(navController.context, "Syncing from Google Fit...", android.widget.Toast.LENGTH_SHORT).show()
                val context = navController.context as android.app.Activity
                com.google.android.gms.fitness.Fitness.getHistoryClient(context, account)
                    .readDailyTotal(com.google.android.gms.fitness.data.DataType.TYPE_STEP_COUNT_DELTA)
                    .addOnSuccessListener { dataSet ->
                        val total = if (dataSet.isEmpty) 0 else dataSet.dataPoints[0].getValue(com.google.android.gms.fitness.data.Field.FIELD_STEPS).asInt()
                        viewModel.setSteps(total)
                    }
                showStepsOptionDialog = false
            }
        }) {
          Text("Sync Google Fit")
        }"""

new_button = """        TextButton(onClick = {
            showStepsOptionDialog = false
            android.widget.Toast.makeText(navController.context, "Syncing steps via Health Connect...", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.syncWithHealthConnect(navController.context)
        }) {
          Text("Sync Device Steps")
        }"""

if old_button in content:
    content = content.replace(old_button, new_button)

# Remove fitnessOptions parameters from BentoDashboard
content = content.replace("fun BentoDashboard(viewModel: ShasthoViewModel, navController: NavHostController, fitnessOptions: FitnessOptions, modifier: Modifier = Modifier) {", "fun BentoDashboard(viewModel: ShasthoViewModel, navController: NavHostController, modifier: Modifier = Modifier) {")

content = content.replace("BentoDashboard(viewModel, navController, fitnessOptions, modifier = Modifier)", "BentoDashboard(viewModel, navController, modifier = Modifier)")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
