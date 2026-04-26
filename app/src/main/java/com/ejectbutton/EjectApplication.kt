package com.ejectbutton

import android.app.Application
import com.ejectbutton.analytics.EjectClarity
import com.ejectbutton.crash.CrashReportManager
import com.ejectbutton.data.EjectPrefs
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.Locale

class EjectApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 자체 크래시 리포팅 (사용자가 메일 보내야 수신 가능, 상세 stack trace 우편 첨부).
        // 사용자 협조에 의존하므로 보고율은 낮지만 (~1-5%) 첨부 정보가 풍부함.
        CrashReportManager.initialize(this)

        // v1.0.10 — Firebase Crashlytics 도 병행. release 빌드만 자동 수집.
        // Crashlytics 는 100% 자동 클라우드 수집 → 미발견 크래시 즉시 인지 가능.
        // debug 빌드는 개발자 디버그 노이즈 방지로 비활성화.
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // v1.2 — Crashlytics 커스텀 키. 크래시 필터/그룹핑 향상.
        // PII 금지: 전화번호/실명/주소 절대 넣지 말 것 (위기 사용자 보호).
        val locale = Locale.getDefault().toLanguageTag()
        val userTier = if (EjectPrefs.loadPremium(this)) "premium" else "free"
        crashlytics.setCustomKey("user_tier", userTier)
        crashlytics.setCustomKey("app_locale", locale)
        crashlytics.setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")

        // v1.1.0 — Microsoft Clarity 세션 녹화 + Custom Tag.
        // Firebase Analytics 의 정량 funnel 을 정성 세션 녹화로 보강.
        // CLARITY_PROJECT_ID 가 비어 있으면 (debug fallback 미설정) init 자체가 no-op.
        EjectClarity.init(this)
        EjectClarity.setSessionTags(
            locale  = locale,
            userTier = userTier,
        )
    }
}
