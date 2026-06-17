# Change Record 2026-06-17 – 00 bis 99 ist Kern

Datum/Zeit: 2026-06-17
Bereich: Master-Index / Protected Core / Datenintegritaet
Schutzklasse: GELB fuer Dokumentation, ROT-relevant fuer Kernregel

## Aenderung

Die Protected-Core-Policy wurde erweitert: Im bestehenden Master-Index gehoeren alle numerisch gefuehrten Top-Level-Bereiche von `00_` bis `99_` zum Kern-Wirkbereich.

## Grund

Patrick hat klargestellt: Von 00 bis 99 ist Kern. Die vorhandene Nummernstruktur darf nicht durch neue, konkurrierende 00-Strukturen ueberbaut werden. Der reale Bestand ist massgebend.

## Betroffene Bereiche

- alle Top-Level-Ordner von `00_` bis `99_` im Master-Index
- bestehende Bereiche wie `00_ZENTRALE`, `01_SORTED`, `02_EVENTS_DB`, `03_EVENTS_DB`, `04_EVIDENCE_MAP`, `04_QUELLENREGISTER`, `06_REPORTS`, `08_SYSTEM_LOGS`, `41_FIONA`, `43_ZOE`, `99_VIEW`
- Protected-Core-Policy
- alle zukuenftigen Audit-, Import-, Sortier- und Strukturentscheidungen

## Wirkung

Kern-Wirkbereich bedeutet nicht, dass jede Datei automatisch ROT ist. Es bedeutet: Der Bereich steht unter Kern-Governance. Jede schreibende Aenderung ist mindestens dokumentationspflichtig. Inhalte koennen intern weiter als ROT, ORANGE, GELB oder GRUEN klassifiziert werden.

## Verbot

Keine neue `00_`-Projektwurzel im Master-Index ohne vorherigen Audit und explizite Freigabe.

## Pruefung

Es wurde nur Dokumentation im Repo angepasst. Es wurden keine Daten verschoben, geloescht, importiert oder umbenannt.

## Rollback

Rollback durch Ruecksetzen der Dokumentationsaenderung in `docs/rules/NEXUS_PROTECTED_CORE_POLICY.md` und Entfernen dieses Change Records.

## Ergebnis

Die Aussage `00 bis 99 ist Kern` ist als Repo-Governance dokumentiert.
