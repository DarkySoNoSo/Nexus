/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { NexusFolder, SystemLog, AuditRule, SearchResultItem } from '../types';

export const INITIAL_FOLDERS: NexusFolder[] = [
  {
    id: "00_START_HIER",
    name: "00_START_HIER",
    index: 0,
    title: "Einstieg & Bedienung",
    description: "Zentrale Anlaufstelle für den aktuellen Systemstatus, die Bedienoberfläche und Schnellstarts.",
    files: [
      {
        name: "STATUS_AKTUELL.md",
        path: "00_START_HIER/STATUS_AKTUELL.md",
        fileType: "markdown",
        sizeBytes: 840,
        lastModified: "2026-06-09T14:30:00Z",
        content: `# Nexus Systemstatus - Stand 2026-06-09

* **Nexus-Version:** v40.44
* **Systemstatus:** Online
* **Aktueller Port:** 8081 (konfiguriert in 'nexus_current_url.txt')
* **LAN-ZUGRIFF:** http://192.168.1.216:8081/
* **TAILSCALE-VPN:** http://100.107.24.67:8081/

## Aktuelle Aufgaben
1. [X] SQLite Master-Katalog synchronisieren
2. [X] Gemini AI Studio Key-Bereitstellung prüfen
3. [ ] Wöchentlichen Timeline-Audit durchführen
4. [ ] Backup-Zieldateien auf G:\\ verifizieren`
      },
      {
        name: "AKTUELLER_AUDIT_STATUS.md",
        path: "00_START_HIER/AKTUELLER_AUDIT_STATUS.md",
        fileType: "markdown",
        sizeBytes: 950,
        lastModified: "2026-06-12T02:00:00Z",
        content: `# Aktueller Core-Audit & Systemzustand - Nexus v40.44

## 1. Compliance-Status: KONFORM
* **Single-Source-of-Truth:** Alle Systemdaten fließen ausschließlich in die SQLite-Master-Datenbank. Keine unkontrollierten Schatten-Kataloge.
* **Feature-Bloat-Index:** 0% (Strenge Einhaltung des minimalistischen Designs, keine unaufgeforderten Berichte oder unnötigen GUI-Spielereien).
* **API-Sicherheit:** Lokale Umgebungsvariablen aktiv, externe Abfragen blockiert wenn Offline-Limit erreicht.

## 2. Laufende System-Audits
- [Passed] **Port-Befähigung 8081:** Keine Belegung durch Ghost-Prozesse.
- [Passed] **Android-Daemon Intervall:** SQLite Offline-Caching \`sync_queue\` verifiziert mit Backoff-Retry.
- [Passed] **UTF-8 BOM Check:** Alle Systemskripte (System.PS1 / main.py) erfolgreich signiert.`
      },
      {
        name: "nexus_current_url.txt",
        path: "00_START_HIER/nexus_current_url.txt",
        fileType: "text",
        sizeBytes: 31,
        lastModified: "2026-06-09T12:00:00Z",
        content: "http://100.107.24.67:8081/"
      }
    ]
  },
  {
    id: "01_ARCHITEKTUR",
    name: "01_ARCHITEKTUR",
    index: 1,
    title: "Systemarchitektur",
    description: "Definition der Datenflüsse, Systemschnittstellen und des Zusammenspiels lokaler Komponenten.",
    files: [
      {
        name: "SYSTEM_DESIGNS.md",
        path: "01_ARCHITEKTUR/SYSTEM_DESIGNS.md",
        fileType: "markdown",
        sizeBytes: 1550,
        lastModified: "2026-06-02T10:00:00Z",
        content: `# Systemarchitektur & Datenflüsse (Nexus v40)

Das gesamte Nexus-System basiert auf dem Prinzip 'Local First / Offline Authorized'. 

## Datenfluss-Modell
  [Lokale Dateien/Mails] ---> [Nexus.PS1 Core] <===> [nexus_catalog.sqlite]
                                    |
                 +------------------+------------------+
                 | (Provider-Adapter Regelauswertung)  |
                 v                                     v
         [Option A: OpenAI]                     [Option B: Gemini]
      (Chef-Hauptlogik & Agenda)              (Zweitmeinung & Fallbacks)

## Architektur-Regeln
1. **Source Attribution:** Jede KI-Antwort muss klar labeln, welches Modell sie generiert hat.
2. **Offline-Blocker:** Sensible Daten (Finanzen, private Mails) dürfen standardmäßig nicht extern verarbeitet werden.
3. **Double-Entry Safeguard:** Kalendereinträge müssen vor dem Schreiben lokal vom Chef bestätigt werden.`
      },
      {
        name: "NEXUS_ARCHITEKTUR.md",
        path: "01_ARCHITEKTUR/NEXUS_ARCHITEKTUR.md",
        fileType: "markdown",
        sizeBytes: 1250,
        lastModified: "2026-06-07T11:00:00Z",
        content: `# NEXUS-Systemarchitektur - Technische Struktur

## 1. Single-Source-of-Truth Prinzip
* Alle Zustände, indexierte Dateien, E-Mails und Verlaufs-Metadaten liegen exklusiv in der SQLite Master-Verzeichnisdatei 'C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\db\\nexus_catalog.sqlite'.
* Die Clients (Web App, Android Daemon, Widgets) kommunizieren ausschließlich über signierte REST-Endpunkte an Port 8081.

## 2. Client-Profile & Rollen
- **Web App Cockpit:** Bietet Patrick ein visuelles Triage-Portal, SQLite-Katalogabfragetool, Auslastungsmetriken und den Dialog-Chef.
- **Android App & Widget:** Lokaler Kivy-Dienst zur Erfassung mobiler Fotos (Zählerstände, Belege) und SMS-Zahlungsbestätigungen mit transaktionssicherer Offline-Speicherung (Errno 13/14-geschützt).`
      },
      {
        name: "NEXUS_GESAMTDOKUMENTATION_STAND_20260607.md",
        path: "01_ARCHITEKTUR/NEXUS_GESAMTDOKUMENTATION_STAND_20260607.md",
        fileType: "markdown",
        sizeBytes: 1680,
        lastModified: "2026-06-07T12:00:00Z",
        content: `# Nexus Gesamtdokumentation - Fassung v40.44

## 1. Das übergeordnete Zielbild von Nexus
Das Nexus-System orchestriert alle digitalen Eingänge, vertraulichen Daten, Finanzen und Systemzustände für Patrick Herzog. 
Es agiert vollkommen selbstbestimmt über den lokalen Indexing-Kern 'Nexus.PS1', welcher die Datenhoheit auf Patricks physikalischem Speicher sichert.

## 2. Orchestration & 'Zweitmeinung'
Der 'Index-Chef' nutzt primär ein offline-sicheres Heuristikmodell für die Vorab-Triage. 
Optional wird eine Zweitmeinung über die Gemini-API eingeholt, um unklare Belege, formlose Rechnungsfotos und komplexe E-Mails semantisch einzuordnen.

* **Schnittstellen-Vertrag:** Keine unautorisierten API-Schreibversuche auf externe Datenbanken. API-Kosten-Loch blockiert alle Anfragen, falls Monatsbudget von 15.00 USD überschritten wird.`
      }
    ]
  },
  {
    id: "02_INDEX_CHEF",
    name: "02_INDEX_CHEF",
    index: 2,
    title: "Chef-Logik & Kontext",
    description: "Regelwerke und Leitfäden für das Lernen von Kontexten und das Treffen plausibler Entscheidungen.",
    files: [
      {
        name: "Chef_Logik_Leitfaden.md",
        path: "02_INDEX_CHEF/Chef_Logik_Leitfaden.md",
        fileType: "markdown",
        sizeBytes: 1210,
        lastModified: "2026-05-28T16:00:00Z",
        content: `# Der Index-Chef Regelkatalog

Dieser Leitfaden definiert, wie Nexus eingehende Nachrichten strukturieren und bearbeiten muss.

## Die 4 Säulen der Chef-Logik
1. **Nachvollziehbarkeit (Verbatim Logs):** Alle getroffenen Entscheidungen werden in der SQLite-Datenbank inklusive Roh-Prompt, verwendetem Tokenpreis und Modell geloggt.
2. **Kosten-Lock (Budget Rules):** Überschreiten die akkumulierten Kosten des laufenden Kalendermonats ein Limit von 15,00 USD, schaltet Nexus automatisch in den 'Local-Offline' Modus und nutzt lokale Heuristiken.
3. **Platzeinteilung & Relevanz:** Eingehende E-Mails/Events werden in die Kategorien 'FOKUS-HEUTE', 'TIMELINE-REVIEW', 'SPÄTER-LESEN' und 'MÜLL' eingeteilt.
4. **Schutz vor AI-Slop:** Keine unaufgeforderten Berichte, keine pseudointellektuellen System-Zustände im UI.`
      },
      {
        name: "INDEX_CHEF_LOGIK.md",
        path: "02_INDEX_CHEF/INDEX_CHEF_LOGIK.md",
        fileType: "markdown",
        sizeBytes: 1320,
        lastModified: "2026-06-07T14:30:00Z",
        content: `# INDEX_CHEF_LOGIK - Entscheidungsverfahren des Chefs

## 1. Priorisierung und Triage
Der Index-Chef ordnet eingehende Events, Fotos und Benachrichtigungen umgehend nach ihrer Dringlichkeit und Relevanz ein. Echte Auszahlungsbelege oder kritische Fehlermeldungen erhalten höchste Priorität.

## 2. Abfangregeln (Security Rules)
* **Kosten-Lock:** Schützt das Tokenbudget. Keine automatisierten Loops ohne manuelle Absegnung.
* **Feature-Governance:** Alle Funktionalitäten müssen einen klaren Mehrwert für den Offline-Masterindex bieten. Unnötige Animationen oder unproduktive Telemetriewerte sind verboten.`
      },
      {
        name: "NEXUS_CHEF_CONTEXT_INTERFACE_GUIDE_20260607.md",
        path: "02_INDEX_CHEF/NEXUS_CHEF_CONTEXT_INTERFACE_GUIDE_20260607.md",
        fileType: "markdown",
        sizeBytes: 1420,
        lastModified: "2026-06-07T16:00:00Z",
        content: `# NEXUS_CHEF_CONTEXT_INTERFACE_GUIDE

## Schnittstellen-Vertrag für KI-Kollaborateure
Dieser Leitfaden regelt, wie externe KI-Modelle Kontexte einlesen und zurückschreiben dürfen.

1. **Keine Parallel-Datenbanken:** Es darf kein zweiter Speicherkanal (z.B. Index in Google Drive oder separate lokale JSONs) eigenmächtig erstellt werden.
2. **Auditierbarkeit:** Jedes geänderte File muss mit einer Log-Meldung im SQLite Master-Katalog registriert werden.
3. **Double-Entry Safeguard:** Änderungen an kritischen Dateien werden im Change-Ledger als DRAFT vermerkt.`
      }
    ]
  },
  {
    id: "03_KOMMUNIKATION",
    name: "03_KOMMUNIKATION",
    index: 3,
    title: "Kommunikation & Timeline",
    description: "Verwaltung eingehender Nachrichten, Logs, historischer Timelines und kritischer Sicherheits-Alerts.",
    files: [
      {
        name: "Timeline_Sample.json",
        path: "03_KOMMUNIKATION/Timeline_Sample.json",
        fileType: "json",
        sizeBytes: 680,
        lastModified: "2026-06-09T15:20:00Z",
        content: `[
  {
    "id": "evt_902183",
    "timestamp": "2026-06-09T15:00:00Z",
    "category": "E-Mail",
    "subject": "Statusbericht Stromabrechnung ewz Zürich",
    "classification": "TIMELINE-REVIEW",
    "chefDecision": "Relevante Kostenstelle erfasst (124.50 CHF). In SQLite Katalog indiziert.",
    "isFlagged": false
  },
  {
    "id": "evt_902184",
    "timestamp": "2026-06-09T16:15:00Z",
    "category": "Mobile Collector",
    "subject": "Foto von Stromzähler hochgeladen",
    "classification": "FOKUS-HEUTE",
    "chefDecision": "Automatisch als Beleg für oben stehende Abrechnung verknüpft.",
    "isFlagged": true
  }
]`
      }
    ]
  },
  {
    id: "04_DATEIEN_UND_KATALOG",
    name: "04_DATEIEN_UND_KATALOG",
    index: 4,
    title: "Dateien & SQLite Katalog",
    description: "Dateisystem-Überwachung, SQLite-Schemata und Sortierungs-Mechanismen für Patricks Dateien.",
    files: [
      {
        name: "sqlite_schema.sql",
        path: "04_DATEIEN_UND_KATALOG/sqlite_schema.sql",
        fileType: "powershell",
        sizeBytes: 950,
        lastModified: "2026-04-12T08:30:00Z",
        content: `-- Nexus Master-Katalog Datenbank-Schema
-- Datenbank: C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\db\\nexus_catalog.sqlite

CREATE TABLE IF NOT EXISTS file_index (
  id TEXT PRIMARY KEY,
  file_name TEXT NOT NULL,
  rel_path TEXT NOT NULL UNIQUE,
  file_size INTEGER,
  last_modified TEXT,
  file_hash TEXT,
  category TEXT,
  embedding_vector TEXT,
  learned_metadata TEXT
);

CREATE TABLE IF NOT EXISTS decision_log (
  id TEXT PRIMARY KEY,
  timestamp TEXT DEFAULT CURRENT_TIMESTAMP,
  input_source TEXT,
  target_action TEXT,
  prompt_hash TEXT,
  response_content TEXT,
  pricing_usd REAL,
  provider_used TEXT,
  is_approved INTEGER DEFAULT 0
);`
      }
    ]
  },
  {
    id: "05_APP_ANDROID",
    name: "05_APP_ANDROID",
    index: 5,
    title: "Nexus Mobile (Android)",
    description: "Schnittstellen zur Android-Collector App, Widget-Integration und APK-Generierung.",
    files: [
      {
        name: "android_api_config.json",
        path: "05_APP_ANDROID/android_api_config.json",
        fileType: "json",
        sizeBytes: 310,
        lastModified: "2026-06-08T20:00:00Z",
        content: `{
  "collectorPort": "8081",
  "widgetRefreshIntervalSec": 300,
  "secureTokenHeader": "X-Nexus-Mobile-Auth",
  "allowedDirectories": [
    "C:\\\\MasterIndex_Storage\\\\communication\\\\mobile_uploads",
    "C:\\\\MasterIndex_Storage\\\\context\\\\quick_notes"
  ]
}`
      },
      {
        name: "main.py",
        path: "05_APP_ANDROID/main.py",
        fileType: "powershell",
        sizeBytes: 2450,
        lastModified: "2026-06-10T19:42:00Z",
        content: `"""
SPDX-License-Identifier: Apache-2.0
Nexus Mobile - Local-First Chef-Cockpit Core
Developed using KivyMD. Event-loop handles SQLite offline queue and local caching.
"""
import sys
import os
import sqlite3
import requests
from kivy.app import App
from kivy.lang import Builder
from kivy.uix.boxlayout import BoxLayout
from kivy.properties import StringProperty, ListProperty, BooleanProperty
from kivy.clock import Clock

KV = """
<ChefCockpit>:
    orientation: 'vertical'
    spacing: 12
    padding: 16
    canvas.before:
        Color:
            rgba: 0.97, 0.98, 0.99, 1
        Rectangle:
            pos: self.pos
            size: self.size

    BoxLayout:
        size_hint_y: None
        height: '56dp'
        canvas.before:
            Color:
                rgba: 0.08, 0.11, 0.18, 1
            RoundedRectangle:
                pos: self.pos
                size: self.size
                radius: [8, 8, 8, 8]
        Label:
            text: "Nexus Master Cockpit"
            font_size: '18sp'
            bold: True
            color: (1, 1, 1, 1)

    Label:
        id: status_lbl
        text: "Konnektivität: " + self.parent.status_label
        size_hint_y: None
        height: '32dp'
        font_name: 'Roboto'
        bold: True
        color: (0.1, 0.7, 0.3, 1) if self.parent.connection_state else (0.8, 0.2, 0.3, 1)

    BoxLayout:
        orientation: 'vertical'
        spacing: 10
        Label:
            text: "Lokaler Queue-Speicher (Double-Entry-Ledger DRAFT-039)"
            color: (0.1, 0.1, 0.1, 1)
            font_size: '14sp'
            bold: True
            size_hint_y: None
            height: '24dp'
            halign: 'left'
"""

class ChefCockpit(BoxLayout):
    status_label = StringProperty("Suche Verbindung zu Port 8081...")
    connection_state = BooleanProperty(False)
    
    def check_connection(self, dt):
        try:
            r = requests.get("http://192.168.1.216:8081/api/system/status", timeout=1.0)
            if r.status_code == 200:
                self.status_label = "Verbunden mit Nexus Core LAN (v40.44)"
                self.connection_state = True
            else:
                self.status_label = "Dienst antwortet mit Status " + str(r.status_code)
                self.connection_state = False
        except Exception:
            self.status_label = "Lokaler Offline-Modus aktiv (Tailscale inaktiv)"
            self.connection_state = False

class NexusMobileApp(App):
    def build(self):
        Builder.load_string(KV)
        root = ChefCockpit()
        Clock.schedule_interval(root.check_connection, 4.0)
        return root

if __name__ == '__main__':
    NexusMobileApp().run()
`
      },
      {
        name: "buildozer.spec",
        path: "05_APP_ANDROID/buildozer.spec",
        fileType: "text",
        sizeBytes: 1540,
        lastModified: "2026-06-10T19:42:00Z",
        content: `[app]
title = Nexus Master App
package.name = nexus_master
package.domain = org.nexus.master
source.dir = .
source.include_exts = py,png,jpg,kv,json
version = 1.0.40

# Kivy required packages
requirements = python3,kivy,kivy_deps.gstreamer,requests,urllib3,certifi,sqlite3

# Android settings
android.api = 33
android.minapi = 21
android.sdk = 33
android.ndk = 25b
android.archs = arm64-v8a

# Application Permissions
android.permissions = INTERNET,ACCESS_NETWORK_STATE,WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE

# Orientation
orientation = portrait

# Fullscreen
fullscreen = 1

# iOS Specific (Skipped for Kivy android bundle target)
[buildozer]
log_level = 2
warn_on_root = 1
`
      },
      {
        name: "sync_manager.py",
        path: "05_APP_ANDROID/sync_manager.py",
        fileType: "powershell",
        sizeBytes: 1850,
        lastModified: "2026-06-10T19:42:00Z",
        content: `"""
SPDX-License-Identifier: Apache-2.0
Nexus Mobile Sync Manager - Implements DRAFT-039 retry loop
and offline caching handler to bypass the 13/14 sync timeout issue.
"""
import sqlite3
import time
import requests

DB_PATH = "nexus_offline_cache.db"

def initialize_offline_db():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("""
        CREATE TABLE IF NOT EXISTS sync_queue (
            id TEXT PRIMARY KEY,
            event_type TEXT NOT NULL,
            payload TEXT NOT NULL,
            retry_count INTEGER DEFAULT 0,
            status TEXT DEFAULT 'PENDING'
        )
    """)
    conn.commit()
    conn.close()

def attempt_sync():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute("SELECT id, event_type, payload, retry_count FROM sync_queue WHERE status = 'PENDING'")
    pending_items = cursor.fetchall()

    for item in pending_items:
        evt_id, evt_type, payload, retries = item
        print(f"[Sync] Versuche Synchronisation für Event {evt_id}. Retry-Count: {retries}")
        
        try:
            # Send payload to Local REST-Core API
            res = requests.post(
                "http://192.168.1.216:8081/api/sync",
                json={"id": evt_id, "type": evt_type, "data": payload},
                timeout=2.0
            )
            
            if res.status_code == 200:
                cursor.execute("UPDATE sync_queue SET status = 'SUCCESS' WHERE id = ?", (evt_id,))
                print(f"[Sync] Event {evt_id} erfolgreich synchronisiert.")
            else:
                raise ValueError(f"HTTP Error {res.status_code}")
                
        except Exception as e:
            new_retries = retries + 1
            status = 'FAILED' if new_retries > 5 else 'PENDING'
            cursor.execute(
                "UPDATE sync_queue SET retry_count = ?, status = ? WHERE id = ?",
                (new_retries, status, evt_id)
            )
            print(f"[Sync Fehler 13/14] ({e}). Bereite Backoff-Retry vor.")

    conn.commit()
    conn.close()
`
      }
    ]
  },
  {
    id: "06_WEB_DASHBOARD",
    name: "06_WEB_DASHBOARD",
    index: 6,
    title: "Web Cockpit",
    description: "Quellcodes und Konfigurationen für die lokale Web-Bedienung und Statusschirme.",
    files: [
      {
        name: "Cockpit_Design.md",
        path: "06_WEB_DASHBOARD/Cockpit_Design.md",
        fileType: "markdown",
        sizeBytes: 680,
        lastModified: "2026-05-15T11:00:00Z",
        content: `# Web Cockpit Layout (Port 8081)

Das Web-Cockpit fungiert als visuelles Feedback für Patrick.

## Layout-Blöcke
1. **Header:** Status-Punkte, Systemversion (v40.44), VPN-Link (Tailscale)
2. **Katalog-Schnellsuche (SQLite):** Direkter Live-Filter über alle indizierten Ordner.
3. **Zweitmeinungs-Assistent:** Direkte Schnittstelle zur Gemini Files API für Hochgeschwindigkeits-Zusammenfassungen und Belegprüfung.`
      }
    ]
  },
  {
    id: "07_BETRIEB_RUNBOOK",
    name: "07_BETRIEB_RUNBOOK",
    index: 7,
    title: "Betrieb & Diagnose",
    description: "Anleitung für Start, Neustart, Behebung von Datenbank-Sperren und Backup-Prozesse.",
    files: [
      {
        name: "Restart_Runbook.md",
        path: "07_BETRIEB_RUNBOOK/Restart_Runbook.md",
        fileType: "markdown",
        sizeBytes: 810,
        lastModified: "2026-06-09T09:00:00Z",
        content: `# Runbook: Nexus-Neustart im Fehlerfall

Sollte Port 8081 blockiert sein oder sqlite gesperrt ('database is locked'), führe folgende Schritte durch:

### 1. PowerShell-Task töten
Stop-Process -Name 'Powershell' -Force
# oder gezielt den Port freigeben:
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8081).OwningProcess -Force

### 2. Hauptscript neu starten
powershell -NoProfile -ExecutionPolicy Bypass -File 'C:\\MasterIndex_Storage\\Nexus.PS1'

### 3. Logdatei überwachen
Get-Content -Path 'C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\communication\\nexus_run.log' -Tail 20 -Wait`
      }
    ]
  },
  {
    id: "08_SICHERHEIT_ZUGRIFF",
    name: "08_SICHERHEIT_ZUGRIFF",
    index: 8,
    title: "Sicherheit & Secrets",
    description: "Definition von Zugriffsschutz, Netzwerkgrenzen und der sicheren Speicherung von API-Keys.",
    files: [
      {
        name: "Security_Rules.md",
        path: "08_SICHERHEIT_ZUGRIFF/Security_Rules.md",
        fileType: "markdown",
        sizeBytes: 1120,
        lastModified: "2026-06-09T14:00:00Z",
        content: `# Nexus Sicherheitsrichtlinien

## 1. Secrets Management
* **KRITISCHE REGEL:** API-Schlüssel ('GEMINI_API_KEY' oder 'OPENAI_API_KEY') dürfen niemals im Klartext in *.md, *.json oder *.ps1 Dateien eingecheckt werden.
* **Windows-Integration:** Schlüssel werden ausschließlich über '[Environment]::GetEnvironmentVariable()' geladen.
* **Optionales Fallback:** Private lokale '.env' in '_NEXUS_SYSTEM', die in der '.gitignore' gelistet ist.

## 2. API Scope & Netzwerk
* Externe Verbindungen nur über TLS 1.3 zu Google-API Endpunkten.
* Keine direkten eingehenden Verbindungen aus dem Web (Zulassung nur über lokales LAN und das private Tailscale IP-Netzwerk '100.x.x.x').`
      },
      {
        name: "SICHERHEIT_UND_ZUGRIFF.md",
        path: "08_SICHERHEIT_ZUGRIFF/SICHERHEIT_UND_ZUGRIFF.md",
        fileType: "markdown",
        sizeBytes: 1250,
        lastModified: "2026-06-07T18:00:00Z",
        content: `# SICHERHEIT_UND_ZUGRIFF - Nexus Governance

## 1. Authentifizierung und Secrets-Abfragen
* **Mobiles APK Authentifizieren:** Schnittstellenaufrufe von Android über den HTTP Header 'X-Nexus-Mobile-Auth' abgesichert. Token wird lokal über HMAC-SHA256 validiert.
* **Windows Host-Umgebung:** Skripte rufen sensible Secrets und Token via lokaler Environment auf, um Missbrauch und Repository-Leaks permanent zu unterbinden.

## 2. Schadensprävention und Kostenkontrolle
* Ein hart kodierter Budget-Check schützt Patrick vor explodierenden API-Rechnungen.
* Jede transaktionskritische Dateioperation wird in try-catch Blöcken überwacht. Fehlermeldungen beenden den Prozess kontrolliert.`
      }
    ]
  },
  {
    id: "09_AUDITS_FEHLER",
    name: "09_AUDITS_FEHLER",
    index: 9,
    title: "Fehler & Audits",
    description: "Red-Team-Ergebnisse, gefundene Sicherheitslücken und Richtlinien zur Fehlervermeidung.",
    files: [
      {
        name: "Red_Team_Audit_Report.md",
        path: "09_AUDITS_FEHLER/Red_Team_Audit_Report.md",
        fileType: "markdown",
        sizeBytes: 740,
        lastModified: "2026-05-10T15:00:00Z",
        content: `# Red Team Audit & Fehlervermeidung (Mai 2026)

* **Gefahr:** API-Key Leakage in Git Repo von AI Studio.
* **Status:** Gelöst. Commit-Webhooks scannen jetzt auf Entropie und verweigern pushes mit Keys.
* **Gefahr:** Verwechslung von OpenAI- und Gemini-Attributionen (Halluzinationen wurden fälschlicherweise dem Core angelastet).
* **Status:** Verpflichtendes JSON-Response-Schema für alle KI-Integrationen eingeführt (erfordert 'sourceAttribution' Objekt).`
      }
    ]
  },
  {
    id: "10_VISUALISIERUNG",
    name: "10_VISUALISIERUNG",
    index: 10,
    title: "Visualisierungen",
    description: "D3/SVG-Karten, Diagramme und Beziehungs-Netzwerke zur visuellen Index-Analyse.",
    files: [
      {
        name: "System_Graph_Schema.json",
        path: "10_VISUALISIERUNG/System_Graph_Schema.json",
        fileType: "json",
        sizeBytes: 520,
        lastModified: "2026-03-24T14:00:00Z",
        content: `{
  "nodes": [
    {"id": "powershell_core", "label": "Nexus-Hauptscript", "group": "system"},
    {"id": "sqlite_db", "label": "sqlite Katalog", "group": "data"},
    {"id": "communication_dir", "label": "communication/", "group": "storage"},
    {"id": "gemini_api", "label": "Gemini API Proxy", "group": "ai"}
  ],
  "links": [
    {"source": "powershell_core", "target": "sqlite_db", "value": 5},
    {"source": "powershell_core", "target": "communication_dir", "value": 3},
    {"source": "powershell_core", "target": "gemini_api", "value": 2}
  ]
}`
      }
    ]
  }
];

export const INITIAL_LOGS: SystemLog[] = [
  {
    id: "log_1",
    timestamp: "2026-06-09T16:50:00Z",
    type: "info",
    source: "PowerShell",
    message: "Starte MasterIndex-Dienst mit Nexus.PS1..."
  },
  {
    id: "log_2",
    timestamp: "2026-06-09T16:50:02Z",
    type: "success",
    source: "SQLite",
    message: "Verbindung zu C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\db\\nexus_catalog.sqlite erfolgreich hergestellt."
  },
  {
    id: "log_3",
    timestamp: "2026-06-09T16:50:03Z",
    type: "info",
    source: "PowerShell",
    message: "Messe aktuellen Live-Port aus: http://127.0.0.1:8081."
  },
  {
    id: "log_4",
    timestamp: "2026-06-09T16:50:04Z",
    type: "success",
    source: "PowerShell",
    message: "Zustands-Datei nexus_current_url.txt erfolgreich aktualisiert."
  },
  {
    id: "log_5",
    timestamp: "2026-06-09T16:51:10Z",
    type: "info",
    source: "Chef-Logik",
    message: "Trigger: E-Mail erkannt. Verarbeite 'ewz Zürich' Kontext..."
  },
  {
    id: "log_6",
    timestamp: "2026-06-09T16:51:12Z",
    type: "success",
    source: "Gemini",
    message: "Gemini API 'Second Opinion' erfolgreich ausgewertet. Kosten: $0.00045. Status: TIMELINE-REVIEW klassifiziert."
  },
  {
    id: "log_7",
    timestamp: "2026-06-09T16:52:00Z",
    type: "success",
    source: "PowerShell",
    message: "System läuft im voll funktionsfähigen Zustand v40.44 stabil."
  },
  {
    id: "log_8",
    timestamp: "2026-06-10T19:42:00Z",
    type: "info",
    source: "PowerShell",
    message: "Master-Manifest für Kivy-Entwurf empfangen. Initialisiere Build-Generierung."
  },
  {
    id: "log_9",
    timestamp: "2026-06-10T19:42:15Z",
    type: "success",
    source: "Chef-Logik",
    message: "Generiert: 05_APP_ANDROID/main.py (Kivy Event-Loop mit SQLite Offline-Queue) erfolgreich geschrieben."
  },
  {
    id: "log_10",
    timestamp: "2026-06-10T19:42:25Z",
    type: "success",
    source: "Chef-Logik",
    message: "Generiert: 05_APP_ANDROID/buildozer.spec (Android SDK 33 target, ARM64 build config)."
  },
  {
    id: "log_11",
    timestamp: "2026-06-10T19:42:30Z",
    type: "success",
    source: "Chef-Logik",
    message: "Generiert: 05_APP_ANDROID/sync_manager.py (DRAFT-039 offline synchronization, backoff mechanism)."
  }
];

export const INITIAL_AUDIT_RULES: AuditRule[] = [
  {
    id: "audit_1",
    category: "Secrets",
    title: "GEMINI_API_KEY Leak-Schutz",
    rule: "Der API-Schlüssel darf nicht in textbasierten Kontextdokumenten oder Skriptdateien gelistet sein.",
    status: "passed",
    feedback: "Sicherheitsprüfung erfolgreich. Der API-Key ist nur als System-Umgebungsvariable geladen."
  },
  {
    id: "audit_2",
    category: "Sicherheit",
    title: "Offline-Modus bei kritischen Aufgaben",
    rule: "Wichtige Systemdateien dürfen nicht unbeaufsichtigt an die API übermittelt werden.",
    status: "passed",
    feedback: "Die Sperre greift. Alle Uploads bedürfen dem bewussten Drücken des PowerShell-Hochladetools."
  },
  {
    id: "audit_3",
    category: "Architektur",
    title: "Eindeutige Provider-Attribution",
    rule: "Jede generierte KI-Antwort muss klar als Modell-Vorschlag ausgewiesen werden.",
    status: "warning",
    feedback: "In manchen alten Python-Heuristiken fehlt das Attributionsobjekt in der SQLite, bitte DB patchen."
  },
  {
    id: "audit_4",
    category: "Betrieb",
    title: "Port-Belegung 8081",
    rule: "Der Standardport 8081 darf nicht von Ghost-Prozessen blockiert sein.",
    status: "passed",
    feedback: "Port ist exklusiv an Nexus.PS1 gebunden."
  }
];

export const SIMULATED_CATALOG_DB: SearchResultItem[] = [
  {
    id: "rec_1",
    title: "Abrechnung Strom ewz Zuerich 2026.pdf",
    category: "Finanzen / Belege",
    snippet: "Rechnungsbetrag: 124.50 CHF, fällig am 15.06.2026. Kundennummer CH-8001-ZUE-ewz. Verbrauch: 2450 kWh.",
    relevance: 100,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\strom\\ewz_Abrechnung_2026.pdf",
    tags: ["Strom", "Finanzen", "2026", "Abrechnung"]
  },
  {
    id: "rec_2",
    title: "Zaehlerstand_Strom_20260609.jpg",
    category: "Foto-Belege / Mobile",
    snippet: "Foto hochgeladen über Nexus Mobile Android App. Zählerwert: 43219.4 kWh (Flur).",
    relevance: 95,
    fileOrigin: "C:\\MasterIndex_Storage\\communication\\mobile_uploads\\Zaehlerstand_Strom_20260609.jpg",
    tags: ["Zählerstand", "Foto", "Strom", "Mobile"]
  },
  {
    id: "rec_3",
    title: "Mietvertrag_Wohnung_Patrick.pdf",
    category: "Dokumente / Verträge",
    snippet: "Wohnungs-Mietvertrag Herzog Patrick, abgeschlossen am 01.10.2021. Kaltmiete: 650 CHF. Nebenkosten: 120 CHF.",
    relevance: 90,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\dokumente\\Mietvertrag.pdf",
    tags: ["Mietvertrag", "Wohnung", "Vertrag"]
  },
  {
    id: "rec_4",
    title: "Steuererklaerung_2025_Entwurf.docx",
    category: "Steuern / Entwürfe",
    snippet: "Zusammenstellung der Werbungskosten 2025, Fahrtkosten, Home-Office Pauschale, Fachliteratur-Erstattung.",
    relevance: 85,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\steuern\\Steuererklaerung_2025_Entwurf.docx",
    tags: ["Steuer", "2025", "Finanzen"]
  },
  {
    id: "rec_5",
    title: "Wartungskalender_Heizung.txt",
    category: "Wartung",
    snippet: "Nächster Termin Heizungswartung geplant für Oktober 2026. Technikerfirma Schlosser & Söhne. Gastherme v2.",
    relevance: 75,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\wartung\\Wartungskalender.txt",
    tags: ["Heizung", "Wartung", "Kalender"]
  },
  {
    id: "rec_6",
    title: "NEXUS_GESAMTDOKUMENTATION_STAND_20260607.md",
    category: "Dokumente / Verträge",
    snippet: "Fundamentales Richtliniendokument mit dem Zielbild von Nexus v40.44. Definiert die Orchestration des Index-Chefs.",
    relevance: 100,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\dokumente\\NEXUS_GESAMTDOKUMENTATION_STAND_20260607.md",
    tags: ["Nexus", "Doku", "Architektur", "SSOT"]
  },
  {
    id: "rec_7",
    title: "NEXUS_ARCHITEKTUR.md",
    category: "Dokumente / Verträge",
    snippet: "Technische Systemarchitektur von Nexus, Datenflüsse zwischen Master-Index und mobilen Daemons.",
    relevance: 95,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\dokumente\\NEXUS_ARCHITEKTUR.md",
    tags: ["Architektur", "Doku", "SQLite"]
  },
  {
    id: "rec_8",
    title: "INDEX_CHEF_LOGIK.md",
    category: "Dokumente / Verträge",
    snippet: "Regelwerke für Budgetkontrolle ($15 limitiertes Tokenbudget) und unbestätigte Termine blockieren.",
    relevance: 95,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\dokumente\\INDEX_CHEF_LOGIK.md",
    tags: ["Chef-Logik", "Doku", "Budget"]
  },
  {
    id: "rec_9",
    title: "NEXUS_CHEF_CONTEXT_INTERFACE_GUIDE_20260607.md",
    category: "Dokumente / Verträge",
    snippet: "Schnittstellenvertrag zur Datenkonsistenz: Verbietet Schatten-Datenbanken und unreglementierte Schreibvorgänge.",
    relevance: 90,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\dokumente\\NEXUS_CHEF_CONTEXT_INTERFACE_GUIDE_20260607.md",
    tags: ["Interface", "Schnittstelle", "Regeln"]
  },
  {
    id: "rec_10",
    title: "SICHERHEIT_UND_ZUGRIFF.md",
    category: "Dokumente / Verträge",
    snippet: "Definition der HMAC-Verschlüsselung für mobile Ingestionen, Firewall-Regeln für das Tailscale (100.x.x.x) Netzwerk.",
    relevance: 95,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\dokumente\\SICHERHEIT_UND_ZUGRIFF.md",
    tags: ["Sicherheit", "Tailscale", "HMAC", "VPN"]
  },
  {
    id: "rec_11",
    title: "AKTUELLER_AUDIT_STATUS.md",
    category: "Fehler & Audits",
    snippet: "Tägliches Überprüfungsprotokoll des Systems. Zeigt Status der UTF-8 Skript-Signierung und Portbelegung 8081.",
    relevance: 80,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\audits\\AKTUELLER_AUDIT_STATUS.md",
    tags: ["Audit", "Status", "Fehler"]
  },
  {
    id: "rec_12",
    title: "NEXUS_CHANGE_DRAFT_LEDGER.md",
    category: "Fehler & Audits",
    snippet: "Aktuelles Arbeitsjournal, registriert alle Änderungen an der Sync-Engine oder dem Triage-Portal vorab als DRAFT.",
    relevance: 90,
    fileOrigin: "C:\\MasterIndex_Storage\\_NEXUS_SYSTEM\\NEXUS_CHANGE_DRAFT_LEDGER.md",
    tags: ["Ledger", "DRAFT", "Änderung"]
  },
  {
    id: "rec_13",
    title: "Zaehlerstand_Gas_20260611.jpg",
    category: "Foto-Belege / Mobile",
    snippet: "Upload per Telegram Webhook. Erfasster Zählerstand Gastherme: 11048.9 m³ für Nebenkosten-Zuschlag.",
    relevance: 85,
    fileOrigin: "C:\\MasterIndex_Storage\\communication\\mobile_uploads\\Zaehlerstand_Gas_20260611.jpg",
    tags: ["Zählerstand", "Foto", "Gas", "Mobile"]
  },
  {
    id: "rec_14",
    title: "Abonnement_Mietwagen_Abrechnung.pdf",
    category: "Finanzen / Belege",
    snippet: "Zahlbeleg Carsharing-Abo Mai 2026: 45,90 CHF. Kreditkarte verifiziert und in sqlite_catalog verbucht.",
    relevance: 80,
    fileOrigin: "C:\\MasterIndex_Storage\\context\\strom\\Mietwagen_Mai_2026.pdf",
    tags: ["Auto", "Finanzen", "2026", "Abrechnung"]
  },
  {
    id: "rec_15",
    title: "ntfy_test_alert_ping.json",
    category: "System-Warnung",
    snippet: "Automatisierte Konnektivitätsprüfung zu ntfy.sh Topic für kritische Push-Hinweise bei unbefugtem SSH-Zugriff.",
    relevance: 70,
    fileOrigin: "C:\\MasterIndex_Storage\\communication\\tests\\ntfy_test_alert_ping.json",
    tags: ["System", "Warmup", "ntfy", "Test"]
  }
];
