import Foundation

/// Observable state of the running timer, exposed to the UI.
struct TimerState: Equatable {
    var isRunning: Bool = false
    var isPaused: Bool = false
    /// Total elapsed time in milliseconds (pauses excluded).
    var elapsedMillis: Int64 = 0
    /// Total duration in milliseconds, or nil if indefinite.
    var totalDurationMillis: Int64?
    var patternName: String = ""
    var nextMilestone: Milestone?
    var inCountdown: Bool = false
    /// Wall-clock millis of the last signal fire. UI watches this value as a change
    /// trigger to play a brief flash animation when a new signal fires.
    var lastSignalFiredAt: Int64 = 0
    var lastSignalWasMilestone: Bool = false

    /// Time remaining in milliseconds, or nil if indefinite.
    var remainingMillis: Int64? {
        guard let total = totalDurationMillis else { return nil }
        return max(0, total - elapsedMillis)
    }

    /// Whether the session has reached its total duration.
    var isFinished: Bool {
        guard let total = totalDurationMillis else { return false }
        return elapsedMillis >= total
    }
}
