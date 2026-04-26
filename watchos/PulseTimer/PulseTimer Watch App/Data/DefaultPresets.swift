import Foundation

/// Built-in presets seeded on first launch. Cannot be deleted.
///
/// Mirrors the live Wear OS DefaultPresets so the two platforms ship identical defaults.
enum DefaultPresets {
    private static let SECOND: Int64 = 1_000
    private static let MINUTE: Int64 = 60 * 1_000

    static var all: [Pattern] {
        [lightningTalk, presentationThirtyMin, simpleMetronome]
    }

    /// 5-Minute Lightning Talk: tap every minute, double-tap at 4:00, pulse x2 at 5:00.
    static var lightningTalk: Pattern {
        Pattern(
            id: "preset_lightning_talk",
            name: "5-Min Lightning Talk",
            isPreset: true,
            totalDurationMillis: 5 * MINUTE,
            repeatingInterval: RepeatingInterval(
                intervalMillis: 60 * SECOND,
                signal: Signal(hapticType: .tap)
            ),
            milestones: [
                Milestone(
                    triggerAtMillis: 4 * MINUTE,
                    signal: Signal(hapticType: .doubleTap),
                    label: "1 minute left"
                ),
                Milestone(
                    triggerAtMillis: 5 * MINUTE,
                    signal: Signal(hapticType: .pulse),
                    repeatCount: 2,
                    label: "Done"
                )
            ]
        )
    }

    /// 30-Minute Presentation: tap every 5 min, milestones at 15/25/28/30, buzz every 30s in last 2 min.
    static var presentationThirtyMin: Pattern {
        Pattern(
            id: "preset_30min_presentation",
            name: "30-Min Presentation",
            isPreset: true,
            totalDurationMillis: 30 * MINUTE,
            repeatingInterval: RepeatingInterval(
                intervalMillis: 5 * MINUTE,
                signal: Signal(hapticType: .tap)
            ),
            milestones: [
                Milestone(
                    triggerAtMillis: 15 * MINUTE,
                    signal: Signal(hapticType: .doubleTap),
                    label: "Halfway"
                ),
                Milestone(
                    triggerAtMillis: 25 * MINUTE,
                    signal: Signal(hapticType: .pulse),
                    label: "5 min left"
                ),
                Milestone(
                    triggerAtMillis: 28 * MINUTE,
                    signal: Signal(hapticType: .pulse),
                    label: "Wrap up"
                ),
                Milestone(
                    triggerAtMillis: 30 * MINUTE,
                    signal: Signal(hapticType: .pulse),
                    repeatCount: 3,
                    label: "Done"
                )
            ],
            countdownWarning: CountdownWarning(
                activateAtMillis: 2 * MINUTE,
                intervalMillis: 30 * SECOND,
                signal: Signal(hapticType: .buzz)
            )
        )
    }

    /// Simple Metronome: tap every 10 seconds, no milestones, no end (runs until stopped).
    static var simpleMetronome: Pattern {
        Pattern(
            id: "preset_simple_metronome",
            name: "Simple Metronome",
            isPreset: true,
            totalDurationMillis: nil,
            repeatingInterval: RepeatingInterval(
                intervalMillis: 10 * SECOND,
                signal: Signal(hapticType: .tap)
            )
        )
    }
}
