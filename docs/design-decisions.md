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

**Complication Types:** Preserve SMALL_IMAGE for Pixel Watch 4's Modular center slot, with pre-tinted cyan active and white ambient PNGs via `setAmbientImage()`. Also offer MONOCHROMATIC_IMAGE (declared as `ICON` in the manifest) for watch faces whose slots accept monochrome icons. That path uses the white PNG so the watch face can tint it. Both types are icon-only launch shortcuts. Do not replace SMALL_IMAGE with monochrome-only support: the Modular center slot did not support it in prior hardware testing. SHORT_TEXT remains unsupported because it forces unwanted text display. Grant verified the monochrome icon renders and launches correctly on Pixel Watch 4 on November 10, 2026. Compatibility on the reported Xiaomi Watch 2 remains pending tester confirmation.

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

### Wear OS Active Pattern Selection

The list highlights the current session by pattern ID, with a Running, Paused, or Completed label and an outline. Other patterns remain visible but cannot start until the user stops or dismisses the current session. A View timer button provides a way back even if the underlying saved pattern was renamed or deleted. This makes the existing single-session safeguard explicit instead of silently opening an unrelated timer when another pattern is tapped. Editing and creating patterns remain available. The session ID clears on Stop/Done while final timer display values remain intact.

Validation: On November 11, 2026, Grant verified all six checks on Pixel Watch 4: running highlight, disabled other patterns without interrupting the session, View timer navigation, paused status, re-enabling patterns after Stop, and starting a different pattern. The list appearance was approved. Debug and signed release builds, Android lint, and whitespace checks passed.

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


## watchOS session navigation refinement (November 16, 2026)

Returning from the timer to the pattern list now preserves the session. The list highlights its pattern and labels it Running, Paused, or Completed, with a View timer action. Tapping another pattern explains that Stop or Done is required first. The active pattern has no edit/delete swipe actions, including while paused or completed. Session identity clears on Stop/Done while final display values remain intact during dismissal.

Elapsed time uses ContinuousClock with a fixed anchor for each running segment. Pausing captures elapsed time up to the pause action, and paused time is excluded on resume. Wall-clock timestamps remain only for signal flash notifications. Existing watchOS completion behavior is retained: a single completion pulse, the manual Done tap target, TimelineView refresh, and a ten-second completion auto-dismiss while the timer screen is visible. Back navigation does not stop the runtime session.

The nonfunctional Audio toggle was removed from the watchOS pattern editor. Existing audioEnabled values remain in stored patterns for schema compatibility. No independent audio playback was added; Apple Watch Silent Mode continues to control the sound paired with system haptics.

Host-side engine regression checks: `bash watchos/tests/run-timer-checks.sh`. These use the production engine and models with WatchKit service doubles. Checks passed for session replacement protection, pause exclusion, resume, completion protection, Stop identity clearing, and restart. An unsigned Release build for generic watchOS and signed development builds installed through Xcode succeeded. Swift macro subprocesses required `-disable-sandbox` when running checks inside the existing execution sandbox; no project build settings were changed for this.

The timer layout places the pattern name in the system navigation title, includes scrollable content with bottom spacing, and uses compact equal-width controls with single-line labels. View timer also appears within the highlighted pattern row so it remains discoverable when the list restores its scroll position.

Apple Watch hardware validation completed November 16, 2026:
- Returning to the list preserved the running timer, and View timer reopened it without resetting.
- Selecting another pattern showed the active-session safeguard.
- Pause remained stable through navigation.
- Navigation title, button bounds, single-line Resume, and active-row View timer passed visual checks.
- Edit/delete actions were unavailable for the active pattern while running or paused, and returned for the custom pattern after Stop.
- Immediate Done and ten-second automatic completion dismissal both passed.
- Two three-minute wrist-down runs with temporary diagnostics delivered both minute cues and completion on time, including after an immediate Stop/restart, with no execution-gap messages.
- After removing all temporary diagnostic UI and state, a final standalone wrist-down run delivered minute-one, minute-two, and completion cues on time. Grant confirmed the diagnostics were gone. Xcode's debugger was stopped for this test.

Runtime callbacks now check session identity before invoking expiration handling or clearing the current session, and the session reference is assigned before starting it. Earlier hardware runs delayed the second cue until wrist raise; the exact cause was not conclusively established, despite the subsequent successful runs. Continue monitoring this behavior.

Known release follow-up: explicitly leaving via the Digital Crown ends the frontmost mindfulness runtime, and overdue cues can fire on reopening. User-facing guidance and interruption handling remain outstanding before the App Store update. No App Store upload has been performed for these changes.
