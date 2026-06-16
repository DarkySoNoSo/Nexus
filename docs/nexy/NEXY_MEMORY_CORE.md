# Nexy Memory Core

## Zweck

Nexy ist der digitale Assistent von Nexus. Sie verwaltet Kontext, Zeitstrahl, Erinnerungen, Fakten, offene Themen und gelernte Regeln.

## Tabellen / Speicherbereiche

### nexy_events
Rohereignisse aus Collector, Chat, Dateien, System und manuellen Eingaben.

Felder:
- id
- created_at
- event_time
- source
- source_ref
- event_type
- title
- body
- raw_payload
- importance
- confidence
- status

### nexy_timeline
Chronologisch verdichtete Ereignisse.

Felder:
- id
- event_id
- timeline_time
- actor
- topic
- summary
- cause
- consequence
- linked_before
- linked_after
- visibility
- status

### nexy_context
Semantische Verknüpfungen.

Felder:
- id
- subject_type
- subject_id
- relation
- object_type
- object_id
- weight
- reason
- created_at

### nexy_facts
Fakten-/Claim-/Unklarheits-Speicher.

Felder:
- id
- statement
- classification
- evidence_ref
- source_event_id
- confidence
- valid_from
- valid_to
- status

classification:
- fact
- claim
- unclear

### nexy_lessons
Gelernte Regeln.

Felder:
- id
- lesson
- trigger_context
- mistake_prevented
- rule
- priority
- created_at
- last_used_at
- status

### nexy_active_focus
Aktuelle offene Themen.

Felder:
- id
- focus_name
- description
- priority
- next_action
- related_context
- due_at
- status
- updated_at

## Grundregeln

1. Collector schreibt Rohdaten nach nexy_events.
2. Nexy liest events, facts, context, timeline und active_focus.
3. Chef darf Aktionen auslösen, aber nicht ungeprüft Fakten überschreiben.
4. Zeitstrahl entsteht aus Ereignissen, nicht aus Bauchgefühl.
5. Fakten brauchen Quelle oder bleiben claim/unclear.
6. Lessons entstehen aus realen Fehlern oder bestätigten Mustern.
7. Active Focus hält nur aktuelle Baustellen, nicht alles.
