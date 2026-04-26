package com.ejectbutton.consent

import android.app.Activity
import android.content.Context
import android.util.Log
import com.ejectbutton.BuildConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

/**
 * v1.2 — UMP (User Messaging Platform) SDK 래퍼.
 *
 * 목적: GDPR/EEA 사용자가 앱 진입 시 광고 동의 다이얼로그를 표시.
 *  - AdMob 대시보드 → Privacy & messaging → GDPR message 가 'Published' 여야 실제 노출.
 *  - 동의 거부 시 personalized ad 비활성화 → non-personalized ad 만 송출 (광고 자체는 유지).
 *  - canRequestAds() == true 여야 MobileAds.initialize() 호출이 안전함.
 *
 * 위기 사용자 보호 (Eject Button 도메인 특수):
 *  - 동의 거부 사용자도 앱 핵심 기능 (가짜 통화) 은 100% 동작해야 함.
 *  - 단, 개인화 광고 차단으로 광고 매출 감소 가능 → free 사용자는 native ad 만 노출 유지.
 *
 * 디버그 EEA 시뮬레이션:
 *  - debug 빌드는 ConsentDebugSettings.DEBUG_GEOGRAPHY_EEA 강제 → 한국에서도 다이얼로그 검증 가능.
 *  - release 빌드는 사용자 IP 기반 자동 판단.
 */
object ConsentManager {

    private const val TAG = "ConsentManager"

    /**
     * 앱 시작 시 1회 호출. consent 정보 갱신 + 필요 시 다이얼로그 자동 표시.
     *
     * @param onCanRequestAds 동의 결과와 무관하게 광고 SDK 초기화 가능 여부 콜백.
     *                        true → MobileAds.initialize() 호출.
     *                        false → consent 미충족, 광고 미초기화 (앱 기능은 정상).
     */
    fun gather(activity: Activity, onCanRequestAds: (Boolean) -> Unit) {
        val params = buildRequestParams(activity)
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                // 정보 갱신 성공 — consent form 필요 여부 판단 후 표시.
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "Consent form error: ${formError.errorCode} ${formError.message}")
                    }
                    val canRequest = consentInformation.canRequestAds()
                    Log.d(TAG, "Consent flow finished. canRequestAds=$canRequest")
                    onCanRequestAds(canRequest)
                }
            },
            { requestError ->
                // 정보 갱신 실패 — 네트워크/시간 초과 등. 보수적으로 광고 미초기화.
                Log.w(TAG, "Consent info update failed: ${requestError.errorCode} ${requestError.message}")
                onCanRequestAds(consentInformation.canRequestAds())
            },
        )
    }

    /**
     * 사용자가 설정에서 'Privacy options' 다시 열고 싶을 때 호출.
     * isPrivacyOptionsRequired() == true 일 때만 의미 있음 (EEA 사용자만).
     */
    fun showPrivacyOptions(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) {
                Log.w(TAG, "Privacy options error: ${error.errorCode} ${error.message}")
            }
            onDismissed()
        }
    }

    /**
     * Settings 화면에서 'Privacy options' 메뉴 노출 여부 결정용.
     * EEA 외 사용자는 false 반환 → 메뉴 숨김.
     */
    fun isPrivacyOptionsRequired(context: Context): Boolean {
        val info = UserMessagingPlatform.getConsentInformation(context)
        return info.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /**
     * 광고 SDK 가 호출해도 안전한지 즉시 확인.
     * gather() 가 끝난 후의 캐시된 결과를 반환.
     */
    fun canRequestAds(context: Context): Boolean {
        return UserMessagingPlatform.getConsentInformation(context).canRequestAds()
    }

    private fun buildRequestParams(activity: Activity): ConsentRequestParameters {
        val builder = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)

        if (BuildConfig.DEBUG) {
            // debug 빌드: EEA 시뮬레이션 + 본인 디바이스만 테스트 (다른 사용자 영향 X).
            // 실제 디바이스 ID 는 Logcat 에서 "Use new ConsentDebugSettings.Builder()
            // .addTestDeviceHashedId(\"XXX\")" 메시지로 안내됨. 비워두면 모든 디버그 디바이스 적용.
            val debugSettings = ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .build()
            builder.setConsentDebugSettings(debugSettings)
        }

        return builder.build()
    }
}
