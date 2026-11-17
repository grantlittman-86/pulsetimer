import Foundation

// Host-side doubles keep these lifecycle checks independent of WatchKit hardware.
@MainActor final class SignalExecutor {
    var signals = 0
    func execute(signal: Signal, audioEnabled: Bool = false, repeatCount: Int = 1) { signals += 1 }
    func cancel() {}
}
@MainActor final class RuntimeSessionController {
    var willExpire: (() -> Void)?
    var interrupted: ((String) -> Void)?
    var canContinue = false
    var starts = 0
    func start() { canContinue = true; starts += 1 }
    func stop() { canContinue = false }
}

@main struct TimerEngineChecks {
    @MainActor static func main() async throws {
        let signals = SignalExecutor()
        let runtime = RuntimeSessionController()
        let engine = TimerEngine(signalExecutor: signals, runtimeController: runtime)
        let first = Pattern(name: "First", repeatingInterval: RepeatingInterval(intervalMillis: 100, signal: Signal()))
        let second = Pattern(name: "Second", totalDurationMillis: 200,
                             repeatingInterval: RepeatingInterval(intervalMillis: 100, signal: Signal()))
        engine.start(pattern: first)
        try await Task.sleep(for: .milliseconds(250))
        engine.start(pattern: second)
        precondition(engine.state.patternId == first.id, "Starting another pattern must preserve the current session")
        engine.pause()
        let pausedElapsed = engine.state.elapsedMillis
        precondition(pausedElapsed >= 200 && engine.state.isPaused)
        try await Task.sleep(for: .milliseconds(250))
        precondition(engine.state.elapsedMillis == pausedElapsed, "Paused time must be excluded")
        engine.resume()
        try await Task.sleep(for: .milliseconds(150))
        engine.pause()
        precondition(engine.state.elapsedMillis >= pausedElapsed + 100)
        let finalElapsed = engine.state.elapsedMillis
        engine.stop()
        precondition(engine.state.patternId == nil && engine.state.elapsedMillis == finalElapsed)
        engine.resume()
        precondition(!engine.state.isRunning, "A stopped session must not resume")
        engine.start(pattern: second)
        try await Task.sleep(for: .milliseconds(400))
        precondition(engine.state.isFinished && engine.state.patternId == second.id)
        engine.start(pattern: first)
        precondition(engine.state.patternId == second.id, "Completion must remain protected until Done")
        engine.pause()
        precondition(!engine.state.isPaused)
        engine.stop()
        engine.start(pattern: first)
        precondition(engine.state.patternId == first.id && !engine.state.isFinished)
        engine.stop()
        precondition(signals.signals > 0)

        engine.start(pattern: first)
        try await Task.sleep(for: .milliseconds(150))
        let beforeInterruption = engine.state.elapsedMillis
        let beforeSignals = signals.signals
        runtime.canContinue = false
        runtime.interrupted?("Left app")
        try await Task.sleep(for: .milliseconds(250))
        precondition(engine.state.isPaused && engine.state.interruptionMessage == "Left app")
        precondition(engine.state.elapsedMillis == beforeInterruption && signals.signals == beforeSignals)
        let starts = runtime.starts
        engine.resume()
        precondition(runtime.starts == starts + 1 && engine.state.interruptionMessage == nil)
        try await Task.sleep(for: .milliseconds(150))
        precondition(engine.state.isRunning && engine.state.elapsedMillis > beforeInterruption)

        // Invalidation can be visible before the delegate task executes.
        runtime.canContinue = false
        let beforeInvalidTick = signals.signals
        try await Task.sleep(for: .milliseconds(150))
        precondition(engine.state.isPaused && signals.signals == beforeInvalidTick)
        engine.resume()
        let beforeGap = engine.state.elapsedMillis
        let beforeGapSignals = signals.signals
        blockMainThread()
        try await Task.sleep(for: .milliseconds(150))
        precondition(engine.state.isPaused && engine.state.elapsedMillis == beforeGap)
        precondition(signals.signals == beforeGapSignals, "Suspension must not replay overdue cues")
        engine.stop()
        runtime.interrupted?("Late callback")
        precondition(!engine.state.isPaused && engine.state.interruptionMessage == nil)
        print("PASS: session protection, pause/resume timing, completion, Stop/Done identity, restart, interruption, and overdue-cue suppression")
    }

    private static func blockMainThread() { Thread.sleep(forTimeInterval: 2.2) }
}
