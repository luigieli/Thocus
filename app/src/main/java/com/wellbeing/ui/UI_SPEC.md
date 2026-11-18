# UI_SPEC.md — Focus Windows Dashboard & Configuration Modal Spec

> Package: `com.wellbeing.ui`

## 1. Responsibilities
1. Dark Theme with Purple / Violet accent system (`#BB86FC` / `#A855F7`).
2. Primary Dashboard with countdown timer, active focus indicator, and Focus Windows list.
   * Hero logo displayed at 72dp using transparent alpha squircle with zero white canvas borders.
   * **Focus Window Cards**: Display original first start and finish times (e.g. `08:00 - 17:00` or `All Day (24h)`), break count (e.g. `No breaks`, `1 break`, or `N breaks`), and target application count.
3. Focus Window Configuration Modal:
   * **Single Window Model**: Configures a unified focus window with optional multiple embedded break parameters without creating split entries.
   * **No Default App Selection**: Opening modal for a new window starts with 0 apps selected (`emptySet()`). Save button is disabled until at least 1 app is selected.
   * **All-Day (24h) Mode**: Toggle to enforce restriction for all 24 hours of selected days. Breaks then define exclusive allowed usage windows (e.g., 6-8, 12-14, 19-23).
   * **Multiple Breaks**: Allows adding, editing, and removing multiple independent break intervals (`+ Add Break`, individual remove actions).
   * **Sticky Footer Action**: The "Save Focus Window" button is permanently docked in a raised bottom bar of the modal sheet.
   * **Self-Contained Scrolling**: The app selector and schedule configuration scroll independently within `weight(1f)` behind the docked save button.
   * Circular Day-of-Week Dots (Android Clock alarm style).
   * Start and Finish time pickers + Break window configuration.
   * Lightweight 120 FPS app item rows with real icons and no technical package names.
4. Permissions & Service Diagnostics:
   * **Usage Access Permission**: Prompt user to grant `Settings.ACTION_USAGE_ACCESS_SETTINGS` with clear description of why screen time tracking is needed.
   * **Display Over Other Apps (Overlay)**: Prompt user to grant `Settings.ACTION_MANAGE_OVERLAY_PERMISSION` (`canDrawOverlays`) required by Android 10+ for background interception.
   * **Exact Alarms**: Check and link to `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` on Android 12+.
   * **OEM Battery Optimization**: Provide quick shortcut to battery optimization settings to prevent OS background task killing.
5. Block Notice Dialog (`BlockNoticeActivity`):
   * Isolated Task Affinity: Runs with `taskAffinity=""` and `launchMode="singleInstance"` to prevent task collision and `START_ABORTED` from `isToBackByHome` on OEMs like HyperOS.
   * Dynamically handles `onNewIntent` to refresh the blocked package name if intercepted repeatedly.

## 2. Invariants
* Strict Object Calisthenics: zero `else`, maximum one indentation level per method, methods < 15 lines.
* Sticky footer remains visible regardless of list scroll position and applies `navigationBarsPadding()` to sit above Android navigation buttons.
* Bottom sheet height is constrained to `fillMaxHeight(0.92f)` to provide structured scrolling.
* App rows use static zero-animation selection indicators to guarantee 120 FPS fast flings.
* App icons are pre-rounded to 96px on IO dispatchers to avoid GPU offscreen saveLayer clipping.
* Single-line text truncation (`maxLines = 1`, `TextOverflow.Ellipsis`) prevents variable-height measurement relayouts.
* Focusing the app search field auto-scrolls the `LazyColumn` to place the search bar at the top of the viewport above the soft keyboard (`imePadding()`).
* When the soft keyboard is dismissed, search focus clears automatically (`WindowInsets.isImeVisible`), and tap interactions re-trigger the top scroll reliably on repeat openings.
* Search input includes an instant clear action (`ClearSearchButton`) and IME search action dismissal.
