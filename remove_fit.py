import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

content = re.sub(r'  private val fitnessOptions = FitnessOptions\.builder\(\).*?\.build\(\)\n', '', content, flags=re.DOTALL)

content = re.sub(r'    // Automatically attempt to access Google Fit if permissions exist.*?    \}\n', '', content, flags=re.DOTALL)

content = re.sub(r'  override fun onActivityResult\(requestCode: Int, resultCode: Int, data: android\.content\.Intent\?\) \{.*?\}\n', '', content, flags=re.DOTALL)

content = re.sub(r'  private fun readDailyStepsFromFit\(account: com\.google\.android\.gms\.auth\.api\.signin\.GoogleSignInAccount\) \{.*?\}\n', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
