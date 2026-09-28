import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

# I need to add Sleep to the dashboard. Let's find "val glucose = " and add sleep
content = content.replace(
    'val glucose = maxOf(metrics?.bloodGlucoseMorning ?: 0f, metrics?.bloodGlucoseNight ?: 0f)',
    'val glucose = maxOf(metrics?.bloodGlucoseMorning ?: 0f, metrics?.bloodGlucoseNight ?: 0f)\n  val sleepHours = metrics?.sleepHours ?: 0f\n  val bloodPressure = metrics?.bloodPressure ?: ""'
)

row4 = """
      // Row 4: Sleep & Blood Pressure
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
            .padding(16.dp)
        ) {
          Column {
            Text(text = "SLEEP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Indigo700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = String.format("%.1f", sleepHours), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Indigo900)
            Text(text = "Hours", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Indigo600)
          }
        }
        // Blood Pressure
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Red50)
            .border(1.dp, Red100, RoundedCornerShape(24.dp))
            .clickable { showBpDialog = true }
            .padding(16.dp)
        ) {
          Column {
            Text(text = "BLOOD PRESSURE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Red700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = if(bloodPressure.isNotBlank()) bloodPressure else "--/--", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Red900)
            Text(text = "mmHg (+ Tap to log)", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Red700)
          }
        }
      }
"""

# Insert row4 after Row 3 (BMI block)
bmi_block_end = """        // BMI
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(Emerald50)
            .border(1.dp, Emerald100, RoundedCornerShape(24.dp))
            .clickable { navController.navigate("weightlog") }
            .padding(16.dp)
        ) {
          val weight = profile?.weightKg ?: 70f
          val heightM = (profile?.heightCm ?: 170f) / 100f
          val bmi = if (heightM > 0) weight / (heightM * heightM) else 0f
          
          Column {
            Text(text = "BMI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald700)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = String.format("%.1f", bmi), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Emerald900)
            Spacer(modifier = Modifier.height(6.dp))
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Emerald200)
            ) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(if(bmi > 0) (bmi / 40f).coerceIn(0f, 1f) else 0f)
                  .fillMaxHeight()
                  .background(if (bmi in 18.5f..24.9f) Emerald500 else if (bmi < 18.5) Blue500 else Orange500)
              )
            }
          }
        }
      }"""

content = content.replace(bmi_block_end, bmi_block_end + '\n' + row4)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
