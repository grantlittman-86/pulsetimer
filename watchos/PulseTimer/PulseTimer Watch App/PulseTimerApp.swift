import SwiftUI

@main
struct PulseTimer_Watch_AppApp: App {
    @State private var repository: PatternRepository
    @State private var signalExecutor: SignalExecutor
    @State private var engine: TimerEngine

    init() {
        let executor = SignalExecutor()
        self._repository = State(initialValue: PatternRepository())
        self._signalExecutor = State(initialValue: executor)
        self._engine = State(initialValue: TimerEngine(signalExecutor: executor))
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(repository)
                .environment(signalExecutor)
                .environment(engine)
                .task { repository.seedPresetsIfNeeded() }
        }
    }
}
