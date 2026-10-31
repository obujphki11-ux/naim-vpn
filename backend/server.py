#!/usr/bin/env python3
"""Naim VPN MVP backend - stdlib only, no pip needed.
Endpoints: GET /health, GET /servers, POST /login, POST /config
Demo login: demo@naimvpn.local / demo1234  (change before real use)
Config: only returns a WireGuard config when the chosen server in
servers.json has a real host + a peer template; otherwise 503
server_not_configured - never fakes a working connection.
"""
import json, os, hashlib, hmac, time
from http.server import BaseHTTPRequestHandler, HTTPServer
from pathlib import Path

BASE = Path(__file__).parent
SERVERS = json.loads((BASE/"servers.json").read_text())
DEMO_USER = os.environ.get("DEMO_USER","demo@naimvpn.local")
DEMO_PASS = os.environ.get("DEMO_PASS","demo1234")
SECRET = os.environ.get("API_SECRET","change-me-local-dev-only")

def make_token(user):
    ts = str(int(time.time()))
    sig = hmac.new(SECRET.encode(), f"{user}.{ts}".encode(), hashlib.sha256).hexdigest()[:32]
    return f"{user}.{ts}.{sig}"

def check_token(tok):
    try:
        user, ts, sig = tok.rsplit(".",2)
        good = hmac.new(SECRET.encode(), f"{user}.{ts}".encode(), hashlib.sha256).hexdigest()[:32]
        return hmac.compare_digest(good, sig) and time.time()-int(ts) < 86400
    except Exception:
        return False

class H(BaseHTTPRequestHandler):
    def _json(self, code, obj):
        body = json.dumps(obj).encode()
        self.send_response(code); self.send_header("Content-Type","application/json")
        self.send_header("Content-Length",str(len(body))); self.end_headers(); self.wfile.write(body)
    def do_GET(self):
        if self.path=="/health": self._json(200,{"ok":True,"service":"naim-vpn-api","version":"0.1.0"})
        elif self.path=="/servers": self._json(200,SERVERS)
        else: self._json(404,{"error":"not_found"})
    def do_POST(self):
        n=int(self.headers.get("Content-Length") or 0)
        try: data=json.loads(self.rfile.read(n) or b"{}")
        except Exception: data={}
        if self.path=="/login":
            if data.get("email")==DEMO_USER and data.get("password")==DEMO_PASS:
                self._json(200,{"token":make_token(DEMO_USER)})
            else: self._json(401,{"error":"invalid_credentials"})
        elif self.path=="/config":
            auth=self.headers.get("Authorization","")
            if not auth.startswith("Bearer ") or not check_token(auth[7:]):
                self._json(401,{"error":"unauthorized"}); return
            sid=data.get("server_id")
            srv=next((s for s in SERVERS["servers"] if s["id"]==sid),None)
            if not srv: self._json(404,{"error":"server_not_found"}); return
            if srv["status"]!="active" or not srv["host"]:
                self._json(503,{"error":"server_not_configured","server":srv["id"],
                    "message":"This server has no real VPS yet. Deploy a VPS, run server/setup-wireguard.sh, set host+status=active in servers.json, then config will be issued."}); return
            self._json(200,{"server":srv["id"],"note":"peer issuance happens on the VPS via add-peer.sh; wire its output here"})
        else: self._json(404,{"error":"not_found"})
    def log_message(self,*a): pass

if __name__=="__main__":
    port=int(os.environ.get("PORT","8787"))
    print(f"naim-vpn-api on :{port}")
    HTTPServer(("0.0.0.0",port),H).serve_forever()
