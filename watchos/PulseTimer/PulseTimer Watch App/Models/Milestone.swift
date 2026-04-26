import Foundation

/// A signal that fires at a specific elapsed time during a session.
struct Milestone: Codable, Equatable, Hashable, Identifiable {
    var id: String
    var triggerAtMillis: Int64
    var signal: Signal
    var repeatCount: Int
    var label: String?

    init(id: String = UUID().uuidString,
         triggerAtMillis: Int64,
         signal: Signal,
         repeatCount: Int = 1,
         label: String? = nil) {
        self.id = id
        self.triggerAtMillis = triggerAtMillis
        self.signal = signal
        self.repeatCount = repeatCount
        self.label = label
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.id = (try? c.decode(String.self, forKey: .id)) ?? UUID().uuidString
        self.triggerAtMillis = try c.decode(Int64.self, forKey: .triggerAtMillis)
        self.signal = try c.decode(Signal.self, forKey: .signal)
        self.repeatCount = (try? c.decode(Int.self, forKey: .repeatCount)) ?? 1
        self.label = try? c.decodeIfPresent(String.self, forKey: .label)
    }
}
