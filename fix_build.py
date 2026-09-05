import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

# I will find the whole signingConfigs block and rewrite it cleanly
signing_configs_pattern = r'signingConfigs\s*\{[^}]*create\("release"\)\s*\{[^\}]*\}[^\}]*\}'
replacement = """signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }"""
content = re.sub(r'signingConfigs\s*\{.*?\}\s*\}', replacement, content, flags=re.DOTALL)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
