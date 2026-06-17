#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

ROOT="$HOME/Nexus-cleanwork"
TS="$(date +%Y%m%d_%H%M%S)"
OUT="$ROOT/docs/audit/NEXUS_DOCTOR_$TS.md"
LATEST="$ROOT/docs/audit/NEXUS_DOCTOR_LATEST.md"

cd "$ROOT"
mkdir -p docs/audit

PASS=0
FAIL=0
WARN=0

ok() { echo "OK   $1"; PASS=$((PASS+1)); }
bad() { echo "FAIL $1"; FAIL=$((FAIL+1)); }
warn() { echo "WARN $1"; WARN=$((WARN+1)); }

check_file() {
  [ -f "$1" ] && ok "Datei vorhanden: $1" || bad "Datei fehlt: $1"
}

check_exec() {
  [ -x "$1" ] && ok "Ausführbar: $1" || bad "Nicht ausführbar: $1"
}

check_endpoint() {
  name="$1"
  url="$2"

  if ! command -v curl >/dev/null 2>&1; then
    warn "curl fehlt, Endpoint-Test übersprungen: $name"
    return 0
  fi

  if curl -sS --max-time 3 "$url" >/tmp/nexus_doctor_endpoint.json 2>/tmp/nexus_doctor_endpoint.err; then
    if grep -q '"ok"[[:space:]]*:[[:space:]]*true' /tmp/nexus_doctor_endpoint.json; then
      ok "$name erreichbar: $url"
    else
      warn "$name antwortet, aber ok=true fehlt: $url"
    fi
  else
    bad "$name nicht erreichbar: $url"
  fi
}

{
  echo "# NEXUS DOCTOR"
  echo ""
  echo "Zeit: $(date -Iseconds)"
  echo "Root: $ROOT"
  echo ""

  echo "## Checks"
  echo '```text'

  command -v git >/dev/null 2>&1 && ok "git vorhanden" || bad "git fehlt"
  command -v python >/dev/null 2>&1 && ok "python vorhanden" || bad "python fehlt"

  if [ -d .git ]; then
    ok "Git-Repo vorhanden"
  else
    bad "Kein Git-Repo"
  fi

  if [ -z "$(git status --short 2>/dev/null || true)" ]; then
    ok "Git working tree clean"
  else
    warn "Git working tree nicht clean"
    git status --short || true
  fi

  check_file "docs/NEXUS_MODULE_REGISTRY.md"
  check_file "backend/companion/dragon_db.py"
  check_file "backend/companion/dragon_cli.py"
  check_file "backend/companion/dragon_bridge_api.py"
  check_exec "tools/start_digi_dragon_bridge.sh"
  check_exec "tools/nexus_start_all.sh"
  check_exec "tools/nexus_stop_all.sh"

  if [ -f backend/nexy/nexy_bridge_api.py ]; then
    ok "Nexy Bridge Datei vorhanden"
  else
    warn "Nexy Bridge Datei fehlt auf diesem Gerät"
  fi

  echo ""
  echo "Python Compile:"
  find backend -type f -name '*.py' 2>/dev/null | sort | while read -r f; do
    echo "CHECK $f"
    python -m py_compile "$f" 2>&1 || true
  done

  echo ""
  echo "Getrackte DB-Dateien:"
  TRACKED_DB="$(git ls-files | grep -E '\.(db|sqlite)$' || true)"
  if [ -z "$TRACKED_DB" ]; then
    ok "Keine DB-Dateien in Git getrackt"
  else
    bad "DB-Dateien sind in Git getrackt:"
    echo "$TRACKED_DB"
  fi

  echo ""
  echo "Lokale DB-Dateien:"
  find data -maxdepth 2 -type f 2>/dev/null -printf '%p %s bytes\n' || true

  echo ""
  echo "Port/Prozesslage:"
  ps -ef | grep -E 'nexy_bridge|dragon_bridge|python' | grep -v grep || true
  if command -v ss >/dev/null 2>&1; then
    ss -ltnp 2>/dev/null | grep -E '8765|8777' || true
  fi

  echo ""
  check_endpoint "Digi Dragon" "http://127.0.0.1:8777/api/dragon/status"
  check_endpoint "Digi Dragon Health" "http://127.0.0.1:8777/api/dragon/health"
  check_endpoint "Nexy" "http://127.0.0.1:8765/api/nexy/status"

  echo ""
  echo "Gefährliche Muster:"
  grep -RIn --exclude-dir=.git --exclude='*.db' --exclude='*.sqlite' 'http://0.0.0.0\|>/tmp/\|/tmp/digi\|AUTO.*Chef\|auto.*api' backend tools docs 2>/dev/null || true

  echo '```'
  echo ""
  echo "## Ergebnis"
  echo ""
  echo "- OK: $PASS"
  echo "- WARN: $WARN"
  echo "- FAIL: $FAIL"
  echo ""

  if [ "$FAIL" -eq 0 ]; then
    echo "**VERDICT: GRÜN oder GELB. Ausbau erlaubt, wenn Warnungen verstanden sind.**"
  else
    echo "**VERDICT: ROT. Kein Feature-Ausbau. Erst FAILs beheben.**"
  fi

} > "$OUT"

cp "$OUT" "$LATEST"

cat "$LATEST"
