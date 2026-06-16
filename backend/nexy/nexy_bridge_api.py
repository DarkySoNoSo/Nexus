#!/usr/bin/env python3
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from pathlib import Path
import json
import sys
import os

sys.path.insert(0, str(Path(__file__).resolve().parent))
from nexy_memory import connect

HOST = os.environ.get("NEXY_HOST", "127.0.0.1")
PORT = int(os.environ.get("NEXY_PORT", "8765"))

def db_rows(sql, args=()):
    con = connect()
    cur = con.cursor()
    cur.execute(sql, args)
    names = [d[0] for d in cur.description]
    rows = [dict(zip(names, r)) for r in cur.fetchall()]
    con.close()
    return rows

def db_one(sql, args=()):
    r = db_rows(sql, args)
    return r[0] if r else None

def counts():
    out = {}
    for t in [
        "nexy_events",
        "nexy_timeline",
        "nexy_context",
        "nexy_facts",
        "nexy_lessons",
        "nexy_active_focus",
    ]:
        out[t] = db_one(f'SELECT COUNT(*) AS c FROM "{t}"')["c"]
    return out

def focus(limit=10):
    return db_rows("""
        SELECT focus_name, description, priority, next_action, related_context, status, updated_at
        FROM nexy_active_focus
        ORDER BY priority DESC, updated_at DESC
        LIMIT ?
    """, (limit,))

def lessons(limit=10):
    return db_rows("""
        SELECT lesson, trigger_context, mistake_prevented, rule, priority, status, created_at
        FROM nexy_lessons
        ORDER BY priority DESC, created_at DESC
        LIMIT ?
    """, (limit,))

def timeline(limit=10):
    return db_rows("""
        SELECT timeline_time, actor, topic, summary, cause, consequence, status
        FROM nexy_timeline
        ORDER BY timeline_time DESC
        LIMIT ?
    """, (limit,))

def facts(limit=10):
    return db_rows("""
        SELECT statement, classification, evidence_ref, confidence, status
        FROM nexy_facts
        ORDER BY confidence DESC
        LIMIT ?
    """, (limit,))

def search(q, limit=10):
    like = f"%{q}%"
    return {
        "query": q,
        "events": db_rows("""
            SELECT event_time, event_type, title, body, source, source_ref
            FROM nexy_events
            WHERE title LIKE ? OR body LIKE ? OR event_type LIKE ?
            ORDER BY event_time DESC
            LIMIT ?
        """, (like, like, like, limit)),
        "timeline": db_rows("""
            SELECT timeline_time, topic, summary, cause, consequence
            FROM nexy_timeline
            WHERE topic LIKE ? OR summary LIKE ? OR cause LIKE ? OR consequence LIKE ?
            ORDER BY timeline_time DESC
            LIMIT ?
        """, (like, like, like, like, limit)),
        "facts": db_rows("""
            SELECT statement, classification, evidence_ref, confidence
            FROM nexy_facts
            WHERE statement LIKE ? OR evidence_ref LIKE ?
            ORDER BY confidence DESC
            LIMIT ?
        """, (like, like, limit)),
        "lessons": db_rows("""
            SELECT lesson, trigger_context, mistake_prevented, rule, priority
            FROM nexy_lessons
            WHERE lesson LIKE ? OR trigger_context LIKE ? OR mistake_prevented LIKE ? OR rule LIKE ?
            ORDER BY priority DESC
            LIMIT ?
        """, (like, like, like, like, limit)),
        "context": db_rows("""
            SELECT subject_type, subject_id, relation, object_type, object_id, weight, reason
            FROM nexy_context
            WHERE subject_id LIKE ? OR relation LIKE ? OR object_id LIKE ? OR reason LIKE ?
            ORDER BY weight DESC
            LIMIT ?
        """, (like, like, like, like, limit)),
    }

def briefing():
    return {
        "status": counts(),
        "focus": focus(5),
        "top_lessons": lessons(8),
        "latest_timeline": timeline(8),
        "facts": facts(8),
    }

class Handler(BaseHTTPRequestHandler):
    def send_json(self, obj, code=200):
        body = json.dumps(obj, ensure_ascii=False, indent=2).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Methods", "GET, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_json({"ok": True})

    def do_GET(self):
        try:
            u = urlparse(self.path)
            qs = parse_qs(u.query)
            limit = int(qs.get("limit", ["10"])[0])

            if u.path in ["/", "/health", "/api/nexy/health"]:
                self.send_json({"ok": True, "service": "nexy_bridge", "counts": counts()})
            elif u.path == "/api/nexy/status":
                self.send_json({"ok": True, "counts": counts()})
            elif u.path == "/api/nexy/focus":
                self.send_json({"ok": True, "items": focus(limit)})
            elif u.path == "/api/nexy/lessons":
                self.send_json({"ok": True, "items": lessons(limit)})
            elif u.path == "/api/nexy/timeline":
                self.send_json({"ok": True, "items": timeline(limit)})
            elif u.path == "/api/nexy/facts":
                self.send_json({"ok": True, "items": facts(limit)})
            elif u.path == "/api/nexy/briefing":
                self.send_json({"ok": True, "briefing": briefing()})
            elif u.path == "/api/nexy/search":
                q = qs.get("q", [""])[0].strip()
                if not q:
                    self.send_json({"ok": False, "error": "missing query parameter q"}, 400)
                else:
                    self.send_json({"ok": True, "result": search(q, limit)})
            else:
                self.send_json({"ok": False, "error": "not found", "path": u.path}, 404)
        except Exception as e:
            self.send_json({"ok": False, "error": str(e)}, 500)

def main():
    print(f"Nexy Bridge läuft auf http://{HOST}:{PORT}")
    print("Endpoints:")
    print("  /api/nexy/status")
    print("  /api/nexy/briefing")
    print("  /api/nexy/search?q=Patrick")
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    server.serve_forever()

if __name__ == "__main__":
    main()
