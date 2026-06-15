# -*- coding: utf-8 -*-
"""
Nexus Android Offline Synchronization Manager
Version: v40.45 contract-aligned

Purpose:
- Keep mobile quick-ingest events locally when Nexus is offline.
- Synchronize only pending/retry events.
- Preserve data on every network/API failure.
- Match the Nexus.PS1 collector contract.
"""

import hashlib
import hmac
import json
import os
import sqlite3
from datetime import datetime, timezone

import requests


DEFAULT_SERVER_URL = "http://100.115.92.2:8081"
DEFAULT_COLLECTOR_ID = "nexus-collector-apk-v1"


class OfflineSyncManager:
    def __init__(self, db_path="nexus_offline.db", server_url=DEFAULT_SERVER_URL, collector_secret=None):
        self.db_path = db_path
        self.server_url = server_url.rstrip("/")
        self.collector_id = DEFAULT_COLLECTOR_ID
        self.collector_secret = collector_secret or os.environ.get("NEXUS_COLLECTOR_SECRET", "")
        self._init_db()

    def _connect(self):
        conn = sqlite3.connect(self.db_path, timeout=15)
        conn.execute("PRAGMA journal_mode=WAL")
        conn.execute("PRAGMA busy_timeout=10000")
        return conn

    def _init_db(self):
        conn = self._connect()
        try:
            cursor = conn.cursor()
            cursor.execute(
                """
                CREATE TABLE IF NOT EXISTS sync_queue (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timestamp TEXT NOT NULL,
                    event_type TEXT NOT NULL,
                    payload TEXT NOT NULL,
                    status TEXT DEFAULT 'PENDING',
                    retry_count INTEGER DEFAULT 0,
                    last_error TEXT
                )
                """
            )
            cursor.execute("CREATE INDEX IF NOT EXISTS idx_sync_queue_status ON sync_queue(status)")
            conn.commit()
        finally:
            conn.close()

    def queue_event(self, event_type, payload_dict):
        timestamp = datetime.now(timezone.utc).isoformat()
        payload_str = json.dumps(payload_dict, ensure_ascii=False, sort_keys=True, separators=(",", ":"))

        conn = self._connect()
        try:
            cursor = conn.cursor()
            cursor.execute(
                """
                INSERT INTO sync_queue (timestamp, event_type, payload, status, retry_count, last_error)
                VALUES (?, ?, ?, 'PENDING', 0, NULL)
                """,
                (timestamp, event_type, payload_str),
            )
            conn.commit()
            return True
        finally:
            conn.close()

    def get_pending_count(self):
        conn = self._connect()
        try:
            cursor = conn.cursor()
            cursor.execute(
                """
                SELECT COUNT(*)
                FROM sync_queue
                WHERE status IN ('PENDING', 'ERROR_RETRY')
                """
            )
            return int(cursor.fetchone()[0])
        finally:
            conn.close()

    def check_connection(self):
        try:
            response = requests.get(f"{self.server_url}/api/health", timeout=3)
            return response.status_code == 200
        except requests.RequestException:
            return False

    def _build_payload(self, item_id, timestamp, event_type, payload_raw):
        payload_data = json.loads(payload_raw)
        payload_data["original_timestamp"] = timestamp
        payload_data["event_type"] = event_type
        payload_data["source"] = payload_data.get("source") or "Android App Client - Mobile"
        payload_data["nexus_agent_meta"] = {
            "client": "Nexus Android Daemon",
            "queue_id": item_id,
            "sync_contract": "nexus-mobile-api-contract-v1",
        }
        return payload_data

    def _encode_json(self, payload_data):
        return json.dumps(payload_data, ensure_ascii=False, sort_keys=True, separators=(",", ":"))

    def _sign_payload(self, timestamp, raw_json):
        if not self.collector_secret:
            return None
        signing_payload = f"{timestamp}\n{raw_json}".encode("utf-8")
        return hmac.new(self.collector_secret.encode("utf-8"), signing_payload, hashlib.sha256).hexdigest()

    def _headers_for_payload(self, timestamp, raw_json):
        headers = {
            "Content-Type": "application/json; charset=utf-8",
            "X-Nexus-Collector": self.collector_id,
        }

        signature = self._sign_payload(timestamp, raw_json)
        if signature:
            headers["X-Nexus-Collector-Ts"] = timestamp
            headers["X-Nexus-Collector-Signature"] = signature

        return headers

    def process_queue_once(self):
        conn = self._connect()
        cursor = conn.cursor()
        success_count = 0

        try:
            cursor.execute(
                """
                SELECT id, timestamp, event_type, payload, retry_count
                FROM sync_queue
                WHERE status IN ('PENDING', 'ERROR_RETRY')
                ORDER BY id ASC
                LIMIT 25
                """
            )
            pending_items = cursor.fetchall()

            if not pending_items:
                return 0

            if not self.check_connection():
                cursor.execute(
                    """
                    UPDATE sync_queue
                    SET status = 'ERROR_RETRY',
                        retry_count = retry_count + 1,
                        last_error = ?
                    WHERE status IN ('PENDING', 'SYNCHRONIZING')
                    """,
                    ("Nexus Master-Server nicht erreichbar.",),
                )
                conn.commit()
                raise ConnectionError("Nexus Master-Server nicht erreichbar. Offline-Queue bleibt erhalten.")

            url = f"{self.server_url}/api/communication/ingest"

            for item_id, ts, ev_type, payload_raw, retry_cnt in pending_items:
                cursor.execute(
                    "UPDATE sync_queue SET status = 'SYNCHRONIZING', last_error = NULL WHERE id = ?",
                    (item_id,),
                )
                conn.commit()

                try:
                    payload_data = self._build_payload(item_id, ts, ev_type, payload_raw)
                    raw_json = self._encode_json(payload_data)
                    request_ts = datetime.now(timezone.utc).isoformat()
                    headers = self._headers_for_payload(request_ts, raw_json)

                    response = requests.post(
                        url,
                        data=raw_json.encode("utf-8"),
                        headers=headers,
                        timeout=8,
                    )

                    if response.status_code == 200:
                        cursor.execute(
                            """
                            UPDATE sync_queue
                            SET status = 'COMPLETED',
                                last_error = NULL
                            WHERE id = ?
                            """,
                            (item_id,),
                        )
                        success_count += 1
                    else:
                        cursor.execute(
                            """
                            UPDATE sync_queue
                            SET status = 'ERROR_RETRY',
                                retry_count = ?,
                                last_error = ?
                            WHERE id = ?
                            """,
                            (int(retry_cnt) + 1, f"HTTP {response.status_code}: {response.text[:500]}", item_id),
                        )
                except Exception as exc:
                    cursor.execute(
                        """
                        UPDATE sync_queue
                        SET status = 'ERROR_RETRY',
                            retry_count = ?,
                            last_error = ?
                        WHERE id = ?
                        """,
                        (int(retry_cnt) + 1, str(exc), item_id),
                    )

                conn.commit()

            return success_count
        finally:
            conn.close()
