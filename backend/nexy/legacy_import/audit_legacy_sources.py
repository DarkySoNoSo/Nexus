#!/usr/bin/env python3
from pathlib import Path
import json
import sqlite3
import hashlib
import os

ROOT = Path(os.environ.get("NEXUS_STORAGE", "/mnt/c/MasterIndex_Storage"))

SOURCES = {
    "patrick_context_profile": ROOT / "context" / "patrick_context_profile.md",
    "communication_context": ROOT / "context" / "communication_context.md",
    "chef_channel": ROOT / "context" / "context_chef_channel.jsonl",
    "nexus_memory": ROOT / "nexus_memory.json",
    "timeline_jsonl": ROOT / "timeline" / "nexus_timeline.jsonl",
    "patrick_timeline_md": ROOT / "timeline" / "patrick_timeline.md",
    "legacy_sqlite": ROOT / "_NEXUS_SYSTEM" / "db" / "nexus_catalog.sqlite",
}

def sha256(path):
    h = hashlib.sha256()
    with path.open("rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()

def text_stats(path):
    text = path.read_text(errors="replace")
    return {
        "lines": text.count("\n") + 1,
        "chars": len(text),
        "preview": text[:500].replace("\r", "").replace("\n", "\\n"),
    }

def jsonl_stats(path):
    total = 0
    valid = 0
    invalid = 0
    first = None
    with path.open("r", encoding="utf-8", errors="replace") as f:
        for line in f:
            if not line.strip():
                continue
            total += 1
            try:
                obj = json.loads(line)
                valid += 1
                if first is None:
                    first = obj
            except Exception:
                invalid += 1
    return {
        "jsonl_lines": total,
        "valid_json": valid,
        "invalid_json": invalid,
        "first_object_keys": list(first.keys()) if isinstance(first, dict) else None,
    }

def json_stats(path):
    try:
        obj = json.loads(path.read_text(errors="replace"))
        if isinstance(obj, dict):
            return {"json_type": "dict", "keys": list(obj.keys())[:50]}
        if isinstance(obj, list):
            return {"json_type": "list", "items": len(obj)}
        return {"json_type": type(obj).__name__}
    except Exception as e:
        return {"json_error": str(e)}

def sqlite_stats(path):
    out = {}
    con = sqlite3.connect(path)
    cur = con.cursor()
    tables = [r[0] for r in cur.execute("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name")]
    out["tables"] = tables
    out["table_counts"] = {}
    for t in tables:
        try:
            out["table_counts"][t] = cur.execute(f'SELECT COUNT(*) FROM "{t}"').fetchone()[0]
        except Exception as e:
            out["table_counts"][t] = f"ERROR: {e}"
    con.close()
    return out

def main():
    report = {}
    for name, path in SOURCES.items():
        item = {"path": str(path), "exists": path.exists()}
        if path.exists():
            item["size_bytes"] = path.stat().st_size
            item["sha256"] = sha256(path)
            suffix = path.suffix.lower()
            try:
                if suffix in [".md", ".txt"]:
                    item.update(text_stats(path))
                elif suffix == ".jsonl":
                    item.update(jsonl_stats(path))
                    item.update(text_stats(path))
                elif suffix == ".json":
                    item.update(json_stats(path))
                    item.update(text_stats(path))
                elif suffix in [".sqlite", ".db"]:
                    item.update(sqlite_stats(path))
                else:
                    item["note"] = "unknown file type"
            except Exception as e:
                item["error"] = str(e)
        report[name] = item

    out_path = Path("docs/nexy/NEXY_LEGACY_AUDIT_REPORT.json")
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")
    print(json.dumps(report, indent=2, ensure_ascii=False))

if __name__ == "__main__":
    main()
