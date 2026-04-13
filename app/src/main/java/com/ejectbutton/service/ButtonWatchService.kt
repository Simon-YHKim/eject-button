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
 * - 디바이스/상황마다 볼륨 키가 조절하는 스트림이 다르기 때문에 여러 스트림
 *   (MUSIC / RING / NOTIFICATION / ALARM / SYSTEM / VOICE_CALL) 을 모두 폴링
 * - 변화 부호로 UP/DOWN 을 판별, [ButtonPatternDetector] 에 전달
 * - 패턴 매칭 시 [SideButtonTrigger.fire] 호출. 가능한 경우 볼륨을 원복
 *   (일부 기기/버전에서는 시스템 볼륨 HUD 를 완전히 막을 수 없음 — 그 경우
 *    감지 자체는 여전히 동작한다)
 *
 * 동작 범위 (의도적):
 * - 앱이 Foreground / Background (서비스 살아있음) 일 때만 동작
 * - 화면 OFF 또는 앱 완전 종료 상태에서는 동작하지 않음
 *
 * 시작/중지:
 * - 사용자가 메인 화면에서 mode=SIDE_BUTTON 을 선택하면 armed=true 로 [start]
 * - 다른 모드로 바꾸면 armed=false 로 [stop]
 *
 * 주의: Foreground 상태에서는 [com.ejectbutton.MainActivity.onKeyDown] 이
 * 볼륨 키를 먼저 가로채 consume 하므로 시스템 볼륨이 변하지 않아
 * 이 ContentObserver 는 발화하지 않는다. Background 진입 후에만 활성.
 */
class ButtonWatchService : Service() {

    companion object {
        private const val NOTIF_CHANNEL = "eject_side_button"
        private const val NOTIF_ID      = 1003

        // 디바이스/상황마다 볼륨 키가 영향을 주는 스트림이 다르므로 모두 감시.
        private val WATCHED_STREAMS = intArrayOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_VOICE_CALL,
        )

        fun start(ctx: Context) {
            ctx.startForegroundService(Intent(ctx, ButtonWatchService::class.java))
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ButtonWatchService::class.java))
        }

        /**
         * 사이드 버튼 활성화 여부에 따라 자동으로 시작/중지.
         * armed 플래그와 명령 설정이 둘 다 켜져 있어야 watcher 가 동작한다.
         * (armed 는 MainScreen 에서 mode=SIDE_BUTTON 일 때만 true 로 설정)
         */
        fun reconcile(ctx: Context) {
            val cmd = EjectPrefs.loadSideButtonCommand(ctx)
            val armed = EjectPrefs.loadSideButtonArmed(ctx)
            if (cmd.isEnabled && armed) start(ctx) else stop(ctx)
        }
    }

    private lateinit var audioManager: AudioManager
    private val handler = Handler(Looper.getMainLooper())
    private val detector = ButtonPatternDetector(SideButtonCommand.DISABLED) {
        SideButtonTrigger.fire(this)
    }
    private val lastVolumes = mutableMapOf<Int, Int>()
    private var observer: ContentObserver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        snapshotAllStreams()
        createChannel()
        registerObserver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // pref 변경에 즉시 반응
        detector.command = EjectPrefs.loadSideButtonCommand(this)
        detector.customSequence = EjectPrefs.loadSideButtonCustomSequence(this)
        try { startForeground(NOTIF_ID, buildNotif()) } catch (_: Exception) {}
        // pref 가 비활성화 상태 또는 armed 가 꺼진 상태로 들어왔다면 자기 자신을 종료
        if (!detector.command.isEnabled || !EjectPrefs.loadSideButtonArmed(this)) {
            stopSelf()
        }
        // 현재 볼륨으로 기준선 재설정
        snapshotAllStreams()
        return START_STICKY
    }

    private fun snapshotAllStreams() {
        for (stream in WATCHED_STREAMS) {
            try {
                lastVolumes[stream] = audioManager.getStreamVolume(stream)
            } catch (_: Exception) {}
        }
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
     * ContentObserver 는 Settings.System 의 어떤 값이든 변경되면 호출되므로
     * 모든 감시 스트림을 순회하며 실제로 값이 변한 스트림을 찾는다.
     *
     * 과거에는 setStreamVolume 원복 호출의 echo 를 차단하기 위해
     * suppressNextChange 플래그를 썼지만, 일부 Android 버전/기기에서
     * setStreamVolume 이 ContentObserver 콜백을 전혀 발생시키지 않는
     * 경우가 있어 플래그가 영구적으로 true 로 굳어 감지가 완전히 죽는
     * 버그가 있었다. 플래그를 버리고 다음 전략으로 대체:
     *
     *  1) 실제 변화를 발견하면 detector 에 먼저 통지한다.
     *  2) 원복(setStreamVolume) 을 시도한 뒤 snapshotAllStreams() 로
     *     모든 감시 스트림을 재스냅샷한다. 이후 발생하는 echo 콜백은
     *     current == last 로 평가되어 자연스럽게 no-op 된다.
     *  3) 따라서 echo 가 오든 오지 않든 상태가 스스로 복구된다.
     */
    private fun handleVolumeChange() {
        for (stream in WATCHED_STREAMS) {
            val current = try {
                audioManager.getStreamVolume(stream)
            } catch (_: Exception) { continue }

            val last = lastVolumes[stream] ?: continue
            if (current == last) continue

            val isUp = current > last

            // 볼륨 HUD 를 숨기기 위해 즉시 원복 시도 (일부 기기에서는 무효).
            try {
                audioManager.setStreamVolume(stream, last, 0)
            } catch (_: Exception) {}

            // 재스냅샷: 원복 후의 값으로 모든 스트림 기준선을 갱신해
            // 이어지는 echo 콜백이 no-op 되도록 한다.
            snapshotAllStreams()

            if (isUp) detector.onVolumeUp() else detector.onVolumeDown()
            return
        }
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
