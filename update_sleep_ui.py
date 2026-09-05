import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# 1. Add state variable
if "var showSleepDialog by remember { mutableStateOf(false) }" not in content:
    content = content.replace(
        'var showBpDialog by remember { mutableStateOf(false) }',
        'var showBpDialog by remember { mutableStateOf(false) }\n  var showSleepDialog by remember { mutableStateOf(false) }'
    )

# 2. Make sleep box clickable
sleep_box_old = """        // Sleep
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(IndigoBg)
            .padding(16.dp)
        )"""

sleep_box_new = """        // Sleep
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(IndigoBg)
            .clickable { showSleepDialog = true }
            .padding(16.dp)
        )"""
content = content.replace(sleep_box_old, sleep_box_new)

# Add "(+ Tap to log)" to sleep
sleep_text_old = 'Text(text = "Hours", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Indigo600)'
sleep_text_new = 'Text(text = "Hours (+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Indigo600)'
content = content.replace(sleep_text_old, sleep_text_new)


# 3. Add Dialog code
dialog_code = """
  if (showSleepDialog) {
    var sleepInput by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showSleepDialog = false },
      title = { Text("Log Sleep") },
      text = {
        OutlinedTextField(
          value = sleepInput,
          onValueChange = { sleepInput = it },
          label = { Text("Sleep (Hours)") },
          keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
        )
      },
      confirmButton = {
        TextButton(onClick = {
          val s = sleepInput.toFloatOrNull()
          if (s != null) {
            viewModel.setSleep(s)
          }
          showSleepDialog = false
        }) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showSleepDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }
"""
if "if (showSleepDialog)" not in content:
    content = content.replace('  if (showBpDialog) {', dialog_code + '\n  if (showBpDialog) {')

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
