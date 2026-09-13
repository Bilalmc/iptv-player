from pathlib import Path
import re
import sys

path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("upstream/OwnTV/app/src/main/AndroidManifest.xml")
s = path.read_text()
s = s.replace('android:label="@string/app_name"', 'android:label="IPTV Player"', 1)

# Replace only the pinned OwnTV MainActivity declaration, explicitly bounded to the
# <application> element so the injected activities can never escape <application>.
application_start = s.find("<application")
application_end = s.find("</application>", application_start)
if application_start < 0 or application_end < 0:
    raise SystemExit("OwnTV application element not found")

prefix = s[:application_start]
application = s[application_start:application_end]
suffix = s[application_end:]

pattern = re.compile(r'\s*<activity\s+android:name="\.MainActivity".*?</activity>', re.DOTALL)
replacement = '''
        <activity
            android:name=".MainActivity"
            android:banner="@drawable/tv_banner"
            android:exported="false"
            android:launchMode="singleTop"
            android:windowSoftInputMode="adjustPan"
            android:theme="@style/Theme.OwnTV.Starting" />

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
        </activity>'''

updated, count = pattern.subn(replacement, application, count=1)
if count != 1:
    raise SystemExit("Pinned OwnTV MainActivity launcher block not found inside application")

path.write_text(prefix + updated + suffix)
print(f"Applied IPTV Player launcher overlay to {path}")
