from __future__ import annotations

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse

from pad_container_db import (
    apply_action,
    export_battle_snapshot,
    get_status,
    ghost_battle,
    init_db,
    profile_exists,
    verify_token,
)


def read_json(handler: BaseHTTPRequestHandler):
    length = int(handler.headers.get("Content-Length", "0") or "0")
    if length <= 0:
        return {}
    raw = handler.rfile.read(length).decode("utf-8")
    if not raw.strip():
        return {}
    return json.loads(raw)


class Handler(BaseHTTPRequestHandler):
    server_version = "DigiPadContainer/0.1"

    def log_message(self, fmt, *args):
        print("%s - - [%s] %s" % (self.client_address[0], self.log_date_time_string(), fmt % args), flush=True)

    def send_json(self, status: int, data):
        body = json.dumps(data, indent=2, ensure_ascii=False).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, X-DigiPad-Token")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_json(200, {"ok": True})

    def token_from_request(self, parsed):
        qs = parse_qs(parsed.query or "")
        token = self.headers.get("X-DigiPad-Token", "")
        if not token:
            token = (qs.get("token", [""])[0] or "").strip()
        return token

    def authorize(self, profile_id: str, parsed) -> bool:
        if not profile_exists(profile_id):
            self.send_json(404, {"ok": False, "error": "unknown profile"})
            return False

        token = self.token_from_request(parsed)
        if not verify_token(profile_id, token):
            self.send_json(403, {"ok": False, "error": "forbidden", "message": "missing or invalid DigiPad token"})
            return False

        return True

    def do_GET(self):
        parsed = urlparse(self.path)
        path = parsed.path

        try:
            if path in ("/health", "/api/pad/health"):
                self.send_json(200, {"ok": True, "service": "digipad_container", "version": "0.1", "port": int(os.getenv("DIGIPAD_PORT", "8788"))})
                return

            parts = [p for p in path.split("/") if p]
            if len(parts) >= 4 and parts[0] == "api" and parts[1] == "pad":
                profile_id = parts[2]
                rest = parts[3:]

                if not self.authorize(profile_id, parsed):
                    return

                if rest == ["status"] or rest == ["pet"]:
                    self.send_json(200, get_status(profile_id))
                    return

                if rest == ["battle", "export"]:
                    self.send_json(200, export_battle_snapshot(profile_id))
                    return

            self.send_json(404, {"ok": False, "error": "not found", "path": path})

        except Exception as exc:
            self.send_json(500, {"ok": False, "error": str(exc)})

    def do_POST(self):
        parsed = urlparse(self.path)
        path = parsed.path

        try:
            parts = [p for p in path.split("/") if p]
            if len(parts) >= 4 and parts[0] == "api" and parts[1] == "pad":
                profile_id = parts[2]
                rest = parts[3:]

                if not self.authorize(profile_id, parsed):
                    return

                payload = read_json(self)

                action_map = {
                    "feed": "feed",
                    "care": "care",
                    "train": "train",
                    "freefight": "freefight",
                    "arena": "arena",
                    "evolve": "evolve",
                }

                if len(rest) == 1 and rest[0] in action_map:
                    result = apply_action(profile_id, action_map[rest[0]], payload)
                    self.send_json(200 if result.get("ok") else 400, result)
                    return

                if rest == ["battle", "ghost"]:
                    result = ghost_battle(profile_id, payload)
                    self.send_json(200 if result.get("ok") else 400, result)
                    return

            self.send_json(404, {"ok": False, "error": "not found", "path": path})

        except Exception as exc:
            self.send_json(500, {"ok": False, "error": str(exc)})


def main():
    init_db()

    host = os.getenv("DIGIPAD_HOST", "0.0.0.0")
    port = int(os.getenv("DIGIPAD_PORT", "8788"))

    server = ThreadingHTTPServer((host, port), Handler)

    print(f"DigiPad Container läuft: http://{host}:{port}/api/pad/health", flush=True)
    print("Offizielle Remote-Schicht für Family-Pad/Profile.", flush=True)
    print("Fiona Status: /api/pad/fiona/status?token=<TOKEN>", flush=True)

    server.serve_forever()


if __name__ == "__main__":
    main()
