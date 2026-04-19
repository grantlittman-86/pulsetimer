package com.grantlittman.wearapp.timer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.grantlittman.wearapp.R
import com.grantlittman.wearapp.data.model.Pattern
import com.grantlittman.wearapp.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground service that runs the timer and delivers haptic signals
 * even when the screen is off.
 *
 * Uses the Wear OS Ongoing Activity API to keep the timer alive
 * when the watch screen times out or goes to ambient mode.
 */
class TimerService : Service() {

    companion object {
        private const val CHANNEL_ID = "pulsetimer_channel"
        private const val NOTIFICATION_ID = 1
        private const val WAKE_LOCK_TAG = "PulseTimer::TimerWakeLock"

        const val ACTION_STOP = "com.grantlittman.wearapp.ACTION_STOP"

        fun startIntent(context: Context): Intent =
            Intent(context, TimerService::class.java)

        fun stopIntent(context: Context): Intent =
            Intent(context, TimerService::class.java).apply {
                action = ACTION_STOP
            }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var signalExecutor: SignalExecutor
    private lateinit var timerEngine: TimerEngine
    private var wakeLock: PowerManager.WakeLock? = null

    /** Binder for the UI to access the service and timer state. */
    inner class TimerBinder : Binder() {
        val service: TimerService get() = this@TimerService
        val engine: TimerEngine get() = timerEngine
        val state: StateFlow<TimerState> get() = timerEngine.state
    }

    private val binder = TimerBinder()

    override fun onCreate() {
        super.onCreate()
        signalExecutor = SignalExecutor(this)
        timerEngine = TimerEngine(signalExecutor, serviceScope)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopTimer()
            return START_NOT_STICKY
        }

        // Go foreground immediately but don't acquire wake lock yet —
        // that happens in startTimer() when actually needed.
        startForeground(NOTIFICATION_ID, buildNotificationBuilder("PulseTimer ready").build())

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopTimer()
        serviceScope.cancel()
        super.onDestroy()
    }

    // -- Public API for the UI --

    fun startTimer(pattern: Pattern) {
        val notificationBuilder = buildNotificationBuilder("Running: ${pattern.name}")
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        acquireWakeLock()

        // Register as an Ongoing Activity so Wear OS keeps the app alive
        // and shows it on the watch face as an active timer
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ongoingActivity = OngoingActivity.Builder(this, NOTIFICATION_ID, notificationBuilder)
            .setStaticIcon(R.mipmap.ic_launcher)
            .setTouchIntent(openIntent)
            .setStatus(
                Status.Builder()
                    .addTemplate("Running: #name#")
                    .addPart("name", Status.TextPart(pattern.name))
                    .build()
            )
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .build()

        ongoingActivity.apply(this)

        timerEngine.start(pattern)
    }

    fun pauseTimer() {
        timerEngine.pause()
        updateNotification("Paused")
    }

    fun resumeTimer() {
        timerEngine.resume()
        val name = timerEngine.state.value.patternName
        updateNotification("Running: $name")
    }

    fun stopTimer() {
        timerEngine.stop()
        releaseWakeLock()
        // Removing the foreground notification also clears the ongoing activity
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    // -- Wake lock management --

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG
            ).apply {
                // Max 4 hours to prevent runaway wake locks
                acquire(4 * 60 * 60 * 1000L)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        wakeLock = null
    }

    // -- Notification --

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "PulseTimer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Timer running notification"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotificationBuilder(text: String): NotificationCompat.Builder {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            stopIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("PulseTimer")
            .setContentText(text)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotificationBuilder(text).build())
    }
}
