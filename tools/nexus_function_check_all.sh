#!/data/data/com.termux/files/usr/bin/bash
set -u

ROOT="$HOME/Nexus-cleanwork"
cd "$ROOT" || exit 1

mkdir -p logs reports
TS="$(date +%Y%m%d_%H%M%S)"
REPORT="reports/nexus_function_check_$TS.txt"
JSON_REPORT="reports/nexus_function_check_$TS.json"

BASE="http://127.0.0.1:8765"

PASS=0
FAIL=0
WARN=0

line(){ printf '%s\n' "$*" | tee -a "$REPORT"; }
ok(){ PASS=$((PASS+1)); line "✅ PASS: $*"; }
fail(){ FAIL=$((FAIL+1)); line "❌ FAIL: $*"; }
warn(){ WARN=$((WARN+1)); line "⚠️ WARN: $*"; }

check_cmd(){
  command -v "$1" >/dev/null 2>&1 && ok "Command vorhanden: $1" || fail "Command fehlt: $1"
}

http_check(){
  name="$1"
  url="$2"
  expect="$3"

  tmp="logs/check_${name}_${TS}.out"
  code="$(curl -sS --max-time 5 -w "%{http_code}" -o "$tmp" "$url" 2>>"$REPORT" || echo "CURL_FAIL")"

  if [ "$code" = "200" ]; then
    ok "$name HTTP 200"
  else
    fail "$name HTTP Code=$code URL=$url"
    line "---- BODY ----"
    head -c 1000 "$tmp" | tee -a "$REPORT"
    line
    return
  fi

  if [ "$expect" = "json" ]; then
    python -m json.tool "$tmp" >/dev/null 2>&1 && ok "$name JSON gültig" || fail "$name JSON ungültig"
    grep -q '"ok"[[:space:]]*:[[:space:]]*true' "$tmp" && ok "$name ok=true" || warn "$name ohne ok=true"
  fi

  if [ "$expect" = "svg" ]; then
    grep -q '<svg' "$tmp" && ok "$name SVG vorhanden" || fail "$name kein SVG"
    grep -q 'NEXY' "$tmp" && ok "$name SVG enthält NEXY" || warn "$name SVG ohne NEXY Text"
  fi

  if [ "$name" = "theme" ]; then
    color_count="$(grep -o '#[0-9a-fA-F]\{6\}' "$tmp" | sort -u | wc -l | tr -d ' ')"
    if [ "$color_count" -ge 8 ]; then
      ok "Theme Palette >= 8 Farben ($color_count)"
    else
      fail "Theme Palette zu klein ($color_count Farben)"
    fi
  fi
}

{
  echo "NEXUS FUNKTIONSKONTROLLE"
  echo "TS: $TS"
  echo "ROOT: $ROOT"
  echo "BASE: $BASE"
  echo
} > "$REPORT"

line "=== 1 SYSTEM ==="
check_cmd python
check_cmd curl
check_cmd grep
check_cmd sed
check_cmd awk
check_cmd find
check_cmd unzip

line
line "=== 2 BRIDGE PROZESS / PORT ==="
if pgrep -af "nexy_bridge_8765.py" | tee -a "$REPORT" >/dev/null; then
  ok "nexy_bridge_8765.py läuft"
else
  fail "nexy_bridge_8765.py läuft NICHT"
fi

if [ -f "$HOME/.nexus/nexy_bridge.pid" ]; then
  PID="$(cat "$HOME/.nexus/nexy_bridge.pid" 2>/dev/null || true)"
  if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
    ok "PID-Datei gültig: $PID"
  else
    warn "PID-Datei vorhanden, aber Prozess nicht gültig: ${PID:-leer}"
  fi
else
  warn "PID-Datei fehlt: $HOME/.nexus/nexy_bridge.pid"
fi

if (ss -ltnp 2>/dev/null || netstat -ltnp 2>/dev/null || true) | grep -q ':8765'; then
  ok "Port 8765 lauscht"
else
  fail "Port 8765 lauscht NICHT"
fi

line
line "=== 3 API ENDPOINTS ==="
http_check "health"   "$BASE/health" "json"
http_check "status"   "$BASE/api/nexy/status" "json"
http_check "briefing" "$BASE/api/nexy/briefing" "json"
http_check "focus"    "$BASE/api/nexy/focus?limit=5" "json"
http_check "timeline" "$BASE/api/nexy/timeline?limit=8" "json"
http_check "search"   "$BASE/api/nexy/search?q=Patrick&limit=5" "json"
http_check "theme"    "$BASE/api/nexy/theme" "json"
http_check "pet"      "$BASE/api/nexy/pet" "svg"

line
line "=== 4 DB CHECK ==="
DBS="$(find data -maxdepth 1 -type f \( -name *.db\ -o -name *.sqlite\ -o -name *.sqlite3\ \) 2>/dev/null | sort -u)"
if [ -n "$DBS" ]; then
  ok "DB-Dateien gefunden"
  echo "$DBS" | tee -a "$REPORT"

  while IFS= read -r db; do
    [ -z "$db" ] && continue
    line
    line "--- DB: $db"
    python - "$db" <<'PY' | tee -a "$REPORT"
import sqlite3, sys, os
db=sys.argv[1]
try:
    con=sqlite3.connect(db)
    cur=con.cursor()
    tables=[r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").fetchall()]
    print("TABLES:", ", ".join(tables) if tables else "NONE")
    for t in tables:
        try:
            c=cur.execute(f'SELECT COUNT(*) FROM "{t}"').fetchone()[0]
            print(f"COUNT {t}: {c}")
        except Exception as e:
            print(f"COUNT {t}: ERROR {e}")
    con.close()
except Exception as e:
    print("DB_ERROR:", type(e).__name__, str(e))
PY
  done <<< "$DBS"
else
  warn "Keine DB-Dateien gefunden"
fi

line
line "=== 5 NEXY MEMORY TABLE EXPECTATION ==="
EXPECTED="nexy_events nexy_timeline nexy_context nexy_facts nexy_lessons nexy_active_focus"
MAIN_DB="./data/nexy.db"

if [ -f "$MAIN_DB" ]; then
  for t in $EXPECTED; do
    if python - "$MAIN_DB" "$t" <<'PY' >/dev/null 2>&1
import sqlite3, sys
db, table = sys.argv[1], sys.argv[2]
con=sqlite3.connect(db)
r=con.execute("SELECT name FROM sqlite_master WHERE type='table' AND name=?", (table,)).fetchone()
con.close()
raise SystemExit(0 if r else 1)
PY
    then
      ok "Tabelle vorhanden: $t"
    else
      fail "Tabelle fehlt: $t"
    fi
  done
else
  warn "$MAIN_DB nicht vorhanden"
fi

line
line "=== 6 ANDROID SOURCE STATIC CHECK ==="
MAIN="$(find . -type f -path '*MainActivity.java' | head -1)"
if [ -n "$MAIN" ]; then
  ok "MainActivity gefunden: $MAIN"

  grep -q '/api/nexy/status' "$MAIN" && ok "App kennt /api/nexy/status" || fail "App kennt /api/nexy/status NICHT"
  grep -q '/api/nexy/briefing' "$MAIN" && ok "App kennt /api/nexy/briefing" || fail "App kennt /api/nexy/briefing NICHT"
  grep -q '/api/nexy/focus' "$MAIN" && ok "App kennt /api/nexy/focus" || fail "App kennt /api/nexy/focus NICHT"
  grep -q '/api/nexy/timeline' "$MAIN" && ok "App kennt /api/nexy/timeline" || fail "App kennt /api/nexy/timeline NICHT"
  grep -q '/api/nexy/search' "$MAIN" && ok "App kennt /api/nexy/search" || fail "App kennt /api/nexy/search NICHT"

  grep -q '/api/nexy/theme' "$MAIN" && ok "App lädt Theme-Endpoint" || fail "App lädt Theme-Endpoint NICHT"
  grep -q '/api/nexy/pet' "$MAIN" && ok "App lädt Pet-Endpoint" || fail "App lädt Pet-Endpoint NICHT"

  grep -q 'Orange' "$MAIN" && warn "Hardcoded Theme Orange vorhanden" || ok "Kein hardcoded Orange gefunden"
  grep -q 'Cyberblau' "$MAIN" && warn "Hardcoded Theme Cyberblau vorhanden" || ok "Kein hardcoded Cyberblau gefunden"
else
  fail "MainActivity.java nicht gefunden"
fi

line
line "=== 7 APK CHECK ==="
APKS="$(find "$HOME" . -type f -name '*.apk' 2>/dev/null | sort -u)"
if [ -n "$APKS" ]; then
  ok "APK-Dateien gefunden"
  echo "$APKS" | tee -a "$REPORT"

  LATEST_APK="$(echo "$APKS" | tail -1)"
  line "--- LATEST APK: $LATEST_APK"
  ls -lh "$LATEST_APK" | tee -a "$REPORT"

  if unzip -l "$LATEST_APK" >/tmp/nexus_apk_list_$TS.txt 2>/dev/null; then
    ok "APK ist unzip-lesbar"
    grep -Ei 'pet|nexy|theme|drawable|asset|png|webp|svg' /tmp/nexus_apk_list_$TS.txt | head -120 | tee -a "$REPORT"
  else
    fail "APK kann nicht gelesen werden"
  fi
else
  warn "Keine APK-Datei gefunden"
fi

line
line "=== 8 LOG ERROR SCAN ==="
LOGS="$(find logs . -maxdepth 4 -type f \( -name '*.log' -o -name '*.txt' \) 2>/dev/null | sort -u)"
if [ -n "$LOGS" ]; then
  ERRORS="$(grep -RInE 'FATAL|EXCEPTION|Exception|Traceback|Error|ERROR|AndroidRuntime|ClassNotFound|NoClassDef|UnsatisfiedLink|Permission denied|HTTP/1.1\" 404|HTTP/1.1\" 500' logs . 2>/dev/null | head -200 || true)"
  if [ -n "$ERRORS" ]; then
    warn "Fehler/Warnings in Logs gefunden"
    echo "$ERRORS" | tee -a "$REPORT"
  else
    ok "Keine kritischen Fehler in Logs gefunden"
  fi
else
  warn "Keine Logs gefunden"
fi

line
line "=== 9 LIVE APP-NAHE FUNKTIONSLISTE ==="
line "Backend/API:"
line "  health   -> getestet"
line "  status   -> getestet"
line "  briefing -> getestet"
line "  focus    -> getestet"
line "  timeline -> getestet"
line "  search   -> getestet"
line "  theme    -> getestet"
line "  pet      -> getestet"
line
line "UI/APK:"
line "  nur statisch prüfbar ohne UI-Instrumentation"
line "  echte UI-Kontrolle: App öffnen und Nexy -> Status/Briefing/Fokus/Timeline/Suche/Pet/Theme drücken"

line
line "=== RESULT ==="
line "PASS=$PASS"
line "WARN=$WARN"
line "FAIL=$FAIL"
line "REPORT=$REPORT"

cat > "$JSON_REPORT" <<EOF
{
  "timestamp": "$TS",
  "pass": $PASS,
  "warn": $WARN,
  "fail": $FAIL,
  "report": "$REPORT"
}
EOF

line "JSON_REPORT=$JSON_REPORT"

if [ "$FAIL" -eq 0 ]; then
  line "FINAL_STATUS=OK"
  exit 0
else
  line "FINAL_STATUS=FAILED"
  exit 1
fi
