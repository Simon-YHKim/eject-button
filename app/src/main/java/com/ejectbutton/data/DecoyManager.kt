package com.ejectbutton.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * v1.2.0 — 위장 (Decoy) launcher 아이콘 관리.
 *
 * AndroidManifest.xml 에 4 개의 `<activity-alias>` 가 비활성 상태로 등록되어 있다.
 * 사용자가 설정 → "위장 아이콘" 에서 옵션을 선택하면 PackageManager 로 alias 의
 * enabled 상태를 토글한다.
 *
 * 동작 모델:
 *  - DEFAULT 선택: MainActivity ENABLED + 4 alias DISABLED → 일반 "Eject Button" 아이콘
 *  - CALCULATOR 선택: MainActivity DISABLED + Calculator alias ENABLED → "계산기" 아이콘
 *  - 그 외 alias 도 동일.
 *
 * 토글 시 사용자 경험 주의사항:
 *  - launcher 가 새 alias 를 인식하기까지 1-2 초 지연이 있을 수 있음.
 *  - 일부 launcher 는 캐시 때문에 화면을 한 번 새로 고침해야 새 아이콘 표시.
 *  - DONT_KILL_APP 플래그 사용 → 토글 도중 앱이 강제 종료되지는 않음.
 *
 * 가정폭력 / 강요 환경에서의 가치:
 *  - "Eject Button" 이라는 이름이 폰 화면에 노출되지 않음.
 *  - 옆 사람이 폰을 봐도 평범한 계산기 / 메모장 / 날씨 / 시계 앱으로 보임.
 *  - 사용자는 그 위장 아이콘을 탭하면 동일하게 Eject Button 이 실행됨 (alias targetActivity).
 */
object DecoyManager {

    /**
     * 사용 가능한 위장 옵션. componentName 은 manifest 의 alias android:name 과
     * 정확히 일치해야 한다.
     */
    enum class Decoy(val componentName: String) {
        DEFAULT("com.ejectbutton.MainActivity"),
        CALCULATOR("com.ejectbutton.launcher.Calculator"),
        MEMO("com.ejectbutton.launcher.Memo"),
        WEATHER("com.ejectbutton.launcher.Weather"),
        CLOCK("com.ejectbutton.launcher.Clock"),
        ;

        companion object {
            fun fromName(name: String?): Decoy =
                values().firstOrNull { it.name == name } ?: DEFAULT
        }
    }

    /**
     * [target] 만 ENABLED 로 만들고 나머지는 모두 DISABLED 로 설정한다.
     * EjectPrefs 에 선택값도 함께 저장해 다음 실행 시 UI 의 라디오 상태 복원에 사용.
     */
    fun setActive(ctx: Context, target: Decoy) {
        val pkg = ctx.packageName
        val pm  = ctx.packageManager
        Decoy.values().forEach { decoy ->
            val component = ComponentName(pkg, decoy.componentName)
            val state = if (decoy == target) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            // DONT_KILL_APP — 토글 도중 앱이 강제 종료되지 않도록 한다.
            // 단, manifest 의 default state 와 다를 때만 PackageManager 가 실제로 변경.
            runCatching {
                pm.setComponentEnabledSetting(component, state, PackageManager.DONT_KILL_APP)
            }
        }
        EjectPrefs.saveDecoy(ctx, target)
    }

    /**
     * 현재 활성 alias 를 PackageManager 에 직접 조회. EjectPrefs 가 sync 안 됐을
     * 가능성에 대비한 fallback 으로 사용.
     */
    fun getActive(ctx: Context): Decoy {
        // 우선 prefs 값 신뢰. 없으면 PackageManager 조회.
        val saved = EjectPrefs.loadDecoy(ctx)
        if (saved != Decoy.DEFAULT) return saved

        val pkg = ctx.packageName
        val pm  = ctx.packageManager
        return Decoy.values().firstOrNull { decoy ->
            val component = ComponentName(pkg, decoy.componentName)
            pm.getComponentEnabledSetting(component) ==
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } ?: Decoy.DEFAULT
    }
}
