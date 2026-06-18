# DigiPad Remote Client Architecture

## Zweck

Der DigiPad-Container ist die geschuetzte Aussenschicht fuer externe Digi-Dragon-/DigiPad-Clients.
Fiona kann mit eigenem Handy spielen, ohne Zugriff auf Patricks privates Nexus zu erhalten.

## Rollen

- Nexus privat: Patrick, Nexi, Collector, Dateien, Timeline, Logs
- DigiPad Container: geschuetzter Spiel-/Remote-Bereich
- Fiona-Profil: eigenes Family-Pad-Profil
- Digi Dragon: Training, Arena, Entwicklung, Battle-Snapshot
- Battle Snapshot: sicherer Kampfexport ohne private Nexus-Daten

## Verbindliche Ports

- Nexy Bridge: 8765, privat
- Digi Dragon Bridge: 8777, optionaler separater Bridge-Port
- DigiPad Container API: 8788, geschuetzte Aussenschicht fuer Fiona/Remote-Pads

## Zugriff

Fionas Handy verbindet sich später nicht mit 127.0.0.1 auf Patricks Gerät.
127.0.0.1 ist immer das jeweilige Gerät selbst.

Fionas Handy braucht später:

- gleiche WLAN-IP von Patricks Host, oder
- Tailscale-IP, oder
- Battle-Code / Snapshot ohne Dauerverbindung

## Erlaubte Endpunkte für Fiona

- /api/pad/fiona/status
- /api/pad/fiona/feed
- /api/pad/fiona/care
- /api/pad/fiona/train
- /api/pad/fiona/freefight
- /api/pad/fiona/arena
- /api/pad/fiona/evolve
- /api/pad/fiona/battle/export
- /api/pad/fiona/battle/ghost

## Gesperrte Bereiche

- /api/nexy/*
- /api/communication/*
- /api/mobile/chef-chat
- /api/timeline/*
- /api/v1/files/*
- /api/collector/*
- /api/admin/*

## Authentifizierung

Jedes Profil bekommt einen lokalen Token.
Der Token wird nicht committed und liegt nur lokal in der SQLite-Datenbank.

Clients senden den Token über:

- Header: X-DigiPad-Token
- oder Query: ?token=...

## Design-Ton

Nicht kleinkindlich.
Nicht peinlich.
Nicht Baby-Modus.

Stil:
- hochwertig
- klar
- modern
- Fantasy/Sci-Fi
- respektvoll
- Family-Profil statt Child-Spielzeug

## Kampfmodell

Phase 1: NPC / Training / Arena
Phase 2: Ghost-Battle über Battle Snapshot
Phase 3: Battle-Code zwischen Pads
Phase 4: optional Live-Battle

## Sicherheitsprinzip

Fiona bekommt kein Nexus.
Fiona bekommt ein eigenes geschütztes DigiPad-Profil.
