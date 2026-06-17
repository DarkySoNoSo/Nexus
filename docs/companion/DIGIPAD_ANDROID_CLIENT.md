# DigiPad Android Client

## Zweck

Die DigiPad-APK ist ein eigener Remote-Client für das Fiona-Profil.
Sie ist von der Nexus-Admin-APK getrennt.

## APKs

- Nexus Admin APK: com.nexus.chefnative
- DigiPad Fiona APK: com.nexus.digipad

## Sicherheitsgrenze

DigiPad benutzt nur:

- /api/pad/fiona/status
- /api/pad/fiona/feed
- /api/pad/fiona/care
- /api/pad/fiona/train
- /api/pad/fiona/freefight
- /api/pad/fiona/arena
- /api/pad/fiona/evolve
- /api/pad/fiona/battle/export

DigiPad enthält keine sichtbaren Nexus-Menüs und keine SMS-/Notification-/Collector-Komponenten im Flavor-Manifest.

## Verbindung

Auf Patricks Host:

- DigiPad Container API: Port 8788

Auf Fionas Handy:

- gleiche WLAN-IP oder Tailscale-IP von Patricks Host eintragen
- Token aus Patricks `.run/fiona_digipad_token.txt` einmalig lokal eintragen

## Wichtig

127.0.0.1 ist immer das jeweilige Gerät selbst.
Fionas Handy nutzt daher nicht 127.0.0.1 von Patricks Gerät, sondern dessen LAN- oder Tailscale-Adresse.
