package com.ejectbutton.data

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * v1.6.10 — EJECT 발사 직전에 trigger 모드별 필수 권한이 모두 충족되는지 검사하는 게이트.
 *
 * 이전(v1.6.9 까지)에는 [com.ejectbutton.MainActivity.requestInitialPermissionsIfNeeded] 가
 * 온보딩 직후 한 번만 권한을 요청하고 `perms_requested` flag 로 재요청을 영구 차단했다.
 * 사용자가 거부하면 EJECT 가 silent fail — 특히 배터리 최적화 제외 거부 시 screen-off 상태에서
 * SHAKE/SIDE_BUTTON 트리거가 반응 안 함 → 사용자는 "앱이 망가졌다" 고 인식.
 *
 * v1.6.10 의 동작: EJECT 누른 시점에 [missing] 으로 미충족 권한 목록을 확보. 비어있으면 그대로
 * arm/fire 진행. 하나라도 빠지면 [com.ejectbutton.ui.main.PermissionGateDialog] 가 사용자에게
 * 다시 안내하고 system intent 를 순차 launch. 모두 grant 되면 비로소 arm/fire.
 *
 * 거부 결과는 저장하지 않으므로 다음 EJECT 시 다시 prompt 된다 (사용자 요청 spec).
 */
object PermissionGate {

    enum class Req {
        /** [Settings.canDrawOverlays] — FakeCallOverlayService 가 통화 UI 를 그리는 데 필수. */
        OVERLAY,

        /** [Manifest.permission.POST_NOTIFICATIONS] — API 33+ 에서 FGS 의 상태바 알림 표시 필수. */
        POST_NOTIFICATIONS,

        /** [PowerManager.isIgnoringBatteryOptimizations] — screen-off / Doze 상태에서 SHAKE·SIDE_BUTTON·delayed 트리거 service 가 살아있게 함. */
        BATTERY_OPT,
    }

    /**
     * 주어진 mode + delay 조합이 정상 작동하려면 어떤 권한이 필요한가.
     * - OVERLAY: 모든 모드 필수 (가짜 통화 화면 그리기).
     * - POST_NOTIFICATIONS: API 33+ 항상 (FGS 알림 가시성).
     * - BATTERY_OPT: 화면 꺼진 채 살아남아야 하는 모드 — SHAKE / SIDE_BUTTON / delay ≥ 10s.
     *   IMMEDIATE (delayMs=0) 는 사용자가 앱 열린 상태에서 누르므로 BATTERY_OPT 불필요.
     */
    fun requiredFor(mode: TriggerMode, delayMs: Long): Set<Req> {
        val reqs = mutableSetOf(Req.OVERLAY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            reqs.add(Req.POST_NOTIFICATIONS)
        }
        val needsBackgroundSurvival = when (mode) {
            TriggerMode.SHAKE, TriggerMode.SIDE_BUTTON -> true
            else -> delayMs >= 10_000L  // 10초 이상은 화면 꺼질 가능성 있음
        }
        if (needsBackgroundSurvival) reqs.add(Req.BATTERY_OPT)
        return reqs
    }

    fun isGranted(ctx: Context, req: Req): Boolean = when (req) {
        Req.OVERLAY -> Settings.canDrawOverlays(ctx)
        Req.POST_NOTIFICATIONS -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
            else ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        Req.BATTERY_OPT -> {
            val pm = ctx.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(ctx.packageName) ?: true
        }
    }

    /**
     * Trigger 모드 기준 미충족 권한 목록. 비어있으면 즉시 arm 가능.
     * 순서는 [Req] enum 선언 순서 (OVERLAY → POST_NOTIFICATIONS → BATTERY_OPT) — UI 가 첫 번째 항목부터
     * 사용자에게 안내하면 자연스러운 흐름.
     */
    fun missing(ctx: Context, mode: TriggerMode, delayMs: Long): List<Req> =
        requiredFor(mode, delayMs)
            .sortedBy { it.ordinal }
            .filterNot { isGranted(ctx, it) }
}
