# PulseTimer — Product Requirements

## Overview

PulseTimer is a haptic and audio feedback timer designed for presenters, facilitators, and meeting leaders. It runs on a smartwatch and delivers discreet timed signals so the wearer always knows where they stand in a session without visibly checking the time.

## Target Platforms

- Wear OS (Pixel Watch, Samsung Galaxy Watch, etc.)
- watchOS (Apple Watch)

## Core Concepts

### Patterns

A pattern defines a complete timer configuration. Each pattern includes:

- **Name**: user-visible label (e.g., "Keynote", "Standup", "Workshop")
- **Total duration**: optional fixed length in minutes. If omitted, the timer runs indefinitely until manually stopped
- **Repeating interval**: a signal that fires on a fixed period (e.g., every 5 minutes)
- **Milestones**: signals that fire at specific elapsed times (e.g., "halfway" at 25 minutes, "wrap up" at 45 minutes)
- **Countdown warning**: optional behavior for the final stretch, with its own interval and signal
- **Audio enabled**: toggle for audio cues alongside haptics

Patterns can be presets (bundled with the app, non-deletable) or user-created.

### Signals

A signal defines what the wearer feels and/or hears at a given moment. Each signal has:

- **Haptic type**: the vibration pattern
  - Tap: short, subtle (~50ms)
  - Buzz: medium (~200ms)
  - Pulse: long, strong (~500ms)
  - Double Tap: two quick taps in succession
- **Intensity**: strength of the vibration (0.0 to 1.0)
- **Audio type** (optional): Click, Beep, or Chime
- **Repeat count**: how many times the signal fires in succession (for milestones)

### Milestones

A milestone fires at a specific elapsed time and uses a configurable signal. Milestones can have:

- **Trigger time**: when to fire (absolute elapsed time in the session)
- **Signal**: which haptic/audio pattern to use
- **Repeat count**: how many times to fire the signal (e.g., 3 buzzes at the halfway mark)
- **Label**: optional name shown on the timer display (e.g., "Halfway", "Wrap up")

### Countdown Warning

An optional final-stretch mode that activates at a specified time before the session ends. Once active, it fires a signal at its own interval (e.g., every 10 seconds for the last minute) to create urgency.

## Preset Patterns

The app ships with three built-in patterns:

### Talk (5 minutes)
- Repeating interval: every 30 seconds, Tap signal
- No milestones, no countdown warning
- Purpose: short talks, lightning rounds

### Meeting (30 minutes)
- Repeating interval: every 5 minutes, Tap signal
- Milestones:
  - 25 minutes: Double Tap, label "Wrap up"
- No countdown warning
- Purpose: standard meetings, 1-on-1s

### Keynote (50 minutes)
- Repeating interval: every 5 minutes, Tap signal
- Milestones:
  - 25 minutes: Buzz, repeat 2x, label "Halfway"
  - 40 minutes: Double Tap, repeat 2x, label "10 min left"
  - 45 minutes: Pulse, repeat 3x, label "Wrap up"
- Countdown warning: activates at 49 minutes, fires every 10 seconds with Tap
- Purpose: conference talks, keynotes, long presentations

## Functional Requirements

### FR-1: Pattern Management

- FR-1.1: Users can view a list of all patterns (presets and custom)
- FR-1.2: Users can create new custom patterns with all configurable fields
- FR-1.3: Users can edit existing custom patterns
- FR-1.4: Users can delete custom patterns
- FR-1.5: Preset patterns cannot be deleted but can be edited (changes create a copy)
- FR-1.6: Patterns are persisted locally on the device
- FR-1.7: Pattern list is sorted by most recently used, then presets, then alphabetical

### FR-2: Timer Execution

- FR-2.1: Tapping a pattern starts the timer immediately
- FR-2.2: The timer display shows: pattern name, elapsed or remaining time, completion percentage, and time until next signal
- FR-2.3: The timer fires haptic signals at each repeating interval
- FR-2.4: The timer fires milestone signals at their configured trigger times
- FR-2.5: The timer activates countdown warning mode at the configured time
- FR-2.6: Users can pause and resume the timer without losing position
- FR-2.7: When paused, elapsed time does not advance and no signals fire
- FR-2.8: When the total duration is reached, the timer shows a completion screen
- FR-2.9: The completion screen shows total elapsed time and a "Done" button
- FR-2.10: Tapping "Done" returns to the pattern list

### FR-3: Background Reliability

- FR-3.1: The timer continues running when the screen turns off
- FR-3.2: The timer continues running when the user navigates away from the app
- FR-3.3: Haptic signals fire reliably regardless of screen state
- FR-3.4: The app supports ambient/always-on display mode during active timers
- FR-3.5: In ambient mode, the display updates at a reduced frequency to save battery
- FR-3.6: The timer appears as an ongoing activity in the system (e.g., recents, notification)

### FR-4: Watch Face Integration

- FR-4.1: The app provides a watch face complication for quick launch
- FR-4.2: Tapping the complication opens the app to the pattern list
- FR-4.3: The complication displays the app icon (no text)
- FR-4.4: The complication provides both active and ambient mode icons

### FR-5: Haptic Feedback

- FR-5.1: Each haptic type (Tap, Buzz, Pulse, Double Tap) produces a distinct vibration
- FR-5.2: Haptic intensity is configurable per signal
- FR-5.3: Haptics fire reliably in both foreground and background states
- FR-5.4: Platform-specific haptic APIs are used for best-quality feedback

### FR-6: Audio Feedback

- FR-6.1: Audio cues are optional and controlled per pattern
- FR-6.2: When enabled, audio fires alongside haptic signals
- FR-6.3: Three audio types are available: Click, Beep, Chime
- FR-6.4: Audio respects the device's volume and do-not-disturb settings

## Non-Functional Requirements

### NFR-1: Performance
- The app should launch and be ready to start a timer within 2 seconds
- Timer accuracy should be within 100ms of the configured trigger times
- The app should not noticeably impact battery life beyond normal watch usage

### NFR-2: Privacy
- No user data is collected, transmitted, or shared
- No network connectivity is required
- All data is stored locally on the device
- No analytics, advertising, or tracking of any kind

### NFR-3: Accessibility
- Timer display should be readable at arm's length
- Haptic signals should be distinguishable from system notifications
- The app should work in both always-on and standard display modes

### NFR-4: Distribution
- Wear OS: Google Play Store (free)
- watchOS: Apple App Store (free)
- Both versions should maintain feature parity as described in this document
