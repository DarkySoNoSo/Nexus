#!/usr/bin/env python3
import sqlite3
from pathlib import Path
from datetime import datetime, timezone
import json
import uuid

DB_PATH = Path("data/nexy.db")

def now():
    return datetime.now(timezone.utc).isoformat()

def uid():
    return str(uuid.uuid4())

def connect():
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(DB_PATH)
    con.row_factory = sqlite3.Row
    return con

def init_db():
    con = connect()
    cur = con.cursor()

    cur.executescript("""
CREATE TABLE IF NOT EXISTS nexy_events (
    id TEXT PRIMARY KEY,
    created_at TEXT NOT NULL,
    event_time TEXT,
    source TEXT,
    source_ref TEXT,
    event_type TEXT,
    title TEXT,
    body TEXT,
    raw_payload TEXT,
    importance INTEGER DEFAULT 0,
    confidence REAL DEFAULT 0.5,
    status TEXT DEFAULT 'open'
);

CREATE TABLE IF NOT EXISTS nexy_timeline (
    id TEXT PRIMARY KEY,
    event_id TEXT,
    timeline_time TEXT,
    actor TEXT,
    topic TEXT,
    summary TEXT,
    cause TEXT,
    consequence TEXT,
    linked_before TEXT,
    linked_after TEXT,
    visibility TEXT DEFAULT 'normal',
    status TEXT DEFAULT 'active'
);

CREATE TABLE IF NOT EXISTS nexy_context (
    id TEXT PRIMARY KEY,
    subject_type TEXT,
    subject_id TEXT,
    relation TEXT,
    object_type TEXT,
    object_id TEXT,
    weight REAL DEFAULT 1.0,
    reason TEXT,
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS nexy_facts (
    id TEXT PRIMARY KEY,
    statement TEXT NOT NULL,
    classification TEXT NOT NULL CHECK(classification IN ('fact','claim','unclear')),
    evidence_ref TEXT,
    source_event_id TEXT,
    confidence REAL DEFAULT 0.5,
    valid_from TEXT,
    valid_to TEXT,
    status TEXT DEFAULT 'active'
);

CREATE TABLE IF NOT EXISTS nexy_lessons (
    id TEXT PRIMARY KEY,
    lesson TEXT NOT NULL,
    trigger_context TEXT,
    mistake_prevented TEXT,
    rule TEXT,
    priority INTEGER DEFAULT 0,
    created_at TEXT NOT NULL,
    last_used_at TEXT,
    status TEXT DEFAULT 'active'
);

CREATE TABLE IF NOT EXISTS nexy_active_focus (
    id TEXT PRIMARY KEY,
    focus_name TEXT NOT NULL,
    description TEXT,
    priority INTEGER DEFAULT 0,
    next_action TEXT,
    related_context TEXT,
    due_at TEXT,
    status TEXT DEFAULT 'active',
    updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS nexy_decisions (
    id TEXT PRIMARY KEY,
    created_at TEXT NOT NULL,
    source TEXT,
    source_event_id TEXT,
    conversation_id TEXT,
    decision_type TEXT,
    decision_value TEXT,
    reason TEXT,
    actor TEXT,
    sync_status TEXT DEFAULT 'queued',
    applied_at TEXT,
    rollback_ref TEXT,
    raw_payload TEXT,
    status TEXT DEFAULT 'active'
);
""")

    con.commit()
    con.close()

def add_event(source, event_type, title, body="", source_ref="", raw_payload=None, importance=0, confidence=0.5):
    con = connect()
    eid = uid()
    con.execute("""
        INSERT INTO nexy_events
        (id, created_at, event_time, source, source_ref, event_type, title, body, raw_payload, importance, confidence, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'open')
    """, (
        eid, now(), now(), source, source_ref, event_type, title, body,
        json.dumps(raw_payload, ensure_ascii=False) if raw_payload is not None else None,
        importance, confidence
    ))
    con.commit()
    con.close()
    return eid

def add_fact(statement, classification="unclear", evidence_ref="", source_event_id="", confidence=0.5):
    con = connect()
    fid = uid()
    con.execute("""
        INSERT INTO nexy_facts
        (id, statement, classification, evidence_ref, source_event_id, confidence, valid_from, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, 'active')
    """, (fid, statement, classification, evidence_ref, source_event_id, confidence, now()))
    con.commit()
    con.close()
    return fid

def set_focus(focus_name, description="", priority=0, next_action="", related_context=""):
    con = connect()
    fid = uid()
    con.execute("""
        INSERT INTO nexy_active_focus
        (id, focus_name, description, priority, next_action, related_context, status, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, 'active', ?)
    """, (fid, focus_name, description, priority, next_action, related_context, now()))
    con.commit()
    con.close()
    return fid

def summary():
    con = connect()
    cur = con.cursor()
    tables = ["nexy_events","nexy_timeline","nexy_context","nexy_facts","nexy_lessons","nexy_active_focus","nexy_decisions"]
    out = {}
    for t in tables:
        out[t] = cur.execute(f"SELECT COUNT(*) FROM {t}").fetchone()[0]
    con.close()
    return out

if __name__ == "__main__":
    init_db()
    print(json.dumps(summary(), indent=2, ensure_ascii=False))
