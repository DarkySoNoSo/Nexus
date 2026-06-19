#!/usr/bin/env python3
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
ANDROID = ROOT / "05_APP_ANDROID_NATIVE/app/src/main/java/com/nexus/collector/MainActivity.java"
BRIDGE = ROOT / "backend/nexy/nexy_bridge_api.py"
LEDGER = ROOT / "NEXUS_CHANGE_DRAFT_LEDGER.md"
POLICY = ROOT / "docs/release/APK_RELEASE_POLICY.md"

changed = []

def read(path: Path) -> str:
    return path.read_text(encoding="utf-8") if path.exists() else ""

def write_if_changed(path: Path, text: str) -> None:
    old = read(path)
    if old != text:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text, encoding="utf-8")
        changed.append(str(path.relative_to(ROOT)))

def patch_android() -> None:
    text = read(ANDROID)

    replacements = {
        'row(p, nav("Cockpit", v -> loadWeb("/")), nav("Kommunikation", v -> loadWeb("/communication")));':
            'row(p, nav("Cockpit", v -> loadWeb("/api/nexy/status")), nav("Kommunikation", v -> loadWeb("/api/communication/conversations?limit=200")));',
        'row(p, nav("Dateien", v -> loadWeb("/files")), nav("Nexi", v -> loadWeb("/chef")));':
            'row(p, nav("Dateien", v -> loadWeb("/api/files/list?path=&limit=200")), nav("Nexi", v -> loadWeb("/api/nexy/briefing")));',
        'webView.loadUrl(NexusConfig.baseUrl(this) + (path == null || path.isEmpty() ? "/" : path));':
            'webView.loadUrl(nexyBridgeBase() + (path == null || path.isEmpty() ? "/" : path));',
    }
    for old, new in replacements.items():
        text = text.replace(old, new)

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
        p.addView(label(permissionRepairText(), 13, false, Color.rgb(230, 225, 216)));
        row(p,
                nav("Notify Zugriff oeffnen", v -> openNotificationAccess()),
                nav("SMS erlauben", v -> requestSms())
        );
        if (Build.VERSION.SDK_INT >= 33) {
            row(p, nav("Post Notifications", v -> requestPostNotifications()));
        }
        row(p, nav("Widget aktualisieren", v -> { NexusMessagesWidgetProvider.updateAll(this); showStatusOnly(); }));
        p.addView(section("SYSTEM"));
        p.addView(label(status(), 13, false, Color.rgb(230, 225, 216)));
    }
'''
    if old_status in text:
        text = text.replace(old_status, new_status)

    helper_anchor = '''    private void sendTestEvent() {
'''
    helpers = '''    private boolean postNotificationsPermission() {
        return Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPostNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !postNotificationsPermission()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1002);
        } else {
            showStatusOnly();
        }
    }

    private String permissionRepairText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Benachrichtigungszugriff: ").append(notificationAccess() ? "aktiv" : "fehlt").append("\\n");
        sb.append("SMS: ").append(smsPermission() ? "erlaubt" : "fehlt").append("\\n");
        if (Build.VERSION.SDK_INT >= 33) {
            sb.append("Post Notifications: ").append(postNotificationsPermission() ? "erlaubt" : "fehlt").append("\\n");
        }
        sb.append("Paket: ").append(getPackageName()).append("\\n");
        sb.append("Wichtig: Zugriffe bleiben nur stabil, wenn neue APKs als Update mit gleicher Signatur installiert werden.");
        return sb.toString();
    }

'''
    if "private String permissionRepairText()" not in text and helper_anchor in text:
        text = text.replace(helper_anchor, helpers + helper_anchor)

    write_if_changed(ANDROID, text)

def patch_bridge() -> None:
    text = read(BRIDGE)

    if "def communication_search(" not in text:
        anchor = "def communication_counters(items):\n"
        func = '''def communication_search(query, limit=2000):
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
        text = text.replace(anchor, func + anchor)

    old_events = '''            elif u.path == "/api/communication/events":
                self.send_json({"ok": True, "items": communication_events(limit)})
'''
    new_events = '''            elif u.path == "/api/communication/events":
                self.send_json({"ok": True, "items": communication_events(limit)})
            elif u.path == "/api/communication/search":
                q = qs.get("q", [""])[0].strip()
                if not q:
                    self.send_json({"ok": False, "error": "missing query parameter q"}, 400)
                else:
                    items = communication_search(q, limit)
                    self.send_json({"ok": True, "query": q, "items": items, "count": len(items)})
'''
    if "/api/communication/search" not in text:
        text = text.replace(old_events, new_events)

    old_tail = '''            elif u.path == "/api/nexy/search":
                q = qs.get("q", [""])[0].strip()
                if not q:
                    self.send_json({"ok": False, "error": "missing query parameter q"}, 400)
                else:
                    self.send_json({"ok": True, "result": search(q, limit)})
            else:
                self.send_json({"ok": False, "error": "not found", "path": u.path}, 404)
'''
    new_tail = '''            elif u.path == "/api/nexy/search":
                q = qs.get("q", [""])[0].strip()
                if not q:
                    self.send_json({"ok": False, "error": "missing query parameter q"}, 400)
                else:
                    self.send_json({"ok": True, "result": search(q, limit)})
            elif u.path == "/communication":
                items = communication_conversations(limit)
                self.send_json({"ok": True, "page": "communication", "items": items, "counters": communication_counters(items)})
            elif u.path == "/files":
                rel_path = qs.get("path", [""])[0]
                try:
                    self.send_json({"ok": True, "page": "files", "data": file_listing(rel_path, limit)})
                except (ValueError, FileNotFoundError) as e:
                    self.send_json({"ok": False, "error": str(e), "path": rel_path}, 400)
            elif u.path == "/timeline":
                self.send_json({"ok": True, "page": "timeline", "items": timeline(limit)})
            elif u.path == "/nexi":
                self.send_json({"ok": True, "page": "nexi", "briefing": briefing()})
            elif u.path == "/status":
                self.send_json({"ok": True, "page": "status", "counts": counts()})
            else:
                self.send_json({"ok": False, "error": "not found", "path": u.path}, 404)
'''
    if 'u.path == "/communication"' not in text:
        text = text.replace(old_tail, new_tail)

    write_if_changed(BRIDGE, text)

def patch_policy() -> None:
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

def patch_ledger() -> None:
    text = read(LEDGER)
    entry = "| DRAFT-080 | Android Repair Phase A | Stabilize Web routing, permission repair entry points, APK identity policy, and non-blocking free Pet overlay. Build-workflow mutation intentionally excluded from Action commit because GitHub blocks workflow file writes without workflows permission. | build-pending | Repair branch `repair/2026-06-19-stabilize-routing-permissions`; backup branch `backup/2026-06-19-before-repair`; validation requires `:app:assembleDebug` and `:app:lintDebug`. |"
    if "DRAFT-080 | Android Repair Phase A" not in text:
        anchor = "\n## Recent Local Ledger Sections Present In Canonical File\n"
        if anchor in text:
            text = text.replace(anchor, "\n" + entry + "\n" + anchor)
        else:
            text = text.rstrip() + "\n\n" + entry + "\n"
        write_if_changed(LEDGER, text)

def run_checks() -> None:
    commands = [
        ["python3", "-m", "py_compile", "backend/nexy/nexy_bridge_api.py"],
        ["gradle", ":app:assembleDebug", "--stacktrace"],
        ["gradle", ":app:lintDebug", "--stacktrace"],
    ]
    for cmd in commands:
        print("RUN", " ".join(cmd), flush=True)
        subprocess.run(cmd, cwd=ROOT, check=True)

def main() -> None:
    patch_android()
    patch_bridge()
    patch_policy()
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
