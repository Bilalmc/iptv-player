# IPTV Player

Android-TV-IPTV-Player mit eigener Produktidentität auf Basis der Open-Source-OwnTV-Engine.

## Produktziel

Eine moderne, fernbedienungsoptimierte IPTV-App mit einer **XCIPTV-inspirierten Informationsarchitektur**: dunkle Oberfläche, große Content-Karten, Live-TV als primärer Einstieg, EPG/Guide, VOD, Serien, Favoriten, Suche und Einstellungen.

Das Projekt übernimmt die vorhandene Streaming-Infrastruktur statt sie neu zu implementieren. OwnTV bringt Xtream, M3U und Stalker-Portale, EPG, Catch-up, Profile, Downloads sowie Compose for TV und ExoPlayer/mpv mit.

## Aktueller Stand

- Produktname: **IPTV Player**
- Application ID: `com.bilalmc.iptvplayer`
- Produktversion: `0.1.0`
- OwnTV: v4.1.7, reproduzierbar gepinnt
- OwnTV_Core: reproduzierbar gepinnt
- Android TV / D-pad-first
- Standard- und x86_64-Builds in CI
- Unit-Tests in CI

Die aktuelle Android-Oberfläche stammt noch aus der eingebundenen OwnTV-App. Der nächste große Umbau ist deshalb bewusst als eigener UI-Layer geplant: Navigation, Home-Hero, Live-Karten, EPG, VOD/Serien-Raster, Favoriten und Player-HUD werden schrittweise unter der Produktidentität von IPTV Player neu gestaltet.

## Entwicklung

```bash
git clone --recurse-submodules https://github.com/Bilalmc/iptv-player.git
cd iptv-player
git submodule update --init --recursive
```

Für eine lokale Entwicklung gegen einen ausgecheckten OwnTV_Core-Stand kann `owntv.corePath` in der lokalen Gradle-Konfiguration gesetzt werden. Zugangsdaten gehören nicht ins Repository.

## Rechtliches

OwnTV und die übernommenen Komponenten stehen unter GPLv3. Änderungen und Weitergabe müssen die jeweiligen Lizenz- und Urheberrechtshinweise erhalten. Die App enthält keine IPTV-Inhalte, Senderabonnements oder Zugangsdaten; Nutzer müssen eigene, rechtmäßig zugängliche Quellen verwenden.
