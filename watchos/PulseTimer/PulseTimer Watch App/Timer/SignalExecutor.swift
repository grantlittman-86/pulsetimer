import Foundation
import WatchKit
import Observation

/// Plays haptic signals on the watch via `WKHapticType`.
///
/// Important watchOS constraint: there is no public API to suppress the system audio
/// that plays alongside any `WKHapticType`. Core Haptics, which would solve this, is
/// iOS-only. The user-facing mechanism for silent operation is the watch's Silent Mode
/// (Control Center → bell icon). When Silent Mode is on, every WKHapticType becomes
/// haptic-only, which is the discreet behavior PulseTimer is designed for.
///
/// As a side effect, `Signal.intensity` is preserved through serialization for
/// cross-platform compatibility but ignored at playback: WKHapticType has no intensity
/// parameter.
@Observable
@MainActor
final class SignalExecutor {
    @ObservationIgnored private var currentTask: Task<Void, Never>?

    /// Fire a signal: play haptic now, optionally play audio, repeat as requested.
    /// A new call cancels any in-progress repeat sequence from a previous call.
    func execute(signal: Signal, audioEnabled: Bool = false, repeatCount: Int = 1) {
        cancel()
        let count = max(1, repeatCount)
        currentTask = Task { @MainActor [signal, audioEnabled, count] in
            for i in 0..<count {
                if Task.isCancelled { return }
                await SignalExecutor.playHaptic(signal.hapticType)
                if audioEnabled {
                    SignalExecutor.playAudio(signal.audioType ?? .beep)
                }
                if i < count - 1 {
                    try? await Task.sleep(for: .milliseconds(SignalExecutor.repeatGap(signal.hapticType)))
                }
            }
        }
    }

    /// Cancel any in-progress repeat sequence. Already-issued WKInterfaceDevice plays
    /// cannot be unqueued by the OS, so a haptic that has just fired may still complete.
    func cancel() {
        currentTask?.cancel()
        currentTask = nil
    }

    /// Mapping from PulseTimer's haptic vocabulary to watchOS WKHapticType.
    /// Validated on Apple Watch SE 2nd gen: `.start` for TAP feels like a reliable
    /// wrist-presence single thump (`.click` was too subtle).
    private static func playHaptic(_ type: HapticType) async {
        let device = WKInterfaceDevice.current()
        switch type {
        case .tap:
            device.play(.start)
        case .buzz:
            device.play(.notification)
        case .pulse:
            device.play(.success)
        case .doubleTap:
            device.play(.start)
            try? await Task.sleep(for: .milliseconds(150))
            device.play(.start)
        }
    }

    /// Audio playback is not yet implemented. Audio is opt-in per pattern (default off).
    /// Implementing this requires bundling short audio assets and configuring AVAudioSession.
    private static func playAudio(_ type: AudioType) {
        // Intentionally empty for v1.
    }

    /// Pause between repeats so they don't blur together on the wrist.
    private static func repeatGap(_ type: HapticType) -> Int {
        switch type {
        case .tap: return 300
        case .buzz: return 400
        case .pulse: return 700
        case .doubleTap: return 500
        }
    }
}
