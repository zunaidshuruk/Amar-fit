import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

# Replace signingConfigs
replacement_signing = """signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }"""
content = re.sub(r'signingConfigs\s*\{.*create\("release"\).*?\}\s*\}', replacement_signing, content, flags=re.DOTALL)

# Replace buildTypes
replacement_build = """buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }"""
content = re.sub(r'buildTypes\s*\{\s*release\s*\{.*?\}\s*\}', replacement_build, content, flags=re.DOTALL)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
