# NEXUS v40.45 - UI SPECIFICATION

## Scope

This file defines the accepted interface for the Android/Kivy Nexus Daemon.

The Daemon is not the full Nexus cockpit. It is the offline-capable mobile ingestion client.

## Screen Order

1. Header
2. Master URL
3. Mobile quick ingest
4. Queue and sync
5. Diagnostic terminal

## Header

Visible title:

```text
NEXUS DAEMON v40.45
```

States:

```text
[ ONLINE ] Verbunden mit Master: <url>
[ OFFLINE ] Keine Verbindung zu: <url>
```

## Master URL

Default:

```text
http://100.115.92.2:8081
```

Rules:

- editable
- full width
- propagated into `OfflineSyncManager.server_url`

## Mobile Quick Ingest

Fields:

```text
Kategorie
Titel / Quelle
Beschreibung / Wert
```

Button:

```text
IN DIE OFFLINE-QUEUE SPEICHERN
```

Rules:

- title is required
- save is local first
- save works without network
- queue counter updates immediately

## Queue And Sync

Counter:

```text
Queue: X Eintraege
```

Button:

```text
JETZT ABGLEICHEN
```

Allowed statuses:

```text
PENDING
SYNCHRONIZING
ERROR_RETRY
COMPLETED
```

## Local Database

File:

```text
nexus_offline.db
```

Android path:

```text
self.user_data_dir/nexus_offline.db
```

Current compatibility table:

```text
sync_queue
```

The previous `sync_queue` table may remain to avoid data migration risk. Its behavior must match the accepted status model.

## Design

Theme:

```text
Cosmic Industrial Black
```

Colors:

```text
Background:   #000000
Panel:        #05070d
Panel 2:      #0a0d14
Border:       #14243b
Neon Cyan:    #00c0ff
Ingest Green: #009b4c
Sync Blue:    #0066cc
Error Red:    #ff4a4a
Text:         #f4f7fb
Subtext:      #9aa7b8
```

Layout constraints:

- no fixed Android `Window.size`
- desktop simulator size only outside Android/iOS
- root layout must fill screen
- portrait scroll must work
- buttons max 40dp high
- no hero image
- no nested card stacks
- no oversized orange blocks

## Acceptance

Accepted only if:

1. Android fullscreen is used.
2. The app does not render as a tiny lower-left surface.
3. Offline save works.
4. Queue counter increases after save.
5. Missing Nexus server does not crash the app.
6. Sync errors are visible but non-destructive.
