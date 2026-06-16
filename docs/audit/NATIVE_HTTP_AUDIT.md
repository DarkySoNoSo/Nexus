# Native HTTP Audit

Stand: Tue Jun 16 20:26:11 CEST 2026

Gefundene HTTP-Stellen in MainActivity:

- /api/widget/messages?limit=20
- /api/communication/events?limit=2000
- /api/widget/message-action
- /api/v1/files/folders
- /api/timeline?limit=80
- /api/communication/chef-log
- /api/mobile/chef-chat
- /api/widget/messages?limit=1

Bewertung:

Diese Calls sind akzeptabel, solange sie nicht aus onCreate() oder showHome() automatisch gestartet werden.

Nächster Prüfpunkt:

- onCreate()
- showHome()
- alle Button-Handler
