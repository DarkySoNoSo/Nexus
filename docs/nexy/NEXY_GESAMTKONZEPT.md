# Nexi Gesamtkonzept

**Projekt:** Nexus / Nexi
**Status:** Zielbild-, Architektur- und Arbeitsanker
**Datum:** 2026-06-18
**Repo-Anker:** `docs/nexy/NEXY_GESAMTKONZEPT.md`

## Kurzdefinition

Nexus ist das Geraest. Nexi ist das einzige Hirn.

Nexi sammelt, normalisiert, bewertet, verknuepft und speichert Kontext so, dass Dateien, Kommunikation, Entscheidungen, Zeitstrahl, Fakten, Claims, Unklarheiten und Lessons rekonstruierbar bleiben.

Chef, Master und Index-Chef sind Legacy-Funktionen innerhalb von Nexi. Sie werden nicht als getrennte produktive Systeme weitergefuehrt.

## Nicht-Ziele

Nexi darf nicht werden:

- ein weiterer chaotischer Dateiordner
- ein Chatbot ohne Gedaechtnis
- ein Funktionsfriedhof in der Android-App
- ein System, das externe Nachrichten ungeprueft als Wahrheit speichert
- ein System, das alte Testdaten als echte Kommunikation anzeigt
- ein System, das Secrets oder private lokale Daten ins Repo schreibt

## Mentales Modell

```text
Input -> Normalisierung -> Bewertung -> Speicherung -> Verknuepfung -> Review -> Entscheidung -> Lernen
```

Die Architekturregel:

```text
Nicht UI = Wahrheit.
Nicht Chat = Wahrheit.
Nicht JSONL = Wahrheit.
Strukturierter Nexi-Zustand = Wahrheit.
```

## Produktive Rollen

| Rolle | Bedeutung |
|---|---|
| Nexus | Repo, Android-Shell, Backend-Anbindung, Docs, Build |
| Nexi | Kontext, Memory, Zeitstrahl, Recall, Entscheidungen, Lessons |
| Collector | Rohdatenlieferant |
| Android App | mobile Bedienoberflaeche |
| Widget | schnelle Aktionsflaeche auf derselben Decision-Queue |
| Dragon | separates System |
| DigiPad | separates Remote-/Pad-System |
| AI-Studio | Entwurf, nicht Produktionswahrheit |

## Kernmodule

### Kommunikation

Nachrichten werden nach Unterhaltung gebuendelt. Jede Nachricht bleibt rekonstruierbar, aber nicht jede Nachricht wird Fokus.

### Entscheidungen

Entscheidungen werden robust gespeichert. App und Widget teilen dieselbe Decision-Queue.

### Zeitstrahl

Der Zeitstrahl wird aus Ereignissen gebaut und lesbar gerendert. Fakten, Claims und Unklarheiten bleiben unterscheidbar.

### Dateien

Die Dateienseite wird ein echter Explorer. Riskante Dateiaktionen brauchen Review und Rollback-Pfad.

### Migration

Dragon und DigiPad werden sauber getrennt. Alte Begriffe duerfen nicht als sichtbare neue Produktwahrheit zurueckkehren.

## Sicherheitsregeln

- Keine Secrets in Git, Markdown, README oder Logs.
- `C:\MasterIndex_Storage` bleibt lokal.
- Keine privaten Daten ins Repo kopieren.
- Keine automatische riskante Dateiaktion ohne Gate.
- Kein Commit ohne Freigabe.

## Arbeitsreihenfolge

1. Ordnung.
2. Reparatur.
3. Ausbau.
4. Design.

Funktionslogik kommt erst nach sauberer Repo-Struktur.

## Naechste Funktionsreihenfolge

1. Nachrichten buendeln nach Unterhaltung.
2. Entscheidungen robust speichern.
3. Widget und App teilen dieselbe Decision-Queue.
4. Zeitstrahl lesbar rendern.
5. Dateienseite als echten Explorer bauen.
6. Dragon sauber migrieren, ohne alte Begriffe sichtbar zu halten.
7. Design danach.
