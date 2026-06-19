# Nexus APK Release Policy

Status: verbindlich fuer Update-Stabilitaet  
Schutzklasse: GELB/ORANGE  
Stand: 2026-06-19

## Ziel

Neue APK-Versionen duerfen Android-Zugriffe nicht verlieren. Die App muss als Update derselben App installiert werden koennen.

## Unveraenderbare Identitaet

```text
applicationId = com.nexus.chefnative
```

Diese ID darf nicht fuer Design-, Namens- oder Flavor-Aenderungen geaendert werden.

## Signaturregel

- Debug-APKs sind Testartefakte.
- Installierbare Haupt-APKs muessen dauerhaft mit derselben Signatur gebaut werden.
- Keystore/Passwoerter gehoeren nicht ins Repo.
- Release-Signing erfolgt lokal oder ueber GitHub Secrets.

## Update-Smoke

Vor einer Aussage wie "Update stabil" muss geprueft werden:

```text
alte App installiert lassen
neue APK darueber installieren
Package identisch
Signatur kompatibel
Notify bleibt aktiv
SMS bleibt aktiv
Bridge-URL bleibt gespeichert
Dragon-State bleibt gespeichert
Outbox bleibt erhalten
```

## Build-Pruefung

Jeder APK-Build muss mindestens pruefen:

```text
APK existiert
APK ist nicht leer
aapt dump badging erfolgreich
package == com.nexus.chefnative
versionCode/versionName stimmen
Manifest enthaelt MainActivity, SMS Receiver, Notification Listener und Widget Provider
```

## Verbot

```text
Keine neue applicationId.
Keine Flavor-APK als Hauptupdate.
Keine Secrets im Repo.
Keine Aussage "UI getestet", wenn die APK nicht installiert und geoeffnet wurde.
```
