import Foundation

/// A distinct signal pattern that activates during the final stretch of a session.
///
/// `activateAtMillis` is the number of milliseconds *before* the end of the session
/// to begin the countdown (e.g., 120_000 = activate with 2 minutes remaining).
struct CountdownWarning: Codable, Equatable, Hashable {
    var activateAtMillis: Int64
    var intervalMillis: Int64
    var signal: Signal
}
