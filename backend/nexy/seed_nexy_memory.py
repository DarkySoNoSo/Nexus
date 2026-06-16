#!/usr/bin/env python3
from nexy_memory import init_db, add_event, add_fact, set_focus, connect, uid, now

def add_lesson(lesson, trigger_context="", mistake_prevented="", rule="", priority=0):
    con = connect()
    lid = uid()
    con.execute("""
        INSERT INTO nexy_lessons
        (id, lesson, trigger_context, mistake_prevented, rule, priority, created_at, status)
        VALUES (?, ?, ?, ?, ?, ?, ?, 'active')
    """, (lid, lesson, trigger_context, mistake_prevented, rule, priority, now()))
    con.commit()
    con.close()
    return lid

def main():
    init_db()

    e1 = add_event(
        source="manual_seed",
        event_type="system_lesson",
        title="Nexus Safe-Start Krise",
        body="Die Android-App machte beim Start automatisch eine Backend-Anfrage über loadHomeSnapshot(). Dadurch wurde der App-Start unnötig abhängig vom PC/Backend. Die Safe-Start-Regel wurde eingeführt: App muss immer offline starten.",
        importance=10,
        confidence=1.0
    )

    add_fact(
        statement="Nexus Android darf beim App-Start keine automatische Backend-Abfrage ausführen.",
        classification="fact",
        evidence_ref="commit 5d480f1",
        source_event_id=e1,
        confidence=1.0
    )

    add_lesson(
        lesson="Safe-Start niemals verletzen.",
        trigger_context="Android-App, onCreate, showHome, Startlogik",
        mistake_prevented="Start-Crash oder Hänger bei offline Backend.",
        rule="Keine HTTP GET/POST Calls aus onCreate() oder showHome().",
        priority=10
    )

    add_lesson(
        lesson="AI-Studio-Code niemals ungeprüft in den Nexus-Kern übernehmen.",
        trigger_context="Repo-Bereinigung, AI-Studio-Quarantäne",
        mistake_prevented="Architekturverlust, Rollenchaos, instabile App.",
        rule="AI-Studio bleibt Quarantäne, Kern bleibt Native/Backend/Nexy.",
        priority=10
    )

    e2 = add_event(
        source="manual_seed",
        event_type="user_context",
        title="Patrick Grundkontext",
        body="Patrick Herzog ist HVAC-Techniker in der Schweiz. Er baut Nexus/Nexy als persönliches digitales Betriebssystem mit Gedächtnis, Zeitstrahl, Kontext, Collector, Chef und Android-Cockpit. Er arbeitet direkt, lösungsorientiert und will keine KI-Floskeln.",
        importance=10,
        confidence=1.0
    )

    add_fact(
        statement="Patrick will Nexy als digitalen Assistenten mit Gedächtnis, Zeitstrahl und Kontext aufbauen.",
        classification="fact",
        evidence_ref="manual project context",
        source_event_id=e2,
        confidence=1.0
    )

    add_fact(
        statement="Nexy ist nicht nur Chat, sondern Assistentin, Kontextschicht und Gedächtniszugang.",
        classification="fact",
        evidence_ref="architecture decision",
        source_event_id=e2,
        confidence=1.0
    )

    set_focus(
        focus_name="Nexy Memory Core",
        description="SQLite-Gedächtnis mit Events, Timeline, Context, Facts, Lessons und Active Focus stabilisieren.",
        priority=10,
        next_action="Recall-Funktionen implementieren: remember, learn, focus, timeline, recall.",
        related_context="Nexus/Nexy Aufbau"
    )

    set_focus(
        focus_name="Android Safe Build",
        description="Neue APK aus bereinigtem Repo bauen und gegen stabile Gold-Version testen.",
        priority=9,
        next_action="Build starten, installieren, prüfen: Start offline, Theme-Wechsel, Header, Navigation.",
        related_context="Nexus Android Native"
    )

    print("OK: Nexy Seed Memory geschrieben.")

if __name__ == "__main__":
    main()
