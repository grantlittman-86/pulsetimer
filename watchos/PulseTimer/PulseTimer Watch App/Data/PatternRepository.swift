import Foundation
import Observation

/// Persists patterns to UserDefaults as a single JSON blob.
///
/// Mirrors the Wear OS PatternRepository (DataStore + Gson) using the platform-equivalent
/// key-value store. The on-disk JSON shape matches the canonical schema in
/// docs/design-decisions.md so patterns are portable between platforms.
@Observable
@MainActor
final class PatternRepository {
    private let defaults: UserDefaults
    private let storageKey = "pulsetimer.patterns.v1"
    private let encoder: JSONEncoder
    private let decoder: JSONDecoder

    private(set) var patterns: [Pattern] = []

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
        self.encoder = JSONEncoder()
        self.decoder = JSONDecoder()
        self.patterns = loadFromDisk()
    }

    /// Seed the built-in presets if the store has no preset entries yet. Idempotent.
    func seedPresetsIfNeeded() {
        guard !patterns.contains(where: { $0.isPreset }) else { return }
        patterns.append(contentsOf: DefaultPresets.all)
        persist()
    }

    func getById(_ id: String) -> Pattern? {
        patterns.first { $0.id == id }
    }

    /// Insert if new, replace if a pattern with the same id already exists.
    func save(_ pattern: Pattern) {
        if let index = patterns.firstIndex(where: { $0.id == pattern.id }) {
            patterns[index] = pattern
        } else {
            patterns.append(pattern)
        }
        persist()
    }

    /// Delete a pattern. Presets are protected and silently ignored.
    func delete(_ pattern: Pattern) {
        guard !pattern.isPreset else { return }
        patterns.removeAll { $0.id == pattern.id }
        persist()
    }

    /// Create a non-preset copy of the given pattern with a new id and name.
    /// Used to implement copy-on-edit for presets.
    @discardableResult
    func duplicate(_ pattern: Pattern, newName: String) -> Pattern {
        var copy = pattern
        copy.id = UUID().uuidString
        copy.name = newName
        copy.isPreset = false
        copy.lastUsedAt = nil
        save(copy)
        return copy
    }

    func markUsed(id: String) {
        guard let index = patterns.firstIndex(where: { $0.id == id }) else { return }
        patterns[index].lastUsedAt = Int64(Date().timeIntervalSince1970 * 1000)
        persist()
    }

    private func persist() {
        do {
            let data = try encoder.encode(patterns)
            defaults.set(data, forKey: storageKey)
        } catch {
            // Encoding our own well-formed structs should not fail. If it does,
            // surface during development; in production, prefer keeping in-memory state
            // over crashing the app.
            assertionFailure("Failed to encode patterns: \(error)")
        }
    }

    private func loadFromDisk() -> [Pattern] {
        guard let data = defaults.data(forKey: storageKey) else { return [] }
        return (try? decoder.decode([Pattern].self, from: data)) ?? []
    }
}
