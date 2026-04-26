import SwiftUI

/// Row that shows the current duration value and pushes a wheel-picker screen
/// where the user can dial in any hour/minute/second combination.
///
/// Setting `allowHours` or `allowSeconds` to false hides the corresponding wheel
/// for contexts where that precision isn't useful (e.g., interval pickers don't need
/// hours; total-duration pickers don't need sub-second precision either).
struct DurationPickerView: View {
    @Binding var millis: Int64
    let label: String
    var allowHours: Bool = false
    var allowSeconds: Bool = true

    var body: some View {
        NavigationLink {
            DurationWheelScreen(
                millis: $millis,
                allowHours: allowHours,
                allowSeconds: allowSeconds
            )
            .navigationTitle(label)
        } label: {
            HStack {
                Text(label)
                Spacer()
                Text(formatDurationShort(millis: millis))
                    .foregroundStyle(.secondary)
            }
        }
    }
}

/// The push-screen with H/M/S wheel pickers. Mirrors how Apple's native Timer app
/// lets you set arbitrary durations.
private struct DurationWheelScreen: View {
    @Binding var millis: Int64
    let allowHours: Bool
    let allowSeconds: Bool

    @State private var hours: Int = 0
    @State private var minutes: Int = 0
    @State private var seconds: Int = 0

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            if allowHours {
                wheel(value: $hours, range: 0..<24, suffix: "h")
            }
            wheel(value: $minutes, range: 0..<60, suffix: "m")
            if allowSeconds {
                wheel(value: $seconds, range: 0..<60, suffix: "s")
            }
        }
        .onAppear { syncFromMillis() }
        .onChange(of: hours) { _, _ in writeMillis() }
        .onChange(of: minutes) { _, _ in writeMillis() }
        .onChange(of: seconds) { _, _ in writeMillis() }
    }

    @ViewBuilder
    private func wheel(value: Binding<Int>, range: Range<Int>, suffix: String) -> some View {
        VStack(spacing: 2) {
            Picker("", selection: value) {
                ForEach(range, id: \.self) { i in
                    Text(String(format: "%02d", i)).tag(i)
                }
            }
            .pickerStyle(.wheel)
            .labelsHidden()
            Text(suffix)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    private func syncFromMillis() {
        let total = Int(max(0, millis / 1000))
        hours = total / 3600
        minutes = (total % 3600) / 60
        seconds = total % 60
    }

    private func writeMillis() {
        let total = Int64(hours) * 3600 + Int64(minutes) * 60 + Int64(seconds)
        millis = total * 1000
    }
}
