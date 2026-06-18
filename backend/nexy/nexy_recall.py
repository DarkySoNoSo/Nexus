#!/usr/bin/env python3
from pathlib import Path
import sys, json, argparse

sys.path.insert(0, str(Path(__file__).resolve().parent))
from nexy_memory import connect

def rows(sql, args=()):
    con = connect()
    con.row_factory = None
    cur = con.cursor()
    cur.execute(sql, args)
    names = [d[0] for d in cur.description]
    out = [dict(zip(names, r)) for r in cur.fetchall()]
    con.close()
    return out

def print_json(obj):
    print(json.dumps(obj, indent=2, ensure_ascii=False))

def summary():
    tables = [
        "nexy_events",
        "nexy_timeline",
        "nexy_context",
        "nexy_facts",
        "nexy_lessons",
        "nexy_active_focus",
    ]
    out = {}
    for t in tables:
        out[t] = rows(f'SELECT COUNT(*) AS count FROM "{t}"')[0]["count"]
    print_json(out)

def focus(limit):
    print_json(rows("""
        SELECT focus_name, description, priority, next_action, related_context, status, updated_at
        FROM nexy_active_focus
        ORDER BY priority DESC, updated_at DESC
        LIMIT ?
    """, (limit,)))

def lessons(limit):
    print_json(rows("""
        SELECT lesson, trigger_context, mistake_prevented, rule, priority, status, created_at
        FROM nexy_lessons
        ORDER BY priority DESC, created_at DESC
        LIMIT ?
    """, (limit,)))

def facts(limit):
    print_json(rows("""
        SELECT statement, classification, evidence_ref, confidence, status
        FROM nexy_facts
        ORDER BY confidence DESC
        LIMIT ?
    """, (limit,)))

def timeline(limit):
    print_json(rows("""
        SELECT timeline_time, actor, topic, summary, cause, consequence, status
        FROM nexy_timeline
        ORDER BY timeline_time DESC
        LIMIT ?
    """, (limit,)))

def search(q, limit):
    like = f"%{q}%"
    result = {
        "events": rows("""
            SELECT event_time, event_type, title, body, source, source_ref
            FROM nexy_events
            WHERE title LIKE ? OR body LIKE ? OR event_type LIKE ?
            ORDER BY event_time DESC
            LIMIT ?
        """, (like, like, like, limit)),
        "timeline": rows("""
            SELECT timeline_time, topic, summary, cause, consequence
            FROM nexy_timeline
            WHERE topic LIKE ? OR summary LIKE ? OR cause LIKE ? OR consequence LIKE ?
            ORDER BY timeline_time DESC
            LIMIT ?
        """, (like, like, like, like, limit)),
        "facts": rows("""
            SELECT statement, classification, evidence_ref, confidence
            FROM nexy_facts
            WHERE statement LIKE ? OR evidence_ref LIKE ?
            ORDER BY confidence DESC
            LIMIT ?
        """, (like, like, limit)),
        "lessons": rows("""
            SELECT lesson, trigger_context, mistake_prevented, rule, priority
            FROM nexy_lessons
            WHERE lesson LIKE ? OR trigger_context LIKE ? OR mistake_prevented LIKE ? OR rule LIKE ?
            ORDER BY priority DESC
            LIMIT ?
        """, (like, like, like, like, limit)),
        "context": rows("""
            SELECT subject_type, subject_id, relation, object_type, object_id, weight, reason
            FROM nexy_context
            WHERE subject_id LIKE ? OR relation LIKE ? OR object_id LIKE ? OR reason LIKE ?
            ORDER BY weight DESC
            LIMIT ?
        """, (like, like, like, like, limit)),
    }
    print_json(result)

def main():
    p = argparse.ArgumentParser(description="Nexy Recall Engine v1")
    p.add_argument("command", choices=["summary", "focus", "lessons", "facts", "timeline", "search"])
    p.add_argument("query", nargs="?", default="")
    p.add_argument("--limit", type=int, default=10)
    args = p.parse_args()

    if args.command == "summary":
        summary()
    elif args.command == "focus":
        focus(args.limit)
    elif args.command == "lessons":
        lessons(args.limit)
    elif args.command == "facts":
        facts(args.limit)
    elif args.command == "timeline":
        timeline(args.limit)
    elif args.command == "search":
        if not args.query:
            raise SystemExit("Suchbegriff fehlt. Beispiel: python backend\\nexy\\nexy_recall.py search Safe-Start")
        search(args.query, args.limit)

if __name__ == "__main__":
    main()
