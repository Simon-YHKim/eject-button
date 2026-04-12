package com.ejectbutton.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.SideButtonCommand
import com.ejectbutton.data.strings

/**
 * 앱이 백그라운드에 있는 동안 볼륨 키 입력을 감지하는 포그라운드 서비스.
 *
 * 동작 원리:
 * - Android 의 ContentObserver 로 Settings.System 의 볼륨 변경을 감지
 * - 변화 부호로 UP/DOWN 을 판별, [ButtonPatternDetector] 에 전달
 * - 패턴 매칭 시 즉시 원래 볼륨값으로 복원하고 [SideButtonTrigger.fire] 호출
 *
 * 동작 범위 (의도적):
 * - 앱이 Foreground / Background (서비스 살아있음) 일 때만 동작
 * - 화면 OFF 또는 앱 완전 종료 상태에서는 동작하지 않음
 *
 * 시작/중지:
 * - 사용자가 설정에서 사이드 버튼 트리거를 활성화 하면 [start]
 * - 사용자가 비활성화 하면 [stop]
 * - 앱 런치 시 pref 가 활성화 상태면 자동 [start]
 *
 * 주의: Foreground 상태에서는 [com.ejectbutton.MainActivity.onKeyDown] 이
 * 볼륨 키를 먼저 가로채 consume 하므로 시스템 볼륨이 변하지 않아
 * 이 ContentObserver 는 발화하지 않는다. Background 진입 후에만 활성.
 */
class ButtonWatchService : Service() {

    companion object {
        private const val NOTIF_CHANNEL = "eject_side_button"
        private const val NOTIF_ID      = 1003

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, ButtonWatchService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ButtonWatchService::class.java))
        }

        /** 사이드 버튼 활성화 여부에 따라 자동으로 시작/중지. */
        fun reconcile(ctx: Context) {
            val cmd = EjectPrefs.loadSideButtonCommand(ctx)
            if (cmd.isEnabled) start(ctx) else stop(ctx)
        }
    }

    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private val detector = ButtonPatternDetector(SideButtonCommand.DISABLED) {
        SideButtonTrigger.fire(this)
    }
    private var lastVolume = 0
    private var observer: ContentObserver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        lastVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        createChannel()
        registerObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // pref 변경에 즉시 반응
        detector.command = EjectPrefs.loadSideButtonCommand(this)
        detector.customSequence = EjectPrefs.loadSideButtonCustomSequence(this)
        try { startForeground(NOTIF_ID, buildNotif()) } catch (_: Exception) {}
        // pref 가 비활성화 상태로 들어왔다면 자기 자신을 종료
        if (!detector.command.isEnabled) {
            stopSelf()
        }
        return START_STICKY
    }

    private fun registerObserver() {
        val obs = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                handleVolumeChange()
            }
        }
        observer = obs
        contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            obs,
        )
    }

    /**
     * 볼륨이 변경되면 부호로 UP/DOWN 을 판별하고 detector 에 전달한다.
     * 같은 값이 두 번 들어올 수도 있으므로 (max/min 도달 시) 변화량 0 도 처리.
     */
    private fun handleVolumeChange() {
        val current = try {
            audioManager.getStreamVolume(AudioManager.STREAM_RING)
        } catch (_: Exception) { return }

        if (current == lastVolume) return

        val isUp = current > lastVolume
        val previous = lastVolume
        // 즉시 원복 — detector 에 전달하기 전에 복원해서 사용자 체감 차이 최소화
        try {
            audioManager.setStreamVolume(
                AudioManager.STREAM_RING,
                previous,
                0, // FLAG 없음 — UI / 사운드 표시 차단
            )
        } catch (_: Exception) {}
        // setStreamVolume 호출이 다시 onChange 를 트리거할 수 있으므로 lastVolume 은 previous 유지
        lastVolume = previous

        if (isUp) detector.onVolumeUp() else detector.onVolumeDown()
    }

    private fun createChannel() {
        val lang = EjectPrefs.loadLanguage(this)
        val strings = lang.strings()
        val ch = NotificationChannel(
            NOTIF_CHANNEL,
            strings.settingSideButton,
            NotificationManager.IMPORTANCE_MIN,
        ).apply { setSound(null, null) }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification {
        val lang = EjectPrefs.loadLanguage(this)
        val strings = lang.strings()
        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle(strings.settingSideButtonArmed)
            .setContentText(strings.settingSideButtonDesc)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        observer?.let {
            try { contentResolver.unregisterContentObserver(it) } catch (_: Exception) {}
        }
        observer = null
        super.onDestroy()
    }
}
