# Nexus

Nexus ist das aktive Geraest fuer Android-App, Backend-Anbindung und Dokumentation.

Nexi ist das einzige Hirn des Systems. Nexi haelt Kontext, Memory, Recall, Zeitstrahl, Entscheidungen, Review und Lernregeln. Die Begriffe Chef, Master und Index-Chef sind Legacy-Funktionen innerhalb von Nexi.

## Aktiver Kern

- `.github/workflows/build-native-apk.yml`
- `05_APP_ANDROID_NATIVE/`
- `backend/nexy/`
- `docs/`
- `AGENTS.md`
- `NEXUS_CHANGE_DRAFT_LEDGER.md`
- `build.gradle`
- `settings.gradle`
- `.gitignore`

## Grenzen

- Dragon ist separat.
- DigiPad ist separat.
- AI-Studio ist Entwurf, nicht Produktionswahrheit.
- Private Daten aus `C:\MasterIndex_Storage` bleiben lokal und werden nicht ins Repo kopiert.

## Zentrale Dokumente

- [Architektur](docs/architecture/NEXUS_CORE_ARCHITECTURE.md)
- [Nexi Memory Core](docs/nexy/NEXY_MEMORY_CORE.md)
- [Nexi Gesamtkonzept](docs/nexy/NEXY_GESAMTKONZEPT.md)
- [Modulregister](docs/NEXUS_MODULE_REGISTRY.md)

## Gold-Regel

Erst Ordnung, dann Reparatur, dann Ausbau. Die Android-App muss ohne Backend starten koennen; Serverzugriffe erfolgen erst nach Benutzeraktion oder klarer Laufzeitentscheidung.
