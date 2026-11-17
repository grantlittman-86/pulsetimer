import SwiftUI

/// Active timer screen shown while a pattern is running.
///
/// Adapts to always-on display via `\.isLuminanceReduced` rather than maintaining
/// two separate layouts: dimmer colors and hidden controls when the wrist is down.
///
/// Back navigation preserves the session. Stop, Done, and completion auto-dismiss
/// explicitly end it while retaining the hardware-tested Done interaction.
///
/// Why the TimelineView wrapping the content: SwiftUI's hit-testing on watchOS becomes
/// unreliable when the view stops re-rendering. During an active timer, `elapsedMillis`
/// updates every 100ms which forces constant body re-renders and keeps hit-testing
/// alive. After timer completion, state is frozen — body re-renders stop entirely, and
/// taps on the Done button silently fail to register (verified empirically: 5+ taps
/// during a 5-second post-completion idle period registered zero events). Wrapping the
/// content in a TimelineView with a periodic 0.1s schedule forces a continuous re-render
/// cadence regardless of state, which keeps the button reliably tappable.
struct TimerView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(TimerEngine.self) private var engine
    @Environment(\.isLuminanceReduced) private var isLuminanceReduced

    /// How long to leave the "Done!" screen visible before auto-dismissing.
    private static let autoDismissAfterCompletionSec: Int = 10

    @State private var flashActive = false
    @State private var lastSeenSignalAt: Int64 = 0

    var body: some View {
        ZStack {
            flashBackground
                .ignoresSafeArea()
                // Cosmetic overlay only — never intercept taps.
                .allowsHitTesting(false)
            // Periodic re-render keeps the view "warm" so taps register reliably.
            TimelineView(.periodic(from: .now, by: 0.1)) { _ in
                ScrollView {
                    content
                }
            }
        }
        .navigationTitle(engine.state.patternName)
        .navigationBarTitleDisplayMode(.inline)
        .onChange(of: engine.state.lastSignalFiredAt) { _, newValue in
            triggerFlash(if: newValue)
        }
        .task(id: engine.state.isFinished) {
            if engine.state.isFinished {
                try? await Task.sleep(for: .seconds(Self.autoDismissAfterCompletionSec))
                if !Task.isCancelled {
                    stopAndDismiss()
                }
            }
        }
    }

    private func stopAndDismiss() {
        engine.stop()
        dismiss()
    }

    private var flashBackground: some View {
        let color: Color
        if !flashActive || isLuminanceReduced {
            color = .clear
        } else if engine.state.inCountdown {
            color = .red.opacity(0.30)
        } else if engine.state.lastSignalWasMilestone {
            color = .orange.opacity(0.35)
        } else {
            color = .blue.opacity(0.25)
        }
        return color.animation(.easeInOut(duration: flashActive ? 0.1 : 0.3), value: flashActive)
    }

    private var content: some View {
        let state = engine.state
        return VStack(spacing: 4) {
            timeDisplay(state)

            if let total = state.totalDurationMillis, total > 0, !state.isFinished {
                let pct = Int(Double(state.elapsedMillis) / Double(total) * 100)
                Text("\(min(100, max(0, pct)))%")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            milestoneHint(state)

            if state.isFinished {
                Text("Done!")
                    .font(.headline)
                    .foregroundStyle(.red)
            }

            controls(state)
                .padding(.top, 4)
        }
        .padding(.horizontal, 8)
        .padding(.top, 4)
        .padding(.bottom, 12)
    }

    @ViewBuilder
    private func timeDisplay(_ state: TimerState) -> some View {
        let displayFinished = state.isFinished ||
            (state.totalDurationMillis != nil && (state.remainingMillis ?? 1) <= 0)
        let timeColor: Color = isLuminanceReduced
            ? .white
            : (state.isFinished || state.inCountdown ? .red : .blue)

        if displayFinished, let total = state.totalDurationMillis {
            Text(formatTime(millis: total))
                .font(.system(size: 34, weight: .bold, design: .rounded))
                .foregroundStyle(timeColor)
            Text("completed")
                .font(.caption2)
                .foregroundStyle(.secondary)
        } else if let _ = state.totalDurationMillis {
            Text(formatTime(millis: state.remainingMillis ?? 0))
                .font(.system(size: 34, weight: .bold, design: .rounded))
                .foregroundStyle(timeColor)
            Text("remaining")
                .font(.caption2)
                .foregroundStyle(.secondary)
        } else {
            Text(formatTime(millis: state.elapsedMillis))
                .font(.system(size: 34, weight: .bold, design: .rounded))
                .foregroundStyle(timeColor)
            Text("elapsed")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    @ViewBuilder
    private func milestoneHint(_ state: TimerState) -> some View {
        if let next = state.nextMilestone {
            let timeUntil = next.triggerAtMillis - state.elapsedMillis
            if timeUntil > 0 {
                let label = next.label ?? "Next"
                Text("\(label) in \(formatDurationShort(millis: timeUntil))")
                    .font(.caption2)
                    .foregroundStyle(isLuminanceReduced ? .gray : .yellow)
                    .lineLimit(1)
            }
        }
    }

    @ViewBuilder
    private func controls(_ state: TimerState) -> some View {
        if isLuminanceReduced {
            EmptyView()
        } else {
            HStack(spacing: 8) {
                if state.isFinished {
                    // Manual tap-target instead of SwiftUI Button. After empirical
                    // testing on hardware, SwiftUI Button has unreliable hit-testing
                    // in the post-timer-completion state on watchOS — taps would
                    // silently fail to register. A manual `.onTapGesture` on a
                    // colored shape is rock-solid in the same conditions.
                    Color.accentColor
                        .clipShape(.rect(cornerRadius: 10))
                        .frame(maxWidth: .infinity, minHeight: 36)
                        .overlay {
                            Text("Done")
                                .font(.headline)
                                .foregroundStyle(.white)
                        }
                        .contentShape(.rect)
                        .onTapGesture {
                            stopAndDismiss()
                        }
                } else if state.isPaused {
                    Button {
                        engine.resume()
                    } label: {
                        controlLabel("Resume", color: .accentColor)
                    }
                    .buttonStyle(.plain)
                    Button {
                        stopAndDismiss()
                    } label: {
                        controlLabel("Stop", color: .red)
                    }
                    .buttonStyle(.plain)
                } else if state.isRunning {
                    Button {
                        engine.pause()
                    } label: {
                        controlLabel("Pause", color: .accentColor)
                    }
                    .buttonStyle(.plain)
                    Button {
                        stopAndDismiss()
                    } label: {
                        controlLabel("Stop", color: .red)
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }

    private func controlLabel(_ title: String, color: Color) -> some View {
        Text(title)
            .font(.system(size: 15, weight: .semibold))
            .lineLimit(1)
            .minimumScaleFactor(0.8)
            .frame(maxWidth: .infinity)
            .frame(height: 36)
            .foregroundStyle(.white)
            .background(color, in: RoundedRectangle(cornerRadius: 10))
            .contentShape(.rect)
    }

    private func triggerFlash(if signalAt: Int64) {
        guard signalAt > 0, signalAt != lastSeenSignalAt else { return }
        lastSeenSignalAt = signalAt
        flashActive = true
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(400))
            flashActive = false
        }
    }
}
