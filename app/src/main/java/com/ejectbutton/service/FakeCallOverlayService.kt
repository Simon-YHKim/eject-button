package com.ejectbutton.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.os.Build
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.*
import androidx.savedstate.*
import com.ejectbutton.ads.AdManager
import com.ejectbutton.data.AppLanguage
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.data.strings
import com.ejectbutton.ui.call.FakeInCallScreen
import com.ejectbutton.ui.call.FakeIncomingCallScreen
import com.ejectbutton.ui.theme.LegacyCallTheme

class FakeCallOverlayService : Service() {

    companion object {
        const val EXTRA_CALLER_NAME  = "caller_name"
        const val EXTRA_CALLER_LABEL = "caller_label"
        const val EXTRA_PROMPTER     = "prompter_hint"
        const val EXTRA_DELAY_MS     = "delay_ms"
        private const val NOTIF_CHANNEL = "eject_call"
        private const val NOTIF_ID      = 1001

        // 전화 종료 후 전면 광고 표시용 플래그
        var showInterstitialOnNextResume = false
            internal set

        fun start(ctx: Context, callerName: String, callerLabel: String, prompter: String, delayMs: Long = 0L) {
            ctx.startForegroundService(
                Intent(ctx, FakeCallOverlayService::class.java).apply {
                    putExtra(EXTRA_CALLER_NAME,  callerName)
                    putExtra(EXTRA_CALLER_LABEL, callerLabel)
                    putExtra(EXTRA_PROMPTER,     prompter)
                    putExtra(EXTRA_DELAY_MS,     delayMs)
                }
            )
        }

        /**
         * 카운트다운 중 취소용 — 아직 벨소리/오버레이가 뜨기 전이라면
         * 서비스만 내리고, 이미 오버레이가 올라가 있으면 그것도 함께 정리된다.
         */
        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, FakeCallOverlayService::class.java))
        }
    }

    private var wm: WindowManager? = null
    private var overlay: ComposeView? = null
    private var ringtone: android.media.Ringtone? = null
    private var vibrator: Vibrator? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var serviceLifecycle: ServiceLifecycleOwner? = null
    private val callState = mutableStateOf(false)
    private var pendingHandler: Handler? = null
    private var isDismissing = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var flashHandler: Handler? = null
    private var flashCameraId: String? = null
    private var flashOn = false

    @Suppress("DEPRECATION")
    private var phoneListener: PhoneStateListener? = null

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        pendingHandler?.removeCallbacksAndMessages(null)
        pendingHandler = null

        overlay?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        overlay = null
        isDismissing = false
        callState.value = false
        stopRing()
        stopFlashBlink()
        releaseWake()

        val callerName  = intent?.getStringExtra(EXTRA_CALLER_NAME)  ?: "Mom"
        val callerLabel = intent?.getStringExtra(EXTRA_CALLER_LABEL) ?: "Mobile"
        val prompter    = intent?.getStringExtra(EXTRA_PROMPTER)     ?: ""
        val delayMs     = intent?.getLongExtra(EXTRA_DELAY_MS, 0L)   ?: 0L

        try { startForeground(NOTIF_ID, buildNotif(delayMs)) } catch (_: Exception) {}
        try { listenRealCall() } catch (_: Exception) {}

        if (delayMs > 0L) {
            val h = Handler(Looper.getMainLooper())
            pendingHandler = h
            h.postDelayed({
                pendingHandler = null
                try { acquireWake() } catch (_: Exception) {}
                try { ring() } catch (_: Exception) {}
                try { startFlashBlink() } catch (_: Exception) {}
                try { showOverlay(callerName, callerLabel, prompter) } catch (_: Exception) { stopSelf() }
            }, delayMs)
        } else {
            try { acquireWake() } catch (_: Exception) {}
            try { ring() } catch (_: Exception) {}
            try { startFlashBlink() } catch (_: Exception) {}
            try { showOverlay(callerName, callerLabel, prompter) } catch (_: Exception) { stopSelf() }
        }
        return START_NOT_STICKY
    }

    /**
     * 화면 OFF 상태에서도 화면을 즉시 켜기 위한 WakeLock.
     * FLAG_SHOW_WHEN_LOCKED + FLAG_TURN_SCREEN_ON 만으로는
     * 일부 기기에서 화면이 켜지지 않는 경우가 있어 보조용으로 함께 사용.
     */
    @Suppress("DEPRECATION")
    private fun acquireWake() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val lock = pm.newWakeLock(
            PowerManager.FULL_WAKE_LOCK
                or PowerManager.ACQUIRE_CAUSES_WAKEUP
                or PowerManager.ON_AFTER_RELEASE,
            "EjectButton:FakeCallWake"
        )
        // 최대 60초 — 사용자가 아무 조작도 안 해도 자동 해제
        lock.acquire(60_000L)
        wakeLock = lock
    }

    private fun releaseWake() {
        try { wakeLock?.takeIf { it.isHeld }?.release() } catch (_: Exception) {}
        wakeLock = null
    }

    /**
     * 카메라 LED 플래시를 500ms 간격으로 깜박임.
     * 사용자가 설정에서 활성화한 경우에만 동작.
     * Android 6.0+ (API 23) 의 CameraManager.setTorchMode 사용.
     */
    private fun startFlashBlink() {
        if (!EjectPrefs.loadFlash(this)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        val cm = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        val cameraId = try {
            cm.cameraIdList.firstOrNull { id ->
                cm.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: Exception) { null } ?: return

        flashCameraId = cameraId
        flashOn = false
        val handler = Handler(Looper.getMainLooper())
        flashHandler = handler

        val tick = object : Runnable {
            override fun run() {
                try {
                    flashOn = !flashOn
                    cm.setTorchMode(cameraId, flashOn)
                } catch (_: Exception) {}
                handler.postDelayed(this, 500L)
            }
        }
        handler.post(tick)
    }

    private fun stopFlashBlink() {
        flashHandler?.removeCallbacksAndMessages(null)
        flashHandler = null
        val cameraId = flashCameraId ?: return
        flashCameraId = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val cm = getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                cm?.setTorchMode(cameraId, false)
            } catch (_: Exception) {}
        }
        flashOn = false
    }

    @Suppress("DEPRECATION")
    private fun showOverlay(callerName: String, callerLabel: String, prompter: String) {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED    // API 27+ 대안 없음 (Service context)
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD    // API 27+ 대안 없음 (Service context)
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,    // API 27+ 대안 없음 (Service context)
            PixelFormat.TRANSLUCENT,
        ).also {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                it.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        val lifecycle = ServiceLifecycleOwner().also {
            serviceLifecycle = it
            it.start()
        }

        // 현재 앱 언어 로드
        val lang = EjectPrefs.loadLanguage(this)
        val strings = lang.strings()

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycle)
            setViewTreeSavedStateRegistryOwner(lifecycle)
            setViewTreeViewModelStoreOwner(lifecycle)
            ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ -> WindowInsetsCompat.CONSUMED }
            setContent {
                // 가짜 전화 화면은 Tactical Cockpit 테마 영향을 받지 않도록
                // 전용 LegacyCallTheme 로 격리해서 래핑한다.
                LegacyCallTheme {
                    androidx.compose.runtime.CompositionLocalProvider(LocalAppStrings provides strings) {
                        if (!callState.value) {
                            FakeIncomingCallScreen(
                                callerName   = callerName,
                                callerLabel  = callerLabel,
                                prompterHint = prompter,
                                onDecline    = { dismiss() },
                                onAccept     = {
                                    stopRing()
                                    stopFlashBlink()
                                    callState.value = true
                                },
                            )
                        } else {
                            FakeInCallScreen(
                                callerName   = callerName,
                                prompterHint = prompter,
                                onEndCall    = { dismiss() },
                            )
                        }
                    }
                }
            }
        }

        overlay = view
        try { wm?.addView(view, params) } catch (_: Exception) { stopSelf(); return }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view.post {
                try {
                    view.windowInsetsController?.let { ctrl ->
                        ctrl.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                        ctrl.systemBarsBehavior =
                            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } catch (_: Exception) {}
            }
        } else {
            @Suppress("DEPRECATION")
            view.systemUiVisibility = (
                android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }
    }

    fun dismiss() {
        if (isDismissing) return
        isDismissing = true

        // 통화 중 상태에서 종료 → 전면 광고 플래그 설정 (무료 사용자만)
        if (callState.value && !EjectPrefs.loadPremium(this)) {
            showInterstitialOnNextResume = true
        }

        val view = overlay
        overlay = null
        stopRing()
        stopFlashBlink()
        releaseWake()
        Handler(Looper.getMainLooper()).post {
            try { wm?.removeView(view) } catch (_: Exception) {}
            stopSelf()
        }
    }

    private fun ring() {
        val ringtoneEnabled  = EjectPrefs.loadRingtone(this)
        val vibrationEnabled = EjectPrefs.loadVibration(this)

        val am   = getSystemService(AUDIO_SERVICE) as AudioManager
        val mode = am.ringerMode  // SILENT / VIBRATE / NORMAL

        // 시스템 무음 모드 → 소리/진동 모두 생략. 화면 + 플래시만 동작.
        if (mode == AudioManager.RINGER_MODE_SILENT) return

        val ringtoneAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        // 오디오 포커스 획득 (리스너 없이 단순 요청)
        try {
            val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(ringtoneAttrs)
                .build()
            audioFocusRequest = focusReq
            am.requestAudioFocus(focusReq)
        } catch (_: Exception) {}

        // 벨소리 (NORMAL 모드 + 설정 허용 시) — 독립 try-catch
        // VIBRATE 모드에서는 시스템 정책상 소리를 내지 않음.
        if (ringtoneEnabled && mode == AudioManager.RINGER_MODE_NORMAL) {
            try {
                val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                if (uri != null) {
                    ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                        audioAttributes = ringtoneAttrs
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) isLooping = true
                        play()
                    }
                }
            } catch (_: Exception) {}
        }

        // 진동 (무음 모드 아닐 때 + 설정 허용 시) — 독립 try-catch
        if (vibrationEnabled && mode != AudioManager.RINGER_MODE_SILENT) {
            try {
                val vib: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager)
                        .defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                if (vib.hasVibrator()) {
                    vibrator = vib
                    // 수신 전화 패턴: 즉시 800ms 진동 → 1000ms 쉬고 반복
                    val pattern = longArrayOf(0, 800, 1000)
                    vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
                }
            } catch (_: Exception) {}
        }
    }

    private fun stopRing() {
        try { ringtone?.stop() } catch (_: Exception) {}
        ringtone = null
        try { vibrator?.cancel() } catch (_: Exception) {}
        vibrator = null
        try {
            audioFocusRequest?.let {
                (getSystemService(AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it)
            }
        } catch (_: Exception) {}
        audioFocusRequest = null
    }

    @Suppress("DEPRECATION")
    private fun listenRealCall() {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        phoneListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                if (state != TelephonyManager.CALL_STATE_IDLE) dismiss()
            }
        }
        tm.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    private fun createChannel() {
        val ch = NotificationChannel(NOTIF_CHANNEL, "Eject 실행 중", NotificationManager.IMPORTANCE_LOW)
            .apply { setSound(null, null) }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotif(delayMs: Long = 0L): Notification {
        val title = if (delayMs > 0L) "⏏ ${delayMs / 1000}s..." else "⏏ Eject Button"
        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .build()
    }

    override fun onDestroy() {
        pendingHandler?.removeCallbacksAndMessages(null)
        if (!isDismissing) {
            overlay?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        }
        stopRing()
        stopFlashBlink()
        releaseWake()
        serviceLifecycle?.stop()
        @Suppress("DEPRECATION")
        try {
            (getSystemService(TELEPHONY_SERVICE) as TelephonyManager)
                .listen(phoneListener, PhoneStateListener.LISTEN_NONE)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}

private class ServiceLifecycleOwner : SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lr   = LifecycleRegistry(this)
    private val ssrc = SavedStateRegistryController.create(this)
    private val vms  = ViewModelStore()

    override val lifecycle: Lifecycle            get() = lr
    override val savedStateRegistry             get() = ssrc.savedStateRegistry
    override val viewModelStore: ViewModelStore  get() = vms

    fun start() {
        ssrc.performRestore(null)
        lr.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lr.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lr.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        lr.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lr.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lr.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        vms.clear()
    }
}
