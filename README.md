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
- Eigener Produkt-Launcher und eigene Compose-for-TV-Shell
- Home-Shell liest Profil, Playlist-Zuordnung, Live-/Movie-/Series-Anzahlen und Live-Favoriten direkt aus OwnTV_Core
- Standard- und x86_64-Builds in CI
- Unit-Tests in CI

Die UI wird jetzt schrittweise von einer reinen Shell zu einer echten produkt-eigenen Oberfläche migriert. Der erste Datenpfad ist bereits produktseitig umgesetzt: `product-ui` verwendet die veröffentlichte `tv.own.owntv:core`-API, Koin und die vorhandenen Room-DAOs. Dadurch bleiben Profile, Quellen, Favoriten und Katalogzahlen mit dem bestehenden OwnTV-Datenbestand synchron.

Als nächste UI-Stufe folgen echte Live-/EPG-Raster, VOD-/Serien-Raster, Suche und der produkt-eigene Player-HUD. Der vorhandene OwnTV-Player bleibt dabei zunächst die Playback-Engine.

## Entwicklung

```bash
git clone --recurse-submodules https://github.com/Bilalmc/iptv-player.git
cd iptv-player
git submodule update --init --recursive
```

Für eine lokale Entwicklung gegen einen ausgecheckten OwnTV_Core-Stand kann `owntv.corePath` in der lokalen Gradle-Konfiguration gesetzt werden. Zugangsdaten gehören nicht ins Repository.

## Rechtliches

OwnTV und die übernommenen Komponenten stehen unter GPLv3. Änderungen und Weitergabe müssen die jeweiligen Lizenz- und Urheberrechtshinweise erhalten. Die App enthält keine IPTV-Inhalte, Senderabonnements oder Zugangsdaten; Nutzer müssen eigene, rechtmäßig zugängliche Quellen verwenden.
