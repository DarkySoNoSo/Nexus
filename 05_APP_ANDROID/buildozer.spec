[app]

# (str) Title of your application
title = Nexus Daemon

# (str) Application version required by Buildozer
version = 1.5.4

# (str) Package name
package.name = nexus_daemon

# (str) Package domain (needed for android package name)
package.domain = org.nexus

# (str) Source code where the main.py lives
source.dir = .

# (str) List of inclusions - file extensions to include in the APK
source.include_exts = py, png, jpg, kv, atlas, spec, db

# (list) Application requirements
# Installs these packages dynamically during compilation inside the APK sandbox!
requirements = python3, kivy, requests, charset-normalizer, idna, urllib3

# (str) Supported orientations (landscape, portrait or all)
orientation = portrait

# (bool) Use fullscreen or not
fullscreen = 1

# (list) Permissions required for the application
# Standard Nexus Permissions (Ingestion over Network, secure local storage, and log camera)
android.permissions = INTERNET, WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, CAMERA, ACCESS_NETWORK_STATE

# (int) Android API target level (matching Patrick's Ledger requirements)
android.api = 33

# (int) Minimum API required (Android 8.0+)
android.minapi = 26

# (int) Android NDK API level. Keep this aligned with android.minapi for python-for-android packaging.
android.ndk_api = 26

# (bool) Use private storage for sqlite files (True prevents permission issues)
android.private_storage = True

# (list) Supported architectures (ARM64 standard for modern Android devices)
android.archs = arm64-v8a

# (str) Icon file
# icon.filename = %(source.dir)s/icon.png

# (str) Presets to compile standard debug build or release build
# For a quick test build: buildozer android debug
p4a.branch = master

[buildozer]
# (int) Log level (0 = error only, 1 = info, 2 = debug and stdout output)
log_level = 2

# (int) Display warning if buildozer is run as root (0 = False, 1 = True)
warn_on_root = 1