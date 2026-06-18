# DigiPad iPhone Webclient

## Zweck

Der DigiPad iPhone Webclient ist keine native iOS-App.
Er ist eine Web-/PWA-Oberfläche auf Port 8788.

## Start

DigiPad hat keinen aktiven Sammelstartpfad im bereinigten Repo.
DigiPad ist aktuell als Android-Flavour im Native-Projekt und als getrennte Remote-Client-Architektur dokumentiert.

## URL

Lokal:
http://127.0.0.1:8788/digipad

Fionas iPhone im gleichen WLAN:
http://<LAN-IP-von-Patricks-Host>:8788/digipad

Tailscale später:
http://<Tailscale-IP-von-Patricks-Host>:8788/digipad

## Token

Der Fiona-Token liegt lokal auf Patricks Host:
.run/fiona_digipad_token.txt

## iPhone Home-Bildschirm

1. Safari öffnen
2. URL öffnen
3. Teilen-Symbol antippen
4. Zum Home-Bildschirm wählen
5. Name: DigiPad
6. Hinzufügen

## Sicherheitsgrenze

Der Webclient nutzt nur:
/api/pad/fiona/*

Er nutzt nicht:
Nexy
Collector
Nachrichten
Dateien
Timeline
Admin
