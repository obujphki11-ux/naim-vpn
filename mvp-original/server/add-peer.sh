#!/bin/bash
# Usage: sudo bash add-peer.sh <peer-name> <server-id> <server-public-key> <server-endpoint> <client-ip>
# Example: sudo bash add-peer.sh demo-peer sg-oracle ABC...= 203.0.113.10:51820 10.8.0.2
set -e
PEER_NAME="${1:?peer name required}"
SERVER_ID="${2:?server id required}"
SERVER_PUB="${3:?server public key required}"
ENDPOINT="${4:?endpoint required, e.g. 203.0.113.10:51820}"
CLIENT_IP="${5:?client IP required, e.g. 10.8.0.2}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PEER_DIR="$SCRIPT_DIR/peers"
mkdir -p "$PEER_DIR"

PEER_PRIV=$(wg genkey)
PEER_PUB=$(echo "$PEER_PRIV" | wg pubkey)

# Append peer to server config (MVP: simple append, admin must avoid duplicates)
cat >> /etc/wireguard/wg0.conf <<EOF

[Peer]
# $PEER_NAME
PublicKey = $PEER_PUB
AllowedIPs = $CLIENT_IP/32
EOF

cat > "$PEER_DIR/${PEER_NAME}_${SERVER_ID}.conf" <<EOF
[Interface]
PrivateKey = $PEER_PRIV
Address = $CLIENT_IP/32
DNS = 1.1.1.1, 8.8.8.8

[Peer]
PublicKey = $SERVER_PUB
Endpoint = $ENDPOINT
AllowedIPs = 0.0.0.0/0, ::/0
PersistentKeepalive = 25
EOF
chmod 600 "$PEER_DIR/${PEER_NAME}_${SERVER_ID}.conf"

wg-quick down wg0 2>/dev/null || true
wg-quick up wg0

echo "Peer added: $PEER_NAME ($PEER_PUB)"
echo "Client config: $PEER_DIR/${PEER_NAME}_${SERVER_ID}.conf"
echo "Copy that file to server/backend/peers/ so the API can serve it to this peer's user."
echo "QR (optional):"
qrencode -t ansiutf8 < "$PEER_DIR/${PEER_NAME}_${SERVER_ID}.conf" || true
