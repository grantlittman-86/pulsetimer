import Foundation
import WatchKit

/// Keeps a mindfulness session frontmost while the wrist is down, for up to one hour.
/// Explicitly leaving the app can invalidate this session. This is not a general
/// background service. Delegate callbacks are scoped to the current session.
@MainActor
final class RuntimeSessionController: NSObject {
    /// Called shortly before the system ends the session. Engine wires a warning haptic here.
    var willExpire: (() -> Void)?

    var interrupted: ((String) -> Void)?
    private var stopping = false
    private var restartRequested = false

    // Also checked before each tick, since invalidation can precede its delegate task.
    var canContinue: Bool {
        restartRequested || (!stopping && session != nil && session?.state != .invalid)
    }

    private var session: WKExtendedRuntimeSession?

    /// Begin an extended runtime session. Idempotent: repeated calls while a session is
    /// already running or scheduled are ignored.
    func start() {
        if stopping {
            restartRequested = true
            return
        }
        if let s = session, s.state != .invalid {
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
        restartRequested = false
        guard let session, !stopping else { return }
        stopping = true
        session.invalidate()
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
        let message: String
        switch reason {
        case .resignedFrontmost:
            message = "You left PulseTimer. Keep it open for wrist-down cues, then tap Resume when ready."
        case .expired:
            message = "The watch's session time limit was reached. Tap Resume to continue."
        default:
            message = "The watch interrupted this session. Tap Resume to try again."
        }
        Task { @MainActor [weak self] in
            guard let self, let current = self.session,
                  ObjectIdentifier(current) == sessionId else { return }
            let wasStopping = self.stopping
            let shouldRestart = self.restartRequested
            self.session = nil
            self.stopping = false
            self.restartRequested = false
            if shouldRestart {
                self.start()
            } else if !wasStopping {
                self.interrupted?(message)
            }
        }
    }
}
