# Nexy Legacy Import Preview

Stand: 2026-06-16T21:49:47.217470+00:00
NEXUS_STORAGE: `C:\MasterIndex_Storage`

## Modus

Preview only. Es wird nichts in nexy.db geschrieben.

## Quellen

| Quelle | Existiert | Größe | Ziel | Hinweis |
|---|---:|---:|---|---|
| patrick_context_profile | True | 7562 | nexy_events, nexy_facts, nexy_context |  |
| communication_context | True | 212075 | nexy_events, nexy_context, nexy_lessons |  |
| chef_channel | True | 25416 | nexy_events, nexy_facts, nexy_lessons |  |
| nexus_memory | True | 3765 | nexy_lessons, nexy_facts | Datei endet auf .json, ist aber Klartext-Regelwerk. |
| timeline_jsonl | True | 665511 | nexy_events, nexy_timeline |  |
| patrick_timeline_md | True | 3148 | nexy_timeline, nexy_facts |  |
| legacy_sqlite | True | 225050624 | all_mapped_tables |  |

## Vorgeschlagene Import-Batches

### identity_context
- Quelle: `patrick_context_profile`
- Modus: `section_based_manual_review_first`
- Risiko: `medium`
- Grund: Identitätsanker, Familien-/Projekt-/Arbeitskontext. Muss sauber in Fact/Claim/Unclear getrennt werden.

### communication_context
- Quelle: `communication_context`
- Modus: `summaries_and_rules_first`
- Risiko: `high`
- Grund: Großes Kommunikationsmaterial, externe Inhalte sind untrusted evidence. Nicht blind als Fakten speichern.

### chef_channel
- Quelle: `chef_channel`
- Modus: `jsonl_event_import_after_schema_mapping`
- Risiko: `medium`
- Grund: Chef-Kanal enthält Nutzerregeln, Entscheidungen und Lernpunkte.

### nexus_memory_rules
- Quelle: `nexus_memory`
- Modus: `parse_as_text_rules`
- Risiko: `low`
- Grund: Enthält Arbeitsregeln und Orchestrator-Anweisungen.

### timeline_jsonl
- Quelle: `timeline_jsonl`
- Modus: `jsonl_timeline_import`
- Risiko: `medium`
- Grund: Laufender echter Zeitstrahl, 1:1 zeitlich relevant.

### patrick_timeline_markdown
- Quelle: `patrick_timeline_md`
- Modus: `markdown_table_parse_later`
- Risiko: `medium`
- Grund: Lesbarer Zeitstrahl aus Kontext-PDF, Belegstatus beachten.

### legacy_sqlite_core_tables
- Quelle: `legacy_sqlite`
- Modus: `table_by_table_with_dedup_and_source_ref`
- Risiko: `high`
- Grund: Sehr große DB. Import muss deduplizieren und darf files/semantic_cards nicht blind kopieren.

## Warnungen

- nexus_memory.json ist kein JSON, sondern Klartext mit .json-Endung.

## Offene Entscheidungen

- Welche Quelle zuerst produktiv importieren? Empfehlung: nexus_memory.json als Lessons, dann chef_learning_rules/context_rules.
- Soll communication_context.md nur als Summary/Regeln importiert werden? Empfehlung: Ja, nicht als rohe Fakten.
- Sollen files und semantic_cards nur referenziert statt kopiert werden? Empfehlung: Ja.
- Soll timeline_jsonl vollständig importiert werden? Empfehlung: Ja, aber mit Dedupe-Key aus ts+kind+title.
