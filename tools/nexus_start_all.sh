#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

cd "$HOME/Nexus-cleanwork"
mkdir -p .run logs

start_service() {
  name="$1"
  cmd="$2"
  health="$3"

  pidfile=".run/${name}.pid"
  logfile="logs/${name}.log"

  if [ -f "$pidfile" ]; then
    oldpid="$(cat "$pidfile" 2>/dev/null || true)"
    if [ -n "${oldpid:-}" ] && kill -0 "$oldpid" 2>/dev/null; then
      echo "$name: läuft bereits PID $oldpid"
      return 0
    fi
  fi

  echo "$name: starte..."
  nohup bash -lc "$cmd" > "$logfile" 2>&1 &
  pid="$!"
  echo "$pid" > "$pidfile"

  sleep 2

  if kill -0 "$pid" 2>/dev/null; then
    echo "$name: PID $pid läuft"
  else
    echo "$name: START FEHLGESCHLAGEN"
    tail -n 80 "$logfile" || true
    return 1
  fi

  if command -v curl >/dev/null 2>&1; then
    echo "$name: Healthcheck $health"
    curl -sS --max-time 3 "$health" | head -c 600 || true
    echo
  fi
}

if [ -f backend/nexy/nexy_bridge_api.py ]; then
  start_service "nexy_bridge" \
    'export NEXY_HOST="0.0.0.0"; export NEXY_PORT="8765"; python -u backend/nexy/nexy_bridge_api.py' \
    'http://127.0.0.1:8765/api/nexy/status'
else
  echo "nexy_bridge: backend/nexy/nexy_bridge_api.py nicht vorhanden, übersprungen"
fi

if [ -f backend/companion/dragon_bridge_api.py ]; then
  start_service "digi_dragon_bridge" \
    'export DIGI_DRAGON_HOST="0.0.0.0"; export DIGI_DRAGON_PORT="8777"; python -u backend/companion/dragon_bridge_api.py' \
    'http://127.0.0.1:8777/api/dragon/status'
else
  echo "digi_dragon_bridge: backend/companion/dragon_bridge_api.py nicht vorhanden, übersprungen"
fi

echo "Start-All fertig."
