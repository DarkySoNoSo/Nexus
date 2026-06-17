# Nexus v40.44 - Android APK Kompiliervorlage & Master-Anleitung
Dieses Handbuch erklärt, wie du den Quellcode in `/05_APP_ANDROID` zu einer physischen, installierbaren Android-App (`.apk`) für dein Handy kompilierst.

Da du den Code auf deinem Smartphone ausführen willst, haben wir für dich eine **schlüsselfertige Cloud-Kompilierung** über GitHub Actions integriert, die dir vollkommen kostenfrei und automatisch dein APK baut.

---

## METHODE A: Automatische Cloud-Kompilierung über GitHub (Empfohlen!)
Du benötigst auf deinem PC **keine** schwere Android-SDK-, NDK- oder Java-Installation. GitHub erledigt den Build-Vorgang für dich in einer isolierten Serverumgebung und stellt dir das fertige APK zum Download bereit.

### Schritt-für-Schritt Anleitung:
1. Verbinde dieses Repository mit deinem privaten GitHub-Account (über das AI Studio Einstellungsmenü oder lade den Code als ZIP herunter und lade ihn bei GitHub hoch).
2. Erstelle in deinem Repository eine neue Datei unter dem Pfad:
   `.github/workflows/android.yml`
3. Kopiere den folgenden YAML-Code vollständig in diese Datei:

```yaml
name: Build Nexus Android Daemon APK

on:
  push:
    branches: [ "main", "master" ]
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout Repository
      uses: actions/checkout@v3

    - name: Set up JDK 17
      uses: actions/setup-java@v3
      with:
        distribution: 'temurin'
        java-version: '17'

    - name: Set up Python
      uses: actions/setup-python@v4
      with:
        python-version: '3.10'

    - name: Install Dependencies
      run: |
        sudo apt-get update
        sudo apt-get install -y libsqlite3-dev build-essential ccache git libffi-dev libssl-dev
        pip install --upgrade pip
        pip install buildozer cython gitpython

    - name: Build Android Debug APK
      run: |
        cd 05_APP_ANDROID
        buildozer android debug
      env:
        # Erlaubt Buildozer als Root auf GitHub Runnern zu laufen
        BUILDOZER_WARN_ON_ROOT: 0

    - name: Upload Compile Artifact
      uses: actions/upload-artifact@v3
      with:
        name: Nexus_Daemon_APK
        path: 05_APP_ANDROID/bin/*.apk
```

4. Sobald du den Code pushst oder die Datei speicherst, startet GitHub im Tab **"Actions"** den Compilation-Job.
5. Nach ca. 10-15 build-Minuten kannst du unter dem erfolgreichen Durchlauf das fertige Paket **`Nexus_Daemon_APK`** anklicken, herunterladen, per USB oder Telegram auf dein Handy übertragen und sofort installieren!

---

## METHODE B: Lokale Kompilierung über Docker (Alternative)
Wenn du Linux oder Windows mit Docker besitzt, kannst du das APK lokal bauen, ohne deine eigene Systemumgebung mit Android-Tools zu belasten.

### Schritt-für-Schritt Anleitung:
1. Öffne ein Terminal im Verzeichnis `/05_APP_ANDROID`.
2. Führe den folgenden Docker-Befehl aus, welcher ein vorbereitetes Buildozer-Image lädt und die Kompilierung startet:

```bash
docker run --rm -v "$(pwd)":/home/user/hostdir -it hergert/buildozer android debug
```

3. Das fertige APK wird nach dem Durchlauf direkt in deinem lokalen Ordner `/05_APP_ANDROID/bin/` abgelegt.

---

## ARCHITEKTUR-HINWEIS FÜR INTEGRATIONS-COMMITS:
* **Tailscale-Fahrrinne**: Das APK sucht standardmäßig nach deinem PC unter der IP `100.115.92.2` (oder deiner entsprechenden Tailscale-IP). Du kannst die IP direkt im Textfeld der App ändern, wenn dein Host eine andere VPN-Adresse besitzt!
* **Offline-Sicherheit (Double-Entry Queue)**: Sollte das Handy offline sein oder das WLAN abbrechen, puffert die App alle deine SMS-, Bildbelege und Eingaben in der hochsicheren lokalen Datenbank `nexus_offline.db`. Erst beim Klick auf **"JETZT ABGLEICHEN"** und bei bestehender Verbindung wird die Queue transaktionssicher per HMAC-Signatur an deine Master-API übertragen!
