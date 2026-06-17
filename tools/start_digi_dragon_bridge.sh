#!/data/data/com.termux/files/usr/bin/bash
set -Eeuo pipefail

cd "$HOME/Nexus-cleanwork"

export DIGI_DRAGON_HOST="0.0.0.0"
export DIGI_DRAGON_PORT="8777"

echo "Starte Digi Dragon Bridge..."
echo "Lokal:   http://127.0.0.1:8777/api/dragon/status"
echo "WLAN:    http://$(ip route get 1.1.1.1 2>/dev/null | awk '{for(i=1;i<=NF;i++) if($i=="src"){print $(i+1); exit}}'):8777/api/dragon/status"
echo ""

python backend/companion/dragon_bridge_api.py
