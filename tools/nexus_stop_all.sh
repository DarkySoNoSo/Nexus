#!/data/data/com.termux/files/usr/bin/bash
set -u

cd "$HOME/Nexus-cleanwork" || exit 1
mkdir -p .run

echo "Stop-All: PID-Files und echte Python-Cmdlines prüfen."

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
      echo "$name: hart stoppe PID $pid"
      kill -9 "$pid" 2>/dev/null || true
    fi
  else
    echo "$name: PID $pid läuft nicht"
  fi

  rm -f "$pidfile"
}

stop_pidfile "nexy_bridge"
stop_pidfile "digi_dragon_bridge"
stop_pidfile "digipad_container"

python - <<'PY'
import os, signal, time

patterns = [
    "backend/nexy/nexy_bridge_api.py",
    "backend/companion/dragon_bridge_api.py",
    "backend/companion/pad_container_api.py",
]

me = os.getpid()
killed = []

for name in os.listdir("/proc"):
    if not name.isdigit():
        continue
    pid = int(name)
    if pid == me:
        continue
    try:
        raw = open(f"/proc/{pid}/cmdline", "rb").read()
        cmd = raw.decode("utf-8", "ignore").replace("\x00", " ").strip()
    except Exception:
        continue

    if any(p in cmd for p in patterns):
        print(f"Stoppe Restprozess PID {pid}: {cmd}")
        try:
            os.kill(pid, signal.SIGTERM)
            killed.append(pid)
        except Exception as e:
            print(f"TERM Fehler PID {pid}: {e}")

time.sleep(1)

for pid in killed:
    try:
        os.kill(pid, 0)
    except ProcessLookupError:
        continue
    except Exception:
        continue
    print(f"Hart stoppe Restprozess PID {pid}")
    try:
        os.kill(pid, signal.SIGKILL)
    except Exception as e:
        print(f"KILL Fehler PID {pid}: {e}")
PY

rm -f .run/nexy_bridge.pid .run/digi_dragon_bridge.pid .run/digipad_container.pid
echo "Stop-All fertig."
