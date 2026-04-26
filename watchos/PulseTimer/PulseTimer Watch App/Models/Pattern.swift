import Foundation

/// A named, saveable timer configuration that defines the complete timing
/// behavior for a session.
///
/// Wire-compatible with the Wear OS Pattern JSON schema documented in
/// docs/design-decisions.md.
struct Pattern: Codable, Equatable, Hashable, Identifiable {
    var id: String
    var name: String
    var isPreset: Bool
    /// Total session length in milliseconds. `nil` means the timer runs until manually stopped.
    var totalDurationMillis: Int64?
    var repeatingInterval: RepeatingInterval
    var milestones: [Milestone]
    var countdownWarning: CountdownWarning?
    var audioEnabled: Bool
    /// Epoch milliseconds of the last time this pattern was started, for sort-by-recency.
    var lastUsedAt: Int64?

    init(id: String = UUID().uuidString,
         name: String,
         isPreset: Bool = false,
         totalDurationMillis: Int64? = nil,
         repeatingInterval: RepeatingInterval,
         milestones: [Milestone] = [],
         countdownWarning: CountdownWarning? = nil,
         audioEnabled: Bool = false,
         lastUsedAt: Int64? = nil) {
        self.id = id
        self.name = name
        self.isPreset = isPreset
        self.totalDurationMillis = totalDurationMillis
        self.repeatingInterval = repeatingInterval
        self.milestones = milestones
        self.countdownWarning = countdownWarning
        self.audioEnabled = audioEnabled
        self.lastUsedAt = lastUsedAt
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.id = (try? c.decode(String.self, forKey: .id)) ?? UUID().uuidString
        self.name = try c.decode(String.self, forKey: .name)
        self.isPreset = (try? c.decode(Bool.self, forKey: .isPreset)) ?? false
        self.totalDurationMillis = try? c.decodeIfPresent(Int64.self, forKey: .totalDurationMillis)
        self.repeatingInterval = try c.decode(RepeatingInterval.self, forKey: .repeatingInterval)
        self.milestones = (try? c.decode([Milestone].self, forKey: .milestones)) ?? []
        self.countdownWarning = try? c.decodeIfPresent(CountdownWarning.self, forKey: .countdownWarning)
        self.audioEnabled = (try? c.decode(Bool.self, forKey: .audioEnabled)) ?? false
        self.lastUsedAt = try? c.decodeIfPresent(Int64.self, forKey: .lastUsedAt)
    }
}
