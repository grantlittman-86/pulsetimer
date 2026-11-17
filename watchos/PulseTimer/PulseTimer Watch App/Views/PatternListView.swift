import SwiftUI

/// Home screen showing all saved patterns and presets.
/// Sorted: most recently used first, then presets, then alphabetical.
struct PatternListView: View {
    @Environment(PatternRepository.self) private var repository
    @Environment(TimerEngine.self) private var engine
    let onPatternSelected: (Pattern) -> Void
    let onViewTimer: () -> Void
    let onCreateNew: () -> Void
    let onEditPattern: (Pattern) -> Void

    private var sortedPatterns: [Pattern] {
        repository.patterns.sorted { lhs, rhs in
            let lhsUsed = lhs.lastUsedAt ?? 0
            let rhsUsed = rhs.lastUsedAt ?? 0
            if lhsUsed != rhsUsed { return lhsUsed > rhsUsed }
            if lhs.isPreset != rhs.isPreset { return lhs.isPreset && !rhs.isPreset }
            return lhs.name.localizedCompare(rhs.name) == .orderedAscending
        }
    }

    var body: some View {
        List {
            if engine.state.patternId != nil {
                Button(action: onViewTimer) {
                    Label("View timer", systemImage: "timer")
                }
            }
            Button(action: onCreateNew) {
                HStack {
                    Image(systemName: "plus.circle")
                    Text("New Pattern")
                        .font(.subheadline)
                    Spacer()
                }
            }
            .buttonStyle(.plain)

            ForEach(sortedPatterns) { pattern in
                Button {
                    onPatternSelected(pattern)
                } label: {
                    PatternRow(pattern: pattern, sessionStatus: sessionStatus(for: pattern))
                }
                .buttonStyle(.plain)
                .listRowBackground(engine.state.patternId == pattern.id
                    ? Color.accentColor.opacity(0.2) : Color.clear)
                .swipeActions(edge: .trailing) {
                    if engine.state.patternId != pattern.id {
                        if !pattern.isPreset {
                            Button(role: .destructive) {
                                guard engine.state.patternId != pattern.id else { return }
                                repository.delete(pattern)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                        Button {
                            onEditPattern(pattern)
                        } label: {
                            Label("Edit", systemImage: "pencil")
                        }
                        .tint(.blue)
                    }
                }
            }
        }
        .navigationTitle("PulseTimer")
    }

    private func sessionStatus(for pattern: Pattern) -> String? {
        guard engine.state.patternId == pattern.id else { return nil }
        if engine.state.isFinished { return "Completed" }
        return engine.state.isPaused ? "Paused" : "Running"
    }
}

private struct PatternRow: View {
    let pattern: Pattern
    let sessionStatus: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(pattern.name)
                    .font(.headline)
                    .lineLimit(1)
                Spacer(minLength: 4)
                if pattern.isPreset {
                    Text("PRESET")
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }
            if let sessionStatus {
                Text(sessionStatus)
                    .font(.caption2.bold())
                    .foregroundStyle(Color.accentColor)
                Label("View timer", systemImage: "timer")
                    .font(.caption2)
                    .foregroundStyle(.primary)
            }
            Text(summary(for: pattern))
                .font(.caption2)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .padding(.vertical, 2)
    }

    private func summary(for p: Pattern) -> String {
        var parts: [String] = []
        if let total = p.totalDurationMillis {
            parts.append(formatDurationShort(millis: total))
        } else {
            parts.append("No limit")
        }
        parts.append("every \(formatDurationShort(millis: p.repeatingInterval.intervalMillis))")
        if !p.milestones.isEmpty {
            parts.append("\(p.milestones.count) milestone\(p.milestones.count == 1 ? "" : "s")")
        }
        return parts.joined(separator: " · ")
    }
}
