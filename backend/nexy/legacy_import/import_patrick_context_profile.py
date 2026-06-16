#!/usr/bin/env python3
from pathlib import Path
import os, sys, re, json, hashlib

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from nexy_memory import init_db, connect, uid, now, add_event, add_fact, set_focus

ROOT = Path(os.environ.get("NEXUS_STORAGE", r"C:\MasterIndex_Storage"))
SRC = ROOT / "context" / "patrick_context_profile.md"

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
        ("legacy_patrick_context_profile", source_ref)
    ).fetchone()
    con.close()
    return row[0] > 0

def split_sections(text):
    sections = []
    current_title = "Vorspann"
    current_lines = []

    for line in text.splitlines():
        if line.startswith("## "):
            if current_lines:
                sections.append((current_title, "\n".join(current_lines).strip()))
            current_title = line.replace("##", "", 1).strip()
            current_lines = []
        else:
            current_lines.append(line)

    if current_lines:
        sections.append((current_title, "\n".join(current_lines).strip()))

    return [(t, b) for t, b in sections if b.strip()]

def insert_context(subject_type, subject_id, relation, object_type, object_id, weight, reason):
    con = connect()
    cid = uid()
    con.execute("""
        INSERT INTO nexy_context
        (id, subject_type, subject_id, relation, object_type, object_id, weight, reason, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (cid, subject_type, subject_id, relation, object_type, object_id, weight, reason, now()))
    con.commit()
    con.close()
    return cid

def main():
    init_db()

    if not SRC.exists():
        raise SystemExit(f"Quelle fehlt: {SRC}")

    text = SRC.read_text(encoding="utf-8", errors="replace")
    digest = sha256_file(SRC)
    source_ref = f"{SRC}#sha256={digest}"

    if already_imported(source_ref):
        print(json.dumps({"ok": True, "skipped": True, "reason": "already_imported"}, indent=2, ensure_ascii=False))
        return

    main_event = add_event(
        source="legacy_patrick_context_profile",
        source_ref=source_ref,
        event_type="identity_context_import",
        title="Patrick Context Profile importiert",
        body=text[:8000],
        raw_payload=json.dumps({"path": str(SRC), "sha256": digest, "chars": len(text)}, ensure_ascii=False),
        importance=10,
        confidence=1.0
    )

    add_fact(
        statement="patrick_context_profile.md ist der primäre persönliche Kontextanker für Nexy.",
        classification="fact",
        evidence_ref=source_ref,
        source_event_id=main_event,
        confidence=1.0
    )

    sections = split_sections(text)
    imported_sections = 0

    for title, body in sections:
        if not body:
            continue

        sec_event = add_event(
            source="legacy_patrick_context_profile",
            source_ref=f"{source_ref}#section={title}",
            event_type="identity_context_section",
            title=f"Kontextprofil: {title}",
            body=body[:6000],
            raw_payload=json.dumps({"section": title, "chars": len(body)}, ensure_ascii=False),
            importance=9,
            confidence=0.9
        )

        insert_context(
            subject_type="person",
            subject_id="Patrick Herzog",
            relation="has_context_section",
            object_type="legacy_profile_section",
            object_id=sec_event,
            weight=9,
            reason=f"Abschnitt aus patrick_context_profile.md: {title}"
        )

        imported_sections += 1

    set_focus(
        focus_name="Patrick-Kontext internalisieren",
        description="Nexy hat patrick_context_profile.md als primären Identitätsanker importiert. Nächster Schritt: Kommunikationskontext und Chef-Kanal kontrolliert verdichten.",
        priority=10,
        next_action="communication_context.md nicht roh als Fakten importieren, sondern Regeln/Summaries extrahieren.",
        related_context="Nexy Identity / Legacy Memory"
    )

    print(json.dumps({
        "ok": True,
        "source": str(SRC),
        "sha256": digest,
        "main_event_id": main_event,
        "sections_imported": imported_sections
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
