#!/usr/bin/env python3
from pathlib import Path
import sqlite3
import json
import os
from datetime import datetime, timezone

ROOT = Path(os.environ.get("NEXUS_STORAGE", r"C:\MasterIndex_Storage"))
OUT_JSON = Path("docs/nexy/NEXY_LEGACY_IMPORT_PREVIEW.json")
OUT_MD = Path("docs/nexy/NEXY_LEGACY_IMPORT_PREVIEW.md")

SOURCES = {
    "patrick_context_profile": ROOT / "context" / "patrick_context_profile.md",
    "communication_context": ROOT / "context" / "communication_context.md",
    "chef_channel": ROOT / "context" / "context_chef_channel.jsonl",
    "nexus_memory": ROOT / "nexus_memory.json",
    "timeline_jsonl": ROOT / "timeline" / "nexus_timeline.jsonl",
    "patrick_timeline_md": ROOT / "timeline" / "patrick_timeline.md",
    "legacy_sqlite": ROOT / "_NEXUS_SYSTEM" / "db" / "nexus_catalog.sqlite",
}

SQLITE_MAPPING = {
    "timeline_events": ["nexy_events", "nexy_timeline"],
    "timeline_claims": ["nexy_facts"],
    "chef_events": ["nexy_events"],
    "chef_facts": ["nexy_facts"],
    "chef_learning_rules": ["nexy_lessons"],
    "learning_rules": ["nexy_lessons"],
    "communication_events": ["nexy_events"],
    "communication_daily_summaries": ["nexy_context"],
    "context_entities": ["nexy_context"],
    "context_relations": ["nexy_context"],
    "context_rules": ["nexy_lessons"],
    "legacy_entries": ["nexy_events"],
    "semantic_cards": ["recall_index_later"],
    "files": ["master_index_reference_only"],
}

def now():
    return datetime.now(timezone.utc).isoformat()

def read_text_preview(path, max_chars=1500):
    text = path.read_text(encoding="utf-8", errors="replace")
    return {
        "lines": text.count("\n") + 1,
        "chars": len(text),
        "preview": text[:max_chars].replace("\r", "")
    }

def read_jsonl_preview(path, limit=5):
    total = 0
    valid = 0
    invalid = 0
    samples = []
    with path.open("r", encoding="utf-8", errors="replace") as f:
        for line in f:
            if not line.strip():
                continue
            total += 1
            try:
                obj = json.loads(line)
                valid += 1
                if len(samples) < limit:
                    samples.append(obj)
            except Exception as e:
                invalid += 1
                if len(samples) < limit:
                    samples.append({"invalid_line_error": str(e), "line": line[:500]})
    return {
        "jsonl_lines": total,
        "valid_json": valid,
        "invalid_json": invalid,
        "samples": samples
    }

def sqlite_table_preview(db_path, table, limit=3):
    con = sqlite3.connect(db_path)
    con.row_factory = sqlite3.Row
    cur = con.cursor()

    exists = cur.execute(
        "SELECT name FROM sqlite_master WHERE type='table' AND name=?",
        (table,)
    ).fetchone()

    if not exists:
        con.close()
        return {"exists": False}

    count = cur.execute(f'SELECT COUNT(*) AS c FROM "{table}"').fetchone()["c"]
    cols = [r["name"] for r in cur.execute(f'PRAGMA table_info("{table}")').fetchall()]

    rows = []
    for row in cur.execute(f'SELECT * FROM "{table}" LIMIT {limit}').fetchall():
        item = {}
        for k in row.keys():
            v = row[k]
            if isinstance(v, bytes):
                item[k] = f"<bytes:{len(v)}>"
            elif isinstance(v, str) and len(v) > 500:
                item[k] = v[:500] + "...<cut>"
            else:
                item[k] = v
        rows.append(item)

    con.close()
    return {
        "exists": True,
        "count": count,
        "columns": cols,
        "sample_rows": rows
    }

def build_preview():
    preview = {
        "created_at": now(),
        "nexus_storage": str(ROOT),
        "mode": "preview_only_no_import",
        "sources": {},
        "proposed_import_batches": [],
        "warnings": [],
        "next_decisions_needed": []
    }

    for name, path in SOURCES.items():
        item = {
            "path": str(path),
            "exists": path.exists(),
            "proposed_targets": []
        }

        if not path.exists():
            item["warning"] = "Quelle nicht gefunden."
            preview["warnings"].append(f"{name}: Quelle nicht gefunden: {path}")
            preview["sources"][name] = item
            continue

        item["size_bytes"] = path.stat().st_size

        if name == "patrick_context_profile":
            item.update(read_text_preview(path))
            item["proposed_targets"] = ["nexy_events", "nexy_facts", "nexy_context"]
            preview["proposed_import_batches"].append({
                "batch": "identity_context",
                "source": name,
                "target": ["nexy_events", "nexy_facts", "nexy_context"],
                "mode": "section_based_manual_review_first",
                "risk": "medium",
                "reason": "Identitätsanker, Familien-/Projekt-/Arbeitskontext. Muss sauber in Fact/Claim/Unclear getrennt werden."
            })

        elif name == "communication_context":
            item.update(read_text_preview(path))
            item["proposed_targets"] = ["nexy_events", "nexy_context", "nexy_lessons"]
            preview["proposed_import_batches"].append({
                "batch": "communication_context",
                "source": name,
                "target": ["nexy_events", "nexy_context", "nexy_lessons"],
                "mode": "summaries_and_rules_first",
                "risk": "high",
                "reason": "Großes Kommunikationsmaterial, externe Inhalte sind untrusted evidence. Nicht blind als Fakten speichern."
            })

        elif name == "chef_channel":
            item.update(read_jsonl_preview(path))
            item["proposed_targets"] = ["nexy_events", "nexy_facts", "nexy_lessons"]
            preview["proposed_import_batches"].append({
                "batch": "chef_channel",
                "source": name,
                "target": ["nexy_events", "nexy_facts", "nexy_lessons"],
                "mode": "jsonl_event_import_after_schema_mapping",
                "risk": "medium",
                "reason": "Chef-Kanal enthält Nutzerregeln, Entscheidungen und Lernpunkte."
            })

        elif name == "nexus_memory":
            item.update(read_text_preview(path))
            item["proposed_targets"] = ["nexy_lessons", "nexy_facts"]
            item["note"] = "Datei endet auf .json, ist aber Klartext-Regelwerk."
            preview["warnings"].append("nexus_memory.json ist kein JSON, sondern Klartext mit .json-Endung.")
            preview["proposed_import_batches"].append({
                "batch": "nexus_memory_rules",
                "source": name,
                "target": ["nexy_lessons", "nexy_facts"],
                "mode": "parse_as_text_rules",
                "risk": "low",
                "reason": "Enthält Arbeitsregeln und Orchestrator-Anweisungen."
            })

        elif name == "timeline_jsonl":
            item.update(read_jsonl_preview(path))
            item["proposed_targets"] = ["nexy_events", "nexy_timeline"]
            preview["proposed_import_batches"].append({
                "batch": "timeline_jsonl",
                "source": name,
                "target": ["nexy_events", "nexy_timeline"],
                "mode": "jsonl_timeline_import",
                "risk": "medium",
                "reason": "Laufender echter Zeitstrahl, 1:1 zeitlich relevant."
            })

        elif name == "patrick_timeline_md":
            item.update(read_text_preview(path))
            item["proposed_targets"] = ["nexy_timeline", "nexy_facts"]
            preview["proposed_import_batches"].append({
                "batch": "patrick_timeline_markdown",
                "source": name,
                "target": ["nexy_timeline", "nexy_facts"],
                "mode": "markdown_table_parse_later",
                "risk": "medium",
                "reason": "Lesbarer Zeitstrahl aus Kontext-PDF, Belegstatus beachten."
            })

        elif name == "legacy_sqlite":
            item["proposed_targets"] = ["all_mapped_tables"]
            tables = {}
            for table, targets in SQLITE_MAPPING.items():
                tables[table] = sqlite_table_preview(path, table)
                tables[table]["proposed_targets"] = targets
            item["mapped_tables"] = tables

            preview["proposed_import_batches"].append({
                "batch": "legacy_sqlite_core_tables",
                "source": name,
                "target": SQLITE_MAPPING,
                "mode": "table_by_table_with_dedup_and_source_ref",
                "risk": "high",
                "reason": "Sehr große DB. Import muss deduplizieren und darf files/semantic_cards nicht blind kopieren."
            })

        preview["sources"][name] = item

    preview["next_decisions_needed"] = [
        "Welche Quelle zuerst produktiv importieren? Empfehlung: nexus_memory.json als Lessons, dann chef_learning_rules/context_rules.",
        "Soll communication_context.md nur als Summary/Regeln importiert werden? Empfehlung: Ja, nicht als rohe Fakten.",
        "Sollen files und semantic_cards nur referenziert statt kopiert werden? Empfehlung: Ja.",
        "Soll timeline_jsonl vollständig importiert werden? Empfehlung: Ja, aber mit Dedupe-Key aus ts+kind+title."
    ]

    return preview

def write_markdown(preview):
    lines = []
    lines.append("# Nexy Legacy Import Preview")
    lines.append("")
    lines.append(f"Stand: {preview['created_at']}")
    lines.append(f"NEXUS_STORAGE: `{preview['nexus_storage']}`")
    lines.append("")
    lines.append("## Modus")
    lines.append("")
    lines.append("Preview only. Es wird nichts in nexy.db geschrieben.")
    lines.append("")
    lines.append("## Quellen")
    lines.append("")
    lines.append("| Quelle | Existiert | Größe | Ziel | Hinweis |")
    lines.append("|---|---:|---:|---|---|")
    for name, item in preview["sources"].items():
        exists = item.get("exists")
        size = item.get("size_bytes", "")
        targets = ", ".join(item.get("proposed_targets", []))
        note = item.get("note") or item.get("warning") or ""
        lines.append(f"| {name} | {exists} | {size} | {targets} | {note} |")
    lines.append("")
    lines.append("## Vorgeschlagene Import-Batches")
    lines.append("")
    for batch in preview["proposed_import_batches"]:
        lines.append(f"### {batch['batch']}")
        lines.append(f"- Quelle: `{batch['source']}`")
        lines.append(f"- Modus: `{batch['mode']}`")
        lines.append(f"- Risiko: `{batch['risk']}`")
        lines.append(f"- Grund: {batch['reason']}")
        lines.append("")
    lines.append("## Warnungen")
    lines.append("")
    if preview["warnings"]:
        for w in preview["warnings"]:
            lines.append(f"- {w}")
    else:
        lines.append("- Keine.")
    lines.append("")
    lines.append("## Offene Entscheidungen")
    lines.append("")
    for d in preview["next_decisions_needed"]:
        lines.append(f"- {d}")
    lines.append("")
    OUT_MD.write_text("\n".join(lines), encoding="utf-8")

def main():
    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    preview = build_preview()
    OUT_JSON.write_text(json.dumps(preview, indent=2, ensure_ascii=False), encoding="utf-8")
    write_markdown(preview)
    print(json.dumps({
        "ok": True,
        "mode": "preview_only_no_import",
        "json": str(OUT_JSON),
        "markdown": str(OUT_MD),
        "warnings": preview["warnings"],
        "batches": [b["batch"] for b in preview["proposed_import_batches"]]
    }, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
