# ACCESSIBILITY.md — Real-Time Interception Spec

> Package: `com.wellbeing.service`

## 1. Responsibilities
Intercept blocked applications in real-time as soon as their windows come into view, immediately display a warning to the user, and present the Thocus focus notice dialog directly in the foreground.

## 2. Accessibility Configuration (`res/xml/accessibility_service_config.xml`)
* `accessibilityEventTypes="typeWindowStateChanged"` (No content or click tracking).
* `accessibilityFeedbackType="feedbackGeneric"`.
* `notificationTimeout="50"`.
* `canRetrieveWindowContent="false"`.

## 3. Foreground Package Cache
`BlockerAccessibilityService` must maintain a high-performance in-memory cache:
```kotlin
object ForegroundTracker {
    @Volatile var currentPackage: String? = null
}
```
* Updated whenever `event.eventType == TYPE_WINDOW_STATE_CHANGED` with a non-system package.
* Consumed by `WindowBoundaryReceiver` when an alarm fires.

## 4. Interception Sequence
1. Receive `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`.
2. Extract `packageName`.
3. Check `NeverBlockWhitelist.isWhitelisted(packageName)`:
   * Settings, dialer, SMS, launcher, `com.wellbeing` $\rightarrow$ Return immediately.
4. Cache `currentPackage = packageName`.
5. Query `ScheduleEngine.isPackageBlocked(packageName)`.
6. If blocked:
   * Launch `BlockNoticeActivity` directly into the foreground.
   * `BlockNoticeActivity` displays the focus lock warning card over a dimmed backdrop.
   * On user dismissal ("Stay Focused" or back press), `BlockNoticeActivity` redirects cleanly to the device Home screen.

## 5. Invariants
* Never perform disk IO or DB queries on the main thread inside `onAccessibilityEvent`.
* Whitelist check must occur before any business logic to prevent lockout bugs.
