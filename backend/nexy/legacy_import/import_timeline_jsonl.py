#!/usr/bin/env python3
from pathlib import Path
import os, sys, json, hashlib

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from nexy_memory import init_db, connect, uid, now, add_event

ROOT = Path(os.environ.get("NEXUS_STORAGE", r"C:\MasterIndex_Storage"))
SRC = ROOT / "timeline" / "nexus_timeline.jsonl"

def sha256_file(path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def already_imported(source_ref):
    con = connect()
    row = con.execute(
        "SELECT COUNT(*) FROM nexy_events WHERE source=? AND source_ref=?",
        ("legacy_timeline_jsonl", source_ref)
    ).fetchone()
    con.close()
    return row[0] > 0

def insert_event(con, *, event_time, source_ref, event_type, title, body, raw_payload, importance=7, confidence=1.0):
    eid = uid()
    con.execute("""
        INSERT INTO nexy_events
        (id, created_at, event_time, source, source_ref, event_type, title, body, raw_payload, importance, confidence, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'active')
    """, (
        eid, now(), event_time, "legacy_timeline_jsonl", source_ref,
        event_type, title, body, raw_payload, importance, confidence
    ))
    return eid

def insert_timeline(con, *, event_id, timeline_time, topic, summary, cause="", consequence=""):
    tid = uid()
    con.execute("""
        INSERT INTO nexy_timeline
        (id, event_id, timeline_time, actor, topic, summary, cause, consequence, linked_before, linked_after, visibility, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'active')
    """, (
        tid, event_id, timeline_time, "Nexus Legacy", topic, summary,
        cause, consequence, "", "", "internal"
    ))
    return tid

def main():
    init_db()

    if not SRC.exists():
        raise SystemExit(f"Quelle fehlt: {SRC}")

    digest = sha256_file(SRC)
    source_ref = f"{SRC}#sha256={digest}"

    if already_imported(source_ref):
        print(json.dumps({"ok": True, "skipped": True, "reason": "already_imported"}, indent=2, ensure_ascii=False))
        return

    imported = 0
    invalid = 0

    con = connect()

    meta_id = insert_event(
        con,
        event_time=now(),
        source_ref=source_ref,
        event_type="legacy_timeline_import",
        title="Legacy Nexus Timeline importiert",
        body="Import aus nexus_timeline.jsonl in nexy_events und nexy_timeline.",
        raw_payload=json.dumps({"path": str(SRC), "sha256": digest}, ensure_ascii=False),
        importance=10,
        confidence=1.0
    )

    with SRC.open("r", encoding="utf-8", errors="replace") as f:
        for line in f:
            if not line.strip():
                continue

            try:
                obj = json.loads(line)
            except Exception:
                invalid += 1
                continue

            ts = obj.get("ts") or obj.get("created_at") or obj.get("time") or now()
            kind = obj.get("kind") or obj.get("type") or "timeline_event"
            title = obj.get("title") or kind
            summary = obj.get("summary") or obj.get("text") or obj.get("body") or title
            data = obj.get("data", {})

            if len(summary) > 4000:
                summary = summary[:4000] + "...<cut>"

            event_id = insert_event(
                con,
                event_time=ts,
                source_ref=f"{source_ref}#line={imported+1}",
                event_type=str(kind),
                title=str(title)[:300],
                body=str(summary),
                raw_payload=json.dumps(obj, ensure_ascii=False),
                importance=7,
                confidence=1.0
            )

            insert_timeline(
                con,
                event_id=event_id,
                timeline_time=ts,
                topic=str(kind)[:200],
                summary=str(summary),
                cause="Legacy-Zeitstrahl",
                consequence="In Nexy-Timeline übernommen"
            )

            imported += 1

    con.commit()
    con.close()

    print(json.dumps({
        "ok": True,
        "source": str(SRC),
        "sha256": digest,
        "meta_event_id": meta_id,
        "timeline_events_imported": imported,
        "invalid_lines": invalid
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
