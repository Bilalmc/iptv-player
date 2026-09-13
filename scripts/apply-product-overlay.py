from pathlib import Path
import sys

path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("upstream/OwnTV/app/src/main/AndroidManifest.xml")
s = path.read_text()

# Keep the pinned OwnTV manifest structurally intact. The product shell is inserted
# immediately before the application's closing tag.
s = s.replace('android:label="@string/app_name"', 'android:label="IPTV Player"', 1)
s = s.replace(
    'android:name=".MainActivity"\n            android:banner="@drawable/tv_banner"\n            android:exported="true"',
    'android:name=".MainActivity"\n            android:banner="@drawable/tv_banner"\n            android:exported="false"',
    1,
)

application_end = s.find("</application>")
if application_end < 0:
    raise SystemExit("OwnTV application closing tag not found")

shell_activity = '''
        <activity
            android:name="com.bilalmc.iptvplayer.ui.IptvShellActivity"
            android:exported="true"
            android:launchMode="singleTop"
            android:windowSoftInputMode="adjustPan"
            android:theme="@style/Theme.IPTVPlayer.Starting">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LEANBACK_LAUNCHER" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:host="play" android:scheme="owntv" />
                <data android:host="open" android:scheme="owntv" />
            </intent-filter>
        </activity>
'''

if 'android:name="com.bilalmc.iptvplayer.ui.IptvShellActivity"' not in s:
    s = s[:application_end] + shell_activity + s[application_end:]

path.write_text(s)
print(f"Applied IPTV Player launcher overlay to {path}")
