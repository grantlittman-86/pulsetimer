import Foundation

/// The core heartbeat of a pattern: a signal that fires at a fixed recurring interval.
struct RepeatingInterval: Codable, Equatable, Hashable {
    var intervalMillis: Int64
    var signal: Signal
}
