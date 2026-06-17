from __future__ import annotations

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

from dragon_db import apply_action, codex, get_state, init_db

def read_json(handler: BaseHTTPRequestHandler):
    length = int(handler.headers.get("Content-Length", "0") or "0")
    if length <= 0:
        return {}
    raw = handler.rfile.read(length).decode("utf-8")
    if not raw.strip():
        return {}
    return json.loads(raw)

class Handler(BaseHTTPRequestHandler):
    server_version = "DigiDragonBridge/0.1"

    def log_message(self, fmt, *args):
        print("%s - - [%s] %s" % (self.client_address[0], self.log_date_time_string(), fmt % args))

    def send_json(self, status: int, data):
        body = json.dumps(data, indent=2, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_json(200, {"ok": True})

    def do_GET(self):
        path = urlparse(self.path).path
        try:
            if path in ("/health", "/api/dragon/health"):
                self.send_json(200, {"ok": True, "service": "digi_dragon", "version": "0.1"})
            elif path == "/api/dragon/status":
                self.send_json(200, {"ok": True, "state": get_state()})
            elif path == "/api/dragon/habitat":
                state = get_state()
                self.send_json(200, {"ok": True, "habitat": state.get("habitat"), "state": state})
            elif path == "/api/dragon/codex":
                self.send_json(200, codex())
            else:
                self.send_json(404, {"ok": False, "error": "Not found", "path": path})
        except Exception as exc:
            self.send_json(500, {"ok": False, "error": str(exc)})

    def do_POST(self):
        path = urlparse(self.path).path
        action_by_path = {
            "/api/dragon/feed": "feed",
            "/api/dragon/care": "care",
            "/api/dragon/train": "train",
            "/api/dragon/freefight": "freefight",
            "/api/dragon/arena": "arena",
            "/api/dragon/evolve": "evolve",
        }
        try:
            action = action_by_path.get(path)
            if not action:
                self.send_json(404, {"ok": False, "error": "Not found", "path": path})
                return
            payload = read_json(self)
            result = apply_action(action, payload)
            self.send_json(200 if result.get("ok") else 400, result)
        except Exception as exc:
            self.send_json(500, {"ok": False, "error": str(exc)})

def main():
    init_db()
    host = os.getenv("DIGI_DRAGON_HOST", "127.0.0.1")
    port = int(os.getenv("DIGI_DRAGON_PORT", "8777"))

    server = ThreadingHTTPServer((host, port), Handler)

    print(f"Digi Dragon Bridge läuft: http://{host}:{port}/api/dragon/status")
    print("GET  /api/dragon/status")
    print("GET  /api/dragon/habitat")
    print("GET  /api/dragon/codex")
    print("POST /api/dragon/feed")
    print("POST /api/dragon/care")
    print("POST /api/dragon/train")
    print("POST /api/dragon/freefight")
    print("POST /api/dragon/arena")
    print("POST /api/dragon/evolve")

    server.serve_forever()

if __name__ == "__main__":
    main()
