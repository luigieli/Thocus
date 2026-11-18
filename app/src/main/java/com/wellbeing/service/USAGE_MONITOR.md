# USAGE_MONITOR.md — UsageStats Interception & Foreground Monitor Spec

> Package: `com.wellbeing.service`

## 1. Responsibilities
Intercept blocked applications during active focus windows using Android's native `UsageStatsManager` (`PACKAGE_USAGE_STATS`), redirect the user to the device Home screen, and present the Thocus focus notice dialog (`BlockNoticeActivity`) in the foreground.

## 2. Why UsageStatsManager Instead of AccessibilityService
* **Zero Bank App Conflicts:** Banking and financial applications (e.g., Nubank, Itaú, Inter, Santander) run anti-fraud SDKs that terminate when an `AccessibilityService` is active. They do not flag `PACKAGE_USAGE_STATS`.
* **Zero Restricted Settings Blocks:** Android 13/14/15/16 "Restricted Settings" (e.g. *"Access to this permission can put your personal and financial info at risk"*) targets accessibility and notification listeners. It does not lock Usage Access.
* **Play Protect Sideload Safety:** Google Play Protect Enhanced Fraud Protection automatically blocks sideloaded APKs requesting `BIND_ACCESSIBILITY_SERVICE`. Removing it restores seamless sideloading and update installations.

## 3. Screen-State Awareness & Battery Protection
* `UsageMonitorService` registers dynamic broadcast receivers for `Intent.ACTION_SCREEN_ON` and `Intent.ACTION_SCREEN_OFF`.
* When the screen is **OFF**, polling loops are completely paused.
* When the screen is **ON** and an active focus window is in effect, the service checks foreground transitions via `UsageStatsManager.queryEvents()`.
* When no focus windows are currently active, polling is suspended until the next alarm boundary.

## 4. Foreground Package Resolution & Interception
The service queries recent events from `UsageStatsManager`:
* Evaluates events of type `UsageEvents.Event.ACTIVITY_RESUMED` across a 60-second sliding window, with a fallback to `queryUsageStats` (`lastTimeUsed`) to guarantee detection even when user is stationary inside an app.
* Checks `NeverBlockWhitelist.isWhitelisted(context, packageName)`:
  * System launcher, dialer, SMS, settings, and own package $\rightarrow$ skip interception.
* If `ScheduleEngine.isPackageBlocked(packageName, activeWindows, now)` is true:
  1. Fires `Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)` with `FLAG_ACTIVITY_NEW_TASK`.
  2. Launches `BlockNoticeActivity` in an isolated task affinity (`taskAffinity=""`, `launchMode="singleInstance"`).
  3. Relies on `Settings.canDrawOverlays(context)` (`SYSTEM_ALERT_WINDOW`) to authorize background activity launches on Android 10+.

## 5. Invariants
* Strict Object Calisthenics: zero `else`, maximum one level of indentation per method, methods < 15 lines.
* Whitelist check must occur before any restriction logic.
* Must run as a persistent Foreground Service with a low-priority notification (`FOREGROUND_SERVICE_SPECIAL_USE`).
* On service creation (`onCreate`), warmly load in-memory cache via `ScheduleRepository.getInstance(context).refreshCache()`.
* Synchronous database operations are forbidden inside the monitoring loop; always consume the in-memory cache from `ScheduleRepository`.
