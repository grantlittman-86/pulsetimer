# PulseTimer — Design Decisions

This document captures architectural and design choices made during development, including the rationale behind each decision. It serves as a reference for maintaining consistency across platforms and for onboarding future contributors.

## Architecture

### Separation of Timer Engine from UI

The timer logic (elapsed time tracking, signal scheduling, state management) is implemented as a standalone engine class that is independent of the UI framework. The UI observes the engine's state via a reactive stream and renders accordingly.

**Rationale:** This separation means the timer engine can run in a background service (Wear OS) or extended runtime session (watchOS) without depending on the UI lifecycle. It also makes the engine testable in isolation and portable across platforms at the design level.

### Pattern as the Central Data Model

All timer behavior flows from a single Pattern object. Rather than having separate configuration screens for intervals, milestones, and countdown warnings, everything is composed into one Pattern that fully describes a timer session.

**Rationale:** A user thinks in terms of "I want a setup for my keynote" not "I want to configure an interval, then add some milestones." The single-pattern model maps to how presenters actually prepare.

### Local-Only Data Storage

Patterns are stored using platform-native local storage (DataStore on Android, UserDefaults or SwiftData on watchOS). There is no cloud sync, no account system, and no network calls.

**Rationale:** A presentation timer is a personal utility. The complexity and privacy implications of cloud sync are not justified. Users who switch devices can recreate their patterns in seconds.

## Platform-Specific Decisions

### Wear OS

**Foreground Service (started, not just bound):** The timer runs as a started foreground service, not a bound-only service. The activity calls `startForegroundService()` before binding. This ensures the service survives when the activity unbinds (screen timeout, navigation away).

**FLAG_KEEP_SCREEN_ON:** The activity sets this flag when a timer starts. Without it, the system backgrounds the activity after ~75 seconds, which prevents the AmbientLifecycleObserver from functioning. The flag keeps the activity in the foreground so ambient mode transitions work correctly. It is cleared when the timer stops.

**OngoingActivity API:** The timer registers as an ongoing activity so it appears in the Wear OS recents and on-going section. This uses `OngoingActivity.Builder` which requires a `NotificationCompat.Builder` (not a built Notification).

**Ambient Mode Tick Rate:** The timer engine ticks at 100ms in interactive mode and 500ms in ambient mode. This reduces CPU wake-ups by 80% when the screen is dimmed while still maintaining acceptable time display accuracy.

**Gson for Serialization:** Patterns are serialized to JSON using Gson for storage in DataStore. Important caveat: Gson uses reflection and bypasses Kotlin's compile-time null safety. All fields read from deserialized objects must be treated as potentially null at runtime, even if declared non-nullable in Kotlin. Defensive null checks are required in the UI layer.

**Complication Type:** The watch face complication uses SMALL_IMAGE (not MONOCHROMATIC_IMAGE or SHORT_TEXT). On Pixel Watch 4's Modular watch face, MONOCHROMATIC_IMAGE is not supported in the center slot, and SHORT_TEXT forces unwanted text display. SMALL_IMAGE with a pre-tinted PNG provides the most consistent rendering. Separate active (cyan) and ambient (white) icons are provided via `setAmbientImage()`.

**R8/ProGuard Disabled:** Minification is disabled for the release build. The app is small enough that APK size is not a concern, and R8's code stripping causes issues with Gson reflection and Compose runtime. This may be revisited if APK size becomes a constraint.

### watchOS (Planned)

**WKExtendedRuntimeSession:** watchOS equivalent of the foreground service. Allows the timer to continue running when the wrist is down or the app is not frontmost. Limited to specific session types; "smart alarm" or "self-care" may be applicable.

**WKHapticType:** watchOS provides a fixed set of haptic types (notification, directionUp, directionDown, success, failure, retry, start, stop, click). These will need to be mapped to PulseTimer's signal types (Tap, Buzz, Pulse, Double Tap) to provide the closest equivalent experience.

**SwiftUI for Wear:** The UI will use SwiftUI with NavigationStack. watchOS SwiftUI is arguably simpler than Compose for Wear OS for an app of this scope.

## UI/UX Decisions

### No Elapsed Time Flash on Completion

When the timer finishes and the user taps "Done," the app navigates back to the pattern list. The timer engine's `stop()` method preserves the final state (elapsed time, pattern name) rather than resetting to defaults. This prevents a brief flash of "0:00" during the asynchronous Compose/SwiftUI navigation transition.

### Pattern List Sort Order

Patterns are sorted by most recently used first, then presets, then alphabetical. This puts the pattern you're most likely to use next at the top of the list, which matters on a small watch screen where scrolling is cumbersome.

### Minimal Complication Design

The watch face complication shows only the app icon with no text. This matches user expectations for a utility complication and avoids the visual clutter of a label on a small watch face.

## Data Format

### Pattern JSON Schema

```json
{
  "id": "uuid-string",
  "name": "Keynote",
  "isPreset": true,
  "totalDurationMillis": 3000000,
  "repeatingInterval": {
    "intervalMillis": 300000,
    "signal": {
      "hapticType": "TAP",
      "intensity": 1.0,
      "audioType": null
    }
  },
  "milestones": [
    {
      "triggerAtMillis": 1500000,
      "signal": {
        "hapticType": "BUZZ",
        "intensity": 1.0,
        "audioType": null
      },
      "repeatCount": 2,
      "label": "Halfway"
    }
  ],
  "countdownWarning": {
    "activateAtMillis": 2940000,
    "intervalMillis": 10000,
    "signal": {
      "hapticType": "TAP",
      "intensity": 1.0,
      "audioType": null
    }
  },
  "audioEnabled": false,
  "lastUsedAt": null
}
```

This JSON schema should be treated as the canonical data format. Both platforms should be able to serialize and deserialize patterns to/from this format, even if the internal storage mechanism differs. This enables potential future features like pattern import/export.
