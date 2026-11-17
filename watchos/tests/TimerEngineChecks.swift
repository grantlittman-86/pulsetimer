import Foundation

// Host-side doubles keep these lifecycle checks independent of WatchKit hardware.
@MainActor final class SignalExecutor {
    var signals = 0
    func execute(signal: Signal, audioEnabled: Bool = false, repeatCount: Int = 1) { signals += 1 }
    func cancel() {}
}
@MainActor final class RuntimeSessionController {
    var willExpire: (() -> Void)?
    func start() {}
    func stop() {}
}

@main struct TimerEngineChecks {
    @MainActor static func main() async throws {
        let signals = SignalExecutor()
        let engine = TimerEngine(signalExecutor: signals)
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
        print("PASS: session protection, pause/resume timing, completion, Stop/Done identity, and restart")
    }
}
