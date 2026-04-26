import Foundation

/// The type of haptic vibration to produce.
enum HapticType: String, Codable, CaseIterable, Equatable, Hashable {
    case tap = "TAP"
    case buzz = "BUZZ"
    case pulse = "PULSE"
    case doubleTap = "DOUBLE_TAP"
}

/// The type of audio tone to play (optional, off by default).
enum AudioType: String, Codable, CaseIterable, Equatable, Hashable {
    case click = "CLICK"
    case beep = "BEEP"
    case chime = "CHIME"
}

/// A single haptic/audio event the watch can produce.
struct Signal: Codable, Equatable, Hashable {
    var hapticType: HapticType
    /// 0.0 to 1.0. Preserved through serialization for cross-platform
    /// JSON compatibility, but ignored at playback on watchOS: WKInterfaceDevice.play()
    /// does not accept an intensity parameter.
    var intensity: Double
    var audioType: AudioType?

    init(hapticType: HapticType = .tap,
         intensity: Double = 1.0,
         audioType: AudioType? = nil) {
        self.hapticType = hapticType
        self.intensity = intensity
        self.audioType = audioType
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        self.hapticType = (try? c.decode(HapticType.self, forKey: .hapticType)) ?? .tap
        self.intensity = (try? c.decode(Double.self, forKey: .intensity)) ?? 1.0
        self.audioType = try? c.decodeIfPresent(AudioType.self, forKey: .audioType)
    }
}
