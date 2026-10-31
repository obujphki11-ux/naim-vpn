#!/usr/bin/env python3
"""
VPN MVP backend — Python standard library only, no pip install needed.

MVP security notes (read before public use):
- Demo auth only: passwords are SHA-256 hashes in users.json, no salt/rate-limit yet.
- Tokens live only in memory and expire after 24 hours.
- Serve this behind Nginx + HTTPS in production. Never expose plain HTTP to real users.
- This API serves WireGuard configs only to the peer mapped to the logged-in user.
"""
import hashlib
import json
import os
import secrets
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs

BASE = os.path.dirname(os.path.abspath(__file__))
PORT = int(os.environ.get("PORT", "8000"))
TOKENS = {}  # token -> {"username": str, "peer": str, "expires": float}


def load_json(name, default):
    path = os.path.join(BASE, name)
    if not os.path.exists(path):
        return default
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def send_json(handler, status, obj):
    body = json.dumps(obj, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(body)))
    handler.end_headers()
    handler.wfile.write(body)


def public_servers():
    servers = load_json("servers.json", {"servers": []})
    out = []
    for s in servers.get("servers", []):
        out.append({
            "id": s["id"],
            "name": s.get("name", s["id"]),
            "country": s.get("country", ""),
            "city": s.get("city", ""),
            "load": s.get("load", 0),
            "status": s.get("status", "online"),
            "type": s.get("type", ""),
        })
    return out


def auth_user(handler):
    token = handler.headers.get("Authorization", "")
    if token.startswith("Bearer "):
        token = token[7:]
    info = TOKENS.get(token)
    if not info or info["expires"] < time.time():
        TOKENS.pop(token, None)
        return None
    return info


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *args):  # keep logs quiet in MVP
        pass

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == "/health":
            return send_json(self, 200, {"ok": True})
        if parsed.path == "/api/servers":
            return send_json(self, 200, {"servers": public_servers()})
        if parsed.path == "/api/config":
            user = auth_user(self)
            if not user:
                return send_json(self, 401, {"error": "unauthorized"})
            server_id = parse_qs(parsed.query).get("server_id", [""])[0]
            if not server_id:
                return send_json(self, 400, {"error": "server_id required"})
            conf_path = os.path.join(BASE, "peers", f"{user['peer']}_{server_id}.conf")
            if not os.path.exists(conf_path):
                return send_json(self, 404, {"error": "no config for this user/server yet"})
            with open(conf_path, encoding="utf-8") as f:
                config = f.read()
            return send_json(self, 200, {"server_id": server_id, "config": config})
        return send_json(self, 404, {"error": "not found"})

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path != "/api/login":
            return send_json(self, 404, {"error": "not found"})
        length = int(self.headers.get("Content-Length", "0") or 0)
        try:
            data = json.loads(self.rfile.read(length).decode("utf-8") or "{}")
        except Exception:
            return send_json(self, 400, {"error": "bad json"})
        users = load_json("users.json", {"users": {}}).get("users", {})
        username = data.get("username", "")
        password = data.get("password", "")
        user = users.get(username)
        if not user:
            return send_json(self, 401, {"error": "invalid login"})
        pw_hash = hashlib.sha256(password.encode("utf-8")).hexdigest()
        if pw_hash != user.get("password_sha256"):
            return send_json(self, 401, {"error": "invalid login"})
        token = secrets.token_hex(24)
        TOKENS[token] = {"username": username, "peer": user["peer"], "expires": time.time() + 24 * 3600}
        return send_json(self, 200, {"token": token, "username": username, "peer": user["peer"]})


if __name__ == "__main__":
    print(f"VPN MVP backend on http://0.0.0.0:{PORT} (MVP only, put HTTPS in front for real users)")
    ThreadingHTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
