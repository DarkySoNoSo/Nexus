# Nexi Memory Core

## Zweck

Nexi ist das einzige Hirn von Nexus. Nexi verwaltet Kontext, Zeitstrahl, Erinnerungen, Fakten, Claims, Unklarheiten, Entscheidungen, offene Themen und gelernte Regeln.

Der Codepfad darf weiter `nexy` heissen. Die fachliche Rolle ist Nexi.

## Legacy-Begriffe

Chef, Master und Index-Chef sind keine getrennten produktiven Gehirne. Sie sind Legacy-Funktionen innerhalb von Nexi:

- Chef = Entscheidungs- und Aktionslogik.
- Master = Langzeit- und Index-Kontext.
- Index-Chef = alte Bezeichnung fuer Orchestrierung und Priorisierung.

Neue Logik soll diese Funktionen in Nexi modellieren, nicht neue parallele Wahrheiten erzeugen.

## Speicherbereiche

### nexi_events

Rohereignisse aus Collector, Chat, Dateien, System und manuellen Eingaben.

Pflichtfelder:

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

### nexi_timeline

Chronologisch verdichtete Ereignisse.

Pflichtfelder:

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

### nexi_context

Semantische Verknuepfungen.

Pflichtfelder:

- id
- subject_type
- subject_id
- relation
- object_type
- object_id
- weight
- reason
- created_at

### nexi_facts

Fakten-, Claim- und Unklarheits-Speicher.

Pflichtfelder:

- id
- statement
- classification
- evidence_ref
- source_event_id
- confidence
- valid_from
- valid_to
- status

Erlaubte Klassifikation:

- fact
- claim
- unclear

### nexi_decisions

Robuste Entscheidungsspeicherung fuer App, Widget und Backend.

Pflichtfelder:

- id
- created_at
- source
- source_event_id
- conversation_id
- decision_type
- decision_value
- reason
- actor
- sync_status
- applied_at
- rollback_ref

### nexi_lessons

Gelernte Regeln.

Pflichtfelder:

- id
- lesson
- trigger_context
- mistake_prevented
- rule
- priority
- created_at
- last_used_at
- status

### nexi_active_focus

Aktuelle offene Themen.

Pflichtfelder:

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

1. Collector schreibt Rohdaten nach events.
2. Nexi liest events, facts, context, timeline, decisions und active_focus.
3. Entscheidungen werden in einer Queue gespeichert, die App und Widget gemeinsam nutzen koennen.
4. Zeitstrahl entsteht aus Ereignissen, nicht aus Vermutungen.
5. Fakten brauchen Quelle oder bleiben claim/unclear.
6. Lessons entstehen aus realen Fehlern, bestaetigten Mustern oder expliziten Patrick-Regeln.
7. Active Focus haelt aktuelle Baustellen, nicht den kompletten Speicher.
8. Dragon und DigiPad duerfen Nexi nicht direkt als produktiven Kern veraendern.
