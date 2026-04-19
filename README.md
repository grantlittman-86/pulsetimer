# PulseTimer

A haptic and audio feedback timer for smartwatches, built for presenters, facilitators, and meeting leaders. PulseTimer taps your wrist at the moments that matter so you stay on pace without visibly checking the time.

## What it does

Create timer patterns with customizable haptic vibrations and optional audio cues. Set milestones at key moments in your talk (halfway, wrap-up, final minute), add repeating interval signals to maintain pacing, and let PulseTimer keep you in control from your wrist.

## Platforms

| Platform | Status | Directory |
|----------|--------|-----------|
| Wear OS (Android) | Released | `wear-os/` |
| watchOS (Apple) | In development | `watchos/` |

## Project Structure

```
pulsetimer/
├── docs/                  Shared product documentation
│   ├── requirements.md    Platform-agnostic feature requirements
│   ├── design-decisions.md    Architectural choices and rationale
│   ├── play-store-listing.md  Google Play Store copy
│   └── assets/            Screenshots and graphics
├── wear-os/               Wear OS (Kotlin/Compose) codebase
├── watchos/               watchOS (Swift/SwiftUI) codebase
└── site/                  GitHub Pages (landing page + privacy policy)
```

## Documentation

- [Product Requirements](docs/requirements.md) — what PulseTimer does, platform-agnostic
- [Design Decisions](docs/design-decisions.md) — why things are built the way they are

## Built with AI

PulseTimer was built from concept to working app in a single evening using Claude as a development partner. The full story is on [LinkedIn](https://www.linkedin.com/in/grantlittman/).

## Privacy

PulseTimer collects no data, requires no network access, and stores everything locally on your watch. See the full [privacy policy](https://apps.grantlittman.com/pulsetimer/privacy-policy.html).

## License

All rights reserved. This source code is provided for reference and educational purposes.
