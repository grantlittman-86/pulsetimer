import WidgetKit
import SwiftUI

/// Watch face complication that quick-launches PulseTimer when tapped.
///
/// Static (single timeline entry) — no time-based updates. Just an icon that the user
/// can place on a watch face slot. Tapping it brings up the app, since watchOS
/// auto-routes complication taps to the parent app for static complications.
///
/// Icon: uses the `waveform.circle` SF Symbol rather than a custom asset. The actual
/// project asset (`ic_complication.png` from the Wear OS side) refused to load via
/// asset-catalog lookup in this widget extension despite being correctly bundled —
/// a known watchOS widget asset quirk. The SF Symbol matches our brand visually
/// (circle + waveform inside) and renders reliably at all sizes.
struct PulseTimerProvider: TimelineProvider {
    func placeholder(in context: Context) -> PulseTimerEntry {
        PulseTimerEntry(date: Date())
    }

    func getSnapshot(in context: Context, completion: @escaping (PulseTimerEntry) -> Void) {
        completion(PulseTimerEntry(date: Date()))
    }

    func getTimeline(in context: Context, completion: @escaping (Timeline<PulseTimerEntry>) -> Void) {
        // Single never-expiring entry. The complication is purely a tap-to-launch target;
        // there's no countdown or live data to render in the slot itself.
        let timeline = Timeline(entries: [PulseTimerEntry(date: Date())], policy: .never)
        completion(timeline)
    }
}

struct PulseTimerEntry: TimelineEntry {
    let date: Date
}

struct PulseTimerComplicationView: View {
    @Environment(\.widgetFamily) private var family

    var body: some View {
        switch family {
        case .accessoryCircular:
            Image(systemName: "waveform.circle.fill")
                .resizable()
                .scaledToFit()
                .foregroundStyle(.cyan)
        case .accessoryCorner:
            Image(systemName: "waveform.circle.fill")
                .resizable()
                .scaledToFit()
                .foregroundStyle(.cyan)
        case .accessoryInline:
            Label("PulseTimer", systemImage: "waveform.circle.fill")
        case .accessoryRectangular:
            HStack(spacing: 6) {
                Image(systemName: "waveform.circle.fill")
                    .foregroundStyle(.cyan)
                    .font(.title2)
                Text("PulseTimer")
                    .font(.headline)
            }
        @unknown default:
            Image(systemName: "waveform.circle.fill")
                .resizable()
                .scaledToFit()
                .foregroundStyle(.cyan)
        }
    }
}

struct PulseTimerComplication: Widget {
    let kind: String = "PulseTimerComplication"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: PulseTimerProvider()) { _ in
            PulseTimerComplicationView()
                .containerBackground(.clear, for: .widget)
        }
        .configurationDisplayName("PulseTimer")
        .description("Quick launch PulseTimer.")
        .supportedFamilies([
            .accessoryCircular,
            .accessoryCorner,
            .accessoryInline,
            .accessoryRectangular
        ])
    }
}

#Preview(as: .accessoryCircular) {
    PulseTimerComplication()
} timeline: {
    PulseTimerEntry(date: .now)
}
