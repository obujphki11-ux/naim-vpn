#!/bin/bash
# Run on a fresh Ubuntu 22.04/24.04 VPS as root.
set -e
apt-get update && apt-get install -y wireguard qrencode
sysctl -w net.ipv4.ip_forward=1
echo 'net.ipv4.ip_forward=1' > /etc/sysctl.d/99-wireguard.conf
cd /etc/wireguard
wg genkey | tee server_private.key | wg pubkey > server_public.key
chmod 600 server_private.key
echo "Server public key: $(cat server_public.key)"
echo "Now create wg0.conf (see README) and: systemctl enable --now wg-quick@wg0 ; ufw allow 51820/udp"
