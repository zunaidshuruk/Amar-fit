import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

bad_block = """            } else {
                showStepsDialog = true // fallback if already authed but can't sync
            }
        }) {
          Text("Sync Google Fit")"""

good_block = """            } else {
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
          Text("Sync Google Fit")"""

if bad_block in content:
    content = content.replace(bad_block, good_block)
else:
    print("Failed to find bad block")

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
