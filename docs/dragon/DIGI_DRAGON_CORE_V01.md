# Digi Dragon Core v0.1

Digi Dragon Core v0.1 ist der lokale Companion-Kern für Nexus.

## Enthalten

- lokaler Drache
- SQLite-DB
- Status
- Füttern
- Pflege
- Training
- Freikämpfe
- Arena
- Evolution
- Attacken
- Habitat
- Bridge API

## Rollenreinheit

- Nexus = Gesamtsystem
- Nexy = Gedächtnis / Kontext / Recall
- Chef = Analyse / Ausführung
- Collector = Rohdaten
- Digi Dragon = Companion-/Game-Schicht

Keine automatische Chef-Ausführung.
Keine automatischen API-Kosten.
Safe-Start bleibt unberührt.

## CLI

python backend/companion/dragon_cli.py status
python backend/companion/dragon_cli.py feed
python backend/companion/dragon_cli.py care
python backend/companion/dragon_cli.py train focus
python backend/companion/dragon_cli.py freefight
python backend/companion/dragon_cli.py arena
python backend/companion/dragon_cli.py evolve
python backend/companion/dragon_cli.py codex

## Bridge

./tools/start_digi_dragon_bridge.sh
