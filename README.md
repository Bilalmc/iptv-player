# IPTV Player

XCIPTV-inspired Android TV IPTV player built on the pinned OwnTV open-source stack.

## Product status

The product shell is now separated from the upstream implementation. `product-ui` owns the branded TV navigation and home experience, while the pinned OwnTV app remains the playback/data engine.

Current product surface:
- Android TV / Google TV, D-pad-first UI
- Home, Live TV, Movies, Series, Favorites and Settings sections
- Live channel previews sourced from OwnTV Core
- Live channel playback through OwnTV launcher deep links
- Existing OwnTV support for M3U, Xtream, Stalker/MAC, EPG/XMLTV, catch-up, VOD, subtitles and DRM
- Shared Room catalog and active-profile state through OwnTV Core

## Upstream pins

- OwnTV: `f17cffb7bc7118675e09c64e02a427d84902f6b3` (v4.1.7)
- OwnTV_Core: `45c1e82c87a2bd2cda27d4d7e78b1cdfc1de2971`

## Build

The repository includes the upstream repositories as git submodules. Clone with recursive submodules enabled.

The product UI consumes the published OwnTV Core artifact. A local Core checkout can be used with `owntv.corePath` in `gradle.properties`.

Build the product UI:

```bash
gradle :product-ui:assembleDebug
```

Build the standard TV APK:

```bash
gradle :app:assembleStandardDebug
```

Build the x86_64 emulator APK:

```bash
gradle :app:assembleX86_64Debug
```

## Licensing

OwnTV is GPLv3. This product remains GPLv3-compatible and is intended for users' own legally accessible IPTV sources.
