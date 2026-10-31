#!/bin/bash
# MVP peer removal: deletes client config files only.
# Admin must also remove the [Peer] block from /etc/wireguard/wg0.conf and restart wg0.
# Usage: sudo bash remove-peer.sh <peer-name>
set -e
PEER_NAME="${1:?peer name required}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
rm -f "$SCRIPT_DIR/peers/${PEER_NAME}_"*.conf "$SCRIPT_DIR/backend/peers/${PEER_NAME}_"*.conf
echo "Removed config files for $PEER_NAME."
echo "IMPORTANT: remove its [Peer] block from /etc/wireguard/wg0.conf, then run: wg-quick down wg0 && wg-quick up wg0"
