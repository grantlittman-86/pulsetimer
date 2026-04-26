import Foundation

/// MM:SS or H:MM:SS clock format. Used for the main time readout.
func formatTime(millis: Int64) -> String {
    let totalSeconds = Int(max(0, millis / 1000))
    let hours = totalSeconds / 3600
    let minutes = (totalSeconds % 3600) / 60
    let seconds = totalSeconds % 60
    if hours > 0 {
        return String(format: "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        return String(format: "%02d:%02d", minutes, seconds)
    }
}

/// Compact human-readable duration (e.g., "5 min", "1m 30s", "2h 15m").
/// Used for pattern summaries and "Next milestone in X" hints.
func formatDurationShort(millis: Int64) -> String {
    let totalSeconds = Int(max(0, millis / 1000))
    let minutes = totalSeconds / 60
    let seconds = totalSeconds % 60

    if minutes >= 60 {
        let hours = minutes / 60
        let remainingMin = minutes % 60
        return remainingMin > 0 ? "\(hours)h \(remainingMin)m" : "\(hours)h"
    } else if minutes > 0 && seconds > 0 {
        return "\(minutes)m \(seconds)s"
    } else if minutes > 0 {
        return "\(minutes) min"
    } else {
        return "\(seconds)s"
    }
}
