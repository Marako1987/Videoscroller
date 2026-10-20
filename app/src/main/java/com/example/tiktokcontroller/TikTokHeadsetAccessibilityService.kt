package com.example.tiktokcontroller

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat

/**
 * TikTok Headphone Controller Service
 *
 * Nutzt Android's AccessibilityService API (API 24+) um:
 * 1. Hardware-Kopfhörer-Tasten (Media Hook / PlayPause) abzufangen
 * 2. Zu prüfen, ob TikTok aktiv im Vordergrund ist
 * 3. Bei 1x Klick: Video pausieren / starten (Tap Screen Mitte)
 * 4. Bei Langem Drücken: 2x Vorspulen (Hold auf Displayrand wie in TikTok)
 * 5. Bei 2x Klick: Nächstes Video (Swipe-Up Geste von unten nach oben)
 * 6. Bei 3x Klick: Video neustarten (Seek auf 0:00 oder Swipe-Down/Up)
 */
class TikTokHeadsetAccessibilityService : AccessibilityService(), HeadsetGestureDetector.OnHeadsetActionListener {

    companion object {
        private const val TAG = "TikTokHeadsetService"
        private const val NOTIFICATION_CHANNEL_ID = "tiktok_controller_fg"
        private const val NOTIFICATION_ID = 1001

        // Bekannte TikTok Package-Namen (Global & Regional)
        private val TIKTOK_PACKAGES = setOf(
            "com.zhiliaoapp.musically",      // TikTok International
            "com.ss.android.ugc.trill",      // TikTok Asia / Alternativ
            "com.ss.android.ugc.aweme"       // Douyin (chinesische Version)
        )
    }

    private lateinit var gestureDetector: HeadsetGestureDetector
    private var isTikTokForeground: Boolean = false
    private var screenWidth: Int = 1080
    private var screenHeight: Int = 2400
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isFastForwardActive: Boolean = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service gestartet")
        
        // Bildschirmmaße für dynamische Gesten-Berechnung ermitteln
        val metrics: DisplayMetrics = resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        // Detector für Klickmuster (1x, 2x, 3x, Long-Press) initialisieren
        gestureDetector = HeadsetGestureDetector(this)

        // Als Foreground Service deklarieren, damit Android den Service nicht beendet
        startForegroundNotification()
    }

    /**
     * Prüft Ereignisse des Fenster-Wechsels, um festzustellen, ob TikTok aktiv ist.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            isTikTokForeground = TIKTOK_PACKAGES.contains(packageName)
            Log.d(TAG, "Aktives Fenster geändert: $packageName (TikTok aktiv: $isTikTokForeground)")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Accessibility Service unterbrochen")
    }

    /**
     * ENTSCHEIDEND: Fängt Tasten-Events (Headset-Hook, Media-Tasten) vor dem System ab!
     * Erfordert android:canRequestFilterKeyEvents="true" in accessibility_service_config.xml
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        // Nur abfangen, wenn TikTok im Vordergrund ist!
        // Sonst sollen Kopfhörer normal für Spotify, Podcasts etc. funktionieren.
        if (!isTikTokForeground) {
            return super.onKeyEvent(event)
        }

        val keyCode = event.keyCode
        val isHeadsetKey = keyCode == KeyEvent.KEYCODE_HEADSETHOOK ||
                           keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
                           keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ||
                           keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE

        if (isHeadsetKey) {
            // An unsere Klick-State-Machine weitergeben
            val handled = gestureDetector.onKeyEvent(event)
            if (handled) {
                // Event konsumieren -> System/Musik-Player reagiert nicht doppelt
                return true
            }
        }

        return super.onKeyEvent(event)
    }

    // ==========================================
    // AKTIONEN & GESTEN-AUSFÜHRUNG
    // ==========================================

    /**
     * 1x Drücken: TikTok pausieren oder fortsetzen
     * In TikTok pausiert/startet ein einfacher Tap in der Bildschirmmitte das Video.
     */
    override fun onSingleClick() {
        Log.i(TAG, "Aktion: 1x Klick -> Play/Pause")
        val centerX = (screenWidth / 2).toFloat()
        val centerY = (screenHeight / 2).toFloat()
        dispatchTap(centerX, centerY, durationMs = 50)
    }

    /**
     * 2x Drücken: Zum nächsten TikTok wischen (Swipe-Up)
     * Zieht den Bildschirm von 80% Höhe auf 20% Höhe nach oben.
     */
    override fun onDoubleClick() {
        Log.i(TAG, "Aktion: 2x Klick -> Nächstes TikTok (Swipe Up)")
        val startX = (screenWidth / 2).toFloat()
        val startY = (screenHeight * 0.78f)
        val endY = (screenHeight * 0.22f)
        dispatchSwipe(startX, startY, startX, endY, durationMs = 180)
    }

    /**
     * 3x Drücken: Aktuelles TikTok von vorne abspielen
     * Tippt entweder ganz links auf die Zeitleiste (0:00) oder wischt kurz zurück und vor.
     */
    override fun onTripleClick() {
        Log.i(TAG, "Aktion: 3x Klick -> Video von vorne abspielen")
        // Variante A: Swipe kurz nach unten und direkt wieder nach oben
        val centerX = (screenWidth / 2).toFloat()
        val topY = (screenHeight * 0.25f)
        val bottomY = (screenHeight * 0.75f)

        // 1. Swipe nach unten (vorheriges Video)
        dispatchSwipe(centerX, topY, centerX, bottomY, durationMs = 120)

        // 2. Nach 150ms direkt wieder vorwärts wischen -> landet bei 0:00 des Videos
        mainHandler.postDelayed({
            dispatchSwipe(centerX, bottomY, centerX, topY, durationMs = 120)
        }, 180)
    }

    /**
     * Lange Drücken Start: TikTok 2x Vorspulen aktivieren
     * TikTok hat ein natives Feature: Drücken & Halten auf den linken/rechten Displayrand
     * aktiviert den 2-fachen Vorspulmodus (2x Speed)!
     */
    override fun onLongPressStart() {
        Log.i(TAG, "Aktion: Lange gedrückt -> 2x Vorspulen startet")
        isFastForwardActive = true
        // Drückt rechts am Bildschirmrand kontinuierlich
        val rightEdgeX = (screenWidth * 0.88f)
        val centerY = (screenHeight * 0.50f)
        dispatchContinuousHold(rightEdgeX, centerY)
    }

    /**
     * Lange Drücken Ende: Taste losgelassen -> 2x Vorspulen beenden
     */
    override fun onLongPressEnd() {
        Log.i(TAG, "Aktion: Losgelassen -> 2x Vorspulen beendet")
        isFastForwardActive = false
    }

    // ==========================================
    // SYNTHETISCHE ACCESSIBILITY-GESTEN
    // ==========================================

    private fun dispatchTap(x: Float, y: Float, durationMs: Long = 50) {
        val path = Path().apply {
            moveTo(x, y)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun dispatchSwipe(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long) {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        dispatchGesture(gesture, null, null)
    }

    private fun dispatchContinuousHold(x: Float, y: Float) {
        // Kontinuierliche Beruehrung ueber StrokeDescription mit willContinue = true
        val path = Path().apply {
            moveTo(x, y)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val stroke = GestureDescription.StrokeDescription(path, 0, 1000, true)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (isFastForwardActive) {
                        // Solange die Taste gehalten wird, weiter halten
                        dispatchContinuousHold(x, y)
                    }
                }
            }, null)
        } else {
            val stroke = GestureDescription.StrokeDescription(path, 0, 1000)
            val gesture = GestureDescription.Builder().addStroke(stroke).build()
            dispatchGesture(gesture, null, null)
        }
    }

    private fun startForegroundNotification() {
        try {
            val channelId = NOTIFICATION_CHANNEL_ID
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "TikTok Kopfhoerer Steuerung",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Haelt den Kopfhoerer-Listener im Hintergrund aktiv"
                }
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }

            val notification: Notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("TikTok Headphone Controller")
                .setContentText("Aktiv: Kopfhoerertasten steuern TikTok")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Foreground Notification konnte nicht gestartet werden: ${e.message}")
        }
    }
}
