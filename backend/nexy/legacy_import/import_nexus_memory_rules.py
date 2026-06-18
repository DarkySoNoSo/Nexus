#!/usr/bin/env python3
from pathlib import Path
import os, sys, re, hashlib, json
from datetime import datetime, timezone

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from nexy_memory import init_db, connect, uid, now, add_event, add_fact

ROOT = Path(os.environ.get("NEXUS_STORAGE", r"C:\MasterIndex_Storage"))
SRC = ROOT / "nexus_memory.json"

def sha256_text(text):
    return hashlib.sha256(text.encode("utf-8", errors="replace")).hexdigest()

def add_lesson(lesson, trigger_context="", mistake_prevented="", rule="", priority=7):
    con = connect()
    lid = uid()
    con.execute("""
        INSERT INTO nexy_lessons
        (id, lesson, trigger_context, mistake_prevented, rule, priority, created_at, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, 'active')
    """, (lid, lesson, trigger_context, mistake_prevented, rule, priority, now()))
    con.commit()
    con.close()
    return lid

def already_imported(source_ref):
    con = connect()
    row = con.execute(
        "SELECT COUNT(*) FROM nexy_events WHERE source=? AND source_ref=?",
        ("legacy_nexus_memory", source_ref)
    ).fetchone()
    con.close()
    return row[0] > 0

def extract_rules(text):
    rules = []
    for line in text.splitlines():
        s = line.strip()
        m = re.match(r"^(\d+)[\.\)]\s+(.+)$", s)
        if m:
            rules.append(m.group(2).strip())
        elif s.startswith("- ") and len(s) > 8:
            rules.append(s[2:].strip())
    clean = []
    seen = set()
    for r in rules:
        if r.lower() not in seen:
            seen.add(r.lower())
            clean.append(r)
    return clean[:80]

def main():
    init_db()

    if not SRC.exists():
        raise SystemExit(f"Quelle fehlt: {SRC}")

    text = SRC.read_text(encoding="utf-8", errors="replace")
    digest = sha256_text(text)
    source_ref = f"{SRC}#sha256={digest}"

    if already_imported(source_ref):
        print(json.dumps({"ok": True, "skipped": True, "reason": "already_imported", "source_ref": source_ref}, indent=2, ensure_ascii=False))
        return

    event_id = add_event(
        source="legacy_nexus_memory",
        source_ref=source_ref,
        event_type="legacy_memory_import",
        title="Legacy Nexus Memory Regelwerk importiert",
        body=text[:5000],
        raw_payload=json.dumps({"path": str(SRC), "sha256": digest, "chars": len(text)}, ensure_ascii=False),
        importance=10,
        confidence=1.0
    )

    add_fact(
        statement="nexus_memory.json ist ein Legacy-Regelwerk im Klartextformat, nicht valides JSON.",
        classification="fact",
        evidence_ref=source_ref,
        source_event_id=event_id,
        confidence=1.0
    )

    rules = extract_rules(text)
    for r in rules:
        add_lesson(
            lesson=r,
            trigger_context="Legacy Nexus Memory / Masterindex-Chef-Orchestrator",
            mistake_prevented="Unkontrolliertes Verschieben, falsche Klassifikation oder nicht nachvollziehbare Entscheidungen.",
            rule=r,
            priority=8
        )

    print(json.dumps({
        "ok": True,
        "source": str(SRC),
        "sha256": digest,
        "event_id": event_id,
        "lessons_imported": len(rules)
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
