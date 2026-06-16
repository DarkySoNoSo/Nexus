#!/usr/bin/env python3
from pathlib import Path
import sys, json, subprocess, os
from datetime import datetime

sys.path.insert(0, str(Path(__file__).resolve().parent))
from nexy_memory import connect

ROOT = Path(os.environ.get("NEXUS_STORAGE", r"C:\MasterIndex_Storage"))
OUT_JSON = Path("docs/nexy/NEXY_LOCAL_HEALTHCHECK.json")
OUT_MD = Path("docs/nexy/NEXY_LOCAL_HEALTHCHECK.md")

LEGACY_PATHS = [
    ROOT / "context" / "patrick_context_profile.md",
    ROOT / "context" / "communication_context.md",
    ROOT / "context" / "context_chef_channel.jsonl",
    ROOT / "nexus_memory.json",
    ROOT / "timeline" / "nexus_timeline.jsonl",
    ROOT / "timeline" / "patrick_timeline.md",
    ROOT / "_NEXUS_SYSTEM" / "db" / "nexus_catalog.sqlite",
]

MIN_COUNTS = {
    "nexy_events": 2000,
    "nexy_timeline": 2000,
    "nexy_context": 10,
    "nexy_facts": 5,
    "nexy_lessons": 90,
    "nexy_active_focus": 2,
}

def fetch(sql, args=()):
    con = connect()
    cur = con.cursor()
    cur.execute(sql, args)
    names = [d[0] for d in cur.description]
    rows = [dict(zip(names, r)) for r in cur.fetchall()]
    con.close()
    return rows

def count_table(table):
    return fetch(f'SELECT COUNT(*) AS c FROM "{table}"')[0]["c"]

def git_status():
    try:
        p = subprocess.run(
            ["git", "status", "--porcelain"],
            capture_output=True,
            text=True,
            timeout=10
        )
        return {
            "ok": p.returncode == 0,
            "clean": p.returncode == 0 and p.stdout.strip() == "",
            "stdout": p.stdout.strip(),
            "stderr": p.stderr.strip(),
        }
    except Exception as e:
        return {"ok": False, "clean": False, "error": str(e)}

def search_count(table, fields, query):
    like = f"%{query}%"
    where = " OR ".join([f"{f} LIKE ?" for f in fields])
    args = tuple([like] * len(fields))
    return fetch(f'SELECT COUNT(*) AS c FROM "{table}" WHERE {where}', args)[0]["c"]

def main():
    report = {
        "created_at": datetime.now().isoformat(timespec="seconds"),
        "nexus_storage": str(ROOT),
        "checks": {},
        "counts": {},
        "legacy_paths": {},
        "recall_tests": {},
        "git": {},
        "overall_ok": True,
        "problems": [],
    }

    db_path = Path("data/nexy.db")
    report["checks"]["local_db_exists"] = db_path.exists()
    if not db_path.exists():
        report["overall_ok"] = False
        report["problems"].append("data/nexy.db fehlt lokal.")

    for table, minimum in MIN_COUNTS.items():
        try:
            c = count_table(table)
            report["counts"][table] = {"count": c, "minimum": minimum, "ok": c >= minimum}
            if c < minimum:
                report["overall_ok"] = False
                report["problems"].append(f"{table} unter Minimum: {c} < {minimum}")
        except Exception as e:
            report["counts"][table] = {"ok": False, "error": str(e)}
            report["overall_ok"] = False
            report["problems"].append(f"{table} konnte nicht geprüft werden: {e}")

    for p in LEGACY_PATHS:
        report["legacy_paths"][str(p)] = p.exists()
        if not p.exists():
            report["overall_ok"] = False
            report["problems"].append(f"Legacy-Quelle fehlt: {p}")

    tests = {
        "Patrick": [
            ("nexy_events", ["title", "body", "event_type"]),
            ("nexy_context", ["subject_id", "relation", "reason"]),
        ],
        "Safe-Start": [
            ("nexy_events", ["title", "body", "event_type"]),
            ("nexy_lessons", ["lesson", "trigger_context", "rule"]),
            ("nexy_facts", ["statement", "evidence_ref"]),
        ],
        "communication": [
            ("nexy_events", ["title", "body", "event_type"]),
            ("nexy_lessons", ["lesson", "trigger_context", "rule"]),
        ],
    }

    for query, targets in tests.items():
        total = 0
        details = {}
        for table, fields in targets:
            try:
                n = search_count(table, fields, query)
            except Exception:
                n = 0
            details[table] = n
            total += n
        report["recall_tests"][query] = {"hits": total, "details": details, "ok": total > 0}
        if total <= 0:
            report["overall_ok"] = False
            report["problems"].append(f"Recall-Test ohne Treffer: {query}")

    report["git"] = git_status()
    if not report["git"].get("ok"):
        report["overall_ok"] = False
        report["problems"].append("Git-Status konnte nicht geprüft werden.")

    OUT_JSON.parent.mkdir(parents=True, exist_ok=True)
    OUT_JSON.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")

    lines = []
    lines.append("# NEXY LOCAL HEALTHCHECK")
    lines.append("")
    lines.append(f"Stand: {report['created_at']}")
    lines.append(f"Overall OK: **{report['overall_ok']}**")
    lines.append("")
    lines.append("## Counts")
    lines.append("")
    for table, item in report["counts"].items():
        lines.append(f"- {table}: {item.get('count')} / min {item.get('minimum')} -> {item.get('ok')}")
    lines.append("")
    lines.append("## Recall Tests")
    lines.append("")
    for q, item in report["recall_tests"].items():
        lines.append(f"- {q}: {item['hits']} Treffer -> {item['ok']}")
    lines.append("")
    lines.append("## Git")
    lines.append("")
    lines.append(f"- clean: {report['git'].get('clean')}")
    if report["git"].get("stdout"):
        lines.append(f"- status: `{report['git'].get('stdout')}`")
    lines.append("")
    lines.append("## Probleme")
    lines.append("")
    if report["problems"]:
        for p in report["problems"]:
            lines.append(f"- {p}")
    else:
        lines.append("- Keine.")
    lines.append("")

    OUT_MD.write_text("\n".join(lines), encoding="utf-8")

    print(json.dumps({
        "ok": report["overall_ok"],
        "counts": report["counts"],
        "recall_tests": report["recall_tests"],
        "git_clean": report["git"].get("clean"),
        "problems": report["problems"],
        "json": str(OUT_JSON),
        "markdown": str(OUT_MD),
    }, indent=2, ensure_ascii=False))

    if not report["overall_ok"]:
        raise SystemExit(2)

if __name__ == "__main__":
    main()
