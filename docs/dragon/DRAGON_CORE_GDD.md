# Dragon Core GDD

Status: verbindliche Zielrichtung
Schutzklasse: GELB
Gueltig ab: 2026-06-18

## Ziel

Digi Dragon ist die lokale Begleiter- und Spielschicht von Nexus. Der Drache ist kein neues Hirn, kein Chef-Ersatz und kein automatischer API-Ausloeser. Er visualisiert, motiviert und bildet Fortschritt spielerisch ab.

## Rollen

| Rolle | Aufgabe | Grenze |
|---|---|---|
| Nexus | App- und Backend-Geruest | keine eigene Gehirnrolle |
| Nexi | Gedaechtnis, Kontext, Recall, Zeitstrahl, Entscheidungen | wird nicht automatisch durch Dragon mutiert |
| Chef | Legacy-Ausfuehrung, Analyse, Aufgabenpfade innerhalb Nexi | keine Dragon-Autostarts |
| Dragon Core | lokaler Begleiter, Habitat, Training, Kampf, Evolution | offline und safe-start-faehig |
| Collector | Rohdatenaufnahme | keine direkten Dragon-Fakten |

## Harte Regeln

- App-Start bleibt safe-start: keine Dragon-HTTP-Calls beim Oeffnen.
- Dragon Core funktioniert lokal und offline.
- Keine automatischen Kosten oder API-Calls durch Dragon Core.
- Nexi darf nur bewusst ueber eine explizite Nutzeraktion einbezogen werden.
- Dragon Core darf motivieren, visualisieren und lokale Werte speichern.
- Dragon Core darf nicht ungefragt Kontext, Zeitstrahl oder Entscheidungen schreiben.

## Ebenen

### Spiel-Ebene

- Training
- Arena
- Freikaempfe
- Attacken
- Evolution
- Habitat-Ausbau

### Begleiter-Ebene

- Bindung
- Stimmung
- Pflege
- Reaktion auf Antippen
- Persoenlichkeitsachsen

### Nexus-Ebene

- spaetere Kopplung an Fokus, Tagesziele, saubere Logs und echte Aktivitaet
- nur lesend oder nach expliziter Nutzeraktion
- keine Rollenmischung mit Nexi

## MVP v1

MVP v1 muss klein, sichtbar und stabil bleiben:

- ein Drache
- ein Habitat
- lokale Speicherung
- visualisierte Werte
- direkte Interaktion per Touch
- Training
- Arena-Grundmodus
- Freikampf-Grundmodus
- Entwicklung/Evolution
- Codex-Uebersicht
- keine Backend-Pflicht

## Implementierungsstandard v1

Der erste spielbare Kern wird lokal in der Android-App ausgefuehrt:

- Training ist kein Platzhalter: Kraft, Ausdauer, Flug, Fokus und Instinkt haben eigene Kosten und eigene Werteffekte.
- Pflege, Ruhe, Training, Freikampf und Arena muessen den gespeicherten Dragon-State sofort sichtbar veraendern.
- Freikampf und Arena nutzen eine lokale Kampfberechnung aus Stats, Bindung, Stimmung, Stress und Gegnerwert.
- Siege und Niederlagen werden getrennt gespeichert.
- XP-Schwellen loesen sichtbare Entwicklungsstufen aus.
- Jede Aktion schreibt ein Ergebnis in die UI, damit der Spieler die Konsequenz sofort versteht.

## Kernwerte

| Wert | Zweck |
|---|---|
| level | Progression |
| xp | Wachstum |
| energy | Aktionsfaehigkeit |
| stress | Belastung |
| mood | Stimmung |
| bond | Bindung |
| wins | Arena-/Kampferfolg |
| strength | physische Kraft |
| endurance | Ausdauer |
| speed | Tempo |
| focus | Konzentration |
| instinct | Instinkt |
| intelligence | Intelligenz |
| willpower | Willenskraft |

## Entwicklungsstufen

1. Ei
2. Jungtier
3. Wyrmling
4. Junger Drache
5. Reifer Drache
6. Erwachter Drache
7. Mythische Form

## Evolutionspfade

- Waechterdrache: Bindung, Balance, Defensive
- Kriegsdrache: Training, Staerke, offensive Siege
- Arkaner Drache: Fokus, Intelligenz, Energie
- Jaegerdrache: Tempo, Instinkt, Freikaempfe
- Kristalldrache: seltene Fokus-/Bindungsform
- Cyberdrache: spaeterer Nexus-/Tech-Pfad

## Habitat

Start-Habitat: kleines Nest.

Ausbaupfade:

- Hoehle
- geheiligtes Lager
- Drachenhort
- schwebende Insel
- mythisches Sanctum

Themen:

- Vulkan
- Waldlichtung
- Kristallhoehle
- Sturmklippe
- Ruinen
- Mondtempel
- Cyber-Nest

## Kampf

Startsystem: rundenbasierter, lokal berechneter Kampf.

Pflichtteile:

- HP
- Energie/Fokus-Kosten
- Initiative
- 4 aktive Attacken
- Status-Effekte spaeter
- Elementtypen spaeter

Startattacken:

- Feuer: Flammenstoss, Aschenklaue, Glutpanzer, Inferno-Konter
- Wasser: Wellenbiss, Tiefenblick, Nebelschild, Abyssaler Sog
- Erde: Steinklaue, Panzerstand, Kristallstoss, Titanenkonter
- Luft: Sturmflug, Windkante, Ausweichen, Himmelssturz
- Schatten: Schattensprung, Nachtklaue, Nebelkoerper, Seelenkonter

Kampfbericht v1:

- jeder Kampf erzeugt einen Gegnernamen und eine Umgebung
- drei lokale Runden werden simuliert
- jede Runde zeigt Attacke, Effekt, Schaden und Gegenschlag
- Ergebnis veraendert XP, Energie, Stress, Stimmung, Bindung sowie Siege/Niederlagen

Aktiver Kampf v1:

- Arena oder Freikampf startet einen persistenten lokalen Battle-State
- Battle-State speichert Gegner, Ort, Rating, Belohnung, Runde, Drache-HP, Gegner-HP, Fokus und Gegnerabsicht
- der Spieler waehlt pro Runde eine von vier Element-Attacken
- Gegnerabsichten sind sichtbar: Schwerer Angriff, Deckung, Offen, Schneller Schlag
- Schild, Konter, Fokus- und Schaden-Attacken reagieren unterschiedlich auf diese Absichten
- Rueckzug beendet den Kampf mit kleinem Energie-/Stresspreis, aber ohne Niederlage

## Training

Trainingsarten:

- Krafttraining
- Ausdauertraining
- Flugtraining
- Fokusmeditation
- Instinkttraining
- Reaktionstraining
- Arena-Sparring
- Elementtraining

Jede Trainingseinheit braucht:

- sichtbaren Effekt
- Energieverbrauch
- XP oder Stat-Anstieg
- Belastung oder Cooldown
- Chance auf Freischaltung spaeter

## UI-Struktur

Hauptscreen:

- Dragon/Habitat gross sichtbar
- Werte und Balken oben oder direkt im Visual
- Hauptaktionen: Zuhause, Training, Arena, Entwicklung
- Sekundaer: Freikampf, Codex, Nexi-Link

Regel:

- keine ueberladene Buttonwand
- jede Aktion muss sichtbaren Effekt haben
- mobil zuerst

## Phasen

### Phase 1

Lokaler Dragon-Core-MVP mit Visual, Touch, Training, Arena, Freikampf, Evolution und Codex.

### Phase 2

Mehr Attacken, Habitate, Gegner, Status-Effekte und sichtbare Evolutionen.

### Phase 3

Kontrollierte Nexi-Anbindung fuer Fokus, Tagesziele und Briefing-Reflexion.

### Phase 4

Boss-Arenen, Story, seltene Formen, Ghost-Battles und saisonale Inhalte.
