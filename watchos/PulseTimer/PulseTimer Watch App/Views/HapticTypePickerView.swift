import SwiftUI

/// Reusable picker for selecting a HapticType. Shown as a navigation-link picker
/// so the choice expands to a full-screen list when tapped.
struct HapticTypePickerView: View {
    @Binding var hapticType: HapticType
    let label: String

    var body: some View {
        Picker(label, selection: $hapticType) {
            ForEach(HapticType.allCases, id: \.self) { type in
                Text(displayName(for: type)).tag(type)
            }
        }
        .pickerStyle(.navigationLink)
    }
}
