# IPTV Player

Android-TV-IPTV-Player auf Basis von OwnTV.

## Aktueller Stand

- OwnTV ist als reproduzierbar gepinnte Upstream-Basis eingebunden.
- OwnTV_Core ist ebenfalls gepinnt.
- Das Gradle-Projekt verwendet `upstream/OwnTV/app` als initiale App-Implementierung.
- Ziel der nächsten Iteration: eigene Produktidentität und eine XCIPTV-inspirierte TV-Oberfläche, ohne die bestehende Playback-, Xtream-, M3U-, Stalker- und EPG-Infrastruktur neu zu erfinden.

Die Upstream-Basis unterstützt Xtream, M3U und Stalker-Portale, EPG, Catch-up, Profile und Downloads und verwendet Compose for TV sowie ExoPlayer/mpv. Die aktuellste OwnTV-Veröffentlichung ist v4.1.7.

## Entwicklung

Das Repository verwendet Git-Submodule. Klonen mit:

```bash
git clone --recurse-submodules https://github.com/Bilalmc/iptv-player.git
cd iptv-player
```

Falls das Repository bereits ohne Submodule geklont wurde:

```bash
git submodule update --init --recursive
```

Für lokale Entwicklung gegen einen ausgecheckten OwnTV_Core-Stand kann `owntv.corePath` in der lokalen Gradle-Konfiguration gesetzt werden. Zugangsdaten gehören nicht ins Repository.

## Lizenz

OwnTV ist GPLv3. Änderungen und Weitergabe müssen die Lizenzbedingungen der übernommenen Komponenten berücksichtigen.
