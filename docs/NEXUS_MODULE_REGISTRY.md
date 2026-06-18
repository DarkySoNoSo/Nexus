# Nexus Modulregister

Status: offizieller Ordnungsanker fuer Nexus und Nexi.

## Grundregel

Keine neue Funktion ohne Modulplatz, Healthcheck und Rollback-Pfad.

## Aktive Module

| Modul | Rolle | Pfad | Status |
|---|---|---|---|
| Nexus Android | mobile Shell und Bedienoberflaeche | `05_APP_ANDROID_NATIVE/` | aktiv |
| Nexi | einziges Hirn: Memory, Kontext, Recall, Zeitstrahl, Entscheidungen | `backend/nexy/` | aktiv |
| Dokumentation | Architektur, Regeln, Memory, Grenzen | `docs/` | aktiv |
| Build | native APK Pipeline | `.github/workflows/build-native-apk.yml` | aktiv |

## Separierte Bereiche

| Bereich | Status | Regel |
|---|---|---|
| Dragon | separat | nicht als aktiver Nexus-Kern behandeln |
| DigiPad | separat | eigene Remote-/Pad-Schicht |
| AI-Studio | Entwurf | keine Produktionswahrheit |
| Chef/Master/Index-Chef | Legacy innerhalb Nexi | keine parallele Hirn-Architektur |

## Harte Grenzen

- Nexus = Geraest.
- Nexi = einziges Hirn.
- Private Daten bleiben in `C:\MasterIndex_Storage` und werden nicht committed.
- Alte Archive, Quarantaenen und generierte Reports gehoeren nicht in den aktiven Kern.
- Android-App muss offline starten.
- Kein Servercall beim App-Start.
- Jede Bridge braucht eigenen Healthcheck, bevor sie als aktiv gilt.
