#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

cd "$HOME/Nexus-cleanwork"
mkdir -p .run

stop_pidfile() {
  name="$1"
  pidfile=".run/${name}.pid"

  if [ ! -f "$pidfile" ]; then
    echo "$name: kein PID-File"
    return 0
  fi

  pid="$(cat "$pidfile" 2>/dev/null || true)"

  if [ -z "${pid:-}" ]; then
    echo "$name: leeres PID-File"
    rm -f "$pidfile"
    return 0
  fi

  if kill -0 "$pid" 2>/dev/null; then
    echo "$name: stoppe PID $pid"
    kill "$pid" 2>/dev/null || true
    sleep 1
    if kill -0 "$pid" 2>/dev/null; then
      echo "$name: hart stoppen PID $pid"
      kill -9 "$pid" 2>/dev/null || true
    fi
  else
    echo "$name: PID $pid läuft nicht"
  fi

  rm -f "$pidfile"
}

stop_matching() {
  pattern="$1"
  echo "Suche Restprozesse: $pattern"
  pids="$(ps -A -o PID= -o ARGS= 2>/dev/null | grep "$pattern" | grep -v grep | awk '{print $1}' || true)"
  if [ -z "${pids:-}" ]; then
    echo "Keine Restprozesse für $pattern"
    return 0
  fi

  for pid in $pids; do
    echo "Stoppe Restprozess PID $pid ($pattern)"
    kill "$pid" 2>/dev/null || true
  done

  sleep 1

  for pid in $pids; do
    if kill -0 "$pid" 2>/dev/null; then
      echo "Hart stoppe Restprozess PID $pid"
      kill -9 "$pid" 2>/dev/null || true
    fi
  done
}

stop_pidfile "nexy_bridge"
stop_pidfile "digi_dragon_bridge"

stop_matching "backend/nexy/nexy_bridge_api.py"
stop_matching "backend/companion/dragon_bridge_api.py"

rm -f .run/nexy_bridge.pid .run/digi_dragon_bridge.pid

echo "Stop-All fertig."
