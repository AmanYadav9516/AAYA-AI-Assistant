# 🌟 AAYA — The Next-Gen Android Voice & Lifestyle AI Assistant

<p align="center">
  <img src="aaya_logo.png" width="220" alt="AAYA Logo" style="border-radius: 50%; box-shadow: 0 0 30px rgba(0, 229, 255, 0.4);" />
</p>

<p align="center">
  <b>A Siri-grade, privacy-first, context-aware AI assistant built natively for Android.</b><br/>
  <i>Engineered with Kotlin, Jetpack Compose, Gemini Tool Calling, Multilingual Contact Matching & Lifestyle Automation.</i>
</p>

---

## ⚡ Key Highlights

* 🎙️ **Streaming Speech Recognition & Barge-In**: Real-time partial transcription with instant voice interruption ("Stop" or changing the command immediately cuts TTS).
* 📳 **Shake to Wake (Screen ON)**: Low-power accelerometer sensor listener that brings up the glowing voice assistant overlay with a quick flick of your phone.
* ⚡ **Ultra-Fast Local Router + Gemini Brain**: Hardware commands (flashlight, volume, DND, app launch) execute in **<50ms** locally; complex conversational logic and scheduling route to **Gemini 1.5 Flash Tool Calling**.
* 👥 **Multilingual Contact Understanding**: Understands Hinglish, Hindi, and English (*"Mummy ko call karo"*, *"Papa ko phone lagao"*, *"Call my brother"*) with alias normalization and confidence levels.
* 🌙 **Lifestyle & Sleep Intelligence**: Learns your bedtime routine, automates Do Not Disturb, keeps alarms safe, and allows VIP contacts to ring.
* 🚨 **VIP Contacts & Emergency Double-Call Rule**: Only priority people can bypass quiet hours. If anyone calls twice in 3 minutes, it triggers an emergency ring.
* 🛡️ **5-Step Dynamic Permission Flow**: Transparently explains why every permission is needed and guarantees **zero data selling or 3rd-party sharing**. Shows `❌ Missing` ➔ `✅ Enabled` in real-time.
* 🧠 **User-Controlled Memory Dashboard**: View, edit, or delete every habit, relationship, and routine AAYA learns.
* 📦 **Ultra-Lightweight APK**: Only **~18 MB** download size. 100% free and open.

---

## 🏗️ Architecture Pipeline

```
                                  +-----------------------+
                                  |  SHAKE SENSOR (Screen |
                                  |   ON) or WAKE WORD    |
                                  +-----------+-----------+
                                              |
                                              v
+--------------------+            +-----------------------+
| Smart Microphone   | ---------> |  Audio Intelligence   | (VAD / Noise filtering /
| (Foreground Serv.) |            |  Streaming STT A + B  |  Barge-in / Stop voice)
+--------------------+            +-----------+-----------+
                                              |
                                              v
                                  +-----------------------+
                                  |    COMMAND ROUTER     |
                                  +-----+-----------+-----+
                                        |           |
            +---------------------------+           +--------------------------+
            v                                                                  v
   [DEVICE COMMANDS]                                                   [COMPLEX / AI REQUESTS]
   Fast & Local (<50ms)                                                Gemini API + Tool Calling
   - Flashlight, Volume, Settings                                      - "Find brother and message..."
   - DND / Sleep / Class Modes                                         - "My Day" briefing & context
   - Alarms, Timers, App Launch                                        - Smart Sleep & Class schedules
            |                                                                  |
            v                                                                  v
   +-------------------------------------------------------------------------------+
   |                      ANDROID HANDS & ACTION EXECUTOR                          |
   |   - Smart Contact Matcher (Mummy -> Mom, 98% confidence, multilingual)        |
   |   - VIP Call Filter & Pre-approved Template Auto-responder                    |
   |   - Memory Engine (Routines, Sleep, Schedules - 100% editable by user)        |
   +-------------------------------------------------------------------------------+
                                              |
                                              v
                                  +-----------------------+
                                  |  Concise Voice TTS    | ("Done", "Calling Mom...")
                                  +-----------------------+
```

---

## 🚀 Automated 1-Click APK Build (GitHub Actions)

You do **not** need to install heavy Android SDKs on your computer to get the APK. 

This repository includes an automated **CI/CD Cloud Build Pipeline** (`.github/workflows/build-apk.yml`):
1. **Push this repository to GitHub**:
   ```bash
   git remote add origin https://github.com/<YOUR_USERNAME>/<YOUR_REPO>.git
   git push -u origin main
   ```
2. **Download Your APK**:
   * Open your repository on GitHub in your browser.
   * Click on the **Actions** tab at the top.
   * Click on the latest workflow run: **Build AAYA Assistant APK**.
   * Under **Artifacts**, click **`AAYA-AI-Assistant-Debug-APK`** to directly download your compiled `.apk` to your phone!
   * Or go to **Releases** on GitHub to get direct public links.

---

## 🔑 Zero-Cost API Setup & Live Diagnostics

To fix the common issue where "API usage is zero":
1. Open the app and navigate to **Settings (⚙️)**.
2. Enter your Gemini API Key.
3. Tap **"Test Live Connection (Ping)"**.
4. The app pings Google's Gemini servers immediately and displays:
   * 🟢 **Status**: Live Connection Verified (HTTP 200)
   * ⏱️ **Latency**: e.g., 280ms
   * 📊 **Request Counter**: Confirms real-time token traffic.

---

## 📱 Tech Stack
* **Language**: Kotlin 2.0 (100% Native)
* **UI**: Jetpack Compose & Material 3 (Dark Glassmorphism Theme)
* **Database**: Room Database (SQLite offline memory)
* **Sensors**: Android `SensorManager` (Low-power Accelerometer)
* **Networking**: OkHttp 4 + Coroutines + Gemini 1.5 Flash
* **Audio**: Android native `SpeechRecognizer` + `TextToSpeech`
