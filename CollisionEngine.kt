Im Folgenden findest du ein kompaktes Beispiel für einen AABB-(Axis Aligned Bounding Box)-Intersect-Check unter Verwendung von Filament-Vektoren (Float3). Der Code ist so gehalten, dass innerhalb der eigentlichen Kollisionsfunktion keine neuen Objekte (und somit kein zusätzlicher Heap-Speicher) angelegt werden. Alles läuft über reine Parameterübergabe und einfache Vergleiche:

--------------------------------------------------------------------------------
package com.example.aabb

import com.google.android.filament.math.Float3

/**
 * Diese Utility-Klasse stellt eine Funktion zur Verfügung, um die Überschneidung
 * zweier AABBs (Axis Aligned Bounding Boxes) zu prüfen.
 *
 * Der Code verzichtet innerhalb der Kollisionsfunktion vollständig auf Heap-Allokation.
 */
object AABBUtils {

    /**
     * Prüft, ob sich zwei Axis Aligned Bounding Boxes (AABB) schneiden. Beide AABBs werden
     * durch ihre minimalen und maximalen Eckpunkte beschrieben. Es werden ausschließlich
     * Vergleichsoperatoren verwendet und keine neuen Objekte erzeugt.
     *
     * @param minA Minimaler Punkt von AABB A (z. B. linke-vordere-untere Ecke).
     * @param maxA Maximaler Punkt von AABB A (z. B. rechte-hintere-obere Ecke).
     * @param minB Minimaler Punkt von AABB B (z. B. linke-vordere-untere Ecke).
     * @param maxB Maximaler Punkt von AABB B (z. B. rechte-hintere-obere Ecke).
     * @return true, wenn eine Überschneidung vorliegt, sonst false.
     */
    fun intersectsAABB(
        minA: Float3, maxA: Float3,
        minB: Float3, maxB: Float3
    ): Boolean {
        // Wenn eine der folgenden Abfragen zutrifft, liegt keine Überschneidung vor
        if (minA.x > maxB.x) return false
        if (maxA.x < minB.x) return false

        if (minA.y > maxB.y) return false
        if (maxA.y < minB.y) return false

        if (minA.z > maxB.z) return false
        if (maxA.z < minB.z) return false

        // Wenn keiner der Achsen getrennt ist, überlappen sich die Boxen
        return true
    }
}
--------------------------------------------------------------------------------

Erläuterung:

1. Parameterübergabe ohne zusätzliche Allokation:  
   Die Float3-Objekte selbst werden zwar (in der Regel) einmal angelegt, aber nicht innerhalb der Kollisionsfunktion. Diese nimmt nur Verweise (Referenzen) zur Laufzeit entgegen. Damit entsteht während des Aufrufs keine zusätzliche Heap-Allokation.

2. Schneller Early-Out:  
   Sobald sich herausstellt, dass eine der Boxen entlang einer Achse "links" oder "unterhalb" der anderen Box liegt, wird das Ergebnis sofort auf false gesetzt und die Funktion abgebrochen. So wird unnötige Rechenzeit gespart.

3. Keine zusätzlichen Datenstrukturen:  
   Es werden keine temporären Listen, Arrays oder sonstigen Objekte angelegt. Alles erfolgt rein über die vorhandenen Float3-Eingaben und Vergleiche.

Auf diese Weise ist die Funktion intersectsAABB für einen schnellen und speicherschonenden AABB-Kollisionscheck geeignet.
