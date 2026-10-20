# TikTok Scroller (Android App)

Offizielle Android-App zur Steuerung von TikTok mit Kopfhörertasten.

## 🚀 Wie erstelle ich die APK-Datei?

### Methode 1: In Android Studio (Empfohlen, dauert 1 Minute)
1. Installiere das kostenlose **Android Studio** (falls noch nicht vorhanden).
2. Klicke auf **File > Open...** und wähle diesen entpackten Projektordner aus.
3. Warte kurz, bis Gradle die Abhängigkeiten geladen hat.
4. Klicke oben im Menü auf:
   **Build > Build Bundle(s) / APK(s) > Build APK(s)**
5. Unten rechts erscheint nach wenigen Sekunden eine Benachrichtigung: **APK(s) generated successfully**.
6. Klicke auf den blauen Link **"locate"** – deine fertige `app-debug.apk` liegt direkt im Ordner `app/build/outputs/apk/debug/`!

### Methode 2: Direkt per USB auf dein Handy installieren (Noch schneller!)
1. Verbinde dein Android-Smartphone per USB-Kabel mit dem PC.
2. Aktiviere auf dem Handy: *Einstellungen > Entwickleroptionen > USB-Debugging*.
3. Wähle oben in Android Studio dein Smartphone aus.
4. Klicke auf den **grünen Play-Button (Run)**.
5. Die App wird automatisch kompiliert, auf dein Handy übertragen und sofort gestartet!

### Methode 3: In der Cloud mit GitHub Actions (Ohne Android Studio auf dem PC)
1. Erstelle ein neues, kostenloses Repository auf **GitHub**.
2. Lade den Inhalt dieses Ordners dort hoch.
3. Klicke auf den Tab **Actions** auf GitHub.
4. Der vorbereitete Workflow `Build TikTok Scroller APK` startet automatisch.
5. Sobald der Haken grün ist, kannst du die fertige `TikTok-Scroller-app-debug.apk` direkt unter **Artifacts** im Browser herunterladen!

## 🎧 Kopfhörer-Steuerung in TikTok
- **1x Drücken**: Video pausieren / weiterspielen (Tap Bildschirmmitte)
- **Gedrückt halten**: 2x Vorspulen (Fast Forward am Bildschirmrand)
- **2x Drücken**: Nächstes TikTok (Swipe-Up von unten nach oben)
- **3x Drücken**: Aktuelles Video zurück auf 0:00 setzen

## 🛡️ Passthrough-Modus (Spotify / YouTube)
Wenn du TikTok verlässt oder minimierst (z.B. in Spotify oder auf dem Startbildschirm bist), schaltet der Hintergrunddienst automatisch ab und deine Kopfhörertasten steuern ganz normal deine Musik weiter!
