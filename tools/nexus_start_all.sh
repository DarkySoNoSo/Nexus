#!/data/data/com.termux/files/usr/bin/bash
set -u

cd "$HOME/Nexus-cleanwork" || exit 1
mkdir -p .run logs

FAIL=0

health_ok() {
  url="$1"
  curl -sS --max-time 3 "$url" >/dev/null 2>&1
}

start_service() {
  name="$1"
  cmd="$2"
  health="$3"
  logfile="logs/${name}.log"
  pidfile=".run/${name}.pid"

  echo "$name: prüfe $health"

  if health_ok "$health"; then
    echo "$name: läuft bereits / Health OK"
    return 0
  fi

  echo "$name: starte..."
  bash -lc "$cmd" > "$logfile" 2>&1 &
  pid="$!"
  echo "$pid" > "$pidfile"

  sleep 2

  if kill -0 "$pid" 2>/dev/null && health_ok "$health"; then
    echo "$name: PID $pid läuft"
    echo "$name: Health OK"
    return 0
  fi

  echo "$name: START ODER HEALTHCHECK FEHLGESCHLAGEN"
  echo "--- $logfile ---"
  tail -n 80 "$logfile" 2>/dev/null || true
  FAIL=1
  return 0
}

start_service "nexy_bridge" \
  'export NEXY_HOST="0.0.0.0"; export NEXY_PORT="8765"; python -u backend/nexy/nexy_bridge_api.py' \
  'http://127.0.0.1:8765/api/nexy/status'

start_service "digi_dragon_bridge" \
  'export DIGI_DRAGON_HOST="0.0.0.0"; export DIGI_DRAGON_PORT="8777"; python -u backend/companion/dragon_bridge_api.py' \
  'http://127.0.0.1:8777/api/dragon/status'

if [ -f backend/companion/pad_container_api.py ]; then
  start_service "digipad_container" \
    'export DIGIPAD_HOST="0.0.0.0"; export DIGIPAD_PORT="8788"; python -u backend/companion/pad_container_api.py' \
    'http://127.0.0.1:8788/api/pad/health'
else
  echo "digipad_container: Datei fehlt, übersprungen"
  FAIL=1
fi

echo "Start-All fertig. FAIL=$FAIL"
exit "$FAIL"
