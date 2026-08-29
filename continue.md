claude --resume "breitling-evo-aerospace-redesign"

---

## 2026-08-29 — Galaxy Watch 6 toolchain + port check

Resume this session with: `claude --resume galaxy-watch6-port` (rename it that if not already).

**Done:**
- Installed Android Studio, JDK, Git, Android SDK (platform 34, build-tools 34.0.0,
  platform-tools, emulator) on this Windows machine.
- Fixed `local.properties` (had a stale `sdk.dir` path from a different machine/user).
- Created Wear OS 5 "Large Round" emulator (`Watch6_WearOS5`) matching the Watch 6 form factor.
- `gradlew assembleDebug` builds clean; app installs/launches/renders correctly on the emulator.
- Code-reviewed for Watch 6 compat (app was originally built targeting Watch 8): all
  layout math is fraction-of-canvas-size based, minSdk 30 covers Watch 6's Wear OS 4/5 —
  no changes needed to port it.

**Known cosmetic issue, not yet fixed:** in NORMAL mode the chrono sweep hand parks at
12 o'clock and visually overlaps/obscures the date digits in the upper LCD (hands are
drawn as the topmost layer over the LCD panels). Not screen-size dependent — same on any
device. User has not yet decided whether to fix it.

**Not done yet / next steps:**
- Pair the physical Galaxy Watch 6 via Wi-Fi ADB debugging and test on real hardware
  (previous watch — a different device — died: got very bright, hot, then permanent white
  screen. That prior install was done the safe standard way, via Android Studio Wi-Fi
  debugging, so it's more likely a hardware/battery defect than something the deploy
  method caused — but test cautiously/supervised the first time regardless: watch that
  ambient/screen-off still kicks in normally, don't leave it running unattended on the
  charger during early tests).
- Decide on and possibly fix the chrono-hand/date-digit overlap above.

---

## 2026-08-29 (later) — Moved project out of OneDrive

Android Studio hit "Unable to delete directory ... Failed to delete some children" on a
rebuild — classic Gradle-on-OneDrive file-lock issue (the project lived under
`OneDrive\Documents\ClaudeWS\WearOS\...`, and OneDrive's sync engine transiently locks
files that Gradle's incremental build needs to rewrite).

**Fix applied:** moved the whole project to
`C:\Users\ander\workspace\WearOS\AviatorChrono-BreitlingEvoAerospace` (outside any
OneDrive-synced folder). Git history/remote tracking intact, confirmed in sync with
`origin/master`. User reopened the project from the new location in Android Studio and
confirmed it works. **All future work should use this new path** — the OneDrive copy's
contents were purged (an empty leftover folder may briefly remain there until OneDrive's
sync engine releases it; harmless, safe to delete manually if it lingers).
