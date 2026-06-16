# NEXY IMPORT MAPPING V1

Stand: 2026-06-16

## Zweck

Dieses Mapping definiert, wie die bestehenden Nexus-/MasterIndex-/Chef-Quellen kontrolliert in Nexys neue Memory-Schicht überführt werden.

Wichtig: Dies ist kein Blindimport. Jede Quelle bleibt unverändert. Die erste Stufe erzeugt nur eine Vorschau.

## Quellen

| Quelle | Typ | Ziel |
|---|---|---|
| patrick_context_profile.md | Identitäts-/Kontextprofil | nexy_events, nexy_facts, nexy_context |
| communication_context.md | Kommunikationshistorie / Kommunikationsregeln | nexy_events, nexy_context, nexy_lessons |
| context_chef_channel.jsonl | Chef-Kanal / Entscheidungslog | nexy_events, nexy_lessons, nexy_facts |
| nexus_memory.json | Klartext-Regelwerk trotz .json-Endung | nexy_lessons, nexy_facts |
| nexus_timeline.jsonl | laufender Zeitstrahl | nexy_events, nexy_timeline |
| patrick_timeline.md | lesbarer Zeitstrahl | nexy_timeline, nexy_facts |
| nexus_catalog.sqlite | altes Gehirn / Katalog / Memory DB | alle Zielbereiche nach Tabellenmapping |

## SQLite-Tabellenmapping

| Legacy-Tabelle | Ziel |
|---|---|
| timeline_events | nexy_timeline + nexy_events |
| timeline_claims | nexy_facts mit classification=claim/unclear |
| chef_events | nexy_events |
| chef_facts | nexy_facts |
| chef_learning_rules | nexy_lessons |
| learning_rules | nexy_lessons |
| communication_events | nexy_events |
| communication_daily_summaries | nexy_context / summaries |
| context_entities | nexy_context |
| context_relations | nexy_context |
| context_rules | nexy_lessons |
| legacy_entries | nexy_events |
| semantic_cards | später: Recall-/Search-Index, nicht blind importieren |
| files | MasterIndex-Dateireferenz, nicht direkt in Nexy kopieren |

## Sicherheitsregeln

1. Alte Quellen niemals überschreiben.
2. Import nur mit source_ref.
3. Jede Aussage bekommt classification: fact, claim oder unclear.
4. Große Tabellen werden zuerst nur gezählt und mit Samples geprüft.
5. semantic_cards und files werden nicht komplett in nexy.db kopiert.
6. Personendaten bleiben lokal.
7. Nexy.db wird vereinheitlichte Erinnerungsschicht, nicht Rohdaten-Müllhalde.
