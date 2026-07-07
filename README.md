# Aviator Chrono

A Wear OS chronograph app styled after the **Breitling EVO Aerospace**, with authentic analog hands and dual amber 7-segment LCD panels. Built for Galaxy Watch 4 and later (any Wear OS 3+ device).

<p align="center">
  <img src="watch_face_clean_blue.png" width="280" alt="Aviator Chrono watch face"/>
</p>

---

## Features

- **Breitling EVO Aerospace dial** — authentic PNG background with bezel, Arabic numerals, and wing badge
- **Analog hands** — hour, minute, and orange second/chrono-sweep hands drawn over the dial
- **Upper LCD panel** — shows date (`MON 07 JUL`) at rest; switches to `CHR` or `CHR 1/100` mode label when the chronograph is active
- **Lower LCD panel** — shows UTC time (`HH:MM:SS`) at rest; switches to chronograph readout when timing
- **Two chrono precision modes**
  - `SEC` — `HH:MM:SS` (default, ideal for navigation timing)
  - `1/100` — `MM:SS.cc` (hundredths of a second)
- **Five LCD colours** — tap the lower panel to cycle: Amber → Green → Red → Blue → Yellow
- **Screen-awake lock** — holds the display on automatically while the chronograph is running in SEC mode (same `FLAG_KEEP_SCREEN_ON` mechanism used by workout apps)
- **No phone required** — standalone app, installs directly on the watch

---

## Usage

| Gesture | Action |
|---|---|
| Tap the dial | Start / stop the chronograph |
| Double-tap the dial while stopped | Reset to `00:00:00` |
| Tap the **upper LCD** | Toggle precision: `SEC` ↔ `1/100` |
| Tap the **lower LCD** | Cycle LCD colour (Amber → Green → Red → Blue → Yellow) |

### Display logic

**Upper LCD**

| State | Shows |
|---|---|
| Chrono at zero | `MON 07 JUL` (date) |
| Chrono running or paused (SEC mode) | `CHR` |
| Chrono running or paused (1/100 mode) | `CHR 1/100` |

**Lower LCD**

| State | Shows |
|---|---|
| Chrono at zero | UTC time — `14:32:07` |
| Chrono active, SEC mode | Elapsed — `00:01:23` |
| Chrono active, 1/100 mode | Elapsed — `01:23.45` |

---

## Building

### Requirements

- **Android Studio Hedgehog** or later (bundled JDK 17+ required)
- Wear OS emulator **or** a physical Galaxy Watch 4 / 5 / 6 / 7 (or any Wear OS 3+ device)

### Steps

1. Clone the repo:
   ```
   git clone https://github.com/Anqv/AviatorChronoApp.git
   ```
2. Open the `AviatorChronoApp` folder in Android Studio. Gradle syncs automatically.
3. Set up a target:
   - **Emulator**: Tools → Device Manager → Create Device → Wear OS → *Large Round*
   - **Physical watch**: enable Developer Options (tap build number 7×), enable ADB over Wi-Fi, then `adb connect <watch-ip>:5555`
4. Press **Run ▶** and select your target.

### Command-line build

```bash
# On Windows, point to Android Studio's bundled JDK:
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew assembleDebug

# Install directly to a connected watch:
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew installDebug
```

The APK ends up at `app/build/outputs/apk/debug/app-debug.apk`.

---

## Project layout

```
AviatorChronoApp/
├── watch_face_clean_blue.png          Breitling dial artwork (background)
└── app/src/main/
    ├── AndroidManifest.xml            Standalone Wear app, no permissions
    ├── res/drawable/
    │   └── watch_face_clean_blue.png  Runtime copy of the dial image
    └── java/com/aviatorchrono/app/
        ├── MainActivity.kt            Compose UI, screen-lock wiring, LCD layout
        ├── ChronoState.kt             Start/stop/reset state machine + LCD colour enum
        ├── AviatorDial.kt             Analog hands Canvas composable + colour tokens
        └── SevenSegmentDisplay.kt     7-segment digit renderer (auto-fits to panel size)
```

---

## Why an app instead of a watch face

Wear OS's Watch Face Format (WFF) is declarative XML — there's no room for a real start/stop/reset state machine or a screen-lock override. A foreground **app** can hold the screen on via `FLAG_KEEP_SCREEN_ON` for as long as it stays in the foreground, which is exactly what navigation timing needs. The app appears in the watch's app list and launches like any other app.

---

## Notes

- `minSdk = 30` — targets Wear OS 3+, covering Watch 4 and every model since. No Samsung-specific APIs are used; it runs on Pixel Watch and other Wear OS 3+ devices too.
- The screen-awake lock is released immediately when the app is backgrounded (`onPause`) or the lock preference is set to OFF — it never holds the screen on unintentionally.
- The launcher icon (`mipmap-*/ic_launcher.png`) is placeholder art — swap in final artwork before publishing.
