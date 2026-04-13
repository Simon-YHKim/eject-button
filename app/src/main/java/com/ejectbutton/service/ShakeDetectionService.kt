package com.ejectbutton.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.os.SystemClock
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.strings
import kotlin.math.sqrt

class ShakeDetectionService : Service(), SensorEventListener {

    companion object {
        private const val NOTIF_CHANNEL = "eject_shake"
        private const val NOTIF_ID      = 1002
        private const val SHAKE_THRESHOLD = 11f   // m/s² (중력 제외)
        private const val SHAKE_COOLDOWN  = 1500L // ms

        fun start(
            ctx: Context,
            callerName: String,
            callerLabel: String,
            prompter: String,
            delayMs: Long = 0L,
        ) {
            ctx.startForegroundService(
                Intent(ctx, ShakeDetectionService::class.java).apply {
                    putExtra("caller_name",  callerName)
                    putExtra("caller_label", callerLabel)
                    putExtra("prompter",     prompter)
                    putExtra("delay_ms",     delayMs)
                }
            )
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ShakeDetectionService::class.java))
        }
    }

    private lateinit var sensorManager: SensorManager
    private var callerName  = "엄마"
    private var callerLabel = "휴대전화"
    private var prompter    = ""
    private var delayMs     = 0L
    private var lastShakeMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        callerName  = intent?.getStringExtra("caller_name")  ?: "엄마"
        callerLabel = intent?.getStringExtra("caller_label") ?: "휴대전화"
        prompter    = intent?.getStringExtra("prompter")     ?: ""
        delayMs     = intent?.getLongExtra("delay_ms", 0L)    ?: 0L

        try { startForeground(NOTIF_ID, buildNotif()) } catch (_: Exception) {}

        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        } else {
            // 센서 없으면 즉시 발신
            FakeCallOverlayService.start(this, callerName, callerLabel, prompter, delayMs)
            stopSelf()
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH

        if (acceleration > SHAKE_THRESHOLD) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastShakeMs > SHAKE_COOLDOWN) {
                lastShakeMs = now
                CountdownBus.start(delayMs)
                FakeCallOverlayService.start(this, callerName, callerLabel, prompter, delayMs)
                // stopSelf() 를 호출하지 않는다 — 서비스가 죽으면 두 번째 흔들기가
                // 감지되지 않아 모드를 껐다 켜야 재트리거가 되는 버그가 있었다.
                // 센서 리스너를 유지하고 SHAKE_COOLDOWN 으로 중복 발화만 방지한다.
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    private fun createChannel() {
        val lang = EjectPrefs.loadLanguage(this)
        val strings = lang.strings()
        val ch = NotificationChannel(NOTIF_CHANNEL, strings.shakeChannelName, NotificationManager.IMPORTANCE_LOW)
            .apply { setSound(null, null) }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification {
        val lang = EjectPrefs.loadLanguage(this)
        val strings = lang.strings()
        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle(strings.shakeNotifTitle)
            .setContentText(strings.shakeNotifText)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .build()
    }
}
