import SwiftUI

/// Home screen showing all saved patterns and presets.
/// Sorted: most recently used first, then presets, then alphabetical.
struct PatternListView: View {
    @Environment(PatternRepository.self) private var repository
    let onPatternSelected: (Pattern) -> Void
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
                    PatternRow(pattern: pattern)
                }
                .buttonStyle(.plain)
                .swipeActions(edge: .trailing) {
                    if !pattern.isPreset {
                        Button(role: .destructive) {
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
        .navigationTitle("PulseTimer")
    }
}

private struct PatternRow: View {
    let pattern: Pattern

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
