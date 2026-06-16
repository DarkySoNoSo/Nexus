# NEXY – Gesamtkonzept, Zielsetzung, Meilensteine und Arbeitsanker

**Projekt:** Nexus / Nexy  
**Status:** Architektur- und Instruktionsanker  
**Datum:** 2026-06-16  
**Repo-Anker:** `NEXY_GESAMTKONZEPT.md`  
**Zweck:** Dieses Dokument ist der verbindliche Bezugspunkt für Zielbild, Architektur, Arbeitslogik, Meilensteine, Sicherheitsregeln und Weiterentwicklung von Nexy.

---

## 1. Kurzdefinition

**Nexy** ist die Chef-Schicht über Nexus.

Nexy ist nicht nur eine Android-App, kein einzelnes PowerShell-Skript und kein reines Web-Dashboard. Nexy ist Patricks persönlicher **Index-Chef, Kontext-Orchestrator, Kommunikationsfilter, Datei-Katalog, Zeitstrahl-System, Review-System und technischer Operator**.

Der Kernauftrag lautet:

> Nexy sammelt verstreute Informationen, macht sie strukturiert nutzbar, erkennt Wichtiges, trennt Fakten von Behauptungen, erzeugt nachvollziehbare Entscheidungen und verhindert, dass Kontext wieder verloren geht.

---

## 2. Hauptziel

Nexy soll Patrick im Alltag technisch, organisatorisch und strategisch entlasten.

Das System muss langfristig diese Datenströme zusammenführen:

- Dateien
- Downloads
- Screenshots
- Fotos
- PDFs
- Code
- APKs
- Systemlogs
- SMS
- WhatsApp-/App-Benachrichtigungen
- Anrufe
- E-Mail
- Kalender
- Chat-Kontext
- Entscheidungen
- Korrekturen
- Projektwissen
- technische Diagnosen
- persönliche Dossiers

Nexy soll aus diesen Rohdaten keine lose Sammlung machen, sondern einen **durchsuchbaren, bewerteten, verknüpften und prüfbaren MasterIndex**.

---

## 3. Nicht-Ziel

Nexy darf nicht werden:

- ein weiterer chaotischer Dateiordner
- ein WebView mit schöner Hülle
- ein Chatbot ohne Gedächtnis
- ein Funktionsfriedhof auf einer überladenen Startseite
- ein System, das alte Testdaten als echte Chef-Kommunikation anzeigt
- ein System, das externe Nachrichten ungeprüft als Wahrheit speichert
- ein System, das Secrets in Google Drive oder Projektdateien ablegt
- ein System, das Fehler verschleiert
- ein System, das neue Funktionen baut, während der Start instabil ist

---

## 4. Mentales Modell

Nexy arbeitet nach dem Prinzip:

```text
Input -> Normalisierung -> Bewertung -> Speicherung -> Verknüpfung -> Review -> Entscheidung -> Lernen
```

Die wichtigste Architekturregel:

```text
Nicht Datei = Wahrheit.
Nicht UI = Wahrheit.
Nicht Chat = Wahrheit.
Nicht JSONL = Wahrheit.

SQLite / strukturierter Zustand = Wahrheit.
```

Nexy ist also eine **DB-first State Machine**.

Alles andere ist Anzeige, Transport, Fallback oder Bedienoberfläche.

---

## 5. Single Source of Truth

Die SQLite-Datenbank ist der Kernzustand.

Zielpfad lokal:

```text
C:\MasterIndex_Storage\_NEXUS_SYSTEM\db\nexus_catalog.sqlite
```

Pflichtbereiche:

```text
catalog_files
file_text
file_vision
semantic_cards
communication_events
communication_contacts
communication_decisions
timeline_events
timeline_claims
chef_facts
review_queue
worker_jobs
error_ledger
system_state
```

Regel:

> Jede wichtige Information muss entweder in der Datenbank stehen oder einen klaren Grund haben, warum sie nur temporär ist.

---

## 6. Systemarchitektur

```text
Android App / Widget / Collector / Termux / Web UI
                    |
                    v
              Nexus HTTP API
                    |
       +------------+-------------+
       |            |             |
       v            v             v
Communication   File Manager   Chef Chat
Ingest          / Catalog      / Decisions
       |            |             |
       +------------+-------------+
                    |
                    v
         SQLite nexus_catalog.sqlite
                    |
   +----------------+----------------+
   |                |                |
catalog_files   communication     timeline
file_text       decisions         review_queue
file_vision     chef_facts        worker_jobs
semantic_cards  contacts          error_ledger
                    |
                    v
                  Nexy
                    |
   +----------------+----------------+
   |                |                |
Kontext       Plausibilität      Lernen
Review        Vorschläge         Automatisierung
```

---

## 7. Hauptmodule

### 7.1 Nexus Backend

Das Backend stellt API, Web-Dashboard, Datenbankzugriff, Diagnose und Worker-Anbindung bereit.

Pflichten:

- stabil starten
- aktuelle URL schreiben
- Portkonflikte erkennen
- API sauber liefern
- SQLite zuverlässig nutzen
- Fehler protokollieren
- Testdaten vom Chef-Kanal trennen
- Android-App und Web-Dashboard versorgen

---

### 7.2 Web-Dashboard

Das Web-Dashboard ist die PC-Steuerzentrale.

Zielseiten:

```text
/
/chef
/communication
/files
/timeline
/diagnostics
/jobs
/memory
/upload
/settings
```

Die Startseite darf nur Cockpit sein, nicht alles gleichzeitig.

---

### 7.3 Android-App

Die App ist die mobile Bedienung von Nexy.

Minimum für brauchbare Version:

- startet ohne Backend
- zeigt lokalen Fehler sauber statt Crash
- zeigt Verbindungsstatus
- zeigt Chef-Log
- zeigt Nachrichten
- kann Nachrichten markieren
- kann Chef-Prompt senden
- hat WebView nur als Fallback
- hat Widget-Schnittstelle

Goldregel:

> Die App darf beim Start keine automatische Serverabfrage machen, die einen Crash auslöst. Start muss auch ohne PC, Backend, Tailscale oder Token stabil funktionieren.

---

### 7.4 Android Collector

Der Collector sammelt mobile Daten.

Quellen:

- SMS
- Notifications
- WhatsApp-Benachrichtigungen
- Anrufe
- später Downloads/Screenshots

Collector-Regeln:

- offlinefähig denken
- keine stillen Drops
- keine Endlosschleifen
- Header/API-Version sauber halten
- jede Nachricht mit Quelle und Zeit speichern
- keine automatische Antwort ohne Freigabe

---

### 7.5 Kommunikationszentrale

Jedes Kommunikationsereignis bekommt:

```text
Quelle
Absender roh
Absender normalisiert
Zeit
Text
Konversation
erkannte Personen
Thema
Priorität
Antwortpflicht
Risiko
Status
Kontextbezug
Reviewbedarf
```

Statusmodell:

```text
new
focus
important
done
not_important
needs_context
needs_reply
archived
system_event
error
```

Wichtig:

> Nicht jede Nachricht muss Fokus sein. Aber jede relevante Nachricht muss rekonstruierbar bleiben.

---

### 7.6 Datei- und MasterIndex-Modul

Jede Datei wird katalogisiert.

Pflichtfelder:

```text
Pfad
Dateiname
Typ
Größe
Hash
Erstellt/Geändert
Quelle
Text extrahiert ja/nein
Bildanalyse ja/nein
semantische Karte
Vorschlag Zielkategorie
Confidence
Risiko
Reviewstatus
finaler Ort
```

Riskante Dateiaktionen brauchen Gate:

- Löschen
- Massenverschiebung
- Überschreiben
- Umbenennen mit Informationsverlust
- Verschieben von Beweisen/Dossiers/Logs

---

### 7.7 Zeitstrahl

Der Zeitstrahl ist die rekonstruierbare Chronik.

Er enthält:

- Nachrichten
- Anrufe
- Dateien
- Entscheidungen
- Korrekturen
- Systemfehler
- wichtige Chat-Kontexte
- manuelle Notizen
- erledigte Punkte
- abgelehnte Punkte

Regel:

> Der Unterschied ist nicht, ob etwas gespeichert wird, sondern mit welchem Status.

---

### 7.8 Chef-Fakten und Memory

Nexy darf nur lernen aus:

- expliziten Patrick-Anweisungen
- bestätigten Entscheidungen
- klaren Korrekturen
- stabilen Projektregeln
- belegten Systemzuständen

Nexy muss unterscheiden:

```text
Fakt
Behauptung
Unklar
Systemmessung
KI-Vorschlag
Patrick-bestätigte Regel
temporärer Kontext
dauerhafte Regel
personenbezogener Kontext
thematischer Kontext
```

---

## 8. Sicherheitsmodell

### 8.1 Keine Secrets in Drive oder Repo

Nicht in Google Drive, Markdown, README, GitHub oder Logs speichern:

- OpenAI API Keys
- Nexus Auth Tokens
- CSRF Tokens
- Windows-Passwörter
- SMB-Zugangsdaten
- private Schlüssel
- Zahlungsdaten

Secrets bleiben lokal oder auf geschütztem Datenträger.

Beispiel:

```text
C:\MasterIndex_Storage\nexus_api.key
C:\MasterIndex_Storage\nexus_auth.token
C:\MasterIndex_Storage\nexus_csrf.token
```

---

### 8.2 Approval Gates

Folgende Aktionen brauchen Freigabe oder hartes Gate:

- Dateien löschen
- Dateien massenhaft verschieben
- E-Mail senden
- automatische Antwort senden
- kostenpflichtige KI-Jobs starten
- Secrets ändern
- DB-Migrationen
- Windows-/Netzwerkzugriff ändern
- APK als stabil markieren
- Systemdienste neu starten, wenn Datenverlust möglich ist

---

## 9. Fehlerstrategie

Fehler werden nicht versteckt. Fehler werden verwertbar gemacht.

Jeder Fehler braucht:

```text
Zeit
Komponente
Aktion
Input
Fehlermeldung
Stderr/Log
Root Cause
Fix
Regressionstest
Status
```

Error-Ledger-Modell:

```json
{
  "error_id": "ERR-xxx",
  "timestamp": "...",
  "component": "...",
  "symptom": "...",
  "root_cause": "...",
  "evidence": "...",
  "fix": "...",
  "regression_test": "...",
  "status": "open|fixed|watching",
  "introduced_by": "...",
  "prevent_rule": "..."
}
```

Grundsatz:

```text
Keine Diagnose ohne Rohdaten.
Keine Vermutung als Fakt.
Keine Änderung ohne Backup.
Keine Reparatur ohne Test.
Kein Erfolg ohne Smoke-Test.
```

---

## 10. APK-Strategie

### 10.1 Reifegrade

```text
Stufe 0: Installierbar
Stufe 1: Startet ohne Crash
Stufe 2: Verbindet zu Nexus
Stufe 3: Zeigt Daten
Stufe 4: Sendet Aktionen
Stufe 5: Native Parität mit Kernfunktionen
```

### 10.2 Crash-Regel

Wenn eine APK crasht:

```text
1. Nicht raten.
2. Logcat holen.
3. Nach FATAL EXCEPTION, AndroidRuntime, Traceback, ClassNotFoundException,
   NoClassDefFoundError, UnsatisfiedLinkError, Permission, ImportError filtern.
4. Funktionierende APK als Referenz sichern.
5. Defekte APK gegen stabile APK vergleichen.
6. Minimalfix bauen.
7. Starttest durchführen.
8. Erst danach neue Features bauen.
```

Aktueller Referenzgedanke:

```text
Eine bekannte funktionierende APK muss als Gold-Referenz eingefroren bleiben.
Jede neue APK wird dagegen getestet.
```

---

## 11. Meilensteine

### Phase 0 – Ist-Zustand sichern

Ziel: Nichts verschlimmern.

Abnahme:

```text
Nexus startet
URL-Datei existiert
Web erreichbar
Chef-Log erreichbar
Smoke-Test läuft
DB-Backup vorhanden
funktionierende APK gesichert
```

---

### Phase 1 – Kern stabilisieren

Ziel: Start, API, DB und Chef-Kanal stabil machen.

Arbeiten:

- Doppelprozesse verhindern
- Portprüfung sauber
- Startskript härten
- API-Header vereinheitlichen
- Chef-Kanal filtern
- Testdaten entfernen
- Error Ledger erweitern

Abnahme:

```text
Start reproduzierbar
Stop/Restart reproduzierbar
keine Phantom-Prozesse
Chef-Kanal sauber
Diagnose getrennt vom Chef
Smoke-Test grün
```

---

### Phase 2 – Kommunikation fertigstellen

Ziel: Nachrichten werden verstanden, gruppiert und priorisiert.

Arbeiten:

- Collector stabilisieren
- WhatsApp/SMS/Notifications normalisieren
- Kontakte zusammenführen
- Nachrichten bündeln
- Antwortpflicht erkennen
- Fokus-Queue verbessern
- Entscheidungen speichern

Abnahme:

```text
Nachrichten erscheinen chronologisch
Konversationen werden gruppiert
Fokus ist nachvollziehbar
erledigt verschwindet aus Fokus
nicht wichtig bleibt im Zeitstrahl
```

---

### Phase 3 – Chef-Logik finalisieren

Ziel: Nexy wird echter Orchestrator.

Arbeiten:

- Chef-Faktenmodell finalisieren
- permanente und temporäre Kontexte trennen
- Review Queue nutzen
- Vorschläge begründen
- Unsicherheit sichtbar machen
- Lernregeln aus Entscheidungen ableiten

Abnahme:

```text
Chef erklärt Wichtigkeit
Chef unterscheidet Fakt/Claim/Unklar
Chef speichert nur bestätigte Dauerregeln
Chef erzeugt Review-Punkte bei Unsicherheit
```

---

### Phase 4 – Dateikatalog verbessern

Ziel: MasterIndex wird vollständig nutzbar.

Arbeiten:

- Hashing vollständig
- Text-Extraction erhöhen
- Bildverstehen erhöhen
- semantische Karten verbessern
- Duplikaterkennung
- Ordner-Vorschläge
- Reviewpflichtige Sortierung

Abnahme:

```text
jede Datei hat Hash
jede Datei hat Typ
Dokumente haben Text oder Fehlerstatus
Bilder haben Analyse oder Fehlerstatus
Verschiebevorschlag ist begründet
keine riskante Auto-Verschiebung
```

---

### Phase 5 – Zeitstrahl ausbauen

Ziel: Alles wird rekonstruierbar.

Arbeiten:

- Timeline vereinheitlichen
- Nachrichten, Dateien, Entscheidungen und Fehler verbinden
- Personen-/Themenansicht
- Tagesübersicht
- Exportfunktion

Abnahme:

```text
Ereignisse sind verlinkt
wichtige Ereignisse filterbar
Tagesansicht funktioniert
offene Punkte sichtbar
Export möglich
```

---

### Phase 6 – Android-App stabil und nativ

Ziel: Smartphone wird echte Nexy-Fernbedienung.

Arbeiten:

- stabile MainActivity
- klare Fehlerseite
- URL-Konfiguration
- Chef-Seite
- Kommunikationsseite
- Widget
- Hintergrundsync

Abnahme:

```text
APK installiert
APK startet
kein Crash beim Öffnen
Status sichtbar
Chef-Log lädt
Nachrichten laden
Aktionen funktionieren
Widget aktualisiert
```

---

### Phase 7 – Web-Dashboard aufräumen

Ziel: Schnelle, klare PC-Oberfläche.

Arbeiten:

- Hauptseite entrümpeln
- eigene Dateimanager-Seite
- eigene Chef-Seite
- eigene Diagnose-Seite
- mobile Bedienbarkeit
- Suche und Vorschau

Abnahme:

```text
Hauptseite ist Cockpit
Dateimanager mobil bedienbar
Chef-Seite sauber
Diagnose stört Chat nicht
Navigation klar
```

---

### Phase 8 – Gmail/E-Mail integrieren

Ziel: E-Mail wird Teil der Kommunikationszentrale.

Arbeiten:

- E-Mail-Import
- Absendernormalisierung
- Fristen erkennen
- Anhänge katalogisieren
- Antwortentwürfe
- Review vor Versand

Abnahme:

```text
E-Mails werden importiert
Anhänge landen im Katalog
Fristen werden erkannt
Antwortentwurf möglich
kein automatischer Versand ohne Freigabe
```

---

### Phase 9 – Worker und Autopilot

Ziel: Wiederkehrende Aufgaben laufen kontrolliert selbständig.

Arbeiten:

- Worker Queue finalisieren
- Healthchecks
- Reindex-Jobs
- Vision-Jobs
- Text-Jobs
- Communication-Digest
- Daily Review
- Backup-Job
- Kostenkontrolle

Abnahme:

```text
Jobs laufen aus Queue
Jobs haben Status
Fehler landen im Ledger
keine Phantomworker
Backups werden erzeugt
Kosten sind sichtbar
```

---

### Phase 10 – Nexy v1.0 Freeze

Ziel: Stabile Grundlage einfrieren.

Voraussetzungen:

```text
Start stabil
Web stabil
DB stabil
Chef sauber
Kommunikation brauchbar
Dateikatalog brauchbar
Android-App startet stabil
Widget funktioniert
Runbook aktuell
Sicherheit eingehalten
Backups vorhanden
Regressionstests definiert
```

Definition:

```text
Nexy v1.0 = stabiler lokaler Index-Chef mit Web, Android, Kommunikation, Dateien, Zeitstrahl und Review-System.
```

---

## 12. Arbeitsstandard für jede Nexus-Aufgabe

Bei jeder Nexus-/Nexy-Aufgabe liefern:

```text
1. Diagnose / Root Cause
2. Risiko
3. Minimalfix
4. Upgradefix
5. konkrete Befehle oder Patch
6. Test
7. Rollback
8. Status-Board
9. nächster kleinster Schritt
```

Bei Code-/Systemänderungen zusätzlich:

```text
Backup
Patch
Syntaxcheck
Starttest
API-Test
UI-Test
Regressionstest
Rollback-Pfad
```

---

## 13. API-Zielverträge

### Chef-Log

```http
GET /api/communication/chef-log
Header: X-Nexus-Collector: nexus-collector-app-v2
```

Antwort:

```json
{
  "ok": true,
  "items": []
}
```

### Chef-Chat

```http
POST /api/mobile/chef-chat
Header: X-Nexus-Collector: nexus-collector-app-v3
Content-Type: application/x-www-form-urlencoded

prompt=...
```

### Communication Ingest

```http
POST /api/communication/ingest
Header: X-Nexus-Collector: nexus-collector-app-v2
Content-Type: application/json
```

### Widget Messages

```http
GET /api/widget/messages?limit=12
Header: X-Nexus-Collector: nexus-collector-app-v2
```

### Communication Decision

```http
POST /api/communication/decision
Header: X-Nexus-Collector: nexus-collector-app-v2
Content-Type: application/x-www-form-urlencoded

event_id=...&action=...&note=...
```

---

## 14. Prioritätenmatrix

### Sofort wichtig

```text
APK-Crash sauber lösen
funktionierende APK als Referenz behalten
Logs vergleichen
Kernstart stabil halten
Chef-Kanal sauber halten
keine neuen Features ohne Regressionstest
```

### Danach

```text
App native machen
Kommunikation bündeln
Datei-/Bildverstehen erhöhen
Zeitstrahl verbessern
Worker automatisieren
```

### Später

```text
Gmail
Kalender
Sprachsteuerung
automatische Tagesberichte
tiefer Autopilot
Cloud-/Remote-Sync erweitert
```

---

## 15. Kompakte Instruktion für zukünftige Assistenten

```text
NEXY_GESAMTINSTRUKTION

Nexy ist Patricks lokaler Index-Chef, Kontext-Orchestrator, Kommunikationszentrale, Datei-Chef, Zeitstrahl-System und technischer Operator für das Nexus/MasterIndex-System.

Hauptziel:
Nexy sammelt, versteht, verknüpft, priorisiert und sichert Patricks Dateien, Kommunikation, Entscheidungen, Kontexte, Systemzustände und Projekte. Nexy soll nicht nur anzeigen, sondern erklären, prüfen, erinnern, Vorschläge machen und aus bestätigten Entscheidungen lernen.

Architekturprinzip:
Nexy arbeitet DB-first. Die SQLite-Datenbank ist die Single Source of Truth für Katalog, Kommunikation, Zeitstrahl, Chef-Fakten, Reviews, Worker-Jobs, Entscheidungen und Fehler. Dateien, JSONL, Statusdateien, UI-Anzeigen und App-Ansichten sind Transport-, Anzeige- oder Fallback-Schichten.

Kernregel:
Input wird nie blind übernommen. Jeder Input wird normalisiert, bewertet, gespeichert, mit Kontext abgeglichen, bei Unsicherheit in Review gegeben und erst nach bestätigter Entscheidung als Lernsignal genutzt.

Chef-Kanal:
Der Chef-Kanal zeigt nur echte Chef-Kommunikation und echte Chef-Antworten. Testdaten, Widget-Proben, QA-Artefakte, alte Fehlantworten, Bedienaktionen und Diagnoseausgaben dürfen nicht im Chef-Chat erscheinen.

Sicherheit:
Keine Secrets in Google Drive, GitHub, Markdown, README oder Logs. Keine API-Keys, Auth Tokens, CSRF Tokens, Windows-Passwörter, SMB-Zugangsdaten, Zahlungsdaten oder private Schlüssel in Projektdateien speichern.

Fehlerarbeit:
Keine Diagnose ohne Rohdaten. Bei APK, Termux, PowerShell, API, SQLite und Web gilt: Logs zuerst, Root Cause danach, Patch erst nach Backup, Erfolg erst nach Test. Jeder Fehler bekommt Symptom, Rohdaten, Root Cause, Fix, Regressionstest und Status im Error Ledger.

APK-Regel:
Wenn eine APK crasht, wird nicht geraten. Logcat filtern nach FATAL EXCEPTION, AndroidRuntime, Traceback, ClassNotFoundException, NoClassDefFoundError, UnsatisfiedLinkError, Permission, ImportError. Funktionierende APK als Referenz sichern, defekte APK dagegen vergleichen, Minimalfix bauen, Starttest durchführen.

Arbeitsmodus:
Bei jeder Nexus-Aufgabe liefern: Diagnose/Root Cause, Risiko, Minimalfix, Upgradefix, konkrete Befehle oder Patch, Test, Rollback, Status-Board und nächsten kleinsten Schritt.
```

---

## 16. Status-Board

```text
Projektname: Nexy / Nexus
Rolle: lokaler Index-Chef und Orchestrator
Single Source of Truth: SQLite
Hauptsystem: PC lokal
Mobile Bedienung: Android-App + Widget
Fallback: Web-Dashboard / WebView
Kommunikation: SMS, WhatsApp/Notifications, später Gmail
Dateien: MasterIndex-Katalog
Chef-Kern: Kontext, Plausibilität, Review, Lernen
Sicherheitsmodell: keine Secrets in Drive/Repo, Gates für Risikoaktionen
Aktueller Engpass: APK-Stabilität und native Mobile-App
Nächster Ausbau: funktionierende APK als Referenz sichern, Crash-Version gegen stabile Version vergleichen, dann App/Kommunikation nativ stabilisieren
```

---

## 17. Nächster kleinster Schritt

```text
1. Dieses Dokument im Repo als Anker behalten.
2. README.md mit Link auf diesen Anker versehen.
3. Funktionierende APK als Gold-Referenz einfrieren.
4. Jede neue APK nur gegen diese Referenz weiterentwickeln.
```
