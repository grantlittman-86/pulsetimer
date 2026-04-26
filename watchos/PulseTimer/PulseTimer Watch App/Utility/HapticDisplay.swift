import Foundation

/// User-facing display name for a HapticType. Used by the test view, milestone rows,
/// and any picker that lists haptic types.
func displayName(for haptic: HapticType) -> String {
    switch haptic {
    case .tap: return "Tap"
    case .buzz: return "Buzz"
    case .pulse: return "Pulse"
    case .doubleTap: return "Double Tap"
    }
}
