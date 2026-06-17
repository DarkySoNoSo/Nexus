#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

cd "$HOME/Nexus-cleanwork"

export DIGI_DRAGON_HOST="${DIGI_DRAGON_HOST:-0.0.0.0}"
export DIGI_DRAGON_PORT="${DIGI_DRAGON_PORT:-8777}"

LAN_IP="$(ip -o -4 addr show scope global 2>/dev/null | awk '{split($4,a,"/"); print a[1]; exit}' || true)"

echo "Starte Digi Dragon Bridge..."
echo ""
echo "Benutzbare URLs:"
echo "Lokal / Android-App: http://127.0.0.1:${DIGI_DRAGON_PORT}/api/dragon/status"

if [ -n "${LAN_IP:-}" ]; then
  echo "Extern im gleichen Netz: http://${LAN_IP}:${DIGI_DRAGON_PORT}/api/dragon/status"
else
  echo "Extern im gleichen Netz: keine lokale IP gefunden"
fi

echo ""
echo "Richtig:"
echo "  Nexy        = http://127.0.0.1:8765/api/nexy/status"
echo "  Digi Dragon = http://127.0.0.1:8777/api/dragon/status"
echo ""
echo "Falsch:"
echo "  Port 8766"
echo "  http://0.0.0.0:${DIGI_DRAGON_PORT}/..."
echo ""
echo "0.0.0.0 ist nur die Server-Bind-Adresse, nicht die Browser-/App-Adresse."
echo ""

exec python -u backend/companion/dragon_bridge_api.py
