# CLAUDE.md

Orientation for Claude Code in this repo. Start here, then read the specific docs linked below for depth.

## What PulseTimer is

A haptic and audio feedback timer for smartwatches, built for presenters, facilitators, and meeting leaders. It taps the wrist at configured milestones and intervals so the wearer stays on pace without visibly checking the time. No network, no accounts, all data local.

## Repo layout

```
pulsetimer/
├── docs/                       Shared product docs (see below)
├── wear-os/                    Released Kotlin/Compose app (com.grantlittman.pulsetimer)
├── watchos/PulseTimer/         Xcode project for the watchOS app
│   ├── PulseTimer Watch App/   Watch app source (synchronized folder group)
│   ├── Complication/           Widget extension source (synchronized folder group)
│   ├── Info.plist              Watch app's plist (explicit, NOT auto-generated)
│   └── PulseTimer.xcodeproj
├── site/pulsetimer/            GitHub Pages landing + privacy policy
└── CLAUDE.md                   This file
```

The Wear OS source still lives under package `com.grantlittman.wearapp.*`. Only the Android `applicationId` was renamed to `com.grantlittman.pulsetimer` for the Play Store.

The watchOS app's App Store listing name is **"PulseTimer: Haptic Cues"** (Apple rejected the simpler "PulseTimer" for similarity to an existing app). On-watch display name remains **"PulseTimer"** for brand consistency with Wear OS.

## Documentation to read

Read these in order when picking up a new task:

1. [`docs/requirements.md`](docs/requirements.md): platform-agnostic product requirements, the preset patterns, functional and non-functional requirements. The source of truth for feature parity between Wear OS and watchOS.
2. [`docs/design-decisions.md`](docs/design-decisions.md): architecture choices with the reasoning. Read before touching the timer engine, service lifecycle, ambient mode, storage, or the complication.
3. [`docs/project-context.md`](docs/project-context.md): tacit knowledge brain dump with origin, hard-won debugging lessons, dev environment, and open questions. **Gitignored.** Present locally for Grant, not in fresh clones or git worktrees. If it is missing from your working tree, check the main repo at `~/Documents/GitHub/pulsetimer/docs/project-context.md` before assuming it does not exist.
4. [`docs/play-store-listing.md`](docs/play-store-listing.md): Google Play Store copy. Reference when any public messaging needs to stay consistent.

## Working with Grant

Grant Littman is Head of Digital with close to 30 years as a technofunctional leader. He is not a developer. He built PulseTimer v1 in a single evening using Claude as a development partner, and the project is personally meaningful because of that.

Collaboration expectations:

- Explain changes in plain language, including the why, not just the what.
- Give specific Android Studio menu paths (for example, "Build > Generate Signed Bundle/APK") rather than assuming IDE fluency.
- When something breaks, explain the root cause, not only the fix.
- Present options when there is a real choice to make.
- Grant iterates fast and pushes back precisely. Respond to corrections promptly and update memory so the same guidance does not need to be given twice.

Writing style for any prose Grant may publish or present as his own (social posts, README, release notes, commit messages, PR descriptions):

- **No em dashes.** Grant flagged them as an AI tell and wants commas, colons, periods, and conjunctions instead. This does not apply to code or code comments.
- Voice is a business leader who built something, not a developer explaining the build.
- Reference his nearly 30 years of technofunctional experience when framing is relevant.

## Build and deploy workflow

### Wear OS

There is **no Gradle wrapper** in this repo. Do not suggest `./gradlew` commands. Builds go through Android Studio:

- Build an APK: Build > Build APK.
- Signed bundle for Play Store: Build > Generate Signed Bundle/APK.
- The release keystore lives at `/Users/grantlittman/pulsetimer-upload-key.jks`, alias `pulsetimer`. Keystore credentials are in `keystore.properties` (gitignored). Template at `keystore.properties.template`.

JDK is Android Studio's bundled JBR at `/Applications/Android Studio.app/Contents/jbr/Contents/Home/`. No system JDK is installed.

ADB is wireless to a Pixel Watch 4 (serial `adb-58231WRCVL119E-CpbEDg._adb-tls-connect._tcp`). An emulator (`emulator-5554`) may also be attached. Always target the physical watch with `adb -s <serial>` when running commands.

### watchOS

Xcode 26.4.1 with watchOS SDK 26.4. Deployment target watchOS 10.0 (lower than the SDK so the Apple Watch SE 2 on watchOS 26.0.2 can run it).

Builds via Xcode UI (Cmd+R) or `xcodebuild`. Schemes are auto-generated; if missing, regenerate via Product → Scheme → Manage Schemes → "Autocreate Schemes Now."

```bash
# Build for the SE simulator
xcodebuild -project watchos/PulseTimer/PulseTimer.xcodeproj \
  -target "PulseTimer Watch App" \
  -destination "id=E485EDAF-4617-490E-AE67-48004503B068" \
  -configuration Debug build

# Archive for App Store distribution
xcodebuild -project watchos/PulseTimer/PulseTimer.xcodeproj \
  -scheme "PulseTimer Watch App" \
  -archivePath /tmp/PulseTimer.xcarchive archive
```

Apple Watch SE 2nd gen is the physical test device, paired via Grant's iPhone 13 mini (iOS 26.1). Both must be on Wi-Fi for development deploys. App Store Connect app record exists. Apple Developer Program membership active. **Read [`watchOS gotchas memory`] before touching the watchOS code** — many subtle traps documented there (asset catalog, hit-testing, deployment targets, WKExtendedRuntimeSession sessions types, etc.).

### Three code locations on disk

- `~/Wear OS Project/`: the Android Studio working copy Grant has used.
- `~/Documents/GitHub/pulsetimer/`: this monorepo (canonical going forward).
- `~/Library/Android/sdk/pulsetimer/`: Cowork session workspace.

When in doubt, this monorepo is canonical.

## Architecture at a glance

### Wear OS

| Layer | File | Role |
|---|---|---|
| UI host | [`MainActivity.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/presentation/MainActivity.kt) | Activity lifecycle, service bind, ambient observer, `FLAG_KEEP_SCREEN_ON` |
| Nav | [`WearApp.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/presentation/WearApp.kt) | Compose navigation host |
| Screens | [`PatternListScreen.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/presentation/screens/PatternListScreen.kt), [`TimerScreen.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/presentation/screens/TimerScreen.kt), [`PatternEditorScreen.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/presentation/screens/PatternEditorScreen.kt) | List, running timer, editor |
| Timer engine | [`TimerEngine.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/timer/TimerEngine.kt) | UI-agnostic tick loop, state flow, signal scheduling |
| Service | [`TimerService.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/timer/TimerService.kt) | Foreground service, OngoingActivity, wake lock |
| Signal output | [`SignalExecutor.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/timer/SignalExecutor.kt) | `VibrationEffect` for haptics, audio playback |
| Data | [`Pattern.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/data/model/Pattern.kt), [`PatternRepository.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/data/repository/PatternRepository.kt) | Central data model, DataStore + Gson persistence |
| Complication | [`PulseTimerComplicationService.kt`](wear-os/app/src/main/java/com/grantlittman/wearapp/complication/PulseTimerComplicationService.kt) | Watch face complication, `SMALL_IMAGE` type |

### watchOS

| Layer | File | Role |
|---|---|---|
| App entry | [`PulseTimerApp.swift`](watchos/PulseTimer/PulseTimer Watch App/PulseTimerApp.swift) | `@main`, wires up `PatternRepository`, `SignalExecutor`, `TimerEngine` via `@State` + `.environment` |
| Nav | [`ContentView.swift`](watchos/PulseTimer/PulseTimer Watch App/ContentView.swift) | NavigationStack, `AppRoute` enum, routes to TimerView and editor |
| Screens | [`PatternListView.swift`](watchos/PulseTimer/PulseTimer Watch App/Views/PatternListView.swift), [`TimerView.swift`](watchos/PulseTimer/PulseTimer Watch App/Views/TimerView.swift), [`PatternEditorView.swift`](watchos/PulseTimer/PulseTimer Watch App/Views/PatternEditorView.swift), [`MilestoneEditorView.swift`](watchos/PulseTimer/PulseTimer Watch App/Views/MilestoneEditorView.swift) | List, timer, editor, milestone sub-editor |
| Pickers | [`DurationPickerView.swift`](watchos/PulseTimer/PulseTimer Watch App/Views/DurationPickerView.swift), [`HapticTypePickerView.swift`](watchos/PulseTimer/PulseTimer Watch App/Views/HapticTypePickerView.swift) | H:M:S wheel duration picker, haptic type picker |
| Timer engine | [`TimerEngine.swift`](watchos/PulseTimer/PulseTimer Watch App/Timer/TimerEngine.swift) | `@Observable @MainActor` Task-based tick loop, direct port of Kotlin engine |
| Runtime session | [`RuntimeSessionController.swift`](watchos/PulseTimer/PulseTimer Watch App/Timer/RuntimeSessionController.swift) | Wraps `WKExtendedRuntimeSession` (mindfulness type, 60-min cap) |
| Signal output | [`SignalExecutor.swift`](watchos/PulseTimer/PulseTimer Watch App/Timer/SignalExecutor.swift) | `WKHapticType`-based playback (Core Haptics is iOS-only). Audio is stubbed. |
| Data | [`Pattern.swift`](watchos/PulseTimer/PulseTimer Watch App/Models/Pattern.swift), [`PatternRepository.swift`](watchos/PulseTimer/PulseTimer Watch App/Data/PatternRepository.swift) | Same JSON schema as Wear OS, `Codable` + `UserDefaults` persistence |
| Complication | [`Complication.swift`](watchos/PulseTimer/Complication/Complication.swift) | WidgetKit static configuration, all complication families, currently uses an SF Symbol stand-in icon |

## Load-bearing decisions (do not regress)

Each of these caused a real debugging session. Flag the history and confirm with Grant before changing.

1. **`FLAG_KEEP_SCREEN_ON` on MainActivity during active timers.** Removing it for battery caused the "75 second kill" twice. Without it, the system backgrounds the activity at about 75 seconds, `AmbientLifecycleObserver` stops firing, and the timer dies.
2. **`TimerService` is started AND bound, never just bound.** `startForegroundService()` is called before bind. `onStop()` does not unbind while a timer is running.
3. **Gson bypasses Kotlin null safety.** Fields declared non-null in Kotlin data classes can be null at runtime after deserialization. Add defensive null checks in UI code touching `Pattern`, `Signal`, `Milestone`, `RepeatingInterval`, `CountdownWarning`, and `TimerState`.
4. **R8 / ProGuard is disabled** (`isMinifyEnabled = false, isShrinkResources = false`). Re-enabling caused opaque obfuscated crashes in Compose internals. APK size is not a concern.
5. **`TimerEngine.stop()` preserves final state.** It does `_state.copy(isRunning = false, ...)`, not `TimerState()`. Resetting causes a visible 0:00 flash during async Compose navigation.
6. **Complication uses `SMALL_IMAGE` with a 400x400 PNG, cyan-blue baked in, separate white ambient icon via `setAmbientImage()`.** `MONOCHROMATIC_IMAGE` disappears from the picker on Pixel Watch 4 Modular. `SHORT_TEXT` forces unwanted text. Vector drawables and smaller PNGs rendered wrong.

## Open questions to verify before claiming a feature works

The brain dump flags these as unverified. Check the code, do not assume:

1. **DND-aware audio muting.** Intent: haptics always fire, audio respects Do Not Disturb. Not confirmed in the current code.
2. **Preset copy-on-edit.** Requirements say editing a preset creates a copy. May be edit-in-place or blocked today.
3. **Pattern import/export.** Data model supports it per the JSON schema in `docs/design-decisions.md`. Feature not built.
4. **Battery optimization beyond ambient tick rate.** `FLAG_KEEP_SCREEN_ON` is still required for the lifecycle. No better approach has been found yet.

## Testing

There are no automated tests. Testing is manual on the physical Pixel Watch 4. The golden path:

1. Build and deploy to watch.
2. Run through each preset pattern (Talk, Meeting, Keynote).
3. Verify haptics fire at configured times.
4. Test pause and resume.
5. Confirm the timer survives a screen timeout (the 75 second test).
6. Check the complication renders correctly in both interactive and ambient.
7. Tap Done and confirm there is no 0:00 flash.

When you cannot physically test a change, say so explicitly rather than claiming success. Type checking and a clean build are not feature verification on this project.

## Workflow model

Three surfaces in play:

- **Claude Code** (you): execution, builds, ADB, git, code edits.
- **Claude.ai Project**: thinking, planning, higher-level design conversation.
- **Cowork**: GUI-only tasks.

GitHub Issues is being used for feature planning going forward.

## Privacy and distribution

- No data collection, no network, no analytics. See [`site/pulsetimer/privacy-policy.html`](site/pulsetimer/privacy-policy.html).
- Google Play Developer account is registered and pending Google's verification review.
- Apple Developer Program is active. App Store Connect app record exists under the name **"PulseTimer: Haptic Cues"**. Bundle IDs registered:
  - `com.grantlittman.PulseTimer`
  - `com.grantlittman.PulseTimer.watchkitapp`
  - `com.grantlittman.PulseTimer.watchkitapp.PulseTimerComplication`
- Archive validation passes. Two known launch blockers tracked in the project state memory: complication icon (SF Symbol stand-in, not the brand asset) and audio playback (stubbed).
