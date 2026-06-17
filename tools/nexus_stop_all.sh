#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

cd "$HOME/Nexus-cleanwork"
mkdir -p .run

stop_pid() {
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

stop_pid "nexy_bridge"
stop_pid "digi_dragon_bridge"

echo "Stop-All fertig."
