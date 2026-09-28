import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    manifest = f.read()

permissions = """
    <uses-permission android:name="android.permission.health.READ_STEPS"/>
    <uses-permission android:name="android.permission.health.READ_SLEEP"/>
    <uses-permission android:name="android.permission.health.READ_BLOOD_PRESSURE"/>
    <uses-permission android:name="android.permission.health.READ_BLOOD_GLUCOSE"/>
"""

activity = """
        <activity android:name=".PrivacyPolicyActivity" android:exported="true">
            <intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
            </intent-filter>
        </activity>
"""

manifest = manifest.replace('<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION"/>', '<uses-permission android:name="android.permission.ACTIVITY_RECOGNITION"/>' + permissions)

manifest = manifest.replace('</application>', activity + '    </application>')

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(manifest)
