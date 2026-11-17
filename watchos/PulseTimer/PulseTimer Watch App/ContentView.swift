import SwiftUI

enum AppRoute: Hashable {
    case timer
    case createPattern
    case editPattern(patternId: String)
}

struct ContentView: View {
    @Environment(PatternRepository.self) private var repository
    @Environment(TimerEngine.self) private var engine

    @State private var showActiveSessionAlert = false
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            PatternListView(
                onPatternSelected: { pattern in
                    if let activeId = engine.state.patternId {
                        if activeId == pattern.id {
                            path.append(AppRoute.timer)
                        } else {
                            showActiveSessionAlert = true
                        }
                        return
                    }
                    engine.start(pattern: pattern)
                    repository.markUsed(id: pattern.id)
                    path.append(AppRoute.timer)
                },
                onViewTimer: { path.append(AppRoute.timer) },
                onCreateNew: { path.append(AppRoute.createPattern) },
                onEditPattern: { pattern in
                    guard engine.state.patternId != pattern.id else { return }
                    path.append(AppRoute.editPattern(patternId: pattern.id))
                }
            )
            .navigationDestination(for: AppRoute.self) { route in
                destination(for: route)
            }
        }
        .alert("Timer session active", isPresented: $showActiveSessionAlert) {
            Button("View timer") { path.append(AppRoute.timer) }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Tap Stop or Done in the current timer before starting another pattern.")
        }
    }

    @ViewBuilder
    private func destination(for route: AppRoute) -> some View {
        switch route {
        case .timer:
            TimerView()
        case .createPattern:
            PatternEditorView(
                initialPattern: defaultNewPattern(),
                canDelete: false,
                onSave: { saved in
                    repository.save(saved)
                    popLast()
                },
                onDelete: {},
                onCancel: { popLast() },
                onPushMilestone: { milestoneId in path.append(milestoneId) }
            )
        case .editPattern(let id):
            if let original = repository.getById(id), engine.state.patternId != id {
                let working = original.isPreset ? makeCopyOfPreset(original) : original
                PatternEditorView(
                    initialPattern: working,
                    canDelete: !original.isPreset,
                    onSave: { saved in
                        guard engine.state.patternId != id else { return }
                        repository.save(saved)
                        popLast()
                    },
                    onDelete: {
                        guard engine.state.patternId != id else { return }
                        repository.delete(original)
                        popLast()
                    },
                    onCancel: { popLast() },
                    onPushMilestone: { milestoneId in path.append(milestoneId) }
                )
            } else {
                Text("Pattern not found")
            }
        }
    }

    private func popLast() {
        if !path.isEmpty {
            path.removeLast()
        }
    }

    private func defaultNewPattern() -> Pattern {
        Pattern(
            name: "My Pattern",
            totalDurationMillis: 10 * 60 * 1000,
            repeatingInterval: RepeatingInterval(
                intervalMillis: 60_000,
                signal: Signal(hapticType: .tap)
            )
        )
    }

    private func makeCopyOfPreset(_ preset: Pattern) -> Pattern {
        var copy = preset
        copy.id = UUID().uuidString
        copy.name = "\(preset.name) (Custom)"
        copy.isPreset = false
        copy.lastUsedAt = nil
        return copy
    }
}
