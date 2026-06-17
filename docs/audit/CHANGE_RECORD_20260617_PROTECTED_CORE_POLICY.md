# Change Record 2026-06-17 – Protected Core Policy

Datum/Zeit: 2026-06-17
Bereich: Governance / Protected Core / Write Documentation
Schutzklasse: GELB fuer Dokumentation, ROT fuer betroffene Kernregel

## Aenderung

Es wurden verbindliche Repo-Dokumente fuer den geschuetzten Nexus-Kern und die Dokumentationspflicht ab Schutzklasse GELB angelegt.

Neue Dateien:

- `docs/rules/NEXUS_PROTECTED_CORE_POLICY.md`
- `docs/rules/WRITE_DOCUMENTATION_POLICY.md`

## Grund

Patrick hat festgelegt, dass alle sehr sensiblen Datenbereiche zentral in einen geschuetzten Kern gehoeren. Dazu zaehlen insbesondere Nexy, Zeitstrahl und Kontext. Zusaetzlich wurde festgelegt, dass Schreiben ab Schutzklasse GELB nur mit Dokumentation erlaubt ist.

## Betroffene Bereiche

- Nexy
- Zeitstrahl
- Kontext
- Fakten / Claims / Unklarheiten
- Lessons / Regeln
- Quellenregister
- Import- und Recovery-Prozesse
- App-, UI-, Workflow- und Skriptaenderungen ab GELB

## Risiko

Ohne diese Regel besteht Risiko fuer Kontextverlust, unkontrollierte Importe, unklare Wahrheitsbildung, nicht nachvollziehbare Patches und Vermischung von Rohdaten mit Fakten.

## Pruefung

Die Aenderung ist rein dokumentarisch. Es wurden keine Datenbanken, Importquellen, App-Dateien oder Laufzeitdienste veraendert.

## Rollback

Rollback erfolgt durch Entfernen oder Ersetzen der beiden neu angelegten Policy-Dateien und dieses Change Records. Da keine Laufzeitdaten veraendert wurden, ist kein DB-Rollback notwendig.

## Ergebnis

Die Dokumentationspflicht ist direkt im Repository dokumentiert. Der Protected-Core-Begriff ist repo-seitig definiert und fuer weitere Arbeiten verbindlich referenzierbar.
