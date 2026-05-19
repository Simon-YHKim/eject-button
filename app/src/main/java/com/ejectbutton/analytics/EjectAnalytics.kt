package com.ejectbutton.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
// v1.1.0 — Clarity 헬퍼는 별도 object 로 두고 여기서 콜만 위임.
// 이렇게 하면 analytics 레이어가 Firebase + Clarity 양쪽을 한 곳에서 부르고
// 호출부 (MainActivity, BillingManager 등) 는 단일 import 로 충분.

/**
 * Firebase Analytics 헬퍼.
 *
 * Round 16 추가. 호출부 전체에서 [FirebaseAnalytics] 인스턴스를 직접 다루지 않고
 * 이 객체의 함수만 부른다. 이벤트 이름과 파라미터 키를 한 곳에 모아두어
 * dashboards / funnels / 광고주 LTV 계산이 같은 스키마를 보도록 한다.
 *
 * Firebase 의 [FirebaseAnalytics] 자체는 lazy 로 첫 [init] 호출 시 만들어진다.
 * 이후엔 [logEjectFired] 같은 헬퍼들이 instance 가 null 이면 조용히 no-op
 * (테스트나 수동 분리 빌드에서도 NPE 가 나지 않도록).
 */
object EjectAnalytics {

    @Volatile private var instance: FirebaseAnalytics? = null

    fun init(ctx: Context) {
        if (instance == null) {
            instance = Firebase.analytics
        }
    }

    /** EJECT 가 실제로 발사된 시점 — 모드(BUTTON/SHAKE/SIDE_BUTTON), 딜레이초, 발신자 ID */
    fun logEjectFired(mode: String, delaySec: Int, scenarioId: String) {
        instance?.logEvent("eject_fired", Bundle().apply {
            putString("mode", mode)
            putLong("delay_sec", delaySec.toLong())
            putString("scenario_id", scenarioId)
        })
        // v1.1.0 — Clarity 정성 funnel: 같은 이벤트를 세션 녹화 위에 마커로 남김.
        EjectClarity.ejectButtonTap(mode, scenarioId)
    }

    /** Actual fake-call overlay display point. No caller name or phone number is uploaded. */
    fun logFakeCallStarted(mode: String, scenarioId: String, callerNamePresent: Boolean) {
        instance?.logEvent("fake_call_started", Bundle().apply {
            putString("mode", mode)
            putString("scenario_id", scenarioId)
            putString("caller_name_present", if (callerNamePresent) "yes" else "no")
        })
        EjectClarity.fakeCallStarted(
            scenarioId = scenarioId,
            callerNamePresent = callerNamePresent,
            mode = mode,
        )
    }

    /** 사용자가 트리거 모드를 바꿨을 때 — funnel 분석에 사용 */
    fun logModeChanged(mode: String) {
        instance?.logEvent("mode_changed", Bundle().apply {
            putString("mode", mode)
        })
    }

    /** 프리미엄 다이얼로그 노출 — 전환 funnel 의 시작 */
    fun logPremiumViewed(reason: String) {
        instance?.logEvent("premium_viewed", Bundle().apply {
            putString("reason", reason)   // gate_mode / gate_time / cta_settings 등
        })
        EjectClarity.premiumPaywallShown(reason)
    }

    /** 프리미엄 구매 성공 (BillingManager 의 PURCHASED 콜백에서) */
    fun logPremiumPurchased(productId: String) {
        instance?.logEvent("premium_purchased", Bundle().apply {
            putString("product_id", productId)
        })
        EjectClarity.premiumPurchaseSuccess(productId)
        EjectClarity.setUserTier("premium") // 구매 직후 user_tier 갱신 → 이후 세션 필터 가능
    }

    /**
     * v1.2 — Conversion event: 사용자가 처음으로 EJECT 를 발사한 순간.
     * Firebase 콘솔에서 'Mark as conversion' 으로 등록 → AdMob 광고 ROI 측정 가능.
     * 한 디바이스에서 1회만 발사 (KEY_FIRST_EJECT_LOGGED 플래그로 dedupe).
     */
    fun logFirstEjectFired(mode: String, scenarioId: String) {
        instance?.logEvent("first_eject_fired", Bundle().apply {
            putString("mode", mode)
            putString("scenario_id", scenarioId)
        })
    }

    /**
     * v1.2 — Conversion event: 첫 시나리오 추가/사용 (활성화 깊이 측정).
     * 가짜 통화 첫 1회 시작 시 logFirstEjectFired 와 함께 발사.
     */
    fun logScenarioFirstUse(scenarioId: String) {
        instance?.logEvent("scenario_first_use", Bundle().apply {
            putString("scenario_id", scenarioId)
        })
    }

    /** 온보딩 완료 — 사용자가 마지막 확인까지 끝내고 다시 보지 않음을 선택한 시점 */
    fun logOnboardingDone(skipFurther: Boolean, stepCount: Int) {
        instance?.logEvent("onboarding_done", Bundle().apply {
            putString("choice", if (skipFurther) "no_more" else "show_again")
            putLong("step_count", stepCount.toLong())
        })
        EjectClarity.onboardingCompleted(stepCount = stepCount, skipFurther = skipFurther)
    }

    /**
     * v1.6.11 — In-App Update (Flexible flow) 의 funnel 이벤트.
     *
     * - logUpdateDownloaded: InstallStateUpdatedListener 가 DOWNLOADED 받은 순간.
     *   사용자가 "재시작" 액션을 보기 전이지만 다운로드 자체는 성공한 시점.
     * - logUpdateRestartClicked: 사용자가 Snackbar "재시작" 액션 탭. completeUpdate()
     *   호출 직전 (cleanup 전) 에 발사 — Firebase SDK 가 worker thread 에서 disk
     *   persist 한 뒤 다음 launch 에 upload 한다.
     *
     * 두 이벤트 비율 (downloaded vs restart_clicked) 이 곧 사용자 자발 갱신율의
     * 객관 지표. v1.7 funnel 확장 시 update_available / update_completed 추가 예정.
     */
    fun logUpdateDownloaded() {
        instance?.logEvent("update_downloaded", Bundle())
    }

    fun logUpdateRestartClicked() {
        instance?.logEvent("update_restart_clicked", Bundle())
    }

    /**
     * v1.6.11 — Application.onCreate 에서 versionCode 가 직전 launch 와 다르면 호출.
     * Play Store 자동 업데이트 / In-App Update / 사이드로드 모두 동일하게 잡힘.
     * Crashlytics 의 update_in_progress custom key (In-App Update flow 에서만 set) 와
     * 결합하면 어떤 경로로 업그레이드된 사용자에게 v1.6.12 회귀가 발생했는지 분리 가능.
     */
    fun logAppUpdated(prevVersionCode: Int, newVersionCode: Int) {
        instance?.logEvent("app_updated", Bundle().apply {
            putLong("prev_version_code", prevVersionCode.toLong())
            putLong("new_version_code", newVersionCode.toLong())
        })
    }
}
