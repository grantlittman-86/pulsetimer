import WidgetKit
import SwiftUI

/// Watch face complication that quick-launches PulseTimer when tapped.
///
/// Static (single timeline entry) — no time-based updates. Just an icon that the user
/// can place on a watch face slot. Tapping it brings up the app, since watchOS
/// auto-routes complication taps to the parent app for static complications.
///
/// The brand mark is drawn with SwiftUI paths, avoiding the asset-catalog lookup
/// failure previously observed on the physical Watch.
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

/// Geometry follows the original 108-unit launcher artwork, cropped to its
/// 72-unit safe zone. No background is drawn on the watch face.
private struct PulseTimerMark: View {
    @Environment(\.widgetRenderingMode) private var renderingMode

    var body: some View {
        GeometryReader { geometry in
            let size = min(geometry.size.width, geometry.size.height)
            let scale = size / 72
            ZStack {
                Path { path in
                    path.addEllipse(in: CGRect(x: 10 * scale, y: 10 * scale,
                                               width: 52 * scale, height: 52 * scale))
                }
                .stroke(renderingMode == .fullColor
                        ? Color(red: 79/255, green: 195/255, blue: 247/255) : .white,
                        lineWidth: 4 * scale)
                Path { path in
                    for rect in [CGRect(x: 34.5, y: 10, width: 3, height: 6),
                                 CGRect(x: 56, y: 34.5, width: 6, height: 3),
                                 CGRect(x: 34.5, y: 56, width: 3, height: 6),
                                 CGRect(x: 10, y: 34.5, width: 6, height: 3)] {
                        path.addRect(rect.applying(CGAffineTransform(scaleX: scale, y: scale)))
                    }
                }
                .fill(renderingMode == .fullColor
                      ? Color(red: 79/255, green: 195/255, blue: 247/255) : .white)
                Path { path in
                    let points: [CGPoint] = [CGPoint(x: 16, y: 36), CGPoint(x: 26, y: 36),
                        CGPoint(x: 29, y: 28), CGPoint(x: 32, y: 44), CGPoint(x: 35, y: 24),
                        CGPoint(x: 38, y: 44), CGPoint(x: 41, y: 28), CGPoint(x: 44, y: 36),
                        CGPoint(x: 56, y: 36)]
                    path.addLines(points.map { CGPoint(x: $0.x * scale, y: $0.y * scale) })
                }
                .stroke(renderingMode == .fullColor
                        ? Color(red: 1, green: 183/255, blue: 77/255) : .white,
                        style: StrokeStyle(lineWidth: 2.5 * scale, lineCap: .round, lineJoin: .round))
                .widgetAccentable()
            }
            .frame(width: size, height: size)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .aspectRatio(1, contentMode: .fit)
        .accessibilityHidden(true)
    }
}

struct PulseTimerComplicationView: View {
    @Environment(\.widgetFamily) private var family

    var body: some View {
        Group {
            switch family {
            case .accessoryInline:
                // Inline slots support text and system-symbol labels reliably.
                Label("PulseTimer", systemImage: "timer")
            case .accessoryRectangular:
                HStack(spacing: 6) {
                    PulseTimerMark().frame(width: 32, height: 32)
                    Text("PulseTimer").font(.headline).minimumScaleFactor(0.8).lineLimit(1)
                }
            default:
                PulseTimerMark()
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Open PulseTimer")
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
