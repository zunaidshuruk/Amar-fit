import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

sleep_block_regex = r"      // Row 5: Sleep\s+Row\(\s+modifier = Modifier\.fillMaxWidth\(\),\s+horizontalArrangement = Arrangement\.spacedBy\(12\.dp\)\s+\)\ {\s+// Sleep\s+Box\(\s+modifier = Modifier\s+\.weight\(1f\)\s+\.clip\(RoundedCornerShape\(24\.dp\)\)\s+\.background\(IndigoBg\)\s+\.clickable\ \{\ showSleepDialog = true\ \}\s+\.padding\(16\.dp\)\s+\)\ \{\s+Column\ \{\s+Text\(text = \"SLEEP TRACKING\", fontSize = 12\.sp, fontWeight = FontWeight\.Bold, color = Indigo700\)\s+Spacer\(modifier = Modifier\.height\(16\.dp\)\)\s+Text\(text = String\.format\(\"%\.1f Hours\", sleepHours\), fontSize = 20\.sp, fontWeight = FontWeight\.Bold, color = Indigo900\)\s+Text\(text = \"\(\+ Tap to log manually\)\", fontSize = 10\.sp, fontWeight = FontWeight\.Medium, color = Indigo700\)\s+\}\s+\}\s+\}\s+"

matches = list(re.finditer(sleep_block_regex, content))

if len(matches) > 1:
    # Remove the second one
    match_to_remove = matches[1]
    content = content[:match_to_remove.start()] + content[match_to_remove.end():]

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
