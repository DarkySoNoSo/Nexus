# Digi Dragon Core v0.1

Digi Dragon ist ein separates lokales Spiel-/Begleitmodul. Es ist nicht der Nexus-Kern und nicht Nexi.

## Aktiver Stand

- Die aktive Digi-Dragon-Funktion liegt aktuell in der Android-App.
- Status, Training, Arena, Ruhe, Tagesreset und Entwicklung laufen lokal in der App.
- Nexi darf optional gefragt werden, aber Digi Dragon mutiert Nexi nicht direkt.
- Eine externe Bridge auf Port 8777 ist optional und derzeit keine Produktionsvoraussetzung.

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
3. Lokale Aktionen direkt in der App nutzen.
4. Nexi nur bewusst ueber `Nexi fragen` einbeziehen.

## Sicherheitsgrenze

Digi Dragon bleibt getrennt. Keine automatische Nexi-Schreibaktion, keine automatische API-Kostenaktion und kein Zugriff auf private Dateien ohne explizite Nexi-/Dateien-Funktion.
