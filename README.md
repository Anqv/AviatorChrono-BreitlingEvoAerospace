# Aviator Chrono — Wear OS App (Galaxy Watch4+)

Standalone Wear OS app (not a watch face) — same design as the Tizen version,
rebuilt to keep the full chronograph logic and the screen-awake lock, which
Wear OS's Watch Face Format doesn't allow.

## Why an app instead of a watch face
Watch Face Format (WFF), required for installing watch faces on Wear OS,
is declarative XML with no room for a real start/stop/reset state machine
or a screen-lock override. A foreground **app**, on the other hand, is
allowed to hold the screen on via `FLAG_KEEP_SCREEN_ON` for as long as
it's in the foreground — the same mechanism workout and timer apps use.
That's what this project does.

## Project layout
```
AviatorChronoApp/
├── settings.gradle.kts
├── build.gradle.kts
└── app/
    ├── build.gradle.kts          Wear Compose dependencies, minSdk 30
    └── src/main/
        ├── AndroidManifest.xml    Standalone Wear app declaration
        ├── java/com/aviatorchrono/app/
        │   ├── MainActivity.kt     Compose UI + screen-lock wiring
        │   ├── ChronoState.kt       Start/stop/reset/precision state machine
        │   └── AviatorDial.kt        Canvas-drawn analog dial
        └── res/
            ├── mipmap-hdpi/ic_launcher.png   (placeholder icon)
            ├── mipmap-xhdpi/ic_launcher.png
            └── values/strings.xml
```

## How to open and run it

1. **Install Android Studio** (`developer.android.com/studio`).
2. **File → Open**, select the `AviatorChronoApp` folder. Let Gradle sync —
   it'll pull the Compose-for-Wear and AndroidX dependencies automatically.
3. **Get a Wear OS target**:
   - **Emulator**: Tools → Device Manager → Create Device → Wear OS
     category → pick a round profile (Large Round matches Watch4/5/6/7).
   - **Physical watch**: on the watch, tap the build number under
     **Settings → About watch → Software** a few times to unlock
     Developer options, then enable **ADB debugging** + **Debug over Wi-Fi**.
     Connect via `adb connect <watch-ip>:5555` (same network as your PC).
4. **Run ▶** in Android Studio, pick your target. No phone pairing or
   companion app is required — `com.google.android.wearable.standalone`
   in the manifest marks it installable directly on the watch.

## Using it
- **Tap the dial** → start/stop the chronograph. **Tap again quickly while
  stopped** → reset to zero.
- **Tap the "SEC"/"1/100" label** (top right) → toggle chronograph
  precision. Defaults to seconds-only for navigation timing.
- **Tap "SCREEN"** (bottom) → manually override the awake-lock at any time:
  - **AUTO** — lock engages automatically once navigation starts
  - **ON** — currently locked, full brightness
  - **OFF** — manually released; stays off regardless of chrono state
- The screen-awake lock only ever holds while this app is in the
  foreground and actively timing in seconds mode — leaving the app or
  switching to hundredths releases it immediately.

## Notes
- `ic_launcher.png` is placeholder art reused from the Tizen version —
  swap it for final artwork, and consider adding a proper adaptive icon
  (`mipmap-anydpi-v26`) before publishing.
- `minSdk = 30` targets Wear OS 3+, which covers Watch4 and every model
  since. Nothing here depends on Samsung-specific APIs — this will also
  run on Pixel Watch or any other Wear OS 3+ device.
- This is a full app, so it appears in the app list and can be launched
  like any other — it won't sit as your background watch face. If you
  ever want a *complication* on your actual watch face that reflects the
  chrono's running state, that's a separate, smaller piece of work using
  the Wear OS Complications API — let me know if you want that added.
