# Mobile UX Rework Notes

Status: Arbeitsrichtung
Datum: 2026-06-18

## Problem

Die Android-App darf nicht wie eine technische Button-Sammlung wirken. Die Zentrale muss erklaeren, welcher Bereich wofuer da ist, und Unterseiten muessen eine erkennbare Aufgabe haben.

## Zentrale

Ziel: Arbeits-Cockpit statt Buttonwand, mit links sichtbarer Navigationsschiene und rechts klarer Arbeitsflaeche.

Module:

- Nexi: direkter Kanal fuer Auftrag und Kontext
- Nachrichten: Eingang, Suche, Entscheidungen
- Zeitstrahl: Chronik und Entscheidungen
- Dateien: sicherer Explorer
- Digi Dragon: lokale Spielschicht
- DigiPad: separater iPhone-Web-/PWA-Client
- Systemstatus: Rechte, Verbindung, Outbox

Regeln:

- Keine permanente Kopf-Taste oben.
- Hauptmenue nutzt eine linke Rail fuer Kernbereiche.
- Die Rail ist als Slide gedacht: schmaler Griff im Ruhezustand, volle Navigation nach Antippen.
- Rechte Flaeche zeigt kompakte Zielkarten und Schnellaktionen.
- Kopfmarke steht gross, zentriert und in der aktiven Akzentfarbe.
- Weitere Akzente: Neon gruen und OLED schwarz.
- Unterseiten haben genau einen Rueckweg zur Zentrale.
- System- und Designfunktionen bleiben unten.
- Technische Bridge-Details gehoeren nicht auf die erste Sicht, ausser die Seite ist ein Systembereich.

## Dragon Core

Ziel: nicht eine lange technische Seite, sondern ein lokaler Begleiter mit klaren Modi.

Modi:

- Zuhause: Drache, Zustand, Pflege, Ruhe, schneller Trainingseinstieg
- Entwicklung: Evolutionswand, Elementpfade, Codex
- Kampf: Training, Arena, Freikampf, Pflege
- System: Bridge, bewusster Nexi-Link, Sicherheitsgrenze

Regeln:

- Dragon bleibt lokal/offline.
- Keine automatischen Nexi- oder API-Calls.
- System-Bridge ist optional und sichtbar getrennt.
- Evolution muss wie ein echter Pfad wirken, nicht wie ein einzelnes Statusfeld.
- Dragon-Visuals brauchen Umgebung: jedes Element bekommt eine erkennbare Habitat-Stimmung.
- Drachenformen muessen silhouette-stark sein: Ei, Jungform, Adult, Ahnform mit Hoernern, Fluegeln, Schwanz, Podest und Licht/Schatten.
- Hauptfigur darf nie wie Vogel, Fisch oder Blob wirken: erkennbar sind Hals, Kopf, Hoerner, Maul, Membranfluegel, Beine/Klauen und Schwanz.

## Naechste Verbesserungen

- Zentrale: echte Kennzahlen pro Modul laden, aber nur auf Knopfdruck oder nach Nutzeraktion.
- Nexi: Chatverlauf als einzelne Karten statt grosser Textblock.
- Zeitstrahl: Filter fuer Fokus, Entscheidung, System und erledigt.
- Dateien: Ordner oben, Dateien unten, langer Pfad einklappbar.
- Dragon: Habitat als eigener Screen, Attacken-Auswahl, Kampfrundenlog, Inventar, sichtbare Evolutionsbedingungen.
