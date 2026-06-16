#!/usr/bin/env python3
from pathlib import Path
import sqlite3
import json
import os

ROOT = Path(os.environ.get("NEXUS_STORAGE", r"C:\MasterIndex_Storage"))
DB = ROOT / "_NEXUS_SYSTEM" / "db" / "nexus_catalog.sqlite"

TARGET_TABLES = [
    "timeline_events",
    "timeline_claims",
    "chef_events",
    "chef_facts",
    "chef_learning_rules",
    "communication_events",
    "communication_daily_summaries",
    "context_entities",
    "context_relations",
    "context_rules",
    "learning_rules",
    "legacy_entries",
    "semantic_cards",
    "files",
]

def table_info(cur, table):
    cols = cur.execute(f'PRAGMA table_info("{table}")').fetchall()
    return [
        {
            "cid": c[0],
            "name": c[1],
            "type": c[2],
            "notnull": c[3],
            "default": c[4],
            "pk": c[5],
        }
        for c in cols
    ]

def sample_rows(cur, table, limit=3):
    rows = cur.execute(f'SELECT * FROM "{table}" LIMIT {limit}').fetchall()
    names = [d[0] for d in cur.description]
    out = []
    for row in rows:
        item = {}
        for k, v in zip(names, row):
            if isinstance(v, bytes):
                item[k] = f"<bytes:{len(v)}>"
            elif isinstance(v, str) and len(v) > 500:
                item[k] = v[:500] + "...<cut>"
            else:
                item[k] = v
        out.append(item)
    return out

def main():
    if not DB.exists():
        raise SystemExit(f"DB nicht gefunden: {DB}")

    con = sqlite3.connect(DB)
    cur = con.cursor()

    report = {
        "db_path": str(DB),
        "exists": DB.exists(),
        "size_bytes": DB.stat().st_size,
        "tables": {}
    }

    all_tables = [r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")]
    for table in TARGET_TABLES:
        if table not in all_tables:
            report["tables"][table] = {"exists": False}
            continue

        count = cur.execute(f'SELECT COUNT(*) FROM "{table}"').fetchone()[0]
        report["tables"][table] = {
            "exists": True,
            "count": count,
            "columns": table_info(cur, table),
            "sample_rows": sample_rows(cur, table, 3),
        }

    con.close()

    out = Path("docs/nexy/NEXY_LEGACY_SQLITE_SCHEMA_REPORT.json")
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
