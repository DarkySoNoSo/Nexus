# Nexus Modulregister

Status: offizieller Ordnungsanker für Nexus/Nexy/Digi Dragon.

## Grundregel

Keine neue Funktion ohne Modulplatz, Healthcheck und Rollback-Pfad.

## Module

| Modul | Rolle | Pfad | DB | Port | Start | Status |
|---|---|---|---|---:|---|---|
| Nexus App | Android Shell / mobiles Cockpit | 05_APP_ANDROID_NATIVE | Android lokal | - | APK | aktiv |
| Nexy | Gedächtnis, Kontext, Recall, Zeitstrahl, Briefing | backend/nexy | data/nexy.db | 8765 | tools/start_nexy_bridge.ps1 / später sh | aktiv |
| Digi Dragon | Companion/Game-Schicht | backend/companion | data/digi_dragon.db | 8777 | tools/start_digi_dragon_bridge.sh | aktiv |
| Chef | Analyse, Ausführung, Entscheidung | backend / API | projektabhängig | variabel | manuell/Bridge | kontrolliert |
| Collector | Rohdaten, SMS, Notifications, Dateien | Android / Collector | Android/Backend | variabel | App | kontrolliert |

## Rollenreinheit

- Nexus = Gesamtsystem.
- Nexy = Gedächtnis, Kontext, Zeitstrahl, Recall.
- Chef = Analyse, Entscheidung, Ausführung.
- Collector = Rohdaten.
- Digi Dragon = Companion, Motivation, Spielschicht.

## Harte Grenzen

- Android-App muss offline starten.
- Kein Servercall beim App-Start.
- Digi Dragon darf keine Chef-Aktion automatisch auslösen.
- Digi Dragon darf Nexy später lesen, aber nicht ungefragt verändern.
- Lokale DB-Dateien werden nicht committed.
- Jede Bridge braucht eigenen Healthcheck.
- Jeder Ausbau braucht vorher Doctor grün oder dokumentierten Grund.


## Verbindliche Portkarte

| Dienst | Port | Richtiger Test | Falsche Annahme |
|---|---:|---|---|
| Nexy Bridge | 8765 | http://127.0.0.1:8765/api/nexy/status | Digi Dragon auf 8765 |
| Digi Dragon Bridge | 8777 | http://127.0.0.1:8777/api/dragon/status | Port 8766 oder 0.0.0.0 im Browser |

Port 8766 ist aktuell kein offizieller Nexus-Dienst.
0.0.0.0 ist nur eine Bind-Adresse und wird nicht als Browser-/App-Ziel verwendet.

## Ports

- Nexy Bridge: 8765
- Digi Dragon Bridge: 8777

## Nächste erlaubte Schritte

1. Doctor grün bekommen.
2. Start/Stop-Scripts stabilisieren.
3. Android Digi-Dragon-Seite erst danach.
