#!/usr/bin/env python3
from pathlib import Path
import sys
import json
from datetime import datetime

sys.path.insert(0, str(Path(__file__).resolve().parent))
from nexy_memory import connect

def fetch(sql, args=()):
    con = connect()
    con.row_factory = None
    cur = con.cursor()
    cur.execute(sql, args)
    names = [d[0] for d in cur.description]
    rows = [dict(zip(names, r)) for r in cur.fetchall()]
    con.close()
    return rows

def one(sql, args=()):
    rows = fetch(sql, args)
    return rows[0] if rows else None

def main():
    counts = {}
    for t in [
        "nexy_events",
        "nexy_timeline",
        "nexy_context",
        "nexy_facts",
        "nexy_lessons",
        "nexy_active_focus",
    ]:
        counts[t] = one(f'SELECT COUNT(*) AS c FROM "{t}"')["c"]

    focus = fetch("""
        SELECT focus_name, description, priority, next_action, related_context
        FROM nexy_active_focus
        WHERE status='active'
        ORDER BY priority DESC, updated_at DESC
        LIMIT 5
    """)

    lessons = fetch("""
        SELECT lesson, trigger_context, mistake_prevented, priority
        FROM nexy_lessons
        WHERE status='active'
        ORDER BY priority DESC, created_at DESC
        LIMIT 10
    """)

    timeline = fetch("""
        SELECT timeline_time, topic, summary
        FROM nexy_timeline
        WHERE status='active'
        ORDER BY timeline_time DESC
        LIMIT 10
    """)

    facts = fetch("""
        SELECT statement, classification, evidence_ref, confidence
        FROM nexy_facts
        WHERE status='active'
        ORDER BY confidence DESC
        LIMIT 10
    """)

    lines = []
    lines.append("# NEXY BRIEFING")
    lines.append("")
    lines.append(f"Stand: {datetime.now().isoformat(timespec='seconds')}")
    lines.append("")
    lines.append("## Speicherstatus")
    lines.append("")
    for k, v in counts.items():
        lines.append(f"- {k}: {v}")
    lines.append("")
    lines.append("## Aktiver Fokus")
    lines.append("")
    for f in focus:
        lines.append(f"### {f['focus_name']} — Priorität {f['priority']}")
        lines.append(f"- Beschreibung: {f['description']}")
        lines.append(f"- Nächster Schritt: {f['next_action']}")
        lines.append(f"- Kontext: {f['related_context']}")
        lines.append("")
    lines.append("## Wichtigste Lessons")
    lines.append("")
    for l in lessons:
        lines.append(f"- **{l['lesson']}**")
        if l.get("trigger_context"):
            lines.append(f"  - Kontext: {l['trigger_context']}")
        if l.get("mistake_prevented"):
            lines.append(f"  - Verhindert: {l['mistake_prevented']}")
    lines.append("")
    lines.append("## Letzte Timeline-Ereignisse")
    lines.append("")
    for t in timeline:
        lines.append(f"- {t['timeline_time']} | {t['topic']}: {t['summary']}")
    lines.append("")
    lines.append("## Gesicherte Fakten")
    lines.append("")
    for f in facts:
        lines.append(f"- [{f['classification']}] {f['statement']} — confidence {f['confidence']}")
    lines.append("")

    out = Path("docs/nexy/NEXY_LOCAL_BRIEFING.md")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text("\n".join(lines), encoding="utf-8")

    print(json.dumps({
        "ok": True,
        "briefing": str(out),
        "counts": counts
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
