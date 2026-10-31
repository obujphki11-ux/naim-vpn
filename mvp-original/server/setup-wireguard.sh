#!/bin/bash
# VPN MVP server setup — Ubuntu 22.04 / 24.04
# Run as root / sudo on a fresh VPS. This is MVP only: add TLS + hardening before public use.
set -e
echo "== Installing WireGuard =="
apt update && apt install -y wireguard wireguard-tools ufw qrencode

WG_DIR="/etc/wireguard"
mkdir -p "$WG_DIR"
cd "$WG_DIR"

if [ ! -f server_private.key ]; then
  wg genkey | tee server_private.key | wg pubkey > server_public.key
  chmod 600 server_private.key
fi
SERVER_PRIV=$(cat server_private.key)
SERVER_PUB=$(cat server_public.key)

if [ ! -f wg0.conf ]; then
cat > wg0.conf <<EOF
[Interface]
Address = 10.8.0.1/24
ListenPort = 51820
PrivateKey = $SERVER_PRIV
# Forward VPN traffic to the internet
PostUp = iptables -A FORWARD -i wg0 -j ACCEPT; iptables -t nat -A POSTROUTING -o eth0 -j MASQUERADE
PostDown = iptables -D FORWARD -i wg0 -j ACCEPT; iptables -t nat -D POSTROUTING -o eth0 -j MASQUERADE
# Peers are added below by add-peer.sh
EOF
  chmod 600 wg0.conf
fi

echo "net.ipv4.ip_forward=1" > /etc/sysctl.d/99-vpn-mvp.conf
sysctl -p /etc/sysctl.d/99-vpn-mvp.conf || true

ufw allow 51820/udp || true
ufw allow 8000/tcp || true
ufw allow OpenSSH || true
echo "NOTE: ufw is not force-enabled here, so you do not lock yourself out. Enable it manually after testing SSH."

wg-quick down wg0 2>/dev/null || true
wg-quick up wg0
systemctl enable wg-quick@wg0 || true

echo ""
echo "== DONE =="
echo "Server public key (put this in servers.json):"
echo "$SERVER_PUB"
echo "Check status with: wg show"
echo "Next: sudo bash add-peer.sh <peer-name> <server-id> $SERVER_PUB <SERVER_IP:51820> 10.8.0.2"
