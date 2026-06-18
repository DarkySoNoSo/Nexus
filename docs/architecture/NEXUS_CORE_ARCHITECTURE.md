# Nexus Core Architecture

## Definition

Nexus ist das Geraest. Es stellt Repo-Struktur, Android-Shell, Backend-Anbindung, Dokumentation, Build-Pfade und Integrationspunkte bereit.

Nexi ist das einzige Hirn. Nexi verwaltet Kontext, Memory, Recall, Zeitstrahl, Entscheidungen, Review, Fakten/Claims/Unklarheiten und Lernregeln.

Chef, Master und Index-Chef sind Legacy-Begriffe fuer Funktionen innerhalb von Nexi. Sie duerfen in neuer produktiver Logik nicht als getrennte Gehirne behandelt werden.

## Produktive Grenzen

| Bereich | Status | Regel |
|---|---|---|
| Nexus | Geraest | Struktur, App, Backend-Anbindung, Docs |
| Nexi | einziges Hirn | Memory, Entscheidungen, Zeitstrahl, Kontext |
| Chef/Master/Index-Chef | Legacy innerhalb Nexi | nur noch als Funktionsnamen oder Migrationskontext |
| Dragon | separat | nicht Teil des aktiven Nexus-Kerns |
| DigiPad | separat | eigene Remote-/Pad-Schicht |
| AI-Studio | Entwurf | keine Produktionswahrheit |

## Aktive Repo-Struktur

```text
.github/workflows/build-native-apk.yml
05_APP_ANDROID_NATIVE/
backend/nexy/
docs/
AGENTS.md
README.md
NEXUS_CHANGE_DRAFT_LEDGER.md
build.gradle
settings.gradle
.gitignore
```

## Datenwahrheit

Private produktive Daten liegen nicht im Repo. `C:\MasterIndex_Storage` bleibt lokal und wird nicht nach Git kopiert.

Nexi speichert produktive Wahrheit strukturiert:

- Ereignisse
- Kontext
- Fakten, Claims und Unklarheiten
- Entscheidungen
- Zeitstrahl
- Lessons
- aktive Fokus-Punkte

Rohdaten, lokale Vaults, Tokens, APKs, Logs und private Datenbanken bleiben ausserhalb des Repos.

## Android-Regeln

1. Die App startet offline.
2. Kein harter Serverzwang beim Start.
3. Widget und App teilen spaeter dieselbe Decision-Queue.
4. UI zeigt nur produktive Begriffe: Nexi fuer Hirn/Assistenz; keine neuen sichtbaren Legacy-Begriffe.

## Ausbau-Reihenfolge

1. Nachrichten nach Unterhaltung buendeln.
2. Entscheidungen robust speichern.
3. Widget und App auf dieselbe Decision-Queue bringen.
4. Zeitstrahl lesbar rendern.
5. Dateienseite als echten Explorer bauen.
6. Dragon sauber migrieren, ohne alte Begriffe sichtbar zu halten.
7. Design danach.
