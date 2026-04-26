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
        FirebaseCrashlytics.getInstance()
            .setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // v1.1.0 — Microsoft Clarity 세션 녹화 + Custom Tag.
        // Firebase Analytics 의 정량 funnel 을 정성 세션 녹화로 보강.
        // CLARITY_PROJECT_ID 가 비어 있으면 (debug fallback 미설정) init 자체가 no-op.
        EjectClarity.init(this)
        EjectClarity.setSessionTags(
            locale  = Locale.getDefault().toLanguageTag(),
            userTier = if (EjectPrefs.loadPremium(this)) "premium" else "free",
        )
    }
}
