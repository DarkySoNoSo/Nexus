#!/usr/bin/env python3
from pathlib import Path
import os, sys, json, hashlib

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from nexy_memory import init_db, connect, uid, now, add_event, add_fact

ROOT = Path(os.environ.get("NEXUS_STORAGE", r"C:\MasterIndex_Storage"))
SRC = ROOT / "context" / "context_chef_channel.jsonl"

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
        ("legacy_chef_channel", source_ref)
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

def main():
    init_db()

    if not SRC.exists():
        raise SystemExit(f"Quelle fehlt: {SRC}")

    digest = sha256_file(SRC)
    source_ref = f"{SRC}#sha256={digest}"

    if already_imported(source_ref):
        print(json.dumps({"ok": True, "skipped": True, "reason": "already_imported"}, indent=2, ensure_ascii=False))
        return

    meta_event = add_event(
        source="legacy_chef_channel",
        source_ref=source_ref,
        event_type="legacy_chef_channel_import",
        title="Legacy Chef-Kanal importiert",
        body="context_chef_channel.jsonl wurde als Chef-/Regel-/Entscheidungsquelle nach Nexy übernommen.",
        raw_payload=json.dumps({"path": str(SRC), "sha256": digest}, ensure_ascii=False),
        importance=10,
        confidence=1.0
    )

    add_fact(
        statement="context_chef_channel.jsonl ist eine Legacy-Quelle für Chef-Regeln, Kontextentscheidungen und Nutzeranweisungen.",
        classification="fact",
        evidence_ref=source_ref,
        source_event_id=meta_event,
        confidence=1.0
    )

    imported_events = 0
    imported_lessons = 0
    invalid = 0

    with SRC.open("r", encoding="utf-8", errors="replace") as f:
        for idx, line in enumerate(f, start=1):
            if not line.strip():
                continue

            try:
                obj = json.loads(line)
            except Exception:
                invalid += 1
                continue

            created = obj.get("created_utc") or now()
            role = obj.get("role", "unknown")
            text = obj.get("text", "")
            section = obj.get("section", "")
            action = obj.get("action", "")

            title = f"Chef-Kanal: {role}"
            if section:
                title += f" / {section}"

            event_id = add_event(
                source="legacy_chef_channel",
                source_ref=f"{source_ref}#line={idx}",
                event_type=f"chef_channel_{role}",
                title=title[:300],
                body=text[:6000],
                raw_payload=json.dumps(obj, ensure_ascii=False),
                importance=8 if role == "user" else 7,
                confidence=1.0
            )
            imported_events += 1

            lower = text.lower()
            if "regel" in lower or "immer" in lower or "niemals" in lower or action in ["context_input", "rule", "lesson"]:
                add_lesson(
                    lesson=text[:1200],
                    trigger_context=f"Chef-Kanal Abschnitt: {section}; Aktion: {action}",
                    mistake_prevented="Verlust wichtiger Nutzerregeln und Chef-Entscheidungen.",
                    rule=text[:1200],
                    priority=9 if role == "user" else 8
                )
                imported_lessons += 1

    print(json.dumps({
        "ok": True,
        "source": str(SRC),
        "sha256": digest,
        "meta_event_id": meta_event,
        "events_imported": imported_events,
        "lessons_imported": imported_lessons,
        "invalid_lines": invalid
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
