package com.ejectbutton.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
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
import com.ejectbutton.ui.call.InCallScreenV2
import com.ejectbutton.ui.call.rememberCallTimer
import com.ejectbutton.ui.call.FakeIncomingCallScreenV2
import com.ejectbutton.ui.theme.LegacyCallTheme

class FakeCallOverlayService : Service() {

    companion object {
        const val EXTRA_CALLER_NAME  = "caller_name"
        const val EXTRA_CALLER_LABEL = "caller_label"
        const val EXTRA_PROMPTER     = "prompter_hint"
        const val EXTRA_DELAY_MS     = "delay_ms"
        // Debug-only: start overlay directly in in-call state (skips incoming ring UI).
        // Used by UI screenshot tests via adb am start-service.
        const val EXTRA_START_IN_CALL = "debug_start_in_call"
        private const val NOTIF_CHANNEL = "eject_call"
        private const val NOTIF_ID      = 1001

        // 전화 종료 후 전면 광고 표시용 플래그
        var showInterstitialOnNextResume = false
            internal set

        /**
         * v1.2 — scenarioId / mode 매개변수 추가 (호출부 호환성).
         *
         * MainActivity 가 EJECT 발사 시 ShakeDetectionService.start 와 시그니처를
         * 일치시키기 위해 받지만 현재는 Intent extras 로만 저장하고 서비스 동작엔
         * 영향 X. v1.3 에서 분석 funnel 보강 예정.
         */
        fun start(
            ctx: Context,
            callerName: String,
            callerLabel: String,
            prompter: String,
            delayMs: Long = 0L,
            scenarioId: String = "",
            mode: String = "button_now",
        ) {
            ctx.startForegroundService(
                Intent(ctx, FakeCallOverlayService::class.java).apply {
                    putExtra(EXTRA_CALLER_NAME,  callerName)
                    putExtra(EXTRA_CALLER_LABEL, callerLabel)
                    putExtra(EXTRA_PROMPTER,     prompter)
                    putExtra(EXTRA_DELAY_MS,     delayMs)
                    putExtra("scenario_id",      scenarioId)
                    putExtra("mode",             mode)
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
    // v1.1.5 — Clarity fakeCallEnded 이벤트의 duration 계산용. callState=true 진입 시점 기록.
    private var callStartedAtMs: Long = 0L
    private var wakeLock: PowerManager.WakeLock? = null

    @Suppress("DEPRECATION")
    private var phoneListener: PhoneStateListener? = null

    // API 31+ replacement for PhoneStateListener.  Held separately so we can
    // unregister it on the correct code path in onDestroy.
    private var telephonyCallback: TelephonyCallback? = null

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
        releaseWake()

        val callerName  = intent?.getStringExtra(EXTRA_CALLER_NAME)  ?: "Mom"
        val callerLabel = intent?.getStringExtra(EXTRA_CALLER_LABEL) ?: "Mobile"
        val prompter    = intent?.getStringExtra(EXTRA_PROMPTER)     ?: ""
        val delayMs     = intent?.getLongExtra(EXTRA_DELAY_MS, 0L)   ?: 0L
        val startInCall = intent?.getBooleanExtra(EXTRA_START_IN_CALL, false) ?: false
        // v1.2 — analytics funnel 컨텍스트 (호출부가 추가로 전달).
        val scenarioId  = intent?.getStringExtra("scenario_id") ?: ""
        val mode        = intent?.getStringExtra("mode")        ?: "button_now"
        callState.value = startInCall

        try { startForeground(NOTIF_ID, buildNotif(delayMs)) } catch (_: Exception) {}
        try { listenRealCall() } catch (_: Exception) {}

        if (delayMs > 0L) {
            val h = Handler(Looper.getMainLooper())
            pendingHandler = h
            h.postDelayed({
                pendingHandler = null
                try { acquireWake() } catch (_: Exception) {}
                try { ring() } catch (_: Exception) {}
                tryShowOverlayOrAbort(callerName, callerLabel, prompter, scenarioId, mode)
            }, delayMs)
        } else {
            try { acquireWake() } catch (_: Exception) {}
            try { ring() } catch (_: Exception) {}
            tryShowOverlayOrAbort(callerName, callerLabel, prompter, scenarioId, mode)
        }
        return START_NOT_STICKY
    }

    /**
     * 오버레이 표시 시도. 실패 시(권한 철회, BadTokenException 등) 링/진동
     * 을 모두 중단하고 사용자에게 토스트로 원인 안내 → silent fail 방지.
     *
     * v1.2 — scenarioId/mode 를 showOverlay 까지 전달해 analytics 가
     * 오버레이가 실제로 떴을 때만 fake_call_started 를 기록하도록.
     */
    private fun tryShowOverlayOrAbort(
        callerName: String,
        callerLabel: String,
        prompter: String,
        scenarioId: String,
        mode: String,
    ) {
        try {
            showOverlay(callerName, callerLabel, prompter, scenarioId, mode)
        } catch (e: Exception) {
            // 링/진동 정리
            stopRing()
            releaseWake()
            // 사용자 피드백
            try {
                val msg = EjectPrefs.loadLanguage(this).strings().sideButtonOverlayRequired
                android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_LONG).show()
            } catch (_: Exception) {}
            stopSelf()
        }
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

    @Suppress("DEPRECATION")
    private fun showOverlay(
        callerName: String,
        callerLabel: String,
        prompter: String,
        scenarioId: String,
        mode: String,
    ) {
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
                    // v1.4.2 — FLAG_SECURE 제거 (테스터 스크린샷 가능). v1.5+ 에서 debug-only 토글로 재구성 예정.
                    // 원래 의도: 가짜 통화 화면이 Recents/스크린샷/화면녹화에 안 잡히게 차단.
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
                            // Round 17 — One UI 8.5 style V2 화면으로 교체.
                            // V2 는 prompterHint 파라미터를 받지 않으므로 제거.
                            // 힌트는 accept 이후 FakeInCallScreen 에서 계속 표시된다.
                            FakeIncomingCallScreenV2(
                                callerName  = callerName,
                                callerLabel = callerLabel,
                                onDecline   = { dismiss() },
                                onAccept    = {
                                    stopRing()
                                    callState.value = true
                                    // v1.1.5 — Clarity duration tracking 시작점.
                                    callStartedAtMs = System.currentTimeMillis()
                                },
                            )
                        } else {
                            // Round 19 — One UI 8.5 style in-call screen (V2).
                            // prompterHint 는 V2 가 직접 지원하지 않음. transcribing 서브텍스트로 대체.
                            // 통화 경과 시간은 rememberCallTimer 헬퍼로 1초마다 증가.
                            val elapsed by rememberCallTimer(startSeconds = 0)
                            InCallScreenV2(
                                callerName          = callerName,
                                callerLabel         = callerLabel,
                                elapsedSeconds      = elapsed,
                                isRecording         = true,
                                statusSubtext       = strings.transcribing,
                                bluetoothDeviceName = resolveBluetoothDeviceName(),
                                onMute              = {},
                                onRecordingToggle   = {},
                                onSpeaker           = {},
                                onKeypad            = {},
                                onBluetooth         = {},
                                onMore              = {},
                                onAssist            = {},
                                onEndCall           = { dismiss() },
                            )
                        }
                    }
                }
            }
        }

        overlay = view
        try { wm?.addView(view, params) } catch (_: Exception) { stopSelf(); return }

        // v1.2 — addView 가 성공한 다음에만 fake_call_started 를 기록.
        // 카운트다운 / arming 단계에서 발사하면 실제로 화면이 안 뜬 경우 (권한 철회 등)
        // 도 conversion 으로 잡혀 데이터가 오염된다. 또한 PII 방지를 위해
        // callerName 자체는 보내지 않고 isNotBlank 로 환산한 Boolean 만 전달.
        com.ejectbutton.analytics.EjectAnalytics.logFakeCallStarted(
            mode = mode,
            scenarioId = scenarioId,
            callerNamePresent = callerName.isNotBlank(),
        )

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

    /**
     * Returns the product name of the currently-routed Bluetooth audio output
     * (e.g. "Galaxy Watch3 (36A1)"), or null when no BT audio device is
     * connected. Uses AudioManager.getDevices which requires NO runtime
     * permission — safe for all supported API levels. Newlines are inserted
     * before any parenthetical suffix so the two-line One-UI label layout
     * lines up with the Bluetooth tile.
     */
    private fun resolveBluetoothDeviceName(): String? {
        return try {
            val am = getSystemService(AUDIO_SERVICE) as? AudioManager ?: return null
            val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val bt = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET)
            } ?: return null
            val name = bt.productName?.toString()?.trim().orEmpty()
            if (name.isEmpty()) null
            else name.replaceFirst(" (", "\n(", ignoreCase = false)
        } catch (_: Exception) {
            null
        }
    }

    fun dismiss() {
        if (isDismissing) return
        isDismissing = true

        // v1.1.5 — Clarity 정성 funnel: 가짜 통화 종료 이벤트 (duration + reason).
        // callState=true 였으면 user_hangup (통화 후 종료), 아니면 ringing 단계 종료.
        val wasInCall = callState.value
        val durationSec = if (wasInCall && callStartedAtMs > 0L) {
            ((System.currentTimeMillis() - callStartedAtMs) / 1000L).coerceAtLeast(0L)
        } else 0L
        val endReason = if (wasInCall) "user_hangup" else "ringing_dismiss"
        com.ejectbutton.analytics.EjectClarity.fakeCallEnded(durationSec, endReason)

        // 통화 중 상태에서 종료 → 전면 광고 플래그 설정 (무료 사용자만)
        if (wasInCall && !EjectPrefs.loadPremium(this)) {
            showInterstitialOnNextResume = true
        }

        val view = overlay
        overlay = null
        stopRing()
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

        // 시스템 무음 모드 → 소리/진동 모두 생략하고 화면만 표시.
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

    /**
     * Listen for real incoming calls so the fake-call overlay dismisses
     * itself if the user's phone actually rings — we don't want to be on
     * top of Samsung's real in-call UI.
     *
     * API 31+ (Android 12+): TelephonyManager.registerTelephonyCallback
     * with a TelephonyCallback.CallStateListener.  Required because
     * PhoneStateListener.listen() is deprecated on S and raises a warning.
     *
     * API 26-30: PhoneStateListener.listen() is still the supported path.
     */
    private fun listenRealCall() {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                override fun onCallStateChanged(state: Int) {
                    if (state != TelephonyManager.CALL_STATE_IDLE) dismiss()
                }
            }
            telephonyCallback = cb
            try { tm.registerTelephonyCallback(mainExecutor, cb) } catch (_: Exception) {}
        } else {
            @Suppress("DEPRECATION")
            phoneListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    if (state != TelephonyManager.CALL_STATE_IDLE) dismiss()
                }
            }
            @Suppress("DEPRECATION")
            tm.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE)
        }
    }

    private fun createChannel() {
        val strings = EjectPrefs.loadLanguage(this).strings()
        val ch = NotificationChannel(NOTIF_CHANNEL, strings.serviceChannelName, NotificationManager.IMPORTANCE_LOW)
            .apply { setSound(null, null) }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotif(delayMs: Long = 0L): Notification {
        val strings = EjectPrefs.loadLanguage(this).strings()
        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle(strings.serviceNotifTitle)
            .setContentText(strings.serviceNotifText)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .build()
    }

    override fun onDestroy() {
        pendingHandler?.removeCallbacksAndMessages(null)
        if (!isDismissing) {
            overlay?.let { try { wm?.removeView(it) } catch (_: Exception) {} }
        }
        stopRing()
        releaseWake()
        serviceLifecycle?.stop()
        // Symmetric teardown — same API-level split as listenRealCall.
        try {
            val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyCallback?.let { tm.unregisterTelephonyCallback(it) }
                telephonyCallback = null
            } else {
                @Suppress("DEPRECATION")
                tm.listen(phoneListener, PhoneStateListener.LISTEN_NONE)
            }
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
