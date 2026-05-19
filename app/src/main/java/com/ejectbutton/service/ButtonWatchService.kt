package com.ejectbutton.service

import android.app.*
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
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
 * Round 10 추가: MediaSession + VolumeProvider 를 '원격 재생' 모드로 등록하면
 * 시스템이 모든 볼륨 키를 AudioManager 보다 먼저 이 세션으로 라우팅한다 → 화면이
 * 꺼진 상태에서도 onAdjustVolume 이 호출되므로 백그라운드·락스크린 모두에서 감지
 * 가능하다. ContentObserver 경로는 다른 미디어 앱이 세션 우선순위를 가져간 경우의
 * fallback 으로 그대로 유지.
 *
 * 동작 범위:
 * - 앱이 Foreground / Background (서비스 살아있음) 에서 동작 — 화면 ON/OFF 무관.
 * - 앱 완전 종료 (스와이프로 recent apps 에서 제거 등) 상태에서는 서비스가 중지되어
 *   동작하지 않음 (안드로이드 제약).
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
        // STREAM_VOICE_CALL 은 통화 중에만 움직이는데, 통화 중에는 아예 감지를
        // 차단(실통화 위에 가짜 전화가 뜨면 안 됨)하므로 여기선 제외한다.
        private val WATCHED_STREAMS = intArrayOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_SYSTEM,
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

    // Round 10 — 화면 OFF 에서도 볼륨 키를 잡기 위한 MediaSession.
    // Remote playback + VolumeProvider 조합으로 시스템이 볼륨 이벤트를 라우팅한다.
    private var mediaSession: MediaSession? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        snapshotAllStreams()
        createChannel()
        registerObserver()
        registerMediaSession()
    }

    /**
     * 화면 OFF / 락스크린 상태에서도 볼륨 키 이벤트를 받기 위한 MediaSession.
     *
     * 원리:
     *  - VolumeProvider.VOLUME_CONTROL_ABSOLUTE 로 만든 "가상 볼륨" 에 대한
     *    원격 재생(remote playback) 세션을 활성화한다.
     *  - 세션이 active 이고 재생 중(STATE_PLAYING) 이면 시스템은 볼륨 키를
     *    AudioManager 보다 먼저 이 세션의 onAdjustVolume 에 전달한다.
     *  - currentVolume 을 그대로 두면 시스템 HUD 도 뜨지 않는다.
     *  - 다른 미디어 앱이 포커스를 가져가면 이 경로가 막히지만, 그 경우에도
     *    ContentObserver fallback 이 동작한다 (사용자 미디어 앱의 볼륨이 변할 때).
     */
    private fun registerMediaSession() {
        val session = MediaSession(this, "EjectButton-ButtonWatch")

        // FLAG_HANDLES_MEDIA_BUTTONS + TRANSPORT_CONTROLS: 시스템이 볼륨/미디어
        // 버튼 이벤트를 이 세션 쪽으로 최우선 라우팅하게 한다. flags 를 안 주면
        // 세션이 'active' 더라도 다른 앱이 우선권을 쥐기 쉽다.
        @Suppress("DEPRECATION")
        session.setFlags(
            MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        session.setPlaybackToRemote(buildVolumeProvider())
        session.setCallback(object : MediaSession.Callback() {
            // 빈 Callback — play/pause 같은 미디어 제어는 무시하지만 세션이
            // callback 을 가지고 있어야 시스템이 active 로 취급한다.
            override fun onPlay() {}
            override fun onPause() {}
            override fun onStop() {}
        })

        // 메타데이터 — 시스템이 세션을 "media" 로 인식하는 데 도움. 표시될 일은 없음.
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, "Eject Button")
            .putString(MediaMetadata.METADATA_KEY_ARTIST, "Standby")
            .build()
        session.setMetadata(metadata)

        // MediaButtonReceiver — 시스템이 볼륨/미디어 버튼 이벤트를 서비스로 전달.
        val mbIntent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            component = ComponentName(this@ButtonWatchService, ButtonWatchService::class.java)
        }
        val mbPi = PendingIntent.getService(
            this, 0, mbIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session.setMediaButtonReceiver(mbPi)

        // 활성 + 재생 상태로 선언해야 볼륨 키 라우팅이 열린다.
        val playbackState = PlaybackState.Builder()
            .setState(PlaybackState.STATE_PLAYING, 0L, 1.0f)
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_STOP
            )
            .build()
        session.setPlaybackState(playbackState)
        session.isActive = true
        mediaSession = session
    }

    /**
     * 볼륨 키 이벤트를 가상 볼륨으로 흡수하면서 detector 에 전달.
     * currentVolume 은 절대 건드리지 않아 시스템 HUD 가 뜨지 않는다.
     */
    private fun buildVolumeProvider(): VolumeProvider =
        object : VolumeProvider(
            VOLUME_CONTROL_ABSOLUTE,
            100,   // max
            50,    // fixed current volume — 변경 안 함
        ) {
            override fun onAdjustVolume(direction: Int) {
                if (!detector.command.isEnabled || !EjectPrefs.loadSideButtonArmed(this@ButtonWatchService)) {
                    return
                }
                // 통화 중에는 가짜 전화가 뜨면 안 되므로 무시.
                if (isInActiveCall()) return

                when {
                    direction > 0 -> detector.onVolumeUp()
                    direction < 0 -> detector.onVolumeDown()
                }
            }

            override fun onSetVolumeTo(volume: Int) { /* no-op — 가상 볼륨은 고정 */ }
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

    /**
     * 실통화 중인지 안전하게 확인. v1.6.10 핫픽스 — 일부 단말 (Samsung Android 16
     * SM-S948N 등) 은 매니페스트에 READ_PHONE_STATE 가 선언돼 있어도 runtime grant
     * 가 없으면 [TelephonyManager.callState] 호출 시 SecurityException 을 던진다.
     * 권한 결핍은 정상적인 런타임 상태로 취급하고 "통화 중 아님 = 진행 허용" 으로
     * graceful degrade.
     */
    private fun isInActiveCall(): Boolean {
        return try {
            val tm = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager
            tm != null && tm.callState != TelephonyManager.CALL_STATE_IDLE
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
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
     * 기준선 갱신 전략 (과거 버그의 원인이었던 snapshotAllStreams 문제 수정):
     *
     *  - 감지된 스트림의 `last` 기준선은 '강제로' last 로 유지한다. 원복용
     *    setStreamVolume 이 일부 기기에서 비동기로 처리되거나 실패해서 그 직후
     *    snapshot 이 아직 복원되지 않은 값을 읽어오면, 이어지는 echo 콜백이
     *    `current != last` 로 해석되어 유령 VOL_UP 이벤트를 만들고 detector 의
     *    큐를 오염시키는 문제가 있었다. 기준선을 last 로 고정하면 echo 가 어떤
     *    타이밍/값으로 오든 자연스럽게 no-op 된다.
     *
     *  - 그 외 스트림은 '현재 값' 으로 스냅샷한다. 한 번의 키 입력이 MUSIC 과
     *    RING 등 여러 스트림을 동시에 내릴 수 있기 때문에, 처리한 스트림
     *    이외의 스트림도 새 기준선으로 받아들여야 다음 사이클에 중복 감지되지
     *    않는다.
     *
     *  - 서비스 재시작 등으로 detector.command 가 DISABLED 로 남아있는 edge
     *    case 를 대비해 방어적으로 pref 를 재로드.
     */
    private fun handleVolumeChange() {
        // 실통화 중이면 볼륨 키는 VOICE_CALL 조절용이고, 가짜 전화가
        // 그 위에 뜨는 건 명백히 잘못된 동작. 감지 자체를 short-circuit.
        if (isInActiveCall()) {
            // 통화 종료 후 곧바로 이전 볼륨과 비교해 유령 이벤트가 나지 않도록
            // 기준선도 현재 값으로 재스냅샷.
            snapshotAllStreams()
            return
        }

        if (!detector.command.isEnabled) {
            detector.command = EjectPrefs.loadSideButtonCommand(this)
            detector.customSequence = EjectPrefs.loadSideButtonCustomSequence(this)
        }

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

            // 처리한 스트림 외 나머지 스트림은 현재 값으로 스냅샷 (동시 하강 흡수).
            for (s in WATCHED_STREAMS) {
                if (s == stream) continue
                try {
                    lastVolumes[s] = audioManager.getStreamVolume(s)
                } catch (_: Exception) {}
            }
            // 감지된 스트림은 last 로 강제 고정 — 비동기 원복 / 실패 / echo 모두 흡수.
            lastVolumes[stream] = last

            if (isUp) detector.onVolumeUp() else detector.onVolumeDown()
            return
        }
    }

    private fun createChannel() {
        val lang = EjectPrefs.loadLanguage(this)
        val strings = lang.strings()
        val ch = NotificationChannel(
            NOTIF_CHANNEL,
            strings.sideButtonChannelName,
            NotificationManager.IMPORTANCE_MIN,
        ).apply { setSound(null, null) }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
    }

    private fun buildNotif(): Notification {
        val lang = EjectPrefs.loadLanguage(this)
        val strings = lang.strings()
        return Notification.Builder(this, NOTIF_CHANNEL)
            .setContentTitle(strings.sideButtonNotifTitle)
            .setContentText(strings.sideButtonNotifText)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        observer?.let {
            try { contentResolver.unregisterContentObserver(it) } catch (_: Exception) {}
        }
        observer = null
        mediaSession?.let {
            try {
                it.isActive = false
                it.release()
            } catch (_: Exception) {}
        }
        mediaSession = null
        super.onDestroy()
    }
}
