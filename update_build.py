import re

with open('app/build.gradle.kts', 'r') as f:
    content = f.read()

# Remove the debugConfig block from signingConfigs
content = re.sub(r'create\("debugConfig"\)\s*\{[^}]+\}', '', content)

# Remove the debug build type assignment for signingConfig
content = re.sub(r'debug\s*\{\s*signingConfig\s*=\s*signingConfigs\.getByName\("debugConfig"\)\s*\}', '', content)
# Or if it's on one line: debug { signingConfig = signingConfigs.getByName("debugConfig") }
content = re.sub(r'debug\s*\{\s*signingConfig\s*=\s*signingConfigs\.getByName\("debugConfig"\)\s*\}', '', content)

with open('app/build.gradle.kts', 'w') as f:
    f.write(content)
