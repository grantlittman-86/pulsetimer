import SwiftUI

/// Main editor for a Pattern. Creates a new pattern when initialPattern has fresh
/// defaults, or edits an existing one when initialPattern is loaded from the repo.
///
/// Edits are held locally in @State and only flushed back to the repository via
/// `onSave`. Cancel discards in-flight changes.
///
/// Editing a preset is handled at the routing layer: the caller passes a non-preset
/// copy of the preset as `initialPattern`. From the editor's perspective, all patterns
/// are non-preset working copies.
struct PatternEditorView: View {
    @Environment(SignalExecutor.self) private var executor

    let initialPattern: Pattern
    let canDelete: Bool
    let onSave: (Pattern) -> Void
    let onDelete: () -> Void
    let onCancel: () -> Void
    /// Callback to push a milestone editor onto the navigation stack.
    /// The path is owned by ContentView; this lets the editor request navigation.
    let onPushMilestone: (String) -> Void

    @State private var name: String
    @State private var hasDuration: Bool
    @State private var totalDurationMillis: Int64
    @State private var intervalMillis: Int64
    @State private var intervalHaptic: HapticType
    @State private var milestones: [Milestone]
    @State private var hasCountdown: Bool
    @State private var countdownActivateAtMillis: Int64
    @State private var countdownIntervalMillis: Int64
    @State private var countdownHaptic: HapticType
    @State private var audioEnabled: Bool

    init(initialPattern: Pattern,
         canDelete: Bool,
         onSave: @escaping (Pattern) -> Void,
         onDelete: @escaping () -> Void,
         onCancel: @escaping () -> Void,
         onPushMilestone: @escaping (String) -> Void) {
        self.initialPattern = initialPattern
        self.canDelete = canDelete
        self.onSave = onSave
        self.onDelete = onDelete
        self.onCancel = onCancel
        self.onPushMilestone = onPushMilestone

        _name = State(initialValue: initialPattern.name)
        _hasDuration = State(initialValue: initialPattern.totalDurationMillis != nil)
        _totalDurationMillis = State(initialValue: initialPattern.totalDurationMillis ?? 10 * 60 * 1000)
        _intervalMillis = State(initialValue: initialPattern.repeatingInterval.intervalMillis)
        _intervalHaptic = State(initialValue: initialPattern.repeatingInterval.signal.hapticType)
        _milestones = State(initialValue: initialPattern.milestones)
        _hasCountdown = State(initialValue: initialPattern.countdownWarning != nil)
        _countdownActivateAtMillis = State(initialValue: initialPattern.countdownWarning?.activateAtMillis ?? 60_000)
        _countdownIntervalMillis = State(initialValue: initialPattern.countdownWarning?.intervalMillis ?? 10_000)
        _countdownHaptic = State(initialValue: initialPattern.countdownWarning?.signal.hapticType ?? .tap)
        _audioEnabled = State(initialValue: initialPattern.audioEnabled)
    }

    var body: some View {
        Form {
            Section("Name") {
                TextField("Name", text: $name)
            }

            Section("Duration") {
                Toggle("Set total", isOn: $hasDuration)
                if hasDuration {
                    DurationPickerView(
                        millis: $totalDurationMillis,
                        label: "Total",
                        allowHours: true,
                        allowSeconds: true
                    )
                }
            }

            Section("Interval") {
                DurationPickerView(
                    millis: $intervalMillis,
                    label: "Every",
                    allowHours: false,
                    allowSeconds: true
                )
                HapticTypePickerView(hapticType: $intervalHaptic, label: "Haptic")
                Button("Try haptic") {
                    executor.execute(signal: Signal(hapticType: intervalHaptic))
                }
            }

            Section("Milestones") {
                ForEach(milestones) { milestone in
                    Button {
                        onPushMilestone(milestone.id)
                    } label: {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(milestone.label?.isEmpty == false ? milestone.label! : "Milestone")
                                .font(.subheadline)
                            Text("\(formatTime(millis: milestone.triggerAtMillis)) · \(displayName(for: milestone.signal.hapticType))")
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .buttonStyle(.plain)
                }
                .onDelete { indices in
                    milestones.remove(atOffsets: indices)
                }
                Button("+ Add milestone") {
                    let new = Milestone(
                        triggerAtMillis: 60_000,
                        signal: Signal(hapticType: .tap)
                    )
                    milestones.append(new)
                    onPushMilestone(new.id)
                }
            }

            Section("Countdown") {
                Toggle("Enable", isOn: $hasCountdown)
                if hasCountdown {
                    DurationPickerView(
                        millis: $countdownActivateAtMillis,
                        label: "Start at",
                        allowHours: false,
                        allowSeconds: true
                    )
                    DurationPickerView(
                        millis: $countdownIntervalMillis,
                        label: "Every",
                        allowHours: false,
                        allowSeconds: true
                    )
                    HapticTypePickerView(hapticType: $countdownHaptic, label: "Haptic")
                    Button("Try haptic") {
                        executor.execute(signal: Signal(hapticType: countdownHaptic))
                    }
                }
            }

            Section("Audio") {
                Toggle("Enabled", isOn: $audioEnabled)
            }

            Section {
                Button("Save", action: saveAndDismiss)
                Button("Cancel", action: onCancel)
                if canDelete {
                    Button("Delete", role: .destructive, action: onDelete)
                }
            }
        }
        .navigationTitle(name.isEmpty ? "Pattern" : name)
        .navigationDestination(for: String.self) { milestoneId in
            if let idx = milestones.firstIndex(where: { $0.id == milestoneId }) {
                MilestoneEditorView(milestone: $milestones[idx])
            }
        }
    }

    private func saveAndDismiss() {
        var saved = initialPattern
        saved.name = name.isEmpty ? "Untitled" : name
        saved.totalDurationMillis = hasDuration ? totalDurationMillis : nil
        saved.repeatingInterval = RepeatingInterval(
            intervalMillis: intervalMillis,
            signal: Signal(hapticType: intervalHaptic)
        )
        saved.milestones = milestones
        saved.countdownWarning = hasCountdown ? CountdownWarning(
            activateAtMillis: countdownActivateAtMillis,
            intervalMillis: countdownIntervalMillis,
            signal: Signal(hapticType: countdownHaptic)
        ) : nil
        saved.audioEnabled = audioEnabled
        onSave(saved)
    }
}
