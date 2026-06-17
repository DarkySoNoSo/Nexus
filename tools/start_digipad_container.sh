#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

cd "$HOME/Nexus-cleanwork"

export DIGIPAD_HOST="${DIGIPAD_HOST:-0.0.0.0}"
export DIGIPAD_PORT="${DIGIPAD_PORT:-8788}"

LAN_IP="$(ip -o -4 addr show scope global 2>/dev/null | awk '{split($4,a,"/"); print a[1]; exit}' || true)"

echo "Starte DigiPad Container API..."
echo ""
echo "Lokal:"
echo "  http://127.0.0.1:${DIGIPAD_PORT}/api/pad/health"
echo ""
if [ -n "${LAN_IP:-}" ]; then
  echo "Gleiches WLAN:"
  echo "  http://${LAN_IP}:${DIGIPAD_PORT}/api/pad/health"
else
  echo "Gleiches WLAN: keine lokale IP gefunden"
fi
echo ""
echo "Portkarte:"
echo "  Nexy        8765 privat"
echo "  Digi Dragon 8777 intern"
echo "  DigiPad     8788 geschützt / Fiona Remote"
echo ""

exec python -u backend/companion/pad_container_api.py
