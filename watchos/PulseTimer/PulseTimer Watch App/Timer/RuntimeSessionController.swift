import Foundation
import WatchKit

/// Keeps a mindfulness session frontmost while the wrist is down, for up to one hour.
/// Explicitly leaving the app can invalidate this session. This is not a general
/// background service. Delegate callbacks are scoped to the current session.
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
        self.session = new
        new.start()
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
        // No additional action is needed once WatchKit starts the session.
    }

    nonisolated func extendedRuntimeSessionWillExpire(
        _ extendedRuntimeSession: WKExtendedRuntimeSession
    ) {
        let sessionId = ObjectIdentifier(extendedRuntimeSession)
        Task { @MainActor [weak self] in
            guard let self, let current = self.session,
                  ObjectIdentifier(current) == sessionId else { return }
            self.willExpire?()
        }
    }

    nonisolated func extendedRuntimeSession(
        _ extendedRuntimeSession: WKExtendedRuntimeSession,
        didInvalidateWith reason: WKExtendedRuntimeSessionInvalidationReason,
        error: Error?
    ) {
        let sessionId = ObjectIdentifier(extendedRuntimeSession)
        Task { @MainActor [weak self] in
            guard let self, let current = self.session,
                  ObjectIdentifier(current) == sessionId else { return }
            self.session = nil
        }
    }
}
