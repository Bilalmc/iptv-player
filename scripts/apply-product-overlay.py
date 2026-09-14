from pathlib import Path
import sys

path = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("upstream/OwnTV/app/src/main/AndroidManifest.xml")

# Launcher replacement is owned by product-ui's library manifest. Keep the pinned
# upstream manifest byte-for-byte intact here so the Android manifest merger can
# apply the product manifest cleanly without mutating XML structure in CI.
if not path.exists():
    raise SystemExit(f"OwnTV manifest not found: {path}")

print(f"OwnTV manifest overlay: no direct XML mutation required for {path}")
