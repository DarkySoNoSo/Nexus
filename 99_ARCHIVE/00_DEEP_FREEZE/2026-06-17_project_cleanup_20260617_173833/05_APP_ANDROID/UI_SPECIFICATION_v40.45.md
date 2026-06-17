# NEXUS v40.45 - UI SPECIFICATION

## Scope

This file defines the accepted interface for the Android/Kivy Nexus Daemon.

The Daemon is not the full Nexus cockpit. It is the offline-capable mobile ingestion client.

## Source Of Truth For Network Defaults

The Android client must follow the live Nexus runtime files and Android Collector config, not old AI Studio mock data.

Current accepted endpoint order:

```text
1. http://192.168.1.216:8081
2. http://100.107.24.67:8081
```

Reason:

- `C:\MasterIndex_Storage\nexus_current_url.json` currently reports `http://192.168.1.216:8081/`.
- `NexusConfig.java` uses the same LAN host and the Tailscale fallback.
- Termux config lists both hosts.

## Screen Order

1. Header
2. Nexus server
3. Mobile quick input
4. Queue and sync
5. System log

## Header

Visible title:

```text
NEXUS MOBILE
```

States:

```text
ONLINE | <url>
OFFLINE | <url>
```

## Nexus Server

Default:

```text
http://192.168.1.216:8081
```

Fallback:

```text
http://100.107.24.67:8081
```

Rules:

- editable
- full width
- propagated into `OfflineSyncManager.server_url`
- successful fallback becomes active runtime URL

## Mobile Quick Input

Fields:

```text
Kategorie
Kurz benennen
Was soll Nexus speichern oder spaeter abgleichen?
```

Button:

```text
LOKAL SPEICHERN
```

Rules:

- title is required
- no fake example payloads
- save is local first
- save works without network
- queue counter updates immediately

## Queue And Sync

Counter:

```text
Queue: X
```

Button:

```text
ABGLEICHEN
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
Orange:       #ff6b14
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
- buttons max 36dp high
- no hero image
- no nested card stacks
- no oversized orange blocks
- no fake data examples

## Acceptance

Accepted only if:

1. Android fullscreen is used.
2. The app does not render as a tiny lower-left surface.
3. Offline save works.
4. Queue counter increases after save.
5. Missing Nexus server does not crash the app.
6. Sync errors are visible but non-destructive.
7. Default URL matches the current Nexus runtime config.
