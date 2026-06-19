#!/usr/bin/env python3
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "05_APP_ANDROID_NATIVE/app/src/main/java/com/nexus/collector/MainActivity.java"
BRIDGE = ROOT / "backend/nexy/nexy_bridge_api.py"
LEDGER = ROOT / "NEXUS_CHANGE_DRAFT_LEDGER.md"
POLICY = ROOT / "docs/release/APK_RELEASE_POLICY.md"
WORKFLOW = ROOT / ".github/workflows/build-native-apk.yml"

changed = []

def write_if_changed(path: Path, text: str):
    old = path.read_text(encoding="utf-8") if path.exists() else ""
    if old != text:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        changed.append(str(path.relative_to(ROOT)))


def patch_android():
    text = ANDROID.read_text(encoding="utf-8")
    original = text

    # WebView / web fallback route hardening. Keep Android native pages, but never point WebView buttons at dead legacy routes.
    replacements = {
        'showWebPage("/communication")': 'showWebPage("/api/communication/conversations?limit=200")',
        'showWebPage("/files")': 'showWebPage("/api/files/list?path=&limit=200")',
        'showWebPage("/nexy")': 'showWebPage("/api/nexy/briefing")',
        'showWebPage("/timeline")': 'showWebPage("/api/nexy/timeline?limit=50")',
        'showWebPage("/status")': 'showWebPage("/api/nexy/status")',
        'showWebPage("/communication?': 'showWebPage("/api/communication/conversations?',
        'showWebPage("/files?': 'showWebPage("/api/files/list?',
    }
    for old, new in replacements.items():
        text = text.replace(old, new)

    # Pet: keep free movement and current transparency, but make non-interactive intent explicit.
    marker = "frame.addView(dragonWanderer(), new FrameLayout.LayoutParams(\n                FrameLayout.LayoutParams.MATCH_PARENT,\n                FrameLayout.LayoutParams.MATCH_PARENT\n        ));"
    if marker in text and "View petOverlay = dragonWanderer();" not in text:
        text = text.replace(marker, "View petOverlay = dragonWanderer();\n        petOverlay.setClickable(false);\n        petOverlay.setFocusable(false);\n        petOverlay.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);\n        frame.addView(petOverlay, new FrameLayout.LayoutParams(\n                FrameLayout.LayoutParams.MATCH_PARENT,\n                FrameLayout.LayoutParams.MATCH_PARENT\n        ));")

    old_status = '''    private void showStatusOnly() {
        clearPage(PAGE_STATUS, "Status", "Aktueller App- und Collector-Zustand.");
        activePanel().addView(label(status(), 13, false, Color.rgb(230, 225, 216)));
    }
'''
    new_status = '''    private void showStatusOnly() {
        clearPage(PAGE_STATUS, "Status", "Aktueller App- und Collector-Zustand.");
        LinearLayout p = activePanel();
        p.addView(section("ZUGRIFFE"));
        p.addView(label(permissionStatusLine(), 13, false, Color.rgb(230, 225, 216)));
        row(p,
                nav("Notify Zugriff oeffnen", v -> openNotificationAccessSettings()),
                nav("SMS erlauben", v -> requestSmsPermission())
        );
        if (Build.VERSION.SDK_INT >= 33) {
            row(p, nav("Post Notifications", v -> requestPostNotificationsPermission()));
        }
        row(p, nav("Widget aktualisieren", v -> { NexusMessagesWidgetProvider.updateAll(this); showStatusOnly(); }));
        p.addView(section("SYSTEM"));
        p.addView(label(status(), 13, false, Color.rgb(230, 225, 216)));
    }
'''
    if old_status in text:
        text = text.replace(old_status, new_status)

    helper_anchor = "    private void showDragonPage() {\n"
    helpers = r'''    private String permissionStatusLine() {
        StringBuilder sb = new StringBuilder();
        sb.append("Benachrichtigungszugriff: ").append(notificationListenerEnabled() ? "aktiv" : "fehlt").append("\n");
        sb.append("SMS: ").append(smsPermissionGranted() ? "erlaubt" : "fehlt").append("\n");
        if (Build.VERSION.SDK_INT >= 33) {
            sb.append("Post Notifications: ").append(postNotificationsGranted() ? "erlaubt" : "fehlt").append("\n");
        }
        sb.append("Paket: ").append(getPackageName()).append("\n");
        sb.append("Hinweis: Zugriffe bleiben nur stabil, wenn neue APKs als Update mit gleicher Signatur installiert werden.");
        return sb.toString();
    }

    private boolean notificationListenerEnabled() {
        try {
            String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
            return enabled != null && enabled.toLowerCase(Locale.ROOT).contains(getPackageName().toLowerCase(Locale.ROOT));
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean smsPermissionGranted() {
        try {
            return checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private boolean postNotificationsGranted() {
        try {
            if (Build.VERSION.SDK_INT < 33) return true;
            return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void openNotificationAccessSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Throwable t) {
            try { startActivity(new Intent(Settings.ACTION_SETTINGS)); } catch (Throwable ignored) {}
        }
    }

    private void requestSmsPermission() {
        try {
            if (!smsPermissionGranted() && Build.VERSION.SDK_INT >= 23) {
                requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, 2001);
            } else {
                showStatusOnly();
            }
        } catch (Throwable ignored) {
            showStatusOnly();
        }
    }

    private void requestPostNotificationsPermission() {
        try {
            if (Build.VERSION.SDK_INT >= 33 && !postNotificationsGranted()) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2002);
            } else {
                showStatusOnly();
            }
        } catch (Throwable ignored) {
            showStatusOnly();
        }
    }

'''
    if "private String permissionStatusLine()" not in text and helper_anchor in text:
        text = text.replace(helper_anchor, helpers + helper_anchor)

    write_if_changed(ANDROID, text)


def patch_bridge():
    text = BRIDGE.read_text(encoding="utf-8")
    original = text

    if "def communication_search(" not in text:
        anchor = "def communication_counters(items):\n"
        func = r'''def communication_search(query, limit=2000):
    q = clean_text(query)
    if not q:
        return []
    like = f"%{q}%"
    rows = db_rows("""
        SELECT id, created_at, event_time, source, source_ref, event_type, title, body, raw_payload, importance, confidence, status
        FROM nexy_events
        WHERE status != 'deleted'
          AND (title LIKE ? OR body LIKE ? OR source LIKE ? OR event_type LIKE ? OR raw_payload LIKE ?)
        ORDER BY COALESCE(event_time, created_at) DESC
        LIMIT ?
    """, (like, like, like, like, like, limit))
    decisions = latest_decisions(rows)
    return [render_event(row, decisions) for row in rows]

'''
        if anchor in text:
            text = text.replace(anchor, func + anchor)

    search_route_anchor = "            elif u.path == \"/api/communication/events\":\n                self.send_json({\"ok\": True, \"items\": communication_events(limit)})\n"
    if "/api/communication/search" not in text and search_route_anchor in text:
        text = text.replace(search_route_anchor, search_route_anchor + "            elif u.path == \"/api/communication/search\":\n                q = qs.get(\"q\", [\"\"])[0].strip()\n                if not q:\n                    self.send_json({\"ok\": False, \"error\": \"missing query parameter q\"}, 400)\n                else:\n                    items = communication_search(q, limit)\n                    self.send_json({\"ok\": True, \"query\": q, \"items\": items, \"count\": len(items)})\n")

    alias_anchor = "            elif u.path == \"/api/nexy/search\":\n                q = qs.get(\"q\", [\"\"])[0].strip()\n                if not q:\n                    self.send_json({\"ok\": False, \"error\": \"missing query parameter q\"}, 400)\n                else:\n                    self.send_json({\"ok\": True, \"result\": search(q, limit)})\n            else:\n                self.send_json({\"ok\": False, \"error\": \"not found\", \"path\": u.path}, 404)\n"
    if "u.path == \"/communication\"" not in text and alias_anchor in text:
        alias_block = "            elif u.path == \"/api/nexy/search\":\n                q = qs.get(\"q\", [\"\"])[0].strip()\n                if not q:\n                    self.send_json({\"ok\": False, \"error\": \"missing query parameter q\"}, 400)\n                else:\n                    self.send_json({\"ok\": True, \"result\": search(q, limit)})\n            elif u.path == \"/communication\":\n                items = communication_conversations(limit)\n                self.send_json({\"ok\": True, \"page\": \"communication\", \"items\": items, \"counters\": communication_counters(items)})\n            elif u.path == \"/files\":\n                rel_path = qs.get(\"path\", [\"\"])[0]\n                try:\n                    self.send_json({\"ok\": True, \"page\": \"files\", \"data\": file_listing(rel_path, limit)})\n                except (ValueError, FileNotFoundError) as e:\n                    self.send_json({\"ok\": False, \"error\": str(e), \"path\": rel_path}, 400)\n            elif u.path == \"/timeline\":\n                self.send_json({\"ok\": True, \"page\": \"timeline\", \"items\": timeline(limit)})\n            elif u.path == \"/nexi\":\n                self.send_json({\"ok\": True, \"page\": \"nexi\", \"briefing\": briefing()})\n            elif u.path == \"/status\":\n                self.send_json({\"ok\": True, \"page\": \"status\", \"counts\": counts()})\n            else:\n                self.send_json({\"ok\": False, \"error\": \"not found\", \"path\": u.path}, 404)\n"
        text = text.replace(alias_anchor, alias_block)

    write_if_changed(BRIDGE, text)


def patch_policy():
    policy = '''# Nexus APK Release Policy

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
'''
    write_if_changed(POLICY, policy)


def patch_workflow():
    if not WORKFLOW.exists():
        return
    text = WORKFLOW.read_text(encoding="utf-8")
    if "Verify APK identity" not in text:
        old = '''      - name: Collect APKs
        run: |
          mkdir -p out
          cp 05_APP_ANDROID_NATIVE/app/build/outputs/apk/debug/app-debug.apk out/Nexus-Nexi-v1.6.176-debug.apk
          ls -lh out
'''
        new = '''      - name: Verify APK identity
        run: |
          APK="05_APP_ANDROID_NATIVE/app/build/outputs/apk/debug/app-debug.apk"
          test -s "$APK"
          aapt dump badging "$APK" | tee apk-badging.txt
          grep -q "package: name='com.nexus.chefnative'" apk-badging.txt
          grep -q "versionCode='176'" apk-badging.txt
          grep -q "versionName='1.6.176-usable-ui'" apk-badging.txt
          aapt dump xmltree "$APK" AndroidManifest.xml | tee apk-manifest.txt
          grep -q "com.nexus.collector.MainActivity" apk-manifest.txt
          grep -q "com.nexus.collector.NexusSmsReceiver" apk-manifest.txt
          grep -q "com.nexus.collector.NexusNotificationListener" apk-manifest.txt
          grep -q "com.nexus.collector.NexusMessagesWidgetProvider" apk-manifest.txt

      - name: Collect APKs
        run: |
          mkdir -p out
          cp 05_APP_ANDROID_NATIVE/app/build/outputs/apk/debug/app-debug.apk out/Nexus-Nexi-v1.6.176-debug.apk
          ls -lh out
'''
        if old in text:
            text = text.replace(old, new)
            write_if_changed(WORKFLOW, text)


def patch_ledger():
    if not LEDGER.exists():
        return
    text = LEDGER.read_text(encoding="utf-8")
    entry = "| DRAFT-080 | Android Repair Phase A | Stabilize Web routing, permission repair entry points, APK identity checks, and non-blocking free Pet overlay. | build-pending | Repair branch `repair/2026-06-19-stabilize-routing-permissions`; backup branch `backup/2026-06-19-before-repair`; GitHub Action must run `:app:assembleDebug` and `:app:lintDebug` before merge. |"
    if "DRAFT-080 | Android Repair Phase A" not in text:
        text = text.replace("\n## Recent Local Ledger Sections Present In Canonical File\n", "\n" + entry + "\n\n## Recent Local Ledger Sections Present In Canonical File\n")
        write_if_changed(LEDGER, text)


def run_checks():
    checks = [
        ["python3", "-m", "py_compile", "backend/nexy/nexy_bridge_api.py"],
        ["gradle", ":app:assembleDebug", "--stacktrace"],
        ["gradle", ":app:lintDebug", "--stacktrace"],
    ]
    for cmd in checks:
        print("RUN", " ".join(cmd), flush=True)
        subprocess.run(cmd, cwd=ROOT, check=True)


def main():
    patch_android()
    patch_bridge()
    patch_policy()
    patch_workflow()
    patch_ledger()
    if changed:
        print("Changed files:")
        for item in changed:
            print("-", item)
    else:
        print("No changes needed.")
    if "--check" in sys.argv:
        run_checks()

if __name__ == "__main__":
    main()
