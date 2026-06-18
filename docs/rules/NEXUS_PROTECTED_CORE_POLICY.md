# Nexus Protected Core Policy

Status: verbindlich
Schutzklasse: ROT
Gueltig ab: 2026-06-17

## Zweck

Der `NEXUS_PROTECTED_CORE` ist die zentrale geschuetzte Wahrheitsschicht des Nexus-Projekts.

Er enthaelt alle Informationen, die fuer Erinnerung, Kontext, Zeitstrahl, Beweisstatus, Regeln, Quellen und langfristige Projektentscheidungen massgeblich sind.

Der Kern ist wichtiger als jedes Feature.

## Master-Index-Wirkbereich

Im bestehenden Master-Index gilt verbindlich:

- Alle numerisch gefuehrten Top-Level-Bereiche von `00_` bis `99_` gehoeren zum Kern-Wirkbereich.
- Diese Bereiche werden nicht als ausserhalb des Kerns behandelt.
- Es wird keine neue konkurrierende `00_`-Projektwurzel angelegt.
- Die bestehende Nummernstruktur wird zuerst inventarisiert, dann klassifiziert und erst danach dokumentiert veraendert.
- Kern-Wirkbereich bedeutet: Jede schreibende Aenderung unter `00_` bis `99_` ist mindestens dokumentationspflichtig.
- Die konkrete Schutzklasse innerhalb des Kern-Wirkbereichs bleibt differenziert: ROT, ORANGE, GELB oder GRUEN, je nach Inhalt und Risiko.

Sichtbare Beispiele aus dem Master-Index:

- `00_ZENTRALE` = Kern-Wirkbereich
- `01_SORTED` = Kern-Wirkbereich
- `02_EVENTS_DB` = Kern-Wirkbereich
- `03_EVENTS_DB` = Kern-Wirkbereich, ROT-Review
- `04_EVIDENCE_MAP` = Kern-Wirkbereich, ROT-Review
- `04_QUELLENREGISTER` = Kern-Wirkbereich, ROT-Review
- `06_REPORTS` = Kern-Wirkbereich
- `08_SYSTEM_LOGS` = Kern-Wirkbereich
- `41_FIONA` = Kern-Wirkbereich, ROT-Review
- `43_ZOE` = Kern-Wirkbereich, ROT-Review
- `99_VIEW` = Kern-Wirkbereich

## Was zum Kern gehoert

Zum geschuetzten Kern gehoeren:

- alle numerischen Master-Index-Bereiche von `00_` bis `99_` als Kern-Wirkbereich
- Nexy-Gedaechtnis
- Zeitstrahl
- Kontext
- Fakten
- Claims
- Unklarheiten
- Lessons
- Regeln
- Quellenregister
- Vault- und Backup-Kette
- Importberichte
- Schutz- und Rollenregeln

## Nexy

Nexy gehoert vollstaendig zum geschuetzten Kern. Dazu gehoeren mindestens:

- `nexy_events`
- `nexy_timeline`
- `nexy_context`
- `nexy_facts`
- `nexy_lessons`
- `nexy_active_focus`

Nexy ist Gedaechtnis, Kontext, Recall, Orientierung und Entscheidungsgrundlage.

## Zeitstrahl

Der Zeitstrahl ist ROT. Er beantwortet:

- wann etwas passiert ist
- aus welcher Quelle es stammt
- wie sicher es ist
- welche Klassifikation gilt
- welcher Import- oder Beweisstatus besteht

Zeitstrahl-Daten duerfen nie ohne Dokumentation veraendert werden.

## Klassifikation

Jeder Kerninhalt benoetigt eine Klassifikation:

- `fact` = belegt oder systemisch beobachtet
- `claim` = uebernommene Aussage oder noch nicht voll verifizierte Kontextbehauptung
- `unclear` = unklar, widerspruechlich oder nicht geprueft
- `untrusted` = Rohquelle oder externe Kommunikation ohne Wahrheitsuebernahme

Ohne Klassifikation kein Kernimport.

## Rollen

### Nexy

Darf lesen, strukturieren, erinnern und verknuepfen. Schreiben nur ueber gepruefte Kernpfade.

### Master

Darf analysieren, planen und ausfuehren. Kernschreiben nur mit Dokumentation, Vault, Pruefung und Rollback.

### Collector

Darf Rohdaten sammeln. Darf nicht direkt Fakten erzeugen.

### Digi Dragon

Darf als getrennte Spielschicht laufen. Darf Nexy, Kontext und Zeitstrahl nicht ungefragt veraendern.

### DigiPad

Darf nur gefilterte Digi-Dragon-/Remote-Daten anzeigen. Darf keine Kern-Rohdaten und keine Adminfunktionen erhalten.

### Android App

Darf anzeigen und kontrollierte Aktionen ausloesen. Darf beim Start keine Kernoperation erzwingen.

## Schreibregel

Schreiben ab GELB ist dokumentationspflichtig.

ROT erfordert zusaetzlich:

- Vault oder Backup
- Quelle
- Hash
- Klassifikation
- Vorher/Nachher-Pruefung
- Healthcheck
- Rollback-Hinweis

## Verbote

Verboten sind:

- neue konkurrierende `00_`-Projektwurzel im Master-Index ohne vorherigen Audit
- Kernimport ohne Vault
- Kernimport ohne Klassifikation
- Rohkommunikation als Fakt
- Digi-Dragon-Direktschreibzugriff in Nexy
- DigiPad-Direktschreibzugriff in Nexy
- App-Start, der Backend oder Kern erzwingt
- Vaults im Repo
- `.nexy_legacy_storage/` im Repo

## Leitsatz

Der `NEXUS_PROTECTED_CORE` ist die zentrale, geschuetzte Wahrheitsschicht von Nexus. Im Master-Index gehoeren alle numerisch gefuehrten Bereiche von `00_` bis `99_` zum Kern-Wirkbereich. Jede Aenderung ab GELB bekommt eine nachvollziehbare Spur. ROT bekommt zusaetzlich Vault, Hash, Klassifikation, Pruefung und Healthcheck.
