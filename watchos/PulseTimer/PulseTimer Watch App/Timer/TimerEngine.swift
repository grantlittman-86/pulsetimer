import Foundation
import Observation

/// The core timer logic. Tracks elapsed time and decides when to fire
/// repeating intervals, milestones, and countdown warnings.
///
/// UI-agnostic: drives state changes, leaves UI rendering to observers.
/// This is a direct port of Wear OS TimerEngine.kt; behavior should match exactly.
@Observable
@MainActor
final class TimerEngine {
    private static let tickFastMs: Int = 100
    private static let tickSlowMs: Int = 500

    private(set) var state: TimerState = TimerState()

    /// Set by the host (e.g., extended runtime session) to slow the tick rate when
    /// the watch is in always-on/ambient mode.
    var isAmbient: Bool = false

    private let signalExecutor: SignalExecutor
    private let runtimeController = RuntimeSessionController()
    private var tickTask: Task<Void, Never>?
    private var pattern: Pattern?

    private var lastIntervalFiredAt: Int64 = 0
    private var firedMilestoneTriggers: Set<Int64> = []
    private var lastCountdownFiredAt: Int64 = 0

    init(signalExecutor: SignalExecutor) {
        self.signalExecutor = signalExecutor
        // When the system is about to end the extended runtime session, fire a strong
        // warning haptic so the user knows the timer is about to stop tracking.
        self.runtimeController.willExpire = { [weak self] in
            guard let self else { return }
            self.signalExecutor.execute(
                signal: Signal(hapticType: .pulse, intensity: 1.0),
                audioEnabled: false,
                repeatCount: 2
            )
        }
    }

    func start(pattern: Pattern) {
        self.pattern = pattern
        lastIntervalFiredAt = 0
        firedMilestoneTriggers.removeAll()
        lastCountdownFiredAt = 0

        var newState = TimerState()
        newState.isRunning = true
        newState.elapsedMillis = 0
        newState.totalDurationMillis = pattern.totalDurationMillis
        newState.patternName = pattern.name
        newState.nextMilestone = Self.findNextMilestone(elapsed: 0, in: pattern.milestones)
        state = newState

        runtimeController.start()
        startTicking()
    }

    func pause() {
        tickTask?.cancel()
        tickTask = nil
        state.isRunning = false
        state.isPaused = true
    }

    func resume() {
        state.isRunning = true
        state.isPaused = false
        startTicking()
    }

    /// Stop completely. State is preserved (not reset) so the UI can keep displaying
    /// final values during the navigation transition. The next `start()` reinitializes.
    func stop() {
        tickTask?.cancel()
        tickTask = nil
        signalExecutor.cancel()
        runtimeController.stop()
        pattern = nil
        state.isRunning = false
        state.isPaused = false
    }

    private func startTicking() {
        tickTask?.cancel()
        tickTask = Task { @MainActor [weak self] in
            var lastTickTime = Self.nowMillis()
            while !Task.isCancelled {
                guard let self else { return }
                let intervalMs = self.isAmbient ? Self.tickSlowMs : Self.tickFastMs
                do {
                    try await Task.sleep(for: .milliseconds(intervalMs))
                } catch {
                    return
                }
                let now = Self.nowMillis()
                let delta = now - lastTickTime
                lastTickTime = now
                self.tick(delta: delta)
            }
        }
    }

    private func tick(delta: Int64) {
        guard let pattern = self.pattern else { return }
        let newElapsed = state.elapsedMillis + delta

        // Reaching total duration ends the session with a final completion signal.
        if let total = pattern.totalDurationMillis, newElapsed >= total {
            _ = checkMilestones(elapsed: newElapsed, pattern: pattern)
            state.elapsedMillis = total
            state.isRunning = false
            state.nextMilestone = nil
            state.lastSignalFiredAt = Self.nowMillis()
            state.lastSignalWasMilestone = true
            // Single PULSE for completion. Wear OS fires 3, but on Apple Watch the
            // haptic motor sits under the display and back-to-back strong pulses
            // make the Done button briefly unresponsive to taps. One pulse plus the
            // visible "Done!" UI is more than enough as a completion cue.
            signalExecutor.execute(
                signal: Signal(hapticType: .pulse, intensity: 1.0),
                audioEnabled: pattern.audioEnabled,
                repeatCount: 1
            )
            tickTask?.cancel()
            tickTask = nil
            // Intentionally NOT ending the runtime session here. We need it to stay
            // alive through the "Done!" screen so the app remains foreground-active
            // and the Done button responds to the first tap. If we end the session
            // here, the screen sleeps after a few idle seconds and the next tap
            // wakes the app instead of registering the dismissal — making the Done
            // button feel stuck. Session is properly ended in `stop()` when the user
            // taps Done.
            return
        }

        state.elapsedMillis = newElapsed
        state.nextMilestone = Self.findNextMilestone(elapsed: newElapsed, in: pattern.milestones)
        state.inCountdown = Self.isInCountdown(elapsed: newElapsed, pattern: pattern)

        // Milestones take priority over the regular interval on the same tick.
        let milestoneFired = checkMilestones(elapsed: newElapsed, pattern: pattern)
        if !milestoneFired {
            checkRepeatingInterval(elapsed: newElapsed, pattern: pattern)
        }
        checkCountdownWarning(elapsed: newElapsed, pattern: pattern)
    }

    private func checkRepeatingInterval(elapsed: Int64, pattern: Pattern) {
        let interval = pattern.repeatingInterval.intervalMillis
        guard interval > 0 else { return }
        let expectedCount = elapsed / interval
        let firedCount = lastIntervalFiredAt / interval
        guard expectedCount > firedCount else { return }
        lastIntervalFiredAt = expectedCount * interval
        state.lastSignalFiredAt = Self.nowMillis()
        state.lastSignalWasMilestone = false
        signalExecutor.execute(
            signal: pattern.repeatingInterval.signal,
            audioEnabled: pattern.audioEnabled
        )
    }

    @discardableResult
    private func checkMilestones(elapsed: Int64, pattern: Pattern) -> Bool {
        var fired = false
        for milestone in pattern.milestones {
            let trigger = milestone.triggerAtMillis
            guard trigger >= 1, trigger <= elapsed,
                  !firedMilestoneTriggers.contains(trigger) else { continue }
            firedMilestoneTriggers.insert(trigger)
            fired = true
            state.lastSignalFiredAt = Self.nowMillis()
            state.lastSignalWasMilestone = true
            signalExecutor.execute(
                signal: milestone.signal,
                audioEnabled: pattern.audioEnabled,
                repeatCount: milestone.repeatCount
            )
        }
        return fired
    }

    private func checkCountdownWarning(elapsed: Int64, pattern: Pattern) {
        guard let warning = pattern.countdownWarning,
              let total = pattern.totalDurationMillis else { return }
        let countdownStart = max(0, total - warning.activateAtMillis)
        guard elapsed >= countdownStart else { return }

        let elapsedInCountdown = elapsed - countdownStart
        let interval = warning.intervalMillis
        guard interval > 0 else { return }

        let lastElapsedInCountdown = max(0, lastCountdownFiredAt - countdownStart)
        let expectedCount = elapsedInCountdown / interval
        let firedCount = lastElapsedInCountdown / interval
        guard expectedCount > firedCount else { return }

        lastCountdownFiredAt = countdownStart + (expectedCount * interval)
        state.lastSignalFiredAt = Self.nowMillis()
        state.lastSignalWasMilestone = false
        signalExecutor.execute(
            signal: warning.signal,
            audioEnabled: pattern.audioEnabled
        )
    }

    private static func isInCountdown(elapsed: Int64, pattern: Pattern) -> Bool {
        guard let warning = pattern.countdownWarning,
              let total = pattern.totalDurationMillis else { return false }
        return elapsed >= (total - warning.activateAtMillis)
    }

    private static func findNextMilestone(elapsed: Int64, in milestones: [Milestone]) -> Milestone? {
        milestones
            .filter { $0.triggerAtMillis > elapsed }
            .min { $0.triggerAtMillis < $1.triggerAtMillis }
    }

    private static func nowMillis() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}
