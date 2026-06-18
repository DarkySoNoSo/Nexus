# Safe Start Rules

Verboten beim App-Start:

- HTTP GET
- HTTP POST
- Widget-Refresh aus MainActivity
- Serverstatus laden
- Nachrichten laden
- Dateien laden
- Zeitstrahl laden
- Nexi-Kanal laden
- Token erzwingen
- Backend erzwingen

Erlaubt beim App-Start:

- UI rendern
- Theme laden
- gespeicherte Einstellungen anzeigen
- lokale Rechte anzeigen
- Hinweis "Backend offline möglich"
