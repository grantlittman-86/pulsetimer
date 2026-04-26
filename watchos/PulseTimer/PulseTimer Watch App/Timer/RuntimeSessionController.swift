import Foundation
import WatchKit

/// Manages a `WKExtendedRuntimeSession` so the timer can keep ticking when the wrist
/// is down or the user navigates away. This is the watchOS analog of the Wear OS
/// foreground service: without it, watchOS suspends the app aggressively after a
/// few seconds of inactivity and the timer dies.
///
/// The system caps extended runtime at roughly 30 minutes for most session contexts.
/// `willExpire` warns us before the session ends; we surface that to the engine so
/// the user gets a final haptic warning rather than silent failure.
@MainActor
final class RuntimeSessionController: NSObject {
    /// Called shortly before the system ends the session. Engine wires a warning haptic here.
    var willExpire: (() -> Void)?

    private var session: WKExtendedRuntimeSession?

    /// Begin an extended runtime session. Idempotent: repeated calls while a session is
    /// already running or scheduled are ignored.
    func start() {
        if let s = session, s.state == .running || s.state == .scheduled {
            return
        }
        let new = WKExtendedRuntimeSession()
        new.delegate = self
        new.start()
        self.session = new
    }

    /// End the current session (if any). Safe to call when no session is active.
    /// The session reference is released in `didInvalidateWith` once the system
    /// confirms the session has fully ended; releasing it here would dealloc the
    /// object mid-invalidation and produce a "Session not running" log noise.
    func stop() {
        session?.invalidate()
    }
}

extension RuntimeSessionController: WKExtendedRuntimeSessionDelegate {
    nonisolated func extendedRuntimeSessionDidStart(
        _ extendedRuntimeSession: WKExtendedRuntimeSession
    ) {
        // No-op. The system surfaces a small status indicator on the watch face.
    }

    nonisolated func extendedRuntimeSessionWillExpire(
        _ extendedRuntimeSession: WKExtendedRuntimeSession
    ) {
        Task { @MainActor [weak self] in
            self?.willExpire?()
        }
    }

    nonisolated func extendedRuntimeSession(
        _ extendedRuntimeSession: WKExtendedRuntimeSession,
        didInvalidateWith reason: WKExtendedRuntimeSessionInvalidationReason,
        error: Error?
    ) {
        Task { @MainActor [weak self] in
            self?.session = nil
        }
    }
}
