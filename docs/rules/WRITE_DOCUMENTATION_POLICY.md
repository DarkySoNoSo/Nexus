# Nexus Write Documentation Policy

Status: verbindlich
Gueltig ab: 2026-06-17

## Kernregel

Schreiben ab Schutzklasse GELB ist nur mit Dokumentation erlaubt.

Das gilt fuer GELB, ORANGE und ROT. Nur GRUEN darf ohne formales Aenderungsprotokoll regeneriert oder geloescht werden, wenn eindeutig klar ist, dass es sich um Cache, Build-Artefakte oder temporaere Dateien handelt.

## Schutzklassen

### ROT

ROT umfasst die zentrale Wahrheitsschicht des Projekts: Nexy, Zeitstrahl, Kontext, Fakten, Claims, Lessons, Quellenregister, Vaults und hochsensible Kerninformationen.

Schreiben in ROT nur mit:

- dokumentiertem Zweck
- Quelle und Quellenpfad
- SHA256 oder gleichwertigem Nachweis
- Vault oder Backup
- Risikoangabe
- Klassifikation: fact, claim, unclear oder untrusted
- Vorher/Nachher-Pruefung
- Healthcheck
- Rollback-Hinweis

### ORANGE

ORANGE umfasst kontrollierte Datenbereiche, private Laufzeitdaten, Companion-Datenbanken, Collector-Zwischenstaende und nichtoeffentliche Reports.

Schreiben in ORANGE nur mit:

- Aenderungsnotiz
- Zweck
- betroffenen Dateien oder Datenbanken
- Risikoangabe
- Backup oder Rueckweg
- Pruefung

### GELB

GELB umfasst UI-Code, App-Code, Styles, Build-Konfiguration, Workflows, technische Dokumentation und Hilfsskripte.

Schreiben in GELB nur mit:

- kurzer Aenderungsdokumentation
- Grund
- betroffenen Dateien
- Pruefung
- Rollback-Hinweis

### GRUEN

GRUEN umfasst Cache, Build-Artefakte, temporaere Dateien und eindeutig regenerierbare Daten.

GRUEN darf ohne formale Dokumentation geaendert oder geloescht werden, sofern der regenerierbare Charakter eindeutig ist.

## Minimaler Change Record

Jede Aenderung ab GELB braucht mindestens:

- Datum/Zeit
- Bereich
- Schutzklasse
- Aenderung
- Grund
- betroffene Dateien oder Datenbanken
- Pruefung
- Rollback
- Ergebnis

## Verbindlicher Satz

Ab GELB wird nichts mehr einfach geaendert. Jede Aenderung bekommt eine Spur. ROT bekommt zusaetzlich Vault, Hash, Vorher/Nachher-Pruefung und Healthcheck.
