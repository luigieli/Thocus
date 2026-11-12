# AGENTS.md — Thocus Project Specification & Agent Protocol

> **Philosophy ("Markdown in /src"):**
> In this repository, Markdown is not secondary documentation shoved into an isolated `/docs` or `/wiki` folder.
> **Markdown sits directly inside `/src` alongside the Kotlin code.** 
> Markdown is the authoritative specification and intent. Code is the compiled artifact of these specifications.

---

## 1. Project Overview & Vision

**Thocus** is a personal, native Android application designed to give the user strict, disciplined control over their screen time. 

The core feature is defining recurrent **Focus Windows** (e.g., Mon–Fri 09:00 to 17:00, with optional internal breaks) during which specified target applications are strictly disallowed from being accessed. Focus windows are single entries containing their overall start/finish times and break count.

### Key Target Properties:
* **Immediate Interception:** If a restricted app is launched, intercept within milliseconds.
* **Boundary Enforcement:** If the user is already inside a restricted app when a time window begins, intercept them immediately at the start second.
* **Minimal Resource Footprint:** Zero polling loops; event-driven via Android OS hooks and exact system alarms.
* **Safety & Resilience:** Critical system services (Phone, Settings, Emergency, the Thocus app itself) are strictly whitelisted and can never be locked.

---

## 2. Directory Layout: Strict Colocation in `/src`

Following Carson Gross's Locality of Behavior (LoB), every package directory owns its own Markdown specification right next to the code:

```text
/
├── AGENTS.md                                # Master agent instructions, global invariants & protocol
├── README.md                                # Setup & quickstart
└── app/
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── java/com/wellbeing/
        │   │   ├── core/                    # Pure domain logic & boundary math
        │   │   │   ├── SCHEDULE_ENGINE.md   # <--- Spec for recurrence, overnight math, evaluation
        │   │   │   ├── ScheduleEngine.kt
        │   │   │   └── TimeWindow.kt
        │   │   │
        │   │   ├── service/                 # Android system services & background receivers
        │   │   │   ├── ACCESSIBILITY.md     # <--- Spec for window interception & foreground cache
        │   │   │   ├── ALARM_SCHEDULER.md   # <--- Spec for exact boundary alarms & reboot recovery
        │   │   │   ├── BlockerAccessibilityService.kt
        │   │   │   ├── BoundaryAlarmScheduler.kt
        │   │   │   └── WindowBoundaryReceiver.kt
        │   │   │
        │   │   ├── data/                    # Room DB & persistence
        │   │   │   ├── DATA_LAYER.md        # <--- Spec for database schemas, DAOs, migrations
        │   │   │   ├── AppDatabase.kt
        │   │   │   └── ScheduleDao.kt
        │   │   │
        │   │   └── ui/                      # Jetpack Compose UI & ViewModels
        │   │       ├── UI_SPEC.md           # <--- Spec for user flows, app picker, permission guards
        │   │       ├── MainActivity.kt
        │   │       └── ...
        │   └── res/
        └── test/                            # Unit tests validating specifications
```

---

## 3. Non-Negotiable Invariants & Guardrails

Any agent modifying this codebase **must** uphold the following rules:

### A. The Never-Block Whitelist
The blocker must NEVER intercept or restrict the following package types:
1. `com.android.settings` (Android Settings)
2. `com.google.android.dialer` / telecom packages (Phone / Emergency dialers)
3. Default SMS apps
4. The system launcher (Home screen)
5. Our own package (`com.wellbeing`)

### B. No Busy-Polling / Battery Protection
* Never run continuous timer loops, background threads sleeping in loops, or polling services.
* Timing must always be scheduled through `AlarmManager.setExactAndAllowWhileIdle()`.
* `AccessibilityService` event types must be scoped strictly to `typeWindowStateChanged` with minimal notification timeouts.

### C. Boot & State Reconciliation
* The system must register an `ACTION_BOOT_COMPLETED` receiver to reschedule alarms when the device restarts.
* The state must be self-healing: evaluating `isCurrentTimeInWindow()` must be deterministic based on system clock and saved rules, not fragile in-memory counters.

---

## 4. Code Style & Cognitive Simplicity: Object Calisthenics

All code generated in this repository must actively minimize cognitive load. Boring, predictable, and immediately readable code is strictly preferred over clever, dense, or functional-gymnastics syntax.

### Core Calisthenics Invariants:
1. **Never Use the `else` Keyword:**
   * Always use guard clauses and early returns.
   * If an operation has alternative branches, return early from each branch. Never write `else` or `else if`.
2. **One Level of Indentation Per Method:**
   * Do not nest loops inside loops, or `if` blocks inside loops.
   * Extract nested logic into dedicated, private single-purpose functions.
3. **No Clever Solutions or Cryptic One-Liners:**
   * Avoid multi-line functional chains (`.filter { }.map { }.flatMap { }.fold { }`).
   * Prefer straightforward, step-by-step logic that reads sequentially.
4. **Use Early Returns (Guard Clauses):**
   * Validate preconditions at the top of the function and exit immediately.
   * Keep the happy path flat and un-indented.
5. **Keep Functions Small (< 15 lines):**
   * Each method must do exactly one thing.
6. **No Abbreviations:**
   * Name variables, parameters, and classes with full, intention-revealing words (e.g., `targetPackageName`, not `pkg`).

---

## 5. Agent Workflow Rules

When given a task:
1. **Locate the Package Spec:** Open the `.md` file in the relevant package directory (e.g., `SCHEDULE_ENGINE.md` inside `core/`).
2. **Spec First:** If introducing a new behavior or changing an invariant, update the markdown file in that directory **before** touching Kotlin code.
3. **Compile to Code:** Write or modify the Kotlin code to fulfill the updated spec.
4. **Verify Edge Cases in Tests:** 
   * Midnight-crossing schedules (e.g., 22:00 to 04:00).
   * Day-of-week bitmasks / rollover.
   * Permission revocation states.
   * App already in foreground when window starts.
