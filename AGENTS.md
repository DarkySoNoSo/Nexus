# Nexus Agent Guidelines

Dieses Dokument ist der erste Leseanker fuer jeden Agenten im Repository.

## Single Source of Truth

1. Nexus ist das Geraest: Repo, Android-Shell, Backend-Anbindung, Dokumentation und Integrationsrahmen.
2. Nexi ist das einzige Hirn: Kontext, Memory, Recall, Zeitstrahl, Entscheidungen, Review und Lernen.
3. Chef, Master und Index-Chef sind Legacy-Begriffe fuer Funktionen innerhalb von Nexi. Sie sind keine getrennten produktiven Gehirne.
4. Dragon ist separat. Dragon-Code gehoert nicht zum aktiven Nexus-Kern.
5. DigiPad ist separat. DigiPad ist eine eigene Remote-/Pad-Schicht und kein Nexi-Kern.
6. AI-Studio ist Entwurf/Experiment. AI-Studio ist keine Produktionswahrheit.

## Aktiver Repo-Kern

Der aktive Kern besteht aus:

- `.github/workflows/build-native-apk.yml`
- `05_APP_ANDROID_NATIVE/`
- `backend/nexy/`
- `docs/`
- `AGENTS.md`
- `README.md`
- `NEXUS_CHANGE_DRAFT_LEDGER.md`
- `build.gradle`
- `settings.gradle`
- `.gitignore`

Alte Archive, Quarantaenen, generierte Reports und private lokale Daten gehoeren nicht in den aktiven Git-Kern.

## Verbindliche Doku-Anker

- `README.md`: kurzer Einstieg und aktive Struktur.
- `docs/architecture/NEXUS_CORE_ARCHITECTURE.md`: Rollen, Grenzen und produktive Architektur.
- `docs/nexy/NEXY_MEMORY_CORE.md`: Nexi-Memory-Modell und Datenregeln.
- `docs/nexy/NEXY_GESAMTKONZEPT.md`: Zielbild und Arbeitsanker fuer Nexi.
- `docs/rules/NEXUS_PROTECTED_CORE_POLICY.md`: Schutzklasse fuer Kernzustand.
- `docs/rules/WRITE_DOCUMENTATION_POLICY.md`: Dokumentationspflicht.

## Arbeitsreihenfolge

1. Ordnung herstellen.
2. Reparatur mit Belegen durchfuehren.
3. Ausbau erst nach stabiler Struktur.

Keine Funktionsreparatur darf alte Begriffe, Testartefakte oder archivierte Entwuerfe wieder als aktive Wahrheit einfuehren.

## Private Daten

`C:\MasterIndex_Storage` bleibt lokal. Private Daten, Tokens, Datenbanken, APK-Artefakte, lokale Logs und Legacy-Speicher werden nicht ins Repo kopiert.

Secrets werden nie in Git, Markdown, README, Logs oder generierte Reports geschrieben.

## Aenderungsregeln

- Kein Commit ohne ausdrueckliche Freigabe.
- Vor Entfernung aus Git muss ein externes Archiv existieren, wenn der entfernte Inhalt nicht bereits anderweitig gesichert ist.
- Riskante Aktionen brauchen nachvollziehbaren Vorher/Nachher-Zustand.
- Erfolg wird nur behauptet, wenn Status, Test oder Log ihn belegt.
