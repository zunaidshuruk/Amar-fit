import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

sleep_box = """
      // Row 5: Sleep
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Sleep
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(IndigoBg)
            .clickable { showSleepDialog = true }
            .padding(16.dp)
        ) {
          Column {
            Text(text = "SLEEP TRACKING", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = String.format("%.1f Hours", sleepHours), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Indigo900)
            Text(text = "(+ Tap to log manually)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Indigo700)
          }
        }
      }
"""

if "Row 5: Sleep" not in content:
    content = content.replace(
        '      Spacer(modifier = Modifier.height(8.dp))',
        sleep_box + '\n      Spacer(modifier = Modifier.height(8.dp))'
    )
    with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
        f.write(content)
