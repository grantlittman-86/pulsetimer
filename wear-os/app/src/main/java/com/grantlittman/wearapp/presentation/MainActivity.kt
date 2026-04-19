package com.grantlittman.wearapp.presentation

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.wear.ambient.AmbientLifecycleObserver
import com.grantlittman.wearapp.data.model.HapticType
import com.grantlittman.wearapp.data.model.Pattern
import com.grantlittman.wearapp.data.model.Signal
import com.grantlittman.wearapp.data.repository.PatternRepository
import com.grantlittman.wearapp.presentation.theme.WearAppTheme
import com.grantlittman.wearapp.timer.SignalExecutor
import com.grantlittman.wearapp.timer.TimerService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: PatternRepository
    private lateinit var signalExecutor: SignalExecutor
    private var timerBinder by mutableStateOf<TimerService.TimerBinder?>(null)
    private var serviceBound = false
    var isAmbient by mutableStateOf(false)
        private set

    // Pattern waiting to start after service binds
    private var pendingPattern: Pattern? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            timerBinder = service as? TimerService.TimerBinder
            serviceBound = true

            // If a pattern was queued before binding completed, start it now
            pendingPattern?.let { pattern ->
                timerBinder?.service?.startTimer(pattern)
                pendingPattern = null
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerBinder = null
            serviceBound = false
        }
    }

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            isAmbient = true
            // Tell the timer engine to slow its tick rate
            timerBinder?.engine?.isAmbient = true
        }

        override fun onExitAmbient() {
            isAmbient = false
            timerBinder?.engine?.isAmbient = false
        }

        override fun onUpdateAmbient() {
            // Called periodically in ambient mode (~once per minute)
            // The timer UI recomposes automatically via StateFlow
        }
    }

    private lateinit var ambientObserver: AmbientLifecycleObserver

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        repository = PatternRepository(applicationContext)
        signalExecutor = SignalExecutor(applicationContext)

        // Set up ambient mode observer
        ambientObserver = AmbientLifecycleObserver(this, ambientCallback)
        lifecycle.addObserver(ambientObserver)

        // Seed default presets on first launch
        lifecycleScope.launch {
            repository.seedPresetsIfNeeded()
        }

        setContent {
            WearAppTheme {
                WearApp(
                    repository = repository,
                    timerBinder = timerBinder,
                    isAmbient = isAmbient,
                    onStartTimer = ::startTimer,
                    onPauseTimer = ::pauseTimer,
                    onResumeTimer = ::resumeTimer,
                    onStopTimer = ::stopTimer,
                    onTrySignal = ::trySignal
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Only bind (don't start foreground) — the service starts foreground when a timer runs
        if (!serviceBound) {
            val intent = TimerService.startIntent(this)
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        // Only unbind if the timer is NOT running.
        // If it's running, the foreground service keeps it alive independently,
        // and we'll rebind in onStart. Unbinding a running timer would remove
        // our binder reference but the started foreground service survives.
        if (serviceBound) {
            val timerRunning = timerBinder?.state?.value?.isRunning == true
            if (!timerRunning) {
                unbindService(serviceConnection)
                serviceBound = false
                timerBinder = null
            }
            // If timer IS running, keep the binding alive so we can reconnect
            // the UI when the user returns from ambient/background
        }
    }

    override fun onDestroy() {
        lifecycle.removeObserver(ambientObserver)
        super.onDestroy()
    }

    private fun startTimer(pattern: Pattern) {
        lifecycleScope.launch {
            repository.markUsed(pattern.id)
        }

        // ALWAYS start as a foreground service so it survives activity lifecycle.
        // Without this, a bind-only service gets destroyed when onStop unbinds it.
        val intent = TimerService.startIntent(this)
        startForegroundService(intent)

        // Keep screen on so the activity stays in the foreground.
        // On Wear OS with AmbientLifecycleObserver, this allows the system to
        // transition to ambient mode (dimmed display) instead of backgrounding
        // the activity entirely.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        if (serviceBound && timerBinder != null) {
            // Service is ready — start immediately
            timerBinder?.service?.startTimer(pattern)
        } else {
            // Service not yet bound — queue pattern and ensure binding
            pendingPattern = pattern
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private fun pauseTimer() {
        timerBinder?.service?.pauseTimer()
    }

    private fun resumeTimer() {
        timerBinder?.service?.resumeTimer()
    }

    private fun stopTimer() {
        timerBinder?.service?.stopTimer()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun trySignal(hapticType: HapticType) {
        lifecycleScope.launch {
            signalExecutor.execute(Signal(hapticType = hapticType))
        }
    }
}
