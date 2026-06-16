# Nexus Core Architecture

## Ziel

Nexus ist ein persönliches Betriebssystem mit digitalem Assistenten.

## Rollen

### Nexy / Nexi
Digitaler Assistent.
Verwaltet Chat, Kontext, Zeitstrahl, Erinnerungen, Zusammenhänge und Zugriff auf den Master Index.

### Chef
Orchestrator.
Steuert Aktionen, Jobs, Diagnose, Entscheidungen und Systemzustand.

### Collector
Datensammler.
Sammelt Rohdaten wie Nachrichten, Dateien, Screenshots, Logs und Ereignisse.
Collector bewertet nicht. Collector liefert Rohdaten.

### Master Index
Langzeitgedächtnis.
Speichert Dateien, Ereignisse, Personen, Themen, Fakten, Claims, Unsicherheiten und Verknüpfungen.

### Zeitstrahl
Chronologische Wahrheit.
Ordnet Ereignisse nach Zeitpunkt, Ursache, Folge, Quelle und Relevanz.

### Android App
Cockpit.
Startet immer ohne Backend.
Keine automatische Serverpflicht beim App-Start.
Funktionen laden erst nach Benutzeraktion.

### PC / Server
Gehirn.
Hält Backend, Worker, Queue, OpenAI-Anbindung, Index und lokale Tools.

## Goldregeln

1. App-Start muss immer offline funktionieren.
2. Kein Servercall in onCreate oder showHome.
3. Nexy ist Kontext- und Chat-Ebene.
4. Chef ist Ausführungs- und Steuerungsebene.
5. Collector sammelt nur Rohdaten.
6. Master Index ist Gedächtnis.
7. Zeitstrahl ist Pflicht.
8. Jede Aussage muss später als Fakt, Claim oder Unklar markierbar sein.
9. AI-Studio-Code bleibt Quarantäne.
10. Stabile APKs sind Gold-Referenz.
