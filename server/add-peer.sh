#!/bin/bash
# Usage: sudo ./add-peer.sh <peer-name> <peer-ip-last-octet>
set -e
NAME="${1:?peer name}"; OCT="${2:?last octet, e.g. 2}"
cd /etc/wireguard
wg genkey | tee "${NAME}_private.key" | wg pubkey > "${NAME}_public.key"
cat > "${NAME}.conf" <<EOF
[Interface]
PrivateKey = $(cat ${NAME}_private.key)
Address = 10.8.0.${OCT}/32
DNS = 1.1.1.1

[Peer]
PublicKey = $(cat server_public.key)
Endpoint = YOUR_SERVER_PUBLIC_IP:51820
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
EOF
echo "Peer config written to /etc/wireguard/${NAME}.conf - also add this peer to wg0.conf and reload: wg syncconf wg0 <(wg-quick strip wg0)"
qrencode -t ansiutf8 < "${NAME}.conf" || true
