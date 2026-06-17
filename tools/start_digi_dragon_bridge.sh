#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

cd "$HOME/Nexus-cleanwork"

export DIGI_DRAGON_HOST="0.0.0.0"
export DIGI_DRAGON_PORT="${DIGI_DRAGON_PORT:-8777}"

LAN_IP="$(ip -o -4 addr show scope global 2>/dev/null | awk '{split($4,a,"/"); print a[1]; exit}' || true)"

echo "Starte Digi Dragon Bridge..."
echo ""
echo "Benutzbare URLs:"
echo "Lokal Browser: http://127.0.0.1:${DIGI_DRAGON_PORT}/api/dragon/status"
if [ -n "${LAN_IP:-}" ]; then
  echo "Netzwerk:      http://${LAN_IP}:${DIGI_DRAGON_PORT}/api/dragon/status"
else
  echo "Netzwerk:      keine lokale IP gefunden"
fi
echo ""
echo "NICHT im Browser benutzen: http://0.0.0.0:${DIGI_DRAGON_PORT}/..."
echo "0.0.0.0 ist nur die Server-Bind-Adresse."
echo ""

python backend/companion/dragon_bridge_api.py
