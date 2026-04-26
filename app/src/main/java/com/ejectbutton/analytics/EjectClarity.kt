package com.ejectbutton.analytics

import android.content.Context
import com.ejectbutton.BuildConfig
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.microsoft.clarity.models.ApplicationFramework
import com.microsoft.clarity.models.LogLevel

/**
 * Microsoft Clarity 헬퍼.
 *
 * v1.1.0 추가. Firebase Analytics 와 병행 운영:
 *  - Firebase Analytics → 정량 funnel (Eject fired count, premium conversion rate)
 *  - Clarity → 정성 세션 녹화 + Custom Tag 로 같은 funnel 을 시각적 검증
 *
 * 호출부에서 SDK 직접 의존 없이 [EjectClarity.foo(...)] 만 부른다 → 향후
 * SDK 교체나 테스트 빌드 분리 (예: F-Droid 빌드에서 Clarity 제거) 가 쉬워짐.
 *
 * 모든 메서드는 init 전 호출돼도 SDK 가 자체적으로 buffer 하므로 NPE 안남.
 */
object EjectClarity {

    /**
     * Application.onCreate() 에서 한 번만 호출. CLARITY_PROJECT_ID 가 비어 있으면
     * (debug fallback 미설정 환경) 초기화를 건너뛴다.
     *
     * Privacy 기본값은 SDK 가 "BalancedMode" — 텍스트 노출은 하되 EditText/암호 필드는
     * 자동 마스킹. 추가 마스킹은 호출부에서 [maskView] 로 지정.
     */
    fun init(context: Context) {
        val projectId = BuildConfig.CLARITY_PROJECT_ID
        if (projectId.isBlank()) return
        val config = ClarityConfig(
            projectId = projectId,
            // debug 빌드만 Verbose 로그 → release 는 noise 차단 (배터리·로그 채널 보호).
            logLevel = if (BuildConfig.DEBUG) LogLevel.Verbose else LogLevel.None,
            // Compose 앱이므로 명시적으로 알림 (Clarity 가 Compose tree 를 정확히 다루도록).
            applicationFramework = ApplicationFramework.Native,
            // SDK 디폴트 BalancedMode — 입력 필드 자동 마스킹 + 텍스트는 노출.
            // Compose TextField 도 이 모드에선 마스킹된다 (위기 사용자 PII 보호).
            // 필요 시 Strict 로 격상 가능. 위기 상황 사용자 = 보수적 기본값 유지.
        )
        Clarity.initialize(context, config)
    }

    /**
     * 세션 시작 직후 호출하면 Clarity 대시보드에서 "한국어 사용자만" 같은 필터가 가능.
     * Premium 상태가 바뀌면 다시 호출해서 user_tier 갱신.
     */
    fun setSessionTags(locale: String, userTier: String) {
        runCatching {
            Clarity.setCustomTag("locale", locale)
            Clarity.setCustomTag("user_tier", userTier)
        }
    }

    fun setUserTier(tier: String) {
        runCatching { Clarity.setCustomTag("user_tier", tier) }
    }

    // ── 핵심 사용자 여정 이벤트 ────────────────────────────────────────────

    /** 메인 EJECT 버튼 탭 — 모드와 발신자 시나리오를 태그로 기록 */
    fun ejectButtonTap(mode: String, scenarioId: String) {
        runCatching {
            Clarity.setCustomTag("scenario_id", scenarioId)
            Clarity.sendCustomEvent("eject_button_tap")
            Clarity.setCustomTag("last_eject_mode", mode)
        }
    }

    /** 가짜 통화 시작 — overlay/incall service 진입 시점 */
    fun fakeCallStarted(scenarioId: String, callerName: String, mode: String) {
        runCatching {
            Clarity.setCustomTag("scenario_id", scenarioId)
            Clarity.setCustomTag("caller_name_present", if (callerName.isNotBlank()) "yes" else "no")
            Clarity.setCustomTag("call_mode", mode)
            Clarity.sendCustomEvent("fake_call_started")
        }
    }

    /** 가짜 통화 종료 — duration / 종료 사유 (user_hangup, timeout, system) */
    fun fakeCallEnded(durationSeconds: Long, endReason: String) {
        runCatching {
            Clarity.setCustomTag("last_call_duration_sec", durationSeconds.toString())
            Clarity.setCustomTag("last_call_end_reason", endReason)
            Clarity.sendCustomEvent("fake_call_ended")
        }
    }

    /** 시나리오/발신자 선택 — chip click */
    fun scenarioSelected(scenarioType: String) {
        runCatching {
            Clarity.setCustomTag("scenario_type", scenarioType)
            Clarity.sendCustomEvent("scenario_selected")
        }
    }

    /** 결제 다이얼로그 노출 — trigger 컨텍스트 (gate_caller, cta_systems_card 등) */
    fun premiumPaywallShown(triggerContext: String) {
        runCatching {
            Clarity.setCustomTag("paywall_trigger", triggerContext)
            Clarity.sendCustomEvent("premium_paywall_shown")
        }
    }

    /** 구매 버튼 클릭 (실제 결제 launch 직전) */
    fun premiumUpgradeClicked(sku: String) {
        runCatching {
            Clarity.setCustomTag("paywall_sku", sku)
            Clarity.sendCustomEvent("premium_upgrade_clicked")
        }
    }

    /** 결제 성공 (PURCHASED 콜백) */
    fun premiumPurchaseSuccess(sku: String) {
        runCatching {
            Clarity.setCustomTag("purchased_sku", sku)
            Clarity.sendCustomEvent("premium_purchase_success")
        }
    }

    /** 권한 요청 결과 — 전화/연락처/알림 등 */
    fun permissionResult(permission: String, granted: Boolean) {
        runCatching {
            Clarity.setCustomTag("last_permission", permission)
            Clarity.sendCustomEvent(
                if (granted) "permission_${permission}_granted"
                else         "permission_${permission}_denied"
            )
        }
    }

    /** 온보딩 완료 — step_count + skip 여부 */
    fun onboardingCompleted(stepCount: Int, skipFurther: Boolean) {
        runCatching {
            Clarity.setCustomTag("onboarding_steps", stepCount.toString())
            Clarity.setCustomTag("onboarding_skip", if (skipFurther) "yes" else "no")
            Clarity.sendCustomEvent("onboarding_completed")
        }
    }
}
