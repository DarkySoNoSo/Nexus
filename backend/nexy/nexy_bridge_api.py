#!/usr/bin/env python3
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.parse import urlparse, parse_qs
from pathlib import Path
import json
import sys
import os
import hashlib
import re

sys.path.insert(0, str(Path(__file__).resolve().parent))
from nexy_memory import connect, init_db, now, uid

HOST = os.environ.get("NEXY_HOST", "127.0.0.1")
PORT = int(os.environ.get("NEXY_PORT", "8765"))
FILE_ROOT = Path(os.environ.get("NEXUS_STORAGE", r"C:\MasterIndex_Storage"))

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
        "nexy_decisions",
    ]:
        out[t] = db_one(f'SELECT COUNT(*) AS c FROM "{t}"')["c"]
    return out

def clean_text(value):
    if value is None:
        return ""
    return str(value).replace("\r", " ").replace("\n", " ").strip()

def compact_key(value):
    text = clean_text(value).lower()
    text = re.sub(r"[^a-z0-9]+", "_", text)
    text = re.sub(r"^_+|_+$", "", text)
    return text or "unknown"

def body_preview(value, limit=220):
    text = clean_text(value)
    return text if len(text) <= limit else text[: limit - 3] + "..."

def payload_of(row):
    raw = row.get("raw_payload") if row else None
    if not raw:
        return {}
    try:
        data = json.loads(raw)
        return data if isinstance(data, dict) else {}
    except Exception:
        return {}

def communication_source(payload, row):
    return clean_text(payload.get("source")) or clean_text(row.get("source")) or "Nexi"

def channel_type(payload, row):
    return clean_text(payload.get("channel_type")) or clean_text(row.get("event_type")) or "message"

def sender_name(payload, row):
    return (
        clean_text(payload.get("sender_raw"))
        or clean_text(payload.get("sender"))
        or clean_text(row.get("title"))
        or communication_source(payload, row)
        or "Unbekannt"
    )

def conversation_key_for(payload, row):
    sender_key = clean_text(payload.get("sender_normalized")) or sender_name(payload, row)
    return "conv:" + compact_key(channel_type(payload, row)) + ":" + compact_key(sender_key)

def decision_rank(action):
    order = {
        "very_important": 90,
        "timeline_focus": 70,
        "needs_reply": 60,
        "done": 20,
        "not_important": 10,
    }
    return order.get(action or "", 40)

def priority_band(payload, row, latest_decision=""):
    if latest_decision == "very_important":
        return "P0"
    semantic = payload.get("semantic") if isinstance(payload.get("semantic"), dict) else {}
    try:
        score = semantic.get("priority_score")
        if score is not None:
            score = float(score)
            if score >= 80:
                return "P0"
            if score >= 55:
                return "P1"
            if score >= 30:
                return "P2"
    except Exception:
        pass
    text = (clean_text(row.get("title")) + " " + clean_text(row.get("body"))).lower()
    if any(word in text for word in ["dringend", "mahnung", "inkasso", "frist", "sofort", "zahlung"]):
        return "P1"
    if semantic.get("requires_response") is True:
        return "P2"
    return "P3"

def latest_decisions(rows):
    ids = set()
    conversations = set()
    for row in rows:
        payload = payload_of(row)
        ids.add(row["id"])
        conversations.add(conversation_key_for(payload, row))
    if not ids and not conversations:
        return {}
    con = connect()
    cur = con.cursor()
    clauses = []
    args = []
    if ids:
        marks = ",".join("?" for _ in ids)
        clauses.append(f"source_event_id IN ({marks})")
        args.extend(sorted(ids))
    if conversations:
        marks = ",".join("?" for _ in conversations)
        clauses.append(f"conversation_id IN ({marks})")
        args.extend(sorted(conversations))
    cur.execute(f"""
        SELECT source_event_id, conversation_id, decision_value, decision_type, created_at
        FROM nexy_decisions
        WHERE status='active' AND ({' OR '.join(clauses)})
        ORDER BY created_at ASC
    """, args)
    out = {}
    for item in cur.fetchall():
        if item["source_event_id"]:
            out[item["source_event_id"]] = dict(item)
        if item["conversation_id"]:
            out[item["conversation_id"]] = dict(item)
    con.close()
    return out

def communication_event_rows(limit):
    return db_rows("""
        SELECT id, created_at, event_time, source, source_ref, event_type, title, body, raw_payload, importance, confidence, status
        FROM nexy_events
        WHERE status != 'deleted'
        ORDER BY COALESCE(event_time, created_at) DESC
        LIMIT ?
    """, (limit,))

def render_event(row, decisions=None):
    payload = payload_of(row)
    key = conversation_key_for(payload, row)
    decision = (decisions or {}).get(row["id"]) or (decisions or {}).get(key) or {}
    latest = decision.get("decision_value", "")
    body = clean_text(payload.get("body")) or clean_text(row.get("body"))
    title = clean_text(payload.get("title")) or clean_text(row.get("title"))
    return {
        "event_id": row["id"],
        "conversation_key": key,
        "event_time": row.get("event_time") or row.get("created_at"),
        "source": communication_source(payload, row),
        "channel_type": channel_type(payload, row),
        "sender": sender_name(payload, row),
        "sender_raw": clean_text(payload.get("sender_raw")),
        "sender_normalized": clean_text(payload.get("sender_normalized")),
        "title": title,
        "body": body,
        "body_preview": body_preview(body or title),
        "priority_band": priority_band(payload, row, latest),
        "suggested_action": action_hint(payload, row, latest),
        "latest_decision": latest,
        "decision_at": decision.get("created_at", ""),
        "semantic": payload.get("semantic") if isinstance(payload.get("semantic"), dict) else {},
        "status": row.get("status") or "open",
    }

def action_hint(payload, row, latest_decision=""):
    if latest_decision == "very_important":
        return "als sehr wichtig markiert"
    if latest_decision == "timeline_focus":
        return "im Zeitstrahl fokussieren"
    semantic = payload.get("semantic") if isinstance(payload.get("semantic"), dict) else {}
    if semantic.get("requires_response") is True:
        return "Antwort pruefen"
    band = priority_band(payload, row, latest_decision)
    if band in ["P0", "P1"]:
        return "zeitnah pruefen"
    return "pruefen"

def communication_events(limit=200):
    rows = communication_event_rows(limit)
    decisions = latest_decisions(rows)
    return [render_event(row, decisions) for row in rows]

def communication_conversations(limit=100):
    rows = communication_event_rows(max(limit * 8, 200))
    decisions = latest_decisions(rows)
    grouped = {}
    for row in rows:
        item = render_event(row, decisions)
        key = item["conversation_key"]
        current = grouped.get(key)
        rank = decision_rank(item["latest_decision"])
        if current is None:
            grouped[key] = {
                "event_id": item["event_id"],
                "conversation_key": key,
                "latest_event_time": item["event_time"],
                "source": item["source"],
                "channel_type": item["channel_type"],
                "sender": item["sender"],
                "sender_raw": item["sender_raw"],
                "body_preview": item["body_preview"],
                "priority_band": item["priority_band"],
                "suggested_action": item["suggested_action"],
                "latest_decision": item["latest_decision"],
                "decision_at": item["decision_at"],
                "message_count": 1,
                "open_count": 0 if item["latest_decision"] in ["done", "not_important"] else 1,
                "_rank": rank,
            }
        else:
            current["message_count"] += 1
            if item["latest_decision"] not in ["done", "not_important"]:
                current["open_count"] += 1
            if rank > current["_rank"]:
                current["priority_band"] = item["priority_band"]
                current["suggested_action"] = item["suggested_action"]
                current["_rank"] = rank
    out = list(grouped.values())
    for item in out:
        item.pop("_rank", None)
    out.sort(key=lambda item: (item.get("latest_decision") in ["done", "not_important"], item["latest_event_time"]), reverse=False)
    out.sort(key=lambda item: item["latest_event_time"], reverse=True)
    return out[:limit]

def communication_counters(items):
    open_items = [i for i in items if i.get("latest_decision") not in ["done", "not_important"]]
    return {
        "focus": len(open_items),
        "alerts": sum(1 for i in open_items if i.get("priority_band") in ["P0", "P1"]),
        "needs_reply": sum(1 for i in open_items if "antwort" in clean_text(i.get("suggested_action")).lower()),
        "total": len(items),
    }

def safe_file_path(rel_path):
    root = FILE_ROOT.resolve()
    requested = (root / clean_text(rel_path).replace("\\", "/")).resolve()
    try:
        requested.relative_to(root)
    except ValueError:
        raise ValueError("path outside file root")
    return root, requested

def file_item(path, root):
    stat = path.stat()
    rel = "" if path == root else path.relative_to(root).as_posix()
    is_dir = path.is_dir()
    return {
        "name": path.name,
        "path": rel,
        "type": "dir" if is_dir else "file",
        "is_dir": is_dir,
        "bytes": 0 if is_dir else stat.st_size,
        "modified": stat.st_mtime,
        "extension": "" if is_dir else path.suffix.lower(),
    }

def file_listing(rel_path="", limit=250):
    root, target = safe_file_path(rel_path)
    if not root.exists():
        return {"root": str(root), "path": "", "parent": "", "exists": False, "items": [], "count": 0}
    if not target.exists():
        raise FileNotFoundError(clean_text(rel_path) or str(root))
    if not target.is_dir():
        raise ValueError("path is not a directory")
    items = []
    for child in target.iterdir():
        try:
            items.append(file_item(child, root))
        except OSError:
            continue
    items.sort(key=lambda item: (0 if item["is_dir"] else 1, item["name"].lower()))
    rel = "" if target == root else target.relative_to(root).as_posix()
    parent = ""
    if target != root:
        parent_path = target.parent
        parent = "" if parent_path == root else parent_path.relative_to(root).as_posix()
    return {
        "root": str(root),
        "path": rel,
        "parent": parent,
        "exists": True,
        "items": items[:limit],
        "count": len(items),
        "returned": min(len(items), limit),
    }

def store_communication_event(payload):
    if not isinstance(payload, dict):
        raise ValueError("JSON object expected")
    timestamp = clean_text(payload.get("timestamp")) or now()
    source = clean_text(payload.get("source")) or "android_collector"
    channel = clean_text(payload.get("channel_type")) or "message"
    sender = clean_text(payload.get("sender_raw")) or clean_text(payload.get("sender_normalized")) or "unknown"
    title = clean_text(payload.get("title")) or sender
    body = clean_text(payload.get("body"))
    digest = hashlib.sha256(json.dumps({
        "timestamp": timestamp,
        "source": source,
        "channel": channel,
        "sender": sender,
        "body": body,
    }, sort_keys=True, ensure_ascii=False).encode("utf-8")).hexdigest()
    source_ref = "communication:" + digest
    existing = db_one("SELECT id FROM nexy_events WHERE source_ref=?", (source_ref,))
    if existing:
        return existing["id"], True
    eid = uid()
    con = connect()
    con.execute("""
        INSERT INTO nexy_events
        (id, created_at, event_time, source, source_ref, event_type, title, body, raw_payload, importance, confidence, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'open')
    """, (
        eid,
        now(),
        timestamp,
        source,
        source_ref,
        channel,
        title,
        body,
        json.dumps(payload, ensure_ascii=False, sort_keys=True),
        1,
        0.7,
    ))
    con.commit()
    con.close()
    return eid, False

def store_decision(event_id, action, source="api", note="", actor="app", scope="conversation"):
    if not event_id:
        raise ValueError("event_id missing")
    if not action:
        raise ValueError("action missing")
    row = db_one("""
        SELECT id, created_at, event_time, source, source_ref, event_type, title, body, raw_payload, importance, confidence, status
        FROM nexy_events
        WHERE id=?
    """, (event_id,))
    conversation_id = event_id if event_id.startswith("conv:") else ""
    if row:
        conversation_id = conversation_key_for(payload_of(row), row) if scope == "conversation" else ""
    did = uid()
    con = connect()
    con.execute("""
        INSERT INTO nexy_decisions
        (id, created_at, source, source_event_id, conversation_id, decision_type, decision_value, reason, actor, sync_status, applied_at, raw_payload, status)
        VALUES (?, ?, ?, ?, ?, 'message_action', ?, ?, ?, 'applied', ?, ?, 'active')
    """, (
        did,
        now(),
        source,
        row["id"] if row else ("" if event_id.startswith("conv:") else event_id),
        conversation_id,
        action,
        note,
        actor,
        now(),
        json.dumps({"event_id": event_id, "action": action, "scope": scope, "note": note}, ensure_ascii=False, sort_keys=True),
    ))
    if row and action == "timeline_focus":
        con.execute("""
            INSERT INTO nexy_timeline
            (id, event_id, timeline_time, actor, topic, summary, cause, consequence, linked_before, linked_after, visibility, status)
            VALUES (?, ?, ?, 'Nexi', 'Kommunikation', ?, 'App/Widget-Fokusentscheidung', 'Im Zeitstrahl sichtbar halten', '', '', 'normal', 'active')
        """, (uid(), row["id"], row["event_time"] or now(), body_preview(row["body"] or row["title"], 300)))
    con.commit()
    con.close()
    return did, conversation_id

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
        self.send_header("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_OPTIONS(self):
        self.send_json({"ok": True})

    def read_body(self):
        length = int(self.headers.get("Content-Length", "0") or "0")
        return self.rfile.read(length).decode("utf-8") if length > 0 else ""

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
            elif u.path in ["/api/nexy/timeline", "/api/timeline"]:
                self.send_json({"ok": True, "items": timeline(limit)})
            elif u.path == "/api/nexy/facts":
                self.send_json({"ok": True, "items": facts(limit)})
            elif u.path == "/api/nexy/briefing":
                self.send_json({"ok": True, "briefing": briefing()})
            elif u.path == "/api/communication/events":
                self.send_json({"ok": True, "items": communication_events(limit)})
            elif u.path == "/api/communication/conversations":
                items = communication_conversations(limit)
                self.send_json({"ok": True, "items": items, "counters": communication_counters(items)})
            elif u.path == "/api/widget/messages":
                items = communication_conversations(limit)
                self.send_json({"ok": True, "items": items, "counters": communication_counters(items)})
            elif u.path in ["/api/files/list", "/api/v1/files/list"]:
                rel_path = qs.get("path", [""])[0]
                try:
                    self.send_json({"ok": True, "data": file_listing(rel_path, limit)})
                except (ValueError, FileNotFoundError) as e:
                    self.send_json({"ok": False, "error": str(e), "path": rel_path}, 400)
            elif u.path == "/api/v1/files/folders":
                try:
                    data = file_listing("", limit)
                    folders = [item for item in data["items"] if item["is_dir"]]
                    self.send_json({"ok": True, "data": {"items": folders, "count": len(folders), "path": ""}})
                except (ValueError, FileNotFoundError) as e:
                    self.send_json({"ok": False, "error": str(e), "path": ""}, 400)
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

    def do_POST(self):
        try:
            u = urlparse(self.path)
            body = self.read_body()
            content_type = self.headers.get("Content-Type", "")
            if u.path == "/api/communication/ingest":
                payload = json.loads(body or "{}")
                event_id, duplicate = store_communication_event(payload)
                self.send_json({"ok": True, "event_id": event_id, "duplicate": duplicate})
            elif u.path == "/api/widget/message-action":
                form = parse_qs(body)
                event_id = form.get("event_id", [""])[0].strip()
                action = form.get("action", [""])[0].strip()
                scope = form.get("scope", ["conversation"])[0].strip() or "conversation"
                note = form.get("note", [""])[0].strip()
                decision_id, conversation_id = store_decision(event_id, action, source="widget_or_app", note=note, actor="android", scope=scope)
                self.send_json({
                    "ok": True,
                    "decision_id": decision_id,
                    "event_id": event_id,
                    "conversation_id": conversation_id,
                    "action": action,
                    "removed": action in ["done", "not_important"],
                })
            else:
                self.send_json({"ok": False, "error": "not found", "path": u.path}, 404)
        except json.JSONDecodeError as e:
            self.send_json({"ok": False, "error": "invalid json", "detail": str(e)}, 400)
        except Exception as e:
            self.send_json({"ok": False, "error": str(e)}, 500)

def main():
    init_db()
    print(f"Nexy Bridge läuft auf http://{HOST}:{PORT}")
    print("Endpoints:")
    print("  /api/nexy/status")
    print("  /api/nexy/briefing")
    print("  /api/nexy/search?q=Patrick")
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    server.serve_forever()

if __name__ == "__main__":
    main()
