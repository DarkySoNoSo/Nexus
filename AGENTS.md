# Nexus System-Agetist Guidelines & Single Source of Truth
Dieses Dokument definiert für die KI (den AI Coding Agent) die fundamentale Struktur, Identität und unumstößlichen Governance-Regeln von Patricks Nexus-System v40.44.

## 1. Das Fundament (Die „Single Source of Truth“)
Diese Dateien definieren, wer du bist und wie Nexus funktioniert:

* **NEXUS_GESAMTDOKUMENTATION_STAND_20260607.md**: Dies ist die wichtigste Datei. Sie enthält das Zielbild von Nexus, die Orchestrationslogik des „Chefs“ und wie das System mit Daten, Kommunikation und Kontext umgeht.
* **NEXUS_ARCHITEKTUR.md**: Beschreibt die technische Struktur, die Single-Source-of-Truth-Prinzipien und die Rolle der verschiedenen Clients (Web App, Android-App, Widget).
* **INDEX_CHEF_LOGIK.md**: Essentiell für das Verständnis, wie der „Index-Chef“ Entscheidungen trifft, Kontext bewertet und das System steuert.

## 2. Kontext & Governance (Die Regeln)
Damit die KI keine „falsche“ Wahrheit bildet, muss sie diese Regeln kennen:

* **NEXUS_CHEF_CONTEXT_INTERFACE_GUIDE_20260607.md**: Definiert den Schnittstellen-Vertrag. Damit weiß die KI, wie sie Kontext lesen und schreiben darf, ohne die Datenbasis zu korrumpieren.
* **SICHERHEIT_UND_ZUGRIFF.md**: Hier ist definiert, wie die Authentifizierung (Legacy/HMAC) und der Zugriff gehandhabt werden muss.
* **docs/rules/NEXUS_PROTECTED_CORE_POLICY.md**: Definiert den zentralen geschützten Kern. Nexy, Zeitstrahl, Kontext, Fakten, Claims, Lessons und Quellenregister gehören zum Kern.
* **docs/rules/WRITE_DOCUMENTATION_POLICY.md**: Definiert die Dokumentationspflicht. Schreiben ab Schutzklasse GELB ist nur mit nachvollziehbarer Dokumentation erlaubt.

## 3. Aktueller Status (Der „Startpunkt“)
* **AKTUELLER_AUDIT_STATUS.md**: Gibt der KI ein Bild davon, wo das Projekt aktuell steht und welche Probleme/Governance-Aufgaben gerade offen sind.
* **NEXUS_CHANGE_DRAFT_LEDGER.md**: Dies ist dein „Arbeitsjournal“. Die KI muss wissen, welche Änderungen als DRAFT gelistet sind, um nicht mit alten Annahmen zu arbeiten.

## 4. Protected-Core-Pflicht

Ab 2026-06-17 gilt verbindlich:

* Nexy, Zeitstrahl, Kontext, Fakten, Claims, Lessons und Quellenregister sind Schutzklasse ROT.
* Änderungen ab Schutzklasse GELB benötigen Dokumentation.
* ROT-Änderungen benötigen zusätzlich Vault/Backup, Quelle, Hash, Klassifikation, Vorher/Nachher-Prüfung, Healthcheck und Rollback-Hinweis.
* DigiDragon und DigiPad dürfen den Kern nicht direkt verändern.
* Kommunikationsrohdaten dürfen nicht blind als Fakten gespeichert werden.
* Vaults und lokale Legacy-Speicher dürfen nicht ins Repository committed werden.
