import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    manifest = f.read()

# Replace the existing intent filter for PrivacyPolicyActivity to include the Android 14 version
old_intent = """<intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
            </intent-filter>"""

new_intent = """<intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
                <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
            </intent-filter>"""

manifest = manifest.replace(old_intent, new_intent)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(manifest)
