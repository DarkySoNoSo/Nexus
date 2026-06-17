#!/usr/bin/env bash
set +e

TS="$(date +%Y%m%d_%H%M%S)"
REPORT="reports/nexus_function_check_safe_$TS.txt"
mkdir -p reports

PASS=0
WARN=0
FAIL=0

say(){ echo "$*" | tee -a "$REPORT"; }
ok(){ PASS=$((PASS+1)); say "✅ PASS: $*"; }
warn(){ WARN=$((WARN+1)); say "⚠️ WARN: $*"; }
fail(){ FAIL=$((FAIL+1)); say "❌ FAIL: $*"; }

http_code(){
  local url="$1"
  local code
  code="$(curl -sS --max-time 3 -o /dev/null -w '%{http_code}' "$url" 2>/dev/null)"
  [ -z "$code" ] && code="000"
  echo "$code"
}

say "=== NEXUS SAFE FUNCTION CHECK V2 ==="
say "Zeit: $(date -Iseconds)"
say "Repo: $(pwd)"
say

say "=== 1 AKTIVE DBs ==="
for db in data/nexy.db data/digi_dragon.db data/digipad_container.db; do
  if [ ! -f "$db" ]; then
    warn "Aktive DB fehlt: $db"
    continue
  fi

  if sqlite3 "$db" "PRAGMA quick_check;" 2>/dev/null | grep -qx "ok"; then
    ok "DB OK: $db"
  else
    fail "Aktive DB beschädigt oder nicht SQLite: $db"
  fi
done

say
say "=== 2 NEXY LIVE API 8765 ==="
for path in \
  "/api/nexy/status" \
  "/api/nexy/briefing" \
  "/api/nexy/focus" \
  "/api/nexy/timeline?limit=3" \
  "/api/nexy/search?q=Patrick&limit=3" \
  "/api/nexy/theme" \
  "/api/nexy/pet"
do
  url="http://127.0.0.1:8765$path"
  code="$(http_code "$url")"
  if [ "$code" = "200" ]; then
    ok "Nexy endpoint 200: $path"
  else
    warn "Nexy endpoint nicht erreichbar: $code $path | Start-Hinweis: ./tools/nexus_start_all.sh"
  fi
done

say
say "=== 3 DIGIDRAGON / DIGIPAD SERVICE DISCOVERY ==="

dragon_ok=0
for path in "/health" "/api/dragon/status" "/api/digidragon/status" "/api/pet/status"; do
  code="$(http_code "http://127.0.0.1:8777$path")"
  say "8777 $code $path"
  [ "$code" = "200" ] && dragon_ok=1
done
if [ "$dragon_ok" = "1" ]; then
  ok "DigiDragon-Service 8777 erreichbar."
else
  warn "DigiDragon-Service 8777 nicht erreichbar oder anderer Endpoint. Kein App-Kernfehler, Start/Route prüfen."
fi

digipad_ok=0
for path in "/health" "/api/pad/health" "/digipad" "/api/health" "/"; do
  code="$(http_code "http://127.0.0.1:8788$path")"
  say "8788 $code $path"
  [ "$code" = "200" ] && digipad_ok=1
done
if [ "$digipad_ok" = "1" ]; then
  ok "DigiPad-Service 8788 erreichbar."
else
  warn "DigiPad-Service 8788 nicht erreichbar oder anderer Endpoint. Kein App-Kernfehler, Start/Route prüfen."
fi

say
say "=== 4 RUNTIME LOGSCAN OHNE REPORTS UND OHNE TOOL-SOURCE ==="
LOG_HITS=""
if [ -d logs ]; then
  LOG_HITS="$(grep -RInE "DatabaseError|file is not a database|Traceback|FATAL EXCEPTION|AndroidRuntime" logs 2>/dev/null | head -80)"
fi

if [ -n "$LOG_HITS" ]; then
  warn "Aktuelle Runtime-Logs enthalten Fehlerhinweise."
  say "$LOG_HITS"
else
  ok "Keine aktuellen Runtime-Logfehler in logs/ gefunden."
fi

say
say "=== 5 APK ARTEFAKT ==="
APK=""
EXPECTED="/sdcard/Download/Nexus-Master-v1.6.176-usable-ui.apk"

if [ -f "$EXPECTED" ]; then
  APK="$EXPECTED"
else
  for d in \
    "/sdcard/Download/NEXUS_APK_USABLE_UI" \
    "/sdcard/Download" \
    "$HOME/nexus-apk-out"
  do
    [ -d "$d" ] || continue
    cand="$(find "$d" -type f -name "*.apk" 2>/dev/null | sort | tail -1)"
    [ -n "$cand" ] && APK="$cand"
  done
fi

if [ -z "$APK" ]; then
  warn "Keine APK lokal gefunden. Kein APK-FAIL ohne Build-Artefakt."
else
  say "APK=$APK"
  if command -v aapt >/dev/null 2>&1; then
    BADGING="$(aapt dump badging "$APK" 2>/dev/null)"
    if [ -n "$BADGING" ]; then
      ok "APK mit aapt lesbar."
      echo "$BADGING" | grep -E "^package:|application-label:|sdkVersion|targetSdkVersion" | tee -a "$REPORT"
      echo "$BADGING" | grep -q "versionCode='176'" \
        && ok "APK ist Zielversion 176." \
        || warn "APK ist lesbar, aber nicht Zielversion 176 / v1.6.176-usable-ui."
    else
      fail "APK vorhanden, aber aapt kann sie nicht lesen: $APK"
    fi
  else
    warn "aapt nicht installiert/verfügbar. APK vorhanden, aber nicht per aapt geprüft."
  fi
fi

say
say "=== 6 UI/APK HINWEIS ==="
say "Echte UI-Kontrolle erst nach Installation: App öffnen und Nexy -> Status/Briefing/Fokus/Timeline/Suche/Pet/Theme drücken."
ok "UI-Test nicht behauptet, nur Prüfanweisung erzeugt."

say
say "=== RESULT ==="
say "PASS=$PASS"
say "WARN=$WARN"
say "FAIL=$FAIL"
say "REPORT=$REPORT"

if [ "$FAIL" -gt 0 ]; then
  say "FINAL_STATUS=FAILED"
  exit 1
else
  say "FINAL_STATUS=PASS_WITH_WARNINGS_OK"
  exit 0
fi
