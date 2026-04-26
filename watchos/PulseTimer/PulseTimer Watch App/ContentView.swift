import SwiftUI

enum AppRoute: Hashable {
    case timer
    case createPattern
    case editPattern(patternId: String)
}

struct ContentView: View {
    @Environment(PatternRepository.self) private var repository
    @Environment(TimerEngine.self) private var engine

    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            PatternListView(
                onPatternSelected: { pattern in
                    engine.start(pattern: pattern)
                    repository.markUsed(id: pattern.id)
                    path.append(AppRoute.timer)
                },
                onCreateNew: { path.append(AppRoute.createPattern) },
                onEditPattern: { pattern in
                    path.append(AppRoute.editPattern(patternId: pattern.id))
                }
            )
            .navigationDestination(for: AppRoute.self) { route in
                destination(for: route)
            }
        }
    }

    @ViewBuilder
    private func destination(for route: AppRoute) -> some View {
        switch route {
        case .timer:
            // TimerView dismisses itself via @Environment(\.dismiss) and stops the
            // engine in its own .onDisappear — no closure needed here.
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
            if let original = repository.getById(id) {
                let working = original.isPreset ? makeCopyOfPreset(original) : original
                PatternEditorView(
                    initialPattern: working,
                    canDelete: !original.isPreset,
                    onSave: { saved in
                        repository.save(saved)
                        popLast()
                    },
                    onDelete: {
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
