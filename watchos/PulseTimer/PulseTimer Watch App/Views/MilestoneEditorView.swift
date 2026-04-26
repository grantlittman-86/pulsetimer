import SwiftUI

/// Sub-screen for editing a single milestone within a pattern.
/// Operates on a binding so changes flow back to the parent editor's milestone array.
struct MilestoneEditorView: View {
    @Environment(SignalExecutor.self) private var executor
    @Binding var milestone: Milestone

    var body: some View {
        Form {
            Section("Trigger Time") {
                DurationPickerView(
                    millis: Binding(
                        get: { milestone.triggerAtMillis },
                        set: { milestone.triggerAtMillis = $0 }
                    ),
                    label: "At",
                    allowHours: true,
                    allowSeconds: true
                )
            }

            Section("Signal") {
                HapticTypePickerView(
                    hapticType: Binding(
                        get: { milestone.signal.hapticType },
                        set: {
                            var sig = milestone.signal
                            sig.hapticType = $0
                            milestone.signal = sig
                        }
                    ),
                    label: "Haptic"
                )
                Button("Try haptic") {
                    executor.execute(signal: milestone.signal)
                }
            }

            Section("Repeat") {
                Picker(selection: Binding(
                    get: { milestone.repeatCount },
                    set: { milestone.repeatCount = $0 }
                )) {
                    ForEach(1...5, id: \.self) { n in
                        Text("\(n)x").tag(n)
                    }
                } label: {
                    Text("Repeat")
                }
                .pickerStyle(.navigationLink)
            }

            Section("Label") {
                TextField("Optional", text: Binding(
                    get: { milestone.label ?? "" },
                    set: { milestone.label = $0.isEmpty ? nil : $0 }
                ))
            }
        }
        .navigationTitle(milestone.label.flatMap { $0.isEmpty ? nil : $0 } ?? "Milestone")
    }
}
