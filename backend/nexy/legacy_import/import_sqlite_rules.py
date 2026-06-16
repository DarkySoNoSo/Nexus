#!/usr/bin/env python3
from pathlib import Path
import os, sys, sqlite3, json, hashlib

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from nexy_memory import init_db, connect, uid, now, add_event

ROOT = Path(os.environ.get("NEXUS_STORAGE", r"C:\MasterIndex_Storage"))
LEGACY_DB = ROOT / "_NEXUS_SYSTEM" / "db" / "nexus_catalog.sqlite"

TABLES = [
    "chef_learning_rules",
    "learning_rules",
    "context_rules",
    "communication_rules",
    "tag_rules",
]

CANDIDATE_FIELDS = [
    "lesson", "rule", "text", "content", "title", "name",
    "description", "summary", "pattern", "action", "decision"
]

def file_hash(path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def already_imported(source_ref):
    con = connect()
    row = con.execute(
        "SELECT COUNT(*) FROM nexy_events WHERE source=? AND source_ref=?",
        ("legacy_sqlite_rules", source_ref)
    ).fetchone()
    con.close()
    return row[0] > 0

def add_lesson(lesson, trigger_context="", mistake_prevented="", rule="", priority=8):
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

def best_text(row):
    parts = []
    keys = row.keys()
    for k in CANDIDATE_FIELDS:
        if k in keys and row[k]:
            v = str(row[k]).strip()
            if v and v not in parts:
                parts.append(v)
    if parts:
        return " | ".join(parts)[:1200]

    fallback = []
    for k in keys:
        v = row[k]
        if v is None:
            continue
        s = str(v).strip()
        if s:
            fallback.append(f"{k}: {s}")
    return " | ".join(fallback)[:1200]

def main():
    init_db()

    if not LEGACY_DB.exists():
        raise SystemExit(f"Legacy-DB fehlt: {LEGACY_DB}")

    db_hash = file_hash(LEGACY_DB)
    source_ref = f"{LEGACY_DB}#sha256={db_hash}#tables={','.join(TABLES)}"

    if already_imported(source_ref):
        print(json.dumps({"ok": True, "skipped": True, "reason": "already_imported"}, indent=2, ensure_ascii=False))
        return

    legacy = sqlite3.connect(LEGACY_DB)
    legacy.row_factory = sqlite3.Row
    cur = legacy.cursor()

    imported = {}
    total_lessons = 0

    event_id = add_event(
        source="legacy_sqlite_rules",
        source_ref=source_ref,
        event_type="legacy_rules_import",
        title="Legacy SQLite Regel-Tabellen importiert",
        body="Import aus chef_learning_rules, learning_rules, context_rules, communication_rules und tag_rules.",
        raw_payload=json.dumps({"db": str(LEGACY_DB), "tables": TABLES}, ensure_ascii=False),
        importance=9,
        confidence=1.0
    )

    for table in TABLES:
        exists = cur.execute(
            "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
            (table,)
        ).fetchone()

        if not exists:
            imported[table] = {"exists": False, "imported": 0}
            continue

        rows = cur.execute(f'SELECT * FROM "{table}"').fetchall()
        count = 0

        for row in rows:
            lesson = best_text(row)
            if not lesson:
                continue

            add_lesson(
                lesson=lesson,
                trigger_context=f"Legacy SQLite Tabelle {table}",
                mistake_prevented="Verlust bestehender Nexus-/Chef-/Kontextregeln bei Migration.",
                rule=lesson,
                priority=8
            )
            count += 1

        imported[table] = {"exists": True, "rows": len(rows), "imported": count}
        total_lessons += count

    legacy.close()

    print(json.dumps({
        "ok": True,
        "event_id": event_id,
        "source_ref": source_ref,
        "imported_tables": imported,
        "total_lessons_imported": total_lessons
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
