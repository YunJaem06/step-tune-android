package hs.project.steptune.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import hs.project.steptune.MainActivity
import hs.project.steptune.R
import hs.project.steptune.core.util.DateFormatter
import hs.project.steptune.data.local.preferences.StepTrackingState
import hs.project.steptune.data.local.preferences.StepTrackingStateDataSource
import hs.project.steptune.domain.model.DailyProgress
import hs.project.steptune.domain.model.UserPreferences
import hs.project.steptune.domain.repository.PedometerRepository
import hs.project.steptune.domain.repository.SettingsRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class StepTrackingService : Service(), SensorEventListener {

    @Inject lateinit var pedometerRepository: PedometerRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var trackingStateDataSource: StepTrackingStateDataSource

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null

    @Volatile
    private var currentPreferences: UserPreferences = UserPreferences()

    @Volatile
    private var currentTrackingState: StepTrackingState = StepTrackingState()

    @Volatile
    private var lastSavedSteps: Int = 0

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        observeDependencies()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        if (stepCounterSensor == null) {
            if (!startForegroundWithNotification(sensorAvailable = false)) {
                return START_NOT_STICKY
            }
            return START_NOT_STICKY
        }

        if (!startForegroundWithNotification(sensorAvailable = true)) {
            return START_NOT_STICKY
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return START_NOT_STICKY
        }

        registerStepCounterSensor()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onSensorChanged(event: SensorEvent?) {
        val rawSensorSteps = event?.values?.firstOrNull()?.toInt() ?: return
        serviceScope.launch {
            persistStepReading(rawSensorSteps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeDependencies() {
        serviceScope.launch {
            settingsRepository.observePreferences().collectLatest { preferences ->
                currentPreferences = preferences
            }
        }

        serviceScope.launch {
            trackingStateDataSource.state.collectLatest { trackingState ->
                currentTrackingState = trackingState
            }
        }
    }

    private suspend fun persistStepReading(rawSensorSteps: Int) {
        val today = DateFormatter.today()
        var trackingState = currentTrackingState

        if (trackingState.trackingDate != today || trackingState.baselineSensorSteps == null) {
            trackingState = StepTrackingState(
                trackingDate = today,
                baselineSensorSteps = rawSensorSteps,
                offsetSteps = 0
            )
            trackingStateDataSource.updateState(today, rawSensorSteps, 0)
            currentTrackingState = trackingState
        } else if (rawSensorSteps < trackingState.baselineSensorSteps) {
            val carriedSteps = pedometerRepository.getDailyProgress(today)?.steps ?: 0
            trackingState = StepTrackingState(
                trackingDate = today,
                baselineSensorSteps = rawSensorSteps,
                offsetSteps = carriedSteps
            )
            trackingStateDataSource.updateState(today, rawSensorSteps, carriedSteps)
            currentTrackingState = trackingState
        }

        val baselineSensorSteps = trackingState.baselineSensorSteps ?: rawSensorSteps
        val steps = (trackingState.offsetSteps + (rawSensorSteps - baselineSensorSteps))
            .coerceAtLeast(trackingState.offsetSteps)

        val distanceMeters = steps * (currentPreferences.stepLengthCm / 100f)
        val calories = calculateCalories(
            distanceMeters = distanceMeters,
            weightKg = currentPreferences.weightKg
        )

        pedometerRepository.upsertDailyProgress(
            DailyProgress(
                date = today,
                steps = steps,
                goal = currentPreferences.dailyGoal,
                distanceMeters = distanceMeters,
                calories = calories
            )
        )

        lastSavedSteps = steps
        updateNotification(sensorAvailable = true)
    }

    private fun calculateCalories(distanceMeters: Float, weightKg: Int): Float {
        val distanceKm = distanceMeters / 1_000f
        return distanceKm * weightKg * 0.57f
    }

    private fun registerStepCounterSensor() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val sensor = stepCounterSensor ?: return
        sensorManager.unregisterListener(this)
        try {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            getString(R.string.tracking_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun startForegroundWithNotification(sensorAvailable: Boolean): Boolean {
        val notification = buildNotification(sensorAvailable)
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }
        return try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
            true
        } catch (_: SecurityException) {
            stopSelf()
            false
        }
    }

    private fun updateNotification(sensorAvailable: Boolean) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            NotificationManagerCompat.from(this).notify(
                NOTIFICATION_ID,
                buildNotification(sensorAvailable)
            )
        } catch (_: SecurityException) {
            stopSelf()
        }
    }

    private fun buildNotification(sensorAvailable: Boolean): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (sensorAvailable) {
            getString(R.string.tracking_notification_steps, lastSavedSteps)
        } else {
            getString(R.string.tracking_notification_sensor_unavailable)
        }

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "step_tracking_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
