# Dragon Image Asset Plan

Status: Vorbereitung
Datum: 2026-06-18

## Ziel

Sobald echte Drachenbilder vorliegen, sollen sie die gezeichneten Platzhalter ersetzen, ohne Dragon-Core-Logik, Safe-Start oder Nexi-Rollen zu vermischen.

## Namensschema

Empfohlenes Muster:

`dragon_<element>_<stage>.png`

Elemente:

- wasser
- erde
- feuer
- luft
- schatten

Stufen:

- ei
- jung
- adult
- ahn

Beispiele:

- `dragon_feuer_ei.png`
- `dragon_feuer_jung.png`
- `dragon_feuer_adult.png`
- `dragon_feuer_ahn.png`

## Anforderungen

- Transparenter Hintergrund.
- Gleiche Blickrichtung je Elementpfad, bevorzugt leicht nach rechts.
- Genug Rand fuer Fluegel, Hoerner und Schwanz.
- Keine Textlabels im Bild.
- Kein eingebauter Hintergrund, weil Habitat separat in der App gezeichnet wird.

## Einbindung

Bis echte Bilder vorhanden sind, zeichnet Android die Drachen per Canvas.
Wenn Bilder vorhanden sind:

1. Assets nach `05_APP_ANDROID_NATIVE/app/src/main/res/drawable/` legen.
2. Auswahl erfolgt ueber Element und Stufe.
3. Canvas-Habitat bleibt aktiv.
4. Bild ersetzt nur die Drachenfigur, nicht Werte, Aktionen oder Habitat.

## Bereits eingebaut

Wasserlinie:

- `dragon_wasser_ei.jpg`
- `dragon_wasser_jung.jpg`
- `dragon_wasser_adult.jpg`
- `dragon_wasser_ahn.jpg`
- `dragon_wasser_habitat.jpg`
- `dragon_wasser_training.jpg`
- `dragon_wasser_arena.jpg`

Erdlinie:

- `dragon_erde_ei.jpg`
- `dragon_erde_jung.jpg`
- `dragon_erde_adult.jpg`
- `dragon_erde_ahn.jpg`
- `dragon_erde_habitat.jpg`
- `dragon_erde_training.jpg`
- `dragon_erde_arena.jpg`

Feuerlinie:

- `dragon_feuer_ei.jpg`
- `dragon_feuer_jung.jpg`
- `dragon_feuer_adult.jpg`
- `dragon_feuer_ahn.jpg`
- `dragon_feuer_habitat.jpg`
- `dragon_feuer_training.jpg`
- `dragon_feuer_arena.jpg`

Luftlinie:

- `dragon_luft_ei.jpg`
- `dragon_luft_jung.jpg`
- `dragon_luft_adult.jpg`
- `dragon_luft_ahn.jpg`
- `dragon_luft_habitat.jpg`
- `dragon_luft_training.jpg`
- `dragon_luft_arena.jpg`

Schattenlinie:

- `dragon_schatten_ei.jpg`
- `dragon_schatten_jung.jpg`
- `dragon_schatten_adult.jpg`
- `dragon_schatten_ahn.jpg`
- `dragon_schatten_habitat.jpg`
- `dragon_schatten_training.jpg`
- `dragon_schatten_arena.jpg`

Diese Bilder werden in der Android-App genutzt, wenn `Element: Wasser`, `Element: Erde`, `Element: Feuer`, `Element: Luft` oder `Element: Schatten` aktiv ist.

## Visuelle Regel

Ein Drache muss als Drache lesbar sein:

- Kopf
- Hoerner
- Hals
- Membranfluegel
- Koerper
- Beine/Klauen
- Schwanz
- klare Silhouette
