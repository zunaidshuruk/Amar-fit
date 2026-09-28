import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    manifest = f.read()

# Add meta-data to PrivacyPolicyActivity
old_activity_end = """        <activity android:name=".PrivacyPolicyActivity" android:exported="true">
            <intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
                <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
            </intent-filter>
        </activity>"""

new_activity = """        <activity android:name=".PrivacyPolicyActivity" android:exported="true">
            <intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
                <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
            </intent-filter>
            <meta-data
                android:name="health_permissions"
                android:resource="@array/health_permissions" />
        </activity>"""

manifest = manifest.replace(old_activity_end, new_activity)

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(manifest)
