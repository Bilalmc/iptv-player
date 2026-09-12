from pathlib import Path
import re
import sys

path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("upstream/OwnTV/app/src/main/AndroidManifest.xml")
s = path.read_text()
s = s.replace('android:label="@string/app_name"', 'android:label="IPTV Player"', 1)
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
if not pattern.search(s):
    raise SystemExit("Pinned OwnTV MainActivity launcher block not found")
path.write_text(pattern.sub(replacement, s, count=1))
print(f"Applied IPTV Player launcher overlay to {path}")
