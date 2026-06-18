# Digi Dragon Core v0.1

Digi Dragon ist ein separates lokales Spiel-/Begleitmodul. Es ist nicht der Nexus-Kern und nicht Nexi.

## Aktiver Stand

- Die aktive Digi-Dragon-Funktion liegt aktuell in der Android-App.
- Status, Habitat-Visual, Touch-Interaktion, Training, Arena, Ruhe, Tagesreset und Entwicklung laufen lokal in der App.
- Der erste echte lokale Spielkern ist aktiv: Kraft-, Ausdauer-, Flug-, Fokus- und Instinkttraining veraendern gespeicherte Werte, verbrauchen Energie, erzeugen Stress und geben XP.
- Pflege und Ruhe wirken unterschiedlich: Pflege staerkt Bindung/Stimmung und gibt minimal XP, Ruhe regeneriert ohne XP-Gewinn.
- Arena und Freikampf berechnen lokal Gegnerwert gegen Drachenwert, speichern Siege/Niederlagen und zeigen das Ergebnis sofort an.
- Arena und Freikampf nutzen jetzt elementbasierte Attacken, Gegnernamen und einen kurzen Rundenbericht mit Schaden, Gegenschlag, XP und Ergebnis.
- Battle-State v1 ist aktiv: Ein Kampf bleibt lokal gespeichert, der Gegner zeigt seine Absicht, und der Nutzer waehlt pro Runde eine Attacke oder Rueckzug.
- XP-Schwellen schalten die sichtbaren Entwicklungsstufen automatisch frei.
- Nexi darf optional gefragt werden, aber Digi Dragon mutiert Nexi nicht direkt.
- Eine externe Bridge auf Port 8777 ist optional und derzeit keine Produktionsvoraussetzung.
- Die verbindliche Zielrichtung liegt in `docs/dragon/DRAGON_CORE_GDD.md`.

## Rollenreinheit

- Nexus = Geruest und App-/Backend-Struktur.
- Nexi = einziges Hirn fuer Kontext, Memory, Recall, Zeitstrahl und Entscheidungen.
- Digi Dragon = separate Spielschicht.
- DigiPad = separater Remote-/Family-Client.

## Nicht mehr aktiv

Die frueheren Python-/Tool-Pfade fuer Digi Dragon sind aus dem aktiven Repo entfernt. Alte CLI- und Bridge-Startbefehle sind keine gueltigen Startpfade mehr.

## Gueltige Nutzung

1. Nexus Android-App oeffnen.
2. `Digi Dragon` oeffnen.
3. Dragon/Habitat antippen oder lokale Aktionen direkt in der App nutzen.
4. Training, Arena, Freikampf, Entwicklung und Codex lokal verwenden.
5. Nexi nur bewusst ueber `Nexi fragen` einbeziehen.

## Sicherheitsgrenze

Digi Dragon bleibt getrennt. Keine automatische Nexi-Schreibaktion, keine automatische API-Kostenaktion und kein Zugriff auf private Dateien ohne explizite Nexi-/Dateien-Funktion.
