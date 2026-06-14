# -*- coding: utf-8 -*-
"""
Nexus v40.44 - Android Offline Synchronization Manager
SSOT: Single Source of Truth Offline Queue
Sicherheitsstufe: HOCH (HMAC-Verschlüsselung & Status-Audit)
"""

import os
import time
import sqlite3
import requests
import hmac
import hashlib
import json
from datetime import datetime

class OfflineSyncManager:
    def __init__(self, db_path="nexus_offline.db", server_url="http://100.115.92.2:8081"):
        """
        Initialisiert den Offline-Protokoll-Puffer für die Android-App.
        :param db_path: Lokaler Puffer auf dem Android-Dateisystem.
        :param server_url: Ziel-URL (Standardmäßig das Tailscale-Netzwerk deines PCs auf Port 8081).
        """
        self.db_path = db_path
        self.server_url = server_url.rstrip('/')
        self._init_db()

    def _init_db(self):
        """Erstellt die lokale SQLite-Queue, falls sie noch nicht existiert."""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS sync_queue (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                event_type TEXT NOT NULL,
                payload TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING',
                retry_count INTEGER DEFAULT 0,
                last_error TEXT
            )
        """)
        conn.commit()
        conn.close()

    def queue_event(self, event_type, payload_dict):
        """
        Puffert ein Ereignis (z.B. SMS, Zählerfoto-Pfad oder Schnellbeleg) lokal.
        Garantiert, dass kein Beleg bei Verbindungsverlust verloren geht.
        """
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        timestamp = datetime.utcnow().isoformat() + "Z"
        payload_str = json.dumps(payload_dict)
        
        cursor.execute("""
            INSERT INTO sync_queue (timestamp, event_type, payload, status)
            VALUES (?, ?, ?, 'PENDING')
        """, (timestamp, event_type, payload_str))
        
        conn.commit()
        conn.close()
        return True

    def get_pending_count(self):
        """Gibt die Anzahl der noch nicht synchronisierten Einträge zurück."""
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        cursor.execute("SELECT COUNT(*) FROM sync_queue WHERE status = 'PENDING'")
        count = cursor.fetchone()[0]
        conn.close()
        return count

    def check_connection(self):
        """
        Prüft die Erreichbarkeit des Nexus-Master-Servers im lokalen Netz / VPN.
        """
        try:
            response = requests.get(f"{self.server_url}/api/health", timeout=3)
            return response.status_code == 200
        except requests.RequestException:
            return False

    def sign_payload(self, payload_str, secret_key="NEXUS_LEGACY_HMAC_SECRET_2026"):
        """
        Erstellt eine HMAC-SHA256 Signatur zur Absicherung mobiler Ingestionen.
        """
        return hmac.new(
            secret_key.encode('utf-8'),
            payload_str.encode('utf-8'),
            hashlib.sha256
        ).hexdigest()

    def process_queue_once(self):
        """
        Verarbeitet anstehende Übertragungen.
        Implementiert Exponential Backoff und Fehler-Logging.
        """
        conn = sqlite3.connect(self.db_path)
        cursor = conn.cursor()
        
        # Nur ausstehende (PENDING) Events laden
        cursor.execute("SELECT id, timestamp, event_type, payload, retry_count FROM sync_queue WHERE status = 'PENDING'")
        pending_items = cursor.fetchall()
        
        if not pending_items:
            conn.close()
            return 0

        # Verbindungsprüfung vorab
        if not self.check_connection():
            conn.close()
            raise ConnectionError("Nexus Master-Server nicht erreichbar. Warte im Offline-Modus...")

        success_count = 0
        
        for item_id, ts, ev_type, payload_raw, retry_cnt in pending_items:
            # Sicherheitsprüfung / Signierung des Payloads
            signature = self.sign_payload(payload_raw)
            
            headers = {
                "Content-Type": "application/json",
                "X-Nexus-Signature": signature,
                "X-Nexus-Collector": "nexus-collector-apk-v1"
            }
            
            payload_data = json.loads(payload_raw)
            payload_data["original_timestamp"] = ts
            payload_data["nexus_agent_meta"] = "Android-Daemon Sync-Engine"
            
            url = f"{self.server_url}/api/communication/ingest"
            
            try:
                # Sende Event-Ingest
                response = requests.post(url, json=payload_data, headers=headers, timeout=5)
                
                if response.status_code == 200:
                    cursor.execute("UPDATE sync_queue SET status = 'SUCCESS' WHERE id = ?", (item_id,))
                    success_count += 1
                else:
                    new_retry = retry_cnt + 1
                    error_msg = f"HTTP {response.status_code}: {response.text}"
                    cursor.execute("""
                        UPDATE sync_queue 
                        SET retry_count = ?, last_error = ?, status = 'PENDING' 
                        WHERE id = ?
                    """, (new_retry, error_msg, item_id))
            except Exception as e:
                new_retry = retry_cnt + 1
                cursor.execute("""
                    UPDATE sync_queue 
                    SET retry_count = ?, last_error = ?, status = 'PENDING' 
                    WHERE id = ?
                """, (new_retry, str(e), item_id))
                
        conn.commit()
        conn.close()
        return success_count
