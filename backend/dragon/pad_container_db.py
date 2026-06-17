from __future__ import annotations

import json
import os
import random
import secrets
import sqlite3
import string
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Optional


ROOT = Path(__file__).resolve().parents[2]
DB_PATH = Path(os.getenv("DIGIPAD_DB", ROOT / "data" / "digipad_container.db"))

STAGE_LABELS = {
    "egg": "Ei",
    "young": "Jungdrache",
    "wyrmling": "Wyrmling",
    "runner": "Drachenläufer",
    "sky": "Himmelsdrache",
    "awakened": "Erwachter Drache",
    "mythic": "Mythischer Nexus-Drache",
}

DEFAULT_ATTACKS = [
    ("ember_bite", "Glutbiss", "fire", "damage", 12, 5, 95),
    ("tail_sweep", "Schweifhieb", "neutral", "damage", 14, 6, 90),
    ("dragon_guard", "Drachengarde", "guard", "shield", 0, 5, 100),
    ("focus_breath", "Fokusatem", "arcane", "buff", 0, 8, 100),
    ("storm_jump", "Sturmsprung", "wind", "damage", 18, 10, 85),
    ("crystal_skin", "Kristallhaut", "crystal", "shield", 0, 10, 100),
]

PROFILE_DEFAULTS = {
    "patrick": {
        "display_name": "Patrick",
        "role": "admin_owner",
        "access_level": "owner_pad",
        "pet_name": "Nexus-Drache",
    },
    "fiona": {
        "display_name": "Fiona",
        "role": "family_user",
        "access_level": "digipad_only",
        "pet_name": "Fiona-Drache",
    },
}


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
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS pad_profiles (
            profile_id TEXT PRIMARY KEY,
            display_name TEXT NOT NULL,
            role TEXT NOT NULL,
            access_level TEXT NOT NULL,
            token TEXT NOT NULL,
            created_at TEXT NOT NULL,
            updated_at TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS pad_pets (
            pet_id TEXT PRIMARY KEY,
            profile_id TEXT NOT NULL,
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

        CREATE TABLE IF NOT EXISTS pad_attacks (
            attack_id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            element TEXT NOT NULL,
            attack_class TEXT NOT NULL,
            power INTEGER NOT NULL,
            cost INTEGER NOT NULL,
            accuracy INTEGER NOT NULL
        );

        CREATE TABLE IF NOT EXISTS pad_pet_attacks (
            pet_id TEXT NOT NULL,
            attack_id TEXT NOT NULL,
            equipped INTEGER NOT NULL DEFAULT 0,
            learned_at TEXT NOT NULL,
            PRIMARY KEY (pet_id, attack_id)
        );

        CREATE TABLE IF NOT EXISTS battle_snapshots (
            code TEXT PRIMARY KEY,
            owner_profile TEXT NOT NULL,
            snapshot_json TEXT NOT NULL,
            created_at TEXT NOT NULL
        );

        CREATE TABLE IF NOT EXISTS pad_events (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            ts TEXT NOT NULL,
            profile_id TEXT NOT NULL,
            kind TEXT NOT NULL,
            title TEXT NOT NULL,
            data_json TEXT NOT NULL
        );
        """
    )


def seed(conn: sqlite3.Connection) -> None:
    now = utc_now()

    for attack in DEFAULT_ATTACKS:
        conn.execute(
            """
            INSERT OR IGNORE INTO pad_attacks
            (attack_id, name, element, attack_class, power, cost, accuracy)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
            attack,
        )

    for profile_id, data in PROFILE_DEFAULTS.items():
        existing = conn.execute(
            "SELECT profile_id FROM pad_profiles WHERE profile_id = ?",
            (profile_id,),
        ).fetchone()

        if existing is None:
            token = secrets.token_urlsafe(32)
            conn.execute(
                """
                INSERT INTO pad_profiles
                (profile_id, display_name, role, access_level, token, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    profile_id,
                    data["display_name"],
                    data["role"],
                    data["access_level"],
                    token,
                    now,
                    now,
                ),
            )

        pet_id = profile_id + "_pet"
        existing_pet = conn.execute(
            "SELECT pet_id FROM pad_pets WHERE pet_id = ?",
            (pet_id,),
        ).fetchone()

        if existing_pet is None:
            conn.execute(
                """
                INSERT INTO pad_pets (
                    pet_id, profile_id, name, stage, level, xp, bond, energy, mood, battle_ready,
                    evolution_path, strength, endurance, speed, focus, instinct, intelligence,
                    willpower, hp, max_hp, habitat_id, updated_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    pet_id,
                    profile_id,
                    data["pet_name"],
                    "egg",
                    1,
                    0,
                    10,
                    85,
                    75,
                    35,
                    "unknown",
                    5,
                    5,
                    5,
                    5,
                    5,
                    5,
                    5,
                    42,
                    42,
                    "starter_nest",
                    now,
                ),
            )
            conn.execute(
                """
                INSERT OR IGNORE INTO pad_pet_attacks
                (pet_id, attack_id, equipped, learned_at)
                VALUES (?, ?, ?, ?)
                """,
                (pet_id, "ember_bite", 1, now),
            )


def init_db() -> Dict[str, Any]:
    with connect() as conn:
        create_schema(conn)
        seed(conn)
        conn.commit()
    return {"ok": True, "db_path": str(DB_PATH)}


def profile_exists(profile_id: str) -> bool:
    with connect() as conn:
        create_schema(conn)
        seed(conn)
        conn.commit()
        row = conn.execute(
            "SELECT profile_id FROM pad_profiles WHERE profile_id = ?",
            (profile_id,),
        ).fetchone()
        return row is not None


def token_for(profile_id: str) -> Optional[str]:
    with connect() as conn:
        create_schema(conn)
        seed(conn)
        conn.commit()
        row = conn.execute(
            "SELECT token FROM pad_profiles WHERE profile_id = ?",
            (profile_id,),
        ).fetchone()
        return None if row is None else str(row["token"])


def verify_token(profile_id: str, token: str) -> bool:
    if not token:
        return False
    with connect() as conn:
        create_schema(conn)
        seed(conn)
        conn.commit()
        row = conn.execute(
            "SELECT token FROM pad_profiles WHERE profile_id = ?",
            (profile_id,),
        ).fetchone()
        if row is None:
            return False
        return secrets.compare_digest(str(row["token"]), str(token))


def add_event(conn: sqlite3.Connection, profile_id: str, kind: str, title: str, data: Dict[str, Any]) -> None:
    conn.execute(
        """
        INSERT INTO pad_events
        (ts, profile_id, kind, title, data_json)
        VALUES (?, ?, ?, ?, ?)
        """,
        (utc_now(), profile_id, kind, title, json.dumps(data, ensure_ascii=False)),
    )


def _pet(conn: sqlite3.Connection, profile_id: str) -> Dict[str, Any]:
    row = conn.execute(
        "SELECT * FROM pad_pets WHERE profile_id = ?",
        (profile_id,),
    ).fetchone()
    if row is None:
        raise ValueError("profile has no pet")
    return dict(row)


def _save_pet(conn: sqlite3.Connection, s: Dict[str, Any]) -> None:
    s["updated_at"] = utc_now()
    conn.execute(
        """
        UPDATE pad_pets SET
            name = ?, stage = ?, level = ?, xp = ?, bond = ?, energy = ?, mood = ?,
            battle_ready = ?, evolution_path = ?, strength = ?, endurance = ?, speed = ?,
            focus = ?, instinct = ?, intelligence = ?, willpower = ?, hp = ?, max_hp = ?,
            habitat_id = ?, updated_at = ?
        WHERE pet_id = ?
        """,
        (
            s["name"],
            s["stage"],
            int(s["level"]),
            int(s["xp"]),
            int(s["bond"]),
            int(s["energy"]),
            int(s["mood"]),
            int(s["battle_ready"]),
            s["evolution_path"],
            int(s["strength"]),
            int(s["endurance"]),
            int(s["speed"]),
            int(s["focus"]),
            int(s["instinct"]),
            int(s["intelligence"]),
            int(s["willpower"]),
            int(s["hp"]),
            int(s["max_hp"]),
            s["habitat_id"],
            s["updated_at"],
            s["pet_id"],
        ),
    )


def _level_up(s: Dict[str, Any]) -> list[str]:
    notes: list[str] = []
    while int(s["xp"]) >= int(s["level"]) * 50:
        needed = int(s["level"]) * 50
        s["xp"] = int(s["xp"]) - needed
        s["level"] = int(s["level"]) + 1
        s["max_hp"] = int(s["max_hp"]) + 8
        s["hp"] = int(s["max_hp"])
        s["strength"] = int(s["strength"]) + 1
        s["endurance"] = int(s["endurance"]) + 1
        s["focus"] = int(s["focus"]) + 1
        s["willpower"] = int(s["willpower"]) + 1
        notes.append(f"Level {s['level']} erreicht")
    return notes


def _determine_path(s: Dict[str, Any]) -> str:
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


def _unlock_attacks(conn: sqlite3.Connection, s: Dict[str, Any]) -> list[str]:
    pet_id = s["pet_id"]
    existing = {
        row["attack_id"]
        for row in conn.execute("SELECT attack_id FROM pad_pet_attacks WHERE pet_id = ?", (pet_id,)).fetchall()
    }

    rules = [(2, "tail_sweep"), (2, "dragon_guard"), (3, "focus_breath"), (4, "storm_jump"), (5, "crystal_skin")]
    unlocked = []

    for level, attack_id in rules:
        if int(s["level"]) >= level and attack_id not in existing:
            conn.execute(
                "INSERT OR IGNORE INTO pad_pet_attacks (pet_id, attack_id, equipped, learned_at) VALUES (?, ?, ?, ?)",
                (pet_id, attack_id, 0, utc_now()),
            )
            row = conn.execute("SELECT name FROM pad_attacks WHERE attack_id = ?", (attack_id,)).fetchone()
            unlocked.append(row["name"] if row else attack_id)

    return unlocked


def get_status(profile_id: str) -> Dict[str, Any]:
    with connect() as conn:
        create_schema(conn)
        seed(conn)
        conn.commit()

        profile = conn.execute(
            "SELECT profile_id, display_name, role, access_level, created_at, updated_at FROM pad_profiles WHERE profile_id = ?",
            (profile_id,),
        ).fetchone()
        if profile is None:
            raise ValueError("unknown profile")

        pet = _pet(conn, profile_id)
        pet["stage_label"] = STAGE_LABELS.get(pet["stage"], pet["stage"])

        attacks = conn.execute(
            """
            SELECT a.attack_id AS id, a.name, a.element, a.attack_class AS class,
                   a.power, a.cost, a.accuracy, pa.equipped, pa.learned_at
            FROM pad_pet_attacks pa
            JOIN pad_attacks a ON a.attack_id = pa.attack_id
            WHERE pa.pet_id = ?
            ORDER BY pa.equipped DESC, a.attack_id ASC
            """,
            (pet["pet_id"],),
        ).fetchall()

        pet["attacks"] = [dict(row) for row in attacks]
        pet["battle_rating"] = battle_rating(pet)

        return {
            "ok": True,
            "profile": dict(profile),
            "pet": pet,
            "db_path": str(DB_PATH),
        }


def battle_rating(pet: Dict[str, Any]) -> int:
    return int(
        int(pet["level"]) * 100
        + int(pet["strength"]) * 8
        + int(pet["endurance"]) * 7
        + int(pet["speed"]) * 7
        + int(pet["focus"]) * 8
        + int(pet["bond"]) * 2
    )


def apply_action(profile_id: str, action: str, payload: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
    payload = payload or {}

    with connect() as conn:
        create_schema(conn)
        seed(conn)

        s = _pet(conn, profile_id)
        message = ""
        details: Dict[str, Any] = {}

        if action == "feed":
            s["energy"] = clamp(int(s["energy"]) + 15)
            s["mood"] = clamp(int(s["mood"]) + 5)
            s["bond"] = clamp(int(s["bond"]) + 1)
            s["battle_ready"] = clamp(int(s["battle_ready"]) + 4)
            message = "Gefüttert."

        elif action == "care":
            s["mood"] = clamp(int(s["mood"]) + 12)
            s["bond"] = clamp(int(s["bond"]) + 3)
            s["energy"] = clamp(int(s["energy"]) + 4)
            message = "Pflege abgeschlossen."

        elif action == "train":
            training_type = str(payload.get("training_type", "focus")).lower().strip()
            if int(s["energy"]) < 10:
                return {"ok": False, "error": "Zu wenig Energie für Training.", "status": get_status(profile_id)}

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
            message = f"Training abgeschlossen: {label}."
            details = {"training_type": training_type, "xp_gain": 18, "energy_cost": 12}

        elif action in ("freefight", "arena"):
            if int(s["energy"]) < 15:
                return {"ok": False, "error": "Zu wenig Energie für Kampf.", "status": get_status(profile_id)}

            mode = action
            enemy = "Wilder Gegner" if mode == "freefight" else "Arena-Gegner"
            player_score = battle_rating(s) + random.randint(0, 90)
            enemy_score = int(s["level"]) * (120 if mode == "arena" else 95) + random.randint(60, 160)
            win = player_score >= enemy_score

            hp_loss = random.randint(3, 12 if mode == "freefight" else 18)
            s["hp"] = max(1, int(s["hp"]) - hp_loss)
            s["energy"] = clamp(int(s["energy"]) - (15 if mode == "freefight" else 22))
            s["battle_ready"] = clamp(int(s["battle_ready"]) - (8 if mode == "freefight" else 12))

            if win:
                xp_gain = 28 if mode == "freefight" else 45
                s["bond"] = clamp(int(s["bond"]) + (2 if mode == "freefight" else 3))
                s["mood"] = clamp(int(s["mood"]) + (8 if mode == "freefight" else 12))
                result = "win"
                message = "Kampf gewonnen."
            else:
                xp_gain = 10 if mode == "freefight" else 16
                s["bond"] = clamp(int(s["bond"]) + 1)
                s["mood"] = clamp(int(s["mood"]) - 4)
                result = "loss"
                message = "Kampf verloren, Erfahrung gesammelt."

            s["xp"] = int(s["xp"]) + xp_gain
            details = {
                "mode": mode,
                "enemy": enemy,
                "result": result,
                "player_score": player_score,
                "enemy_score": enemy_score,
                "xp_gain": xp_gain,
                "hp_loss": hp_loss,
            }

        elif action == "evolve":
            stage = s["stage"]
            next_stage = None
            if stage == "egg" and int(s["level"]) >= 2 and int(s["bond"]) >= 12:
                next_stage = "young"
            elif stage == "young" and int(s["level"]) >= 4 and int(s["bond"]) >= 20:
                next_stage = "wyrmling"
            elif stage == "wyrmling" and int(s["level"]) >= 7 and int(s["bond"]) >= 35:
                next_stage = "runner"
            elif stage == "runner" and int(s["level"]) >= 10 and int(s["bond"]) >= 50:
                next_stage = "sky"

            if next_stage:
                old_path = s["evolution_path"]
                s["stage"] = next_stage
                s["evolution_path"] = _determine_path(s)
                s["max_hp"] = int(s["max_hp"]) + 15
                s["hp"] = int(s["max_hp"])
                s["mood"] = clamp(int(s["mood"]) + 15)
                s["battle_ready"] = clamp(int(s["battle_ready"]) + 20)
                message = f"Evolution erfolgreich: {STAGE_LABELS.get(next_stage, next_stage)}."
                details = {"from": stage, "to": next_stage, "path_before": old_path, "path_after": s["evolution_path"]}
            else:
                message = "Evolution noch nicht möglich."
                details = {"stage": stage, "level": s["level"], "bond": s["bond"]}

        else:
            return {"ok": False, "error": f"unknown action: {action}"}

        level_notes = _level_up(s)
        unlocks = _unlock_attacks(conn, s)
        _save_pet(conn, s)
        add_event(conn, profile_id, "pad.action", message, {"action": action, "details": details, "level_notes": level_notes, "unlocks": unlocks})
        conn.commit()

    result = get_status(profile_id)
    result.update({"action": action, "message": message, "details": details, "level_notes": level_notes, "unlocks": unlocks})
    return result


def make_battle_code(profile_id: str) -> str:
    prefix = profile_id.upper()[:6]
    suffix = "".join(secrets.choice(string.ascii_uppercase + string.digits) for _ in range(5))
    return f"{prefix}-{suffix}"


def export_battle_snapshot(profile_id: str) -> Dict[str, Any]:
    status = get_status(profile_id)
    pet = status["pet"]

    snapshot = {
        "profile_id": profile_id,
        "display_name": status["profile"]["display_name"],
        "pet_name": pet["name"],
        "stage": pet["stage"],
        "stage_label": pet["stage_label"],
        "level": pet["level"],
        "evolution_path": pet["evolution_path"],
        "battle_rating": pet["battle_rating"],
        "stats": {
            "strength": pet["strength"],
            "endurance": pet["endurance"],
            "speed": pet["speed"],
            "focus": pet["focus"],
            "instinct": pet["instinct"],
            "intelligence": pet["intelligence"],
            "willpower": pet["willpower"],
        },
        "attacks": [
            {
                "id": a["id"],
                "name": a["name"],
                "element": a["element"],
                "class": a["class"],
                "power": a["power"],
                "accuracy": a["accuracy"],
            }
            for a in pet["attacks"][:6]
        ],
    }

    code = make_battle_code(profile_id)

    with connect() as conn:
        create_schema(conn)
        seed(conn)
        conn.execute(
            """
            INSERT INTO battle_snapshots
            (code, owner_profile, snapshot_json, created_at)
            VALUES (?, ?, ?, ?)
            """,
            (code, profile_id, json.dumps(snapshot, ensure_ascii=False), utc_now()),
        )
        conn.commit()

    return {"ok": True, "code": code, "snapshot": snapshot}


def get_snapshot_by_code(code: str) -> Optional[Dict[str, Any]]:
    with connect() as conn:
        create_schema(conn)
        seed(conn)
        row = conn.execute(
            "SELECT snapshot_json FROM battle_snapshots WHERE code = ?",
            (code,),
        ).fetchone()
        if row is None:
            return None
        return json.loads(row["snapshot_json"])


def ghost_battle(profile_id: str, payload: Dict[str, Any]) -> Dict[str, Any]:
    opponent = payload.get("opponent")
    code = str(payload.get("code", "")).strip()

    if not opponent and code:
        opponent = get_snapshot_by_code(code)

    if not isinstance(opponent, dict):
        return {"ok": False, "error": "opponent snapshot or valid code required"}

    status = get_status(profile_id)
    pet = status["pet"]

    own_score = int(pet["battle_rating"]) + random.randint(0, 100)
    opp_score = int(opponent.get("battle_rating", 100)) + random.randint(0, 100)
    win = own_score >= opp_score

    reward = apply_action(profile_id, "train", {"training_type": "instinct"})
    result = {
        "ok": True,
        "mode": "ghost_battle",
        "result": "win" if win else "loss",
        "own_score": own_score,
        "opponent_score": opp_score,
        "opponent": opponent,
        "reward_message": reward.get("message", ""),
        "status": get_status(profile_id),
    }

    with connect() as conn:
        create_schema(conn)
        seed(conn)
        add_event(conn, profile_id, "pad.ghost_battle", result["result"], result)
        conn.commit()

    return result
