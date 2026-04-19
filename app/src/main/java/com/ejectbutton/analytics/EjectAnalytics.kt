package com.ejectbutton.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

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
    }

    /** 프리미엄 구매 성공 (BillingManager 의 PURCHASED 콜백에서) */
    fun logPremiumPurchased(productId: String) {
        instance?.logEvent("premium_purchased", Bundle().apply {
            putString("product_id", productId)
        })
    }

    /** 온보딩 완료 — "다시 안 봄" / "다음에 또 봄" 분기 */
    fun logOnboardingDone(skipFurther: Boolean) {
        instance?.logEvent("onboarding_done", Bundle().apply {
            putString("choice", if (skipFurther) "no_more" else "show_again")
        })
    }
}
