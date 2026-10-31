#!/bin/bash
# Add a server entry to servers.json (backend folder).
# This only adds the LIST entry. It does NOT create a live server.
# You must first run setup-wireguard.sh on that VM, then add-peer.sh for each user.
# Usage: bash add-server.sh <id> <name> <country> <city> <type> [status]
# Example: bash add-server.sh de-oracle "Germany" "Germany" "Frankfurt" "Always Free (separate account)"
set -e
cd "$(dirname "$0")"
ID="${1:?server id required}"
NAME="${2:?name required}"
COUNTRY="${3:?country required}"
CITY="${4:?city required}"
TYPE="${5:?type required, e.g. Always Free / Trial 12 months}"
STATUS="${6:-setup-needed}"
python3 - "$ID" "$NAME" "$COUNTRY" "$CITY" "$TYPE" "$STATUS" <<'PY'
import json, sys, os
path = "servers.json"
data = {"servers": []}
if os.path.exists(path):
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
if any(s["id"] == sys.argv[1] for s in data["servers"]):
    print(f"Server id already exists: {sys.argv[1]}")
    sys.exit(1)
data["servers"].append({
    "id": sys.argv[1], "name": sys.argv[2], "country": sys.argv[3],
    "city": sys.argv[4], "type": sys.argv[5], "status": sys.argv[6], "load": 0,
})
with open(path, "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)
print(f"Added: {sys.argv[1]} — {sys.argv[2]} ({sys.argv[5]})")
print("Next: 1) setup-wireguard.sh on that VM  2) add-peer.sh for each user  3) copy peer .conf to backend/peers/")
PY
