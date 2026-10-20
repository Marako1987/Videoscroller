package com.example.tiktokcontroller

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent

/**
 * State-Machine zur Auswertung von Kopfhörer-Tastenklicks:
 * - Erkennt Einzeldruck (1x), Doppeldruck (2x), Dreifachdruck (3x)
 * - Erkennt Halten/Long-Press mit Start- und End-Events
 */
class HeadsetGestureDetector(
    private val listener: OnHeadsetActionListener,
    private val multiClickWindowMs: Long = 300L,   // Zeitspanne für Folgeklicks
    private val longPressThresholdMs: Long = 500L  // Ab wann gilt es als gehalten
) {

    interface OnHeadsetActionListener {
        fun onSingleClick()
        fun onDoubleClick()
        fun onTripleClick()
        fun onLongPressStart()
        fun onLongPressEnd()
    }

    private val handler = Handler(Looper.getMainLooper())
    private var clickCount = 0
    private var isHolding = false
    private var isDown = false

    // Runnable für das Ende des Mehrfachklick-Zeitfensters
    private val clickEvaluationRunnable = Runnable {
        when (clickCount) {
            1 -> listener.onSingleClick()
            2 -> listener.onDoubleClick()
            3 -> listener.onTripleClick()
            else -> if (clickCount > 3) listener.onTripleClick()
        }
        clickCount = 0
    }

    // Runnable für Long-Press-Erkennung
    private val longPressRunnable = Runnable {
        if (isDown) {
            isHolding = true
            // Klickzähler zurücksetzen, da es ein Halten war
            clickCount = 0
            handler.removeCallbacks(clickEvaluationRunnable)
            listener.onLongPressStart()
        }
    }

    /**
     * Verarbeitet KeyEvent.ACTION_DOWN und ACTION_UP
     */
    fun onKeyEvent(event: KeyEvent): Boolean {
        when (event.action) {
            KeyEvent.ACTION_DOWN -> {
                // Manche Kopfhörer feuern wiederholte DOWN-Events bei dauerhaftem Drücken
                if (event.repeatCount == 0) {
                    isDown = true
                    // Timer für langes Drücken starten
                    handler.postDelayed(longPressRunnable, longPressThresholdMs)
                }
                return true
            }

            KeyEvent.ACTION_UP -> {
                isDown = false
                handler.removeCallbacks(longPressRunnable)

                if (isHolding) {
                    // Halten wurde beendet
                    isHolding = false
                    listener.onLongPressEnd()
                    return true
                }

                // Normaler kurzer Klick: Zähler inkrementieren
                clickCount++
                handler.removeCallbacks(clickEvaluationRunnable)

                if (clickCount >= 3) {
                    // Sofort 3x ausführen, kein weiteres Warten nötig
                    handler.post(clickEvaluationRunnable)
                } else {
                    // Wartezeit einräumen, ob noch ein 2. oder 3. Klick folgt
                    handler.postDelayed(clickEvaluationRunnable, multiClickWindowMs)
                }
                return true
            }
        }
        return false
    }

    fun reset() {
        handler.removeCallbacksAndMessages(null)
        clickCount = 0
        isHolding = false
        isDown = false
    }
}
