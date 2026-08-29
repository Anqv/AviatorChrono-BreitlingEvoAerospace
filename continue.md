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
