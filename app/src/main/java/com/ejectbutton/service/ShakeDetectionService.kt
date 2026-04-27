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

        /**
         * v1.2 — scenarioId / mode 매개변수 추가.
         *
         * 호출부 (MainActivity) 가 conversion event 발사 시 같은 스코프에서 첨부할
         * 컨텍스트를 서비스 시작 시점에 기록하기 위함. 현재는 디버그 로깅 목적으로만
         * Intent extras 에 저장하고 Service 동작에는 영향 X. v1.3 에서 Crashlytics
         * setCustomKey 로 흔들기 트리거 도달률 분석에 활용 예정.
         */
        fun start(
            ctx: Context,
            callerName: String,
            callerLabel: String,
            prompter: String,
            delayMs: Long = 0L,
            scenarioId: String = "",
            mode: String = "shake",
        ) {
            ctx.startForegroundService(
                Intent(ctx, ShakeDetectionService::class.java).apply {
                    putExtra("caller_name",  callerName)
                    putExtra("caller_label", callerLabel)
                    putExtra("prompter",     prompter)
                    putExtra("delay_ms",     delayMs)
                    putExtra("scenario_id",  scenarioId)
                    putExtra("mode",         mode)
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
    // v1.2 — 호출부 (MainActivity) 가 EJECT 발사 컨텍스트로 함께 넘기는 메타.
    private var scenarioId  = ""
    private var mode        = "shake"
    /**
     * v1.2 — 한 번 armed 가 한 번만 발사되도록 보장. SHAKE_COOLDOWN 만으로는
     * 동일 셰이크 중 여러 SensorEvent 가 임계값을 넘기면 multi-trigger 가 가능했음.
     * 사용자가 SHAKE 모드를 다시 활성화 (start) 하면 onStartCommand 에서 reset.
     */
    private var hasTriggered = false

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
        scenarioId  = intent?.getStringExtra("scenario_id")  ?: ""
        mode        = intent?.getStringExtra("mode")         ?: "shake"
        hasTriggered = false

        try { startForeground(NOTIF_ID, buildNotif()) } catch (_: Exception) {}

        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        } else {
            // 센서 없으면 즉시 발신
            FakeCallOverlayService.start(
                this, callerName, callerLabel, prompter, delayMs, scenarioId, mode,
            )
            stopSelf()
        }
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (hasTriggered) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val acceleration = sqrt((x * x + y * y + z * z).toDouble()) - SensorManager.GRAVITY_EARTH

        if (acceleration > SHAKE_THRESHOLD) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastShakeMs > SHAKE_COOLDOWN) {
                lastShakeMs = now
                hasTriggered = true
                CountdownBus.start(delayMs)
                FakeCallOverlayService.start(
                    this, callerName, callerLabel, prompter, delayMs, scenarioId, mode,
                )
                // stopSelf() 를 호출하지 않는다 — 서비스가 죽으면 두 번째 흔들기가
                // 감지되지 않아 모드를 껐다 켜야 재트리거가 되는 버그가 있었다.
                // 센서 리스너를 유지하고 hasTriggered + SHAKE_COOLDOWN 으로 중복 발화만 방지한다.
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onDestroy() {
        // v1.0.10 — onCreate 에서 sensorManager 초기화가 실패한 (혹은 onCreate 가
        // 호출되지 않고 곧장 onDestroy 가 호출되는) edge case 에서 lateinit
        // UninitializedPropertyAccessException 으로 죽는 것을 방지.
        if (::sensorManager.isInitialized) {
            sensorManager.unregisterListener(this)
        }
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
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .build()
    }
}
