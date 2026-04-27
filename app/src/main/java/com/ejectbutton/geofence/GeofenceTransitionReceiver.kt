package com.ejectbutton.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.SideButtonCommand
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/**
 * v1.4.0 — GeofencingClient 가 진입/이탈 감지 시 호출하는 BroadcastReceiver.
 *
 * 동작:
 *  - GEOFENCE_TRANSITION_ENTER → 사용자 설정에 따라 sidebtn 모드 ON.
 *    진입 직전 모드를 [EjectPrefs] 의 KEY_SIDE_BUTTON_PREV_MODE 에 기록.
 *  - GEOFENCE_TRANSITION_EXIT → 기록된 이전 모드로 원복.
 *
 * 권한 거부 / 위치 서비스 OFF / API 오류 등 발생 시 안전하게 묵음 처리 — 앱이 죽지 않음.
 *
 * 가정폭력 use case 에서의 동작:
 *  - 위험 장소 (예: 가해자 거주지) 도달 직전부터 sidebtn ON → 측면 버튼 1번으로 즉시 EJECT.
 *  - 안전 영역 복귀 시 자동 OFF → 일상 사용 시 오발 방지.
 */
class GeofenceTransitionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent)
        if (event == null) {
            Log.w(TAG, "GeofencingEvent.fromIntent returned null")
            return
        }
        if (event.hasError()) {
            Log.w(TAG, "Geofence error: code=${event.errorCode}")
            return
        }

        val transition = event.geofenceTransition
        val triggered = event.triggeringGeofences.orEmpty()
        if (triggered.isEmpty()) {
            Log.w(TAG, "No triggering geofences")
            return
        }

        when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> handleEnter(context, triggered)
            Geofence.GEOFENCE_TRANSITION_EXIT  -> handleExit(context, triggered)
            else -> Log.d(TAG, "Unhandled transition type: $transition")
        }
    }

    /**
     * 등록된 위치 진입 시: 현재 모드를 백업 + sidebtn 모드 ON.
     * 같은 위치에 이미 진입한 상태에서 또 ENTER 가 와도 (재시작 등 OS edge case),
     * KEY_SIDE_BUTTON_PREV_MODE 가 이미 있으면 덮어쓰지 않음 (DISABLED 가 백업으로 남아야 정상 복귀).
     */
    private fun handleEnter(context: Context, geofences: List<Geofence>) {
        val ids = geofences.joinToString { it.requestId }
        Log.i(TAG, "ENTER: $ids — sidebtn mode → VOL_UP_DOUBLE")

        val current = EjectPrefs.loadSideButtonCommand(context)
        // 이미 sidebtn 활성화 모드면 백업 안 하고 그대로 (재진입 방지).
        // 처음 ENTER (백업 prev 없음) 일 때만 현재 모드를 저장 — EXIT 시 원복용.
        if (!EjectPrefs.hasGeofencePrevMode(context)) {
            EjectPrefs.saveGeofencePrevMode(context, current.name)
        }
        // 지오펜스 활성 시 default 사이드 버튼 명령. VOL_UP_DOUBLE 이 가장 자연스러운
        // 빠른 trigger (볼륨 위 두 번 = ~0.5s 액션).
        // 다음 세션 — 사용자가 GeofenceConfig 에서 trigger mode 자유 선택 가능하게 확장.
        EjectPrefs.saveSideButtonCommand(context, SideButtonCommand.VOL_UP_DOUBLE)
    }

    /**
     * 위치 이탈 시: 백업해둔 이전 모드로 원복. 백업이 없으면 DISABLED 로 안전 fallback.
     */
    private fun handleExit(context: Context, geofences: List<Geofence>) {
        val ids = geofences.joinToString { it.requestId }
        val prev = EjectPrefs.loadGeofencePrevMode(context)
        Log.i(TAG, "EXIT: $ids — restore mode → ${prev ?: "DISABLED (fallback)"}")

        val restoreMode = prev?.let { runCatching { SideButtonCommand.valueOf(it) }.getOrNull() }
            ?: SideButtonCommand.DISABLED
        EjectPrefs.saveSideButtonCommand(context, restoreMode)
        EjectPrefs.clearGeofencePrevMode(context)
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
