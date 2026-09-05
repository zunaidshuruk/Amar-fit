import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

sleep_block = """      // Row 5: Sleep
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
      }"""

new_sleep_hr_block = """      // Row 5: Sleep & Heart Rate
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
        
        // Heart Rate
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Red50)
            .border(1.dp, Red100, RoundedCornerShape(24.dp))
            .padding(16.dp)
        ) {
          Column {
            Text(text = "HEART RATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if(heartRate > 0) "$heartRate bpm" else "--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Red900)
            Text(text = "(Synced automatically)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Red700)
          }
        }
      }"""

content = content.replace(sleep_block, new_sleep_hr_block)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
