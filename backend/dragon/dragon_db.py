from __future__ import annotations

import json
import os
import random
import sqlite3
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Optional

ROOT = Path(__file__).resolve().parents[2]
DB_PATH = Path(os.getenv("DIGI_DRAGON_DB", ROOT / "data" / "digi_dragon.db"))

STAGE_LABELS = {
    "egg": "Ei",
    "young": "Jungdrache",
    "wyrmling": "Wyrmling",
    "runner": "Drachenläufer",
    "sky": "Himmelsdrache",
    "awakened": "Erwachter Drache",
    "mythic": "Mythischer Nexus-Drache",
}

ATTACKS = [
    ("ember_bite", "Glutbiss", "fire", "damage", 12, 5, 95, "Basis-Feuerschaden.", "starter"),
    ("tail_sweep", "Schweifhieb", "neutral", "damage", 14, 6, 90, "Solider physischer Angriff.", "level_2"),
    ("dragon_guard", "Drachengarde", "guard", "shield", 0, 5, 100, "Erhöht Verteidigung.", "level_2"),
    ("focus_breath", "Fokusatem", "arcane", "buff", 0, 8, 100, "Erhöht Fokus.", "level_3"),
    ("storm_jump", "Sturmsprung", "wind", "damage", 18, 10, 85, "Schneller Angriff.", "level_4"),
    ("crystal_skin", "Kristallhaut", "crystal", "shield", 0, 10, 100, "Defensiver Buff.", "level_5"),
    ("shadow_flight", "Schattenflug", "shadow", "control", 16, 12, 80, "Kontrollangriff.", "level_6"),
    ("thunder_roar", "Donnerbrüllen", "lightning", "debuff", 10, 10, 88, "Debuff.", "arena_unlock"),
    ("healing_spark", "Heilfunke", "light", "heal", 0, 12, 100, "Kleine Heilung.", "bond_unlock"),
    ("skyfall", "Himmelssturz", "finisher", "damage", 35, 25, 70, "Finisher.", "evolution_unlock"),
]

HABITATS = [
    ("starter_nest", "Kleines Nest", "forest_ruin", 1, 1, 0),
    ("crystal_cave", "Kristallhöhle", "crystal", 1, 0, 2),
    ("storm_cliff", "Sturmklippe", "wind", 1, 0, 1),
    ("ember_hollow", "Gluthöhle", "fire", 1, 0, 0),
]

def utc_now() -> str:
    return datetime.now(timezone.utc).isoformat()

def clamp(v: int, low: int = 0, high: int = 100) -> int:
    return max(low, min(high, int(v)))

def connect() -> sqlite3.Connection:
    DB_PATH.parent.mkdir(parents=True, exist_ok=True)
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn

def create_schema(conn: sqlite3.Connection) -> None:
    conn.executescript("""
    CREATE TABLE IF NOT EXISTS dragon_profile (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        stage TEXT NOT NULL,
        level INTEGER NOT NULL,
        xp INTEGER NOT NULL,
        bond INTEGER NOT NULL,
        energy INTEGER NOT NULL,
        mood INTEGER NOT NULL,
        battle_ready INTEGER NOT NULL,
        evolution_path TEXT NOT NULL,
        strength INTEGER NOT NULL,
        endurance INTEGER NOT NULL,
        speed INTEGER NOT NULL,
        focus INTEGER NOT NULL,
        instinct INTEGER NOT NULL,
        intelligence INTEGER NOT NULL,
        willpower INTEGER NOT NULL,
        hp INTEGER NOT NULL,
        max_hp INTEGER NOT NULL,
        habitat_id TEXT NOT NULL,
        updated_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS attacks (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        element TEXT NOT NULL,
        class TEXT NOT NULL,
        power INTEGER NOT NULL,
        cost INTEGER NOT NULL,
        accuracy INTEGER NOT NULL,
        effect TEXT NOT NULL,
        unlock_condition TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS dragon_attacks (
        dragon_id TEXT NOT NULL,
        attack_id TEXT NOT NULL,
        equipped INTEGER NOT NULL DEFAULT 0,
        learned_at TEXT NOT NULL,
        PRIMARY KEY (dragon_id, attack_id)
    );

    CREATE TABLE IF NOT EXISTS habitats (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        theme TEXT NOT NULL,
        level INTEGER NOT NULL,
        bond_buff INTEGER NOT NULL,
        focus_buff INTEGER NOT NULL
    );

    CREATE TABLE IF NOT EXISTS training_sessions (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        ts TEXT NOT NULL,
        training_type TEXT NOT NULL,
        result_json TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS battle_records (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        ts TEXT NOT NULL,
        mode TEXT NOT NULL,
        enemy TEXT NOT NULL,
        result TEXT NOT NULL,
        details_json TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS dragon_events (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        ts TEXT NOT NULL,
        kind TEXT NOT NULL,
        title TEXT NOT NULL,
        data_json TEXT NOT NULL
    );
    """)

def seed_data(conn: sqlite3.Connection) -> None:
    now = utc_now()

    conn.execute("""
    INSERT OR IGNORE INTO dragon_profile (
        id, name, stage, level, xp, bond, energy, mood, battle_ready,
        evolution_path, strength, endurance, speed, focus, instinct,
        intelligence, willpower, hp, max_hp, habitat_id, updated_at
    )
    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
    """, (
        "main_dragon", "Nexus-Drache", "egg", 1, 0, 10, 80, 70, 35,
        "unknown", 5, 5, 5, 5, 5, 5, 5, 40, 40, "starter_nest", now
    ))

    for a in ATTACKS:
        conn.execute("""
        INSERT OR IGNORE INTO attacks
        (id, name, element, class, power, cost, accuracy, effect, unlock_condition)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """, a)

    for h in HABITATS:
        conn.execute("""
        INSERT OR IGNORE INTO habitats
        (id, name, theme, level, bond_buff, focus_buff)
        VALUES (?, ?, ?, ?, ?, ?)
        """, h)

    conn.execute("""
    INSERT OR IGNORE INTO dragon_attacks
    (dragon_id, attack_id, equipped, learned_at)
    VALUES (?, ?, ?, ?)
    """, ("main_dragon", "ember_bite", 1, now))

def init_db() -> Dict[str, Any]:
    with connect() as conn:
        create_schema(conn)
        seed_data(conn)
        conn.commit()
    return {"ok": True, "db_path": str(DB_PATH)}

def add_event(conn: sqlite3.Connection, kind: str, title: str, data: Dict[str, Any]) -> None:
    conn.execute(
        "INSERT INTO dragon_events (ts, kind, title, data_json) VALUES (?, ?, ?, ?)",
        (utc_now(), kind, title, json.dumps(data, ensure_ascii=False)),
    )

def base_state(conn: sqlite3.Connection) -> Dict[str, Any]:
    row = conn.execute("SELECT * FROM dragon_profile WHERE id = ?", ("main_dragon",)).fetchone()
    if row is None:
        seed_data(conn)
        row = conn.execute("SELECT * FROM dragon_profile WHERE id = ?", ("main_dragon",)).fetchone()
    return dict(row)

def save_state(conn: sqlite3.Connection, s: Dict[str, Any]) -> None:
    s["updated_at"] = utc_now()
    conn.execute("""
    UPDATE dragon_profile SET
        name=?, stage=?, level=?, xp=?, bond=?, energy=?, mood=?, battle_ready=?,
        evolution_path=?, strength=?, endurance=?, speed=?, focus=?, instinct=?,
        intelligence=?, willpower=?, hp=?, max_hp=?, habitat_id=?, updated_at=?
    WHERE id=?
    """, (
        s["name"], s["stage"], int(s["level"]), int(s["xp"]), int(s["bond"]),
        int(s["energy"]), int(s["mood"]), int(s["battle_ready"]),
        s["evolution_path"], int(s["strength"]), int(s["endurance"]),
        int(s["speed"]), int(s["focus"]), int(s["instinct"]),
        int(s["intelligence"]), int(s["willpower"]), int(s["hp"]),
        int(s["max_hp"]), s["habitat_id"], s["updated_at"], s["id"]
    ))

def level_up(s: Dict[str, Any]) -> list[str]:
    notes = []
    while int(s["xp"]) >= int(s["level"]) * 50:
        need = int(s["level"]) * 50
        s["xp"] = int(s["xp"]) - need
        s["level"] = int(s["level"]) + 1
        s["max_hp"] = int(s["max_hp"]) + 8
        s["hp"] = int(s["max_hp"])
        s["strength"] = int(s["strength"]) + 1
        s["endurance"] = int(s["endurance"]) + 1
        s["focus"] = int(s["focus"]) + 1
        s["willpower"] = int(s["willpower"]) + 1
        notes.append(f"Level {s['level']} erreicht")
    return notes

def unlock_by_progress(conn: sqlite3.Connection, s: Dict[str, Any]) -> list[str]:
    rules = [(2, "tail_sweep"), (2, "dragon_guard"), (3, "focus_breath"), (4, "storm_jump"), (5, "crystal_skin"), (6, "shadow_flight")]
    existing = {
        r["attack_id"]
        for r in conn.execute("SELECT attack_id FROM dragon_attacks WHERE dragon_id=?", (s["id"],)).fetchall()
    }
    unlocked = []

    for min_level, attack_id in rules:
        if int(s["level"]) >= min_level and attack_id not in existing:
            conn.execute(
                "INSERT OR IGNORE INTO dragon_attacks (dragon_id, attack_id, equipped, learned_at) VALUES (?, ?, ?, ?)",
                (s["id"], attack_id, 0, utc_now()),
            )
            name = conn.execute("SELECT name FROM attacks WHERE id=?", (attack_id,)).fetchone()["name"]
            unlocked.append(name)

    if int(s["bond"]) >= 60 and "healing_spark" not in existing:
        conn.execute(
            "INSERT OR IGNORE INTO dragon_attacks (dragon_id, attack_id, equipped, learned_at) VALUES (?, ?, ?, ?)",
            (s["id"], "healing_spark", 0, utc_now()),
        )
        unlocked.append("Heilfunke")

    return unlocked

def determine_path(s: Dict[str, Any]) -> str:
    focus_score = int(s["focus"]) + int(s["intelligence"]) + int(s["willpower"])
    war_score = int(s["strength"]) + int(s["endurance"])
    speed_score = int(s["speed"]) + int(s["instinct"])
    if focus_score >= war_score + 5 and focus_score >= speed_score:
        return "crystal_focus"
    if war_score >= focus_score and war_score >= speed_score:
        return "war_dragon"
    if speed_score >= war_score and speed_score >= focus_score:
        return "storm_hunter"
    if int(s["bond"]) >= 70:
        return "guardian"
    return "balanced_nexus"

def get_state() -> Dict[str, Any]:
    with connect() as conn:
        create_schema(conn)
        seed_data(conn)
        conn.commit()

        s = base_state(conn)
        s["stage_label"] = STAGE_LABELS.get(s["stage"], s["stage"])

        attacks = conn.execute("""
        SELECT a.*, da.equipped, da.learned_at
        FROM dragon_attacks da
        JOIN attacks a ON a.id = da.attack_id
        WHERE da.dragon_id=?
        ORDER BY da.equipped DESC, a.id ASC
        """, (s["id"],)).fetchall()

        habitat = conn.execute("SELECT * FROM habitats WHERE id=?", (s["habitat_id"],)).fetchone()

        s["attacks"] = [dict(r) for r in attacks]
        s["habitat"] = dict(habitat) if habitat else None
        s["db_path"] = str(DB_PATH)
        return s

def apply_action(action: str, payload: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    payload = payload or {}

    with connect() as conn:
        create_schema(conn)
        seed_data(conn)
        s = base_state(conn)
        details = {}
        message = ""

        if action == "feed":
            s["energy"] = clamp(int(s["energy"]) + 15)
            s["mood"] = clamp(int(s["mood"]) + 5)
            s["bond"] = clamp(int(s["bond"]) + 1)
            s["battle_ready"] = clamp(int(s["battle_ready"]) + 5)
            message = "Der Drache wurde gefüttert."

        elif action == "care":
            s["mood"] = clamp(int(s["mood"]) + 12)
            s["bond"] = clamp(int(s["bond"]) + 3)
            s["energy"] = clamp(int(s["energy"]) + 4)
            message = "Der Drache wurde gepflegt."

        elif action == "train":
            training_type = str(payload.get("training_type", "focus")).lower().strip()
            if int(s["energy"]) < 10:
                return {"ok": False, "error": "Zu wenig Energie für Training.", "state": get_state()}

            training_map = {
                "strength": ("Krafttraining", "strength", 2, "willpower", 1),
                "endurance": ("Ausdauertraining", "endurance", 2, "hp", 2),
                "speed": ("Flugtraining", "speed", 2, "instinct", 1),
                "focus": ("Fokusmeditation", "focus", 2, "intelligence", 1),
                "instinct": ("Instinkttraining", "instinct", 2, "speed", 1),
            }

            label, stat1, inc1, stat2, inc2 = training_map.get(training_type, training_map["focus"])
            s[stat1] = int(s[stat1]) + inc1
            s[stat2] = int(s[stat2]) + inc2
            s["energy"] = clamp(int(s["energy"]) - 12)
            s["battle_ready"] = clamp(int(s["battle_ready"]) + 8)
            s["mood"] = clamp(int(s["mood"]) - 2)
            s["xp"] = int(s["xp"]) + 18

            details = {"training_type": training_type, "label": label, "xp_gain": 18, "energy_cost": 12}
            conn.execute(
                "INSERT INTO training_sessions (ts, training_type, result_json) VALUES (?, ?, ?)",
                (utc_now(), training_type, json.dumps(details, ensure_ascii=False)),
            )
            message = f"Training abgeschlossen: {label}."

        elif action in ("freefight", "arena"):
            mode = action
            enemy = "Wilder Schattenläufer" if mode == "freefight" else "Arena-Prüfer"
            if int(s["energy"]) < 15:
                return {"ok": False, "error": "Zu wenig Energie für Kampf.", "state": get_state()}

            player_score = (
                int(s["strength"]) * 2
                + int(s["endurance"]) * 1.4
                + int(s["speed"]) * 1.4
                + int(s["focus"]) * 1.2
                + int(s["battle_ready"]) * 0.7
                + random.randint(0, 18)
            )
            enemy_score = int(s["level"]) * (18 if mode == "arena" else 14) + random.randint(5, 30)
            win = player_score >= enemy_score

            hp_loss = random.randint(3, 12 if mode == "freefight" else 18)
            s["hp"] = max(1, int(s["hp"]) - hp_loss)
            s["energy"] = clamp(int(s["energy"]) - (15 if mode == "freefight" else 22))
            s["battle_ready"] = clamp(int(s["battle_ready"]) - (8 if mode == "freefight" else 12))

            if win:
                xp_gain = 28 if mode == "freefight" else 45
                bond_gain = 2 if mode == "freefight" else 3
                mood_gain = 8 if mode == "freefight" else 12
                result = "win"
                message = "Freikampf gewonnen." if mode == "freefight" else "Arena-Kampf gewonnen."
            else:
                xp_gain = 10 if mode == "freefight" else 16
                bond_gain = 1
                mood_gain = -5
                result = "loss"
                message = "Freikampf verloren, aber Erfahrung gesammelt." if mode == "freefight" else "Arena-Kampf verloren, aber Fortschritt gespeichert."

            s["xp"] = int(s["xp"]) + xp_gain
            s["bond"] = clamp(int(s["bond"]) + bond_gain)
            s["mood"] = clamp(int(s["mood"]) + mood_gain)

            details = {
                "mode": mode,
                "enemy": enemy,
                "result": result,
                "player_score": round(player_score, 2),
                "enemy_score": round(enemy_score, 2),
                "xp_gain": xp_gain,
                "hp_loss": hp_loss,
            }
            conn.execute(
                "INSERT INTO battle_records (ts, mode, enemy, result, details_json) VALUES (?, ?, ?, ?, ?)",
                (utc_now(), mode, enemy, result, json.dumps(details, ensure_ascii=False)),
            )

        elif action == "evolve":
            stage = s["stage"]
            next_stage = None
            reason = ""

            if stage == "egg" and int(s["level"]) >= 2 and int(s["bond"]) >= 12:
                next_stage = "young"
                reason = "Ei erwacht durch Level und Bindung."
            elif stage == "young" and int(s["level"]) >= 4 and int(s["bond"]) >= 20:
                next_stage = "wyrmling"
                reason = "Jungdrache wächst zum Wyrmling."
            elif stage == "wyrmling" and int(s["level"]) >= 7 and int(s["bond"]) >= 35:
                next_stage = "runner"
                reason = "Wyrmling wird zum Drachenläufer."
            elif stage == "runner" and int(s["level"]) >= 10 and int(s["bond"]) >= 50:
                next_stage = "sky"
                reason = "Drachenläufer wird zum Himmelsdrachen."

            if next_stage:
                before = s["evolution_path"]
                s["stage"] = next_stage
                s["evolution_path"] = determine_path(s)
                s["max_hp"] = int(s["max_hp"]) + 15
                s["hp"] = int(s["max_hp"])
                s["mood"] = clamp(int(s["mood"]) + 15)
                s["battle_ready"] = clamp(int(s["battle_ready"]) + 20)
                message = f"Evolution erfolgreich: {STAGE_LABELS.get(next_stage, next_stage)}."
                details = {"from": stage, "to": next_stage, "reason": reason, "path_before": before, "path_after": s["evolution_path"]}
            else:
                message = "Evolution noch nicht möglich."
                details = {"current_stage": stage, "level": s["level"], "bond": s["bond"], "needed": "Mehr Level und Bindung."}

        else:
            return {"ok": False, "error": f"Unbekannte Aktion: {action}", "state": get_state()}

        level_notes = level_up(s)
        unlocks = unlock_by_progress(conn, s)
        save_state(conn, s)

        event = {
            "action": action,
            "payload": payload,
            "message": message,
            "details": details,
            "level_notes": level_notes,
            "unlocks": unlocks,
        }
        add_event(conn, "dragon.action", message, event)
        conn.commit()

    return {"ok": True, "action": action, "message": message, "details": details, "level_notes": level_notes, "unlocks": unlocks, "state": get_state()}

def codex() -> Dict[str, Any]:
    with connect() as conn:
        create_schema(conn)
        seed_data(conn)
        conn.commit()

        attacks = [dict(r) for r in conn.execute("SELECT * FROM attacks ORDER BY id").fetchall()]
        habitats = [dict(r) for r in conn.execute("SELECT * FROM habitats ORDER BY id").fetchall()]
        battles = [dict(r) for r in conn.execute("SELECT id, ts, mode, enemy, result, details_json FROM battle_records ORDER BY id DESC LIMIT 20").fetchall()]
        events = [dict(r) for r in conn.execute("SELECT id, ts, kind, title, data_json FROM dragon_events ORDER BY id DESC LIMIT 20").fetchall()]

    return {"ok": True, "attacks": attacks, "habitats": habitats, "recent_battles": battles, "recent_events": events}
