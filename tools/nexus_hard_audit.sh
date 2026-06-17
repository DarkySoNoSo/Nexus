#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

ROOT="$HOME/Nexus-cleanwork"
TS="$(date +%Y%m%d_%H%M%S)"
OUT="$ROOT/docs/audit/NEXUS_HARD_AUDIT_$TS.md"

cd "$ROOT"

section() {
  echo ""
  echo "# $1"
  echo ""
}

cmd_block() {
  echo '```text'
  "$@" 2>&1 || true
  echo '```'
}

{
  echo "# NEXUS HARD AUDIT"
  echo ""
  echo "Zeit: $(date -Iseconds)"
  echo "Root: $ROOT"
  echo "Host: $(uname -a)"
  echo ""

  section "1. Git Status"
  cmd_block git status --short
  cmd_block git log --oneline -n 20

  section "2. Top-Level Struktur"
  cmd_block find . -maxdepth 2 -type d | sort

  section "3. Wichtige Dateien"
  cmd_block find backend tools docs .github 2>/dev/null -maxdepth 4 -type f | sort

  section "4. Python Compile Check"
  echo '```text'
  PYFILES="$(find backend -type f -name '*.py' 2>/dev/null | sort || true)"
  if [ -n "$PYFILES" ]; then
    echo "$PYFILES" | while read -r f; do
      echo "CHECK $f"
      python -m py_compile "$f" 2>&1 || true
    done
  else
    echo "Keine Python-Dateien gefunden."
  fi
  echo '```'

  section "5. Lokale DB-Dateien"
  cmd_block find data -maxdepth 2 -type f 2>/dev/null -printf '%p %s bytes\n'

  section "6. SQLite Tabellen, falls sqlite3 vorhanden"
  echo '```text'
  if command -v sqlite3 >/dev/null 2>&1; then
    for db in $(find data -type f -name '*.db' -o -name '*.sqlite' 2>/dev/null | sort); do
      echo "DB: $db"
      sqlite3 "$db" ".tables" 2>&1 || true
      echo ""
    done
  else
    echo "sqlite3 nicht installiert."
  fi
  echo '```'

  section "7. Bridge Scripts"
  cmd_block sed -n '1,220p' tools/start_digi_dragon_bridge.sh
  if [ -f tools/start_nexy_bridge.ps1 ]; then
    cmd_block sed -n '1,220p' tools/start_nexy_bridge.ps1
  fi

  section "8. Port- und Prozesslage"
  cmd_block ps -ef
  cmd_block sh -c "command -v ss >/dev/null 2>&1 && ss -ltnp || true"
  cmd_block sh -c "command -v netstat >/dev/null 2>&1 && netstat -ltnp || true"

  section "9. Endpoint Tests"
  cmd_block curl -sS --max-time 2 http://127.0.0.1:8765/api/nexy/status
  cmd_block curl -sS --max-time 2 http://127.0.0.1:8777/api/dragon/status
  cmd_block curl -sS --max-time 2 http://127.0.0.1:8777/api/dragon/health

  section "10. Gefährliche oder verwirrende Muster"
  cmd_block grep -RIn --exclude-dir=.git --exclude='*.db' --exclude='*.sqlite' '0.0.0.0\|/tmp/\|NEXY_HOST\|DIGI_DRAGON_HOST\|8777\|8765' backend tools docs 2>/dev/null

  section "11. Ungetrackte / lokale Artefakte"
  cmd_block git status --short --ignored

  section "12. Harte Bewertung"
  echo "- Wenn Git nicht clean ist: kein Ausbau."
  echo "- Wenn Python Compile fehlschlägt: kein Ausbau."
  echo "- Wenn Bridge-URL leer oder 0.0.0.0 als Browserziel gedruckt wird: Script fixen."
  echo "- Wenn DB-Dateien getrackt werden: sofort aus Git entfernen."
  echo "- Wenn Endpoint nicht erreichbar ist: zuerst Bridge/Port lösen."
  echo "- Wenn Android noch nicht stabil angebunden ist: keine neuen UI-Features."

} > "$OUT"

echo "Audit geschrieben:"
echo "$OUT"
echo ""
tail -n 40 "$OUT"
