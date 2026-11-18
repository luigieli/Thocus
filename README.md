# Thocus

> **Strict, disciplined screen-time control for Android.**

Thocus is a native, minimalist Android application designed to enforce recurrent **Focus Windows** (e.g., Monday–Friday 09:00 to 17:00, with optional scheduled breaks) during which specified target applications are strictly disallowed from being accessed.

---

## Key Features

- **Instant Interception:** Detects and blocks restricted applications within milliseconds of launch.
- **Boundary Precision:** Automatically catches users who are already inside a restricted app when a focus window begins.
- **Battery-Friendly Architecture:** Zero continuous polling loops; event-driven via Android's `UsageStatsManager` and exact system alarms via `AlarmManager.setExactAndAllowWhileIdle()`.
- **Zero Banking App Conflicts:** Powered by modern Usage Stats monitoring rather than `AccessibilityService`, eliminating collisions with banking anti-fraud SDKs (e.g., Nubank, Itaú, Inter).
- **Safety by Design:** Critical system packages (Emergency dialers, Phone, SMS, and System Launchers) are dynamically whitelisted and can never be locked out.
- **Locality of Behavior (LoB):** All architectural specifications live directly inside `/src` alongside the code.

---

## Installation

Download the latest release APK directly from GitHub:

🔗 **[Download Latest Release (GitHub Releases)](https://github.com/luigieli/Thocus/releases/latest)**

1. Download `app-release.apk` to your Android device.
2. Open the downloaded file to initiate installation.
3. Launch **Thocus** and grant the required permissions (Usage Access, Display over other apps, and Alarms).

---

## Troubleshooting & APK Trustiness

Because Thocus is a personal open-source tool distributed outside the Google Play Store, Android security features may show prompts during installation and permission setup:

### 1. Google Play Protect Warning ("Unrecognized app")
- **Cause:** The APK is self-signed and not indexed on Google Play.
- **Solution:** Tap **"More details"** and then tap **"Install anyway"**.

### 2. Android 13+ / 14+ / 15+ "Restricted Setting" (Enhanced Confirmation Mode)
- **Cause:** Starting with Android 13, Android restricts sensitive permissions (such as **Usage Access** and **Display over other apps**) on sideloaded APKs to prevent fraud. You may see a message stating *"Access to this permission can put your personal and financial info at risk"*.
- **Solution:**
  1. Go to your phone's **Settings** > **Apps** > **See all apps** > **Thocus** (or long-press the Thocus icon and tap **App info**).
  2. Tap the **three dots (⋮)** in the top-right corner.
  3. Select **"Allow restricted settings"**.
  4. Confirm using your fingerprint, PIN, or pattern.
  5. Return to Thocus and grant **Usage Access** and **Display over other apps**.

### 3. "Display over other apps" (Overlay Permission)
- **Cause:** Android 10+ restricts Background Activity Launches (BAL). Thocus requires overlay permission so the background monitor can bring the Block Notice activity to the foreground immediately upon intercepting a blocked app.
- **Solution:** Follow the in-app prompt or go to **Settings** > **Apps** > **Special app access** > **Display over other apps** > **Thocus** and toggle it **On**.

### 4. OEM Battery Optimization (Xiaomi / HyperOS / Motorola / Samsung)
- **Cause:** Aggressive OEM battery managers may kill background services if set to "Optimized".
- **Solution:** In **App info** > **Battery**, select **"Unrestricted"**.

---

## Building from Source

### Prerequisites
- **JDK 17** or higher
- **Android SDK** with Platform API 34+ and Build Tools installed
- `git`

### Clone the Repository
```bash
git clone https://github.com/luigieli/Thocus.git
cd Thocus
```

### Build APKs

- **Build Debug APK:**
  ```bash
  ./gradlew assembleDebug
  ```
  *Output:* `app/build/outputs/apk/debug/app-debug.apk`

- **Build Release APK:**
  ```bash
  ./gradlew assembleRelease
  ```
  *Output:* `app/build/outputs/apk/release/app-release.apk`

### Run Unit Tests
```bash
./gradlew test
```

---

## Architecture & Development Protocol

Thocus follows the **"Markdown in `/src`"** philosophy: each package directory owns its authoritative specification colocated right next to the Kotlin code:

- Core scheduling math: [`core/SCHEDULE_ENGINE.md`](app/src/main/java/com/wellbeing/core/SCHEDULE_ENGINE.md)
- Foreground monitoring: [`service/USAGE_MONITOR.md`](app/src/main/java/com/wellbeing/service/USAGE_MONITOR.md)
- Boundary alarm scheduling: [`service/ALARM_SCHEDULER.md`](app/src/main/java/com/wellbeing/service/ALARM_SCHEDULER.md)
- Database & persistence: [`data/DATA_LAYER.md`](app/src/main/java/com/wellbeing/data/DATA_LAYER.md)
- Compose UI & MVI architecture: [`ui/UI_SPEC.md`](app/src/main/java/com/wellbeing/ui/UI_SPEC.md)

---

## License

This project is licensed under the terms described in the repository.
