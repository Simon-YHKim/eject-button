package com.ejectbutton.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.ejectbutton.BuildConfig
import com.ejectbutton.data.EjectPrefs
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AdManager {

    /**
     * 전면 광고 최소 간격 — 한 번 띄운 뒤 이 시간 이내에는 다시 띄우지 않는다.
     * AdMob 정책상 "사용자가 앱을 쓰는 동안 과도하게 전면 광고로 흐름을 끊지 말 것"
     * 을 지키기 위한 frequency cap.
     */
    private const val INTERSTITIAL_MIN_INTERVAL_MS = 60_000L

    /**
     * v1.0.9 — 전면 광고 일별 상한.
     * SystemClock 기반 60초 cap 만 있으면 사용자가 1시간 동안 60번 광고에 노출될 수 있다.
     * 일별 cap 으로 사용자 경험과 AdMob 정책 (과도한 노출 시 계정 경고) 양쪽을 보호.
     * 카운터는 UTC epoch day 기준이며 EjectPrefs 에 저장 → 앱 재시작 후에도 유지.
     */
    private const val MAX_INTERSTITIALS_PER_DAY = 10

    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false
    private var lastInterstitialShownMs: Long = 0L

    /**
     * 프리미엄(광고 제거) 사용자 여부. true 이면 네이티브/전면 모두 로드하지 않고
     * 이미 로드된 광고는 파기한다.
     */
    @Volatile private var isPremium: Boolean = false

    /**
     * v1.3.0 — 광고 제거 일회성 unlock (eject_remove_ads_lifetime ₩3,300).
     * isPremium (월 구독) 과 별도. 둘 중 하나라도 true 면 광고 비활성.
     */
    @Volatile private var isAdsRemoved: Boolean = false

    /** 광고 비활성 여부. premium 구독 OR 일회성 광고 제거 OR 둘 다. */
    private val adsDisabled: Boolean get() = isPremium || isAdsRemoved

    private val _nativeAd = MutableStateFlow<NativeAd?>(null)
    val nativeAd: StateFlow<NativeAd?> = _nativeAd

    fun initialize(context: Context) {
        if (!isInitialized) {
            MobileAds.initialize(context) {}
            isInitialized = true
        }
        if (adsDisabled) return
        // 웜 스타트로 Activity 가 재생성돼 이전 세션의 onDestroy() 에서 네이티브 광고가
        // 파기된 상태일 수 있다. 슬롯이 비어 있다면 다시 로드해 메인 화면에 노출한다.
        if (_nativeAd.value == null) {
            loadNativeAd(context)
        }
        if (interstitialAd == null) {
            loadInterstitial(context)
        }
    }

    /**
     * 프리미엄 상태 변경 알림.
     * - 프리미엄 → 현재 로드된 네이티브/전면 광고를 즉시 파기
     * - 일반 → 필요 시 재로드
     */
    fun setPremium(context: Context, premium: Boolean) {
        if (isPremium == premium) return
        isPremium = premium
        applyAdsState(context)
    }

    /**
     * v1.3.0 — 광고 제거 일회성 (eject_remove_ads_lifetime) 상태 변경.
     * setPremium 과 동일 패턴. 광고 즉시 파기 또는 재로드.
     */
    fun setAdsRemoved(context: Context, removed: Boolean) {
        if (isAdsRemoved == removed) return
        isAdsRemoved = removed
        applyAdsState(context)
    }

    /**
     * isPremium 또는 isAdsRemoved 변화 시 호출. adsDisabled 가 true 면 모든 광고
     * 즉시 파기, false 면 (isInitialized 인 경우만) 재로드.
     */
    private fun applyAdsState(context: Context) {
        if (adsDisabled) {
            _nativeAd.value?.destroy()
            _nativeAd.value = null
            interstitialAd = null
        } else if (isInitialized) {
            if (_nativeAd.value == null) loadNativeAd(context)
            if (interstitialAd == null) loadInterstitial(context)
        }
    }

    // ── 네이티브 광고 ────────────────────────────────────────────────────────

    /**
     * v1.6.6 — 메인 native ad 를 image-only 로 제한.
     *
     * 사용자 디자인 결정: 메인 화면 상시 광고는 컴팩트 1행 배너 (80dp). video 광고는
     *   1) 80dp 사이즈에서 비정상 노출 + AdMob validator video size warning,
     *   2) 자동 재생으로 사용자 주의 분산.
     *
     * 해결책 2단:
     *  (a) NativeAdOptions.setMediaAspectRatio(SQUARE) — AdMob 에 정사각 image 광고
     *      선호도 시그널. video 광고 매칭 빈도 낮춤.
     *  (b) forNativeAd 콜백에서 ad.mediaContent.hasVideoContent() 체크 후 video 면
     *      즉시 destroy + 재로드. 100% 차단.
     *
     * 동영상은 전화 종료 후 Interstitial 광고에서만 노출 (FakeCallOverlayService 종료
     * → MainActivity onResume → showInterstitialIfReady). Interstitial 은 AdMob 이
     * 자동 image/video mix 라 별도 강제 없음.
     */
    fun loadNativeAd(context: Context) {
        if (adsDisabled) return
        val adLoader = AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_ID)
            .forNativeAd { ad ->
                if (adsDisabled) {
                    ad.destroy()
                    return@forNativeAd
                }
                // Image-only 강제: video 광고는 destroy + 다음 요청 (멱등 재로드).
                if (ad.mediaContent?.hasVideoContent() == true) {
                    Log.d("AdManager", "Native ad rejected: video content (image-only mode)")
                    ad.destroy()
                    return@forNativeAd
                }
                _nativeAd.value?.destroy()
                _nativeAd.value = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.d("AdManager", "Native ad failed: ${error.message}")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    // SQUARE: 정사각 image 광고 선호. video 매칭 빈도 낮춤 + 80dp 정사각
                    // MediaView 디자인과도 정합.
                    .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_SQUARE)
                    .build()
            )
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    // ── 전면 광고 ────────────────────────────────────────────────────────────

    fun loadInterstitial(context: Context) {
        if (adsDisabled) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    if (adsDisabled) return
                    interstitialAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.d("AdManager", "Interstitial failed: ${error.message}")
                }
            }
        )
    }

    fun showInterstitialIfReady(activity: Activity, onDismissed: () -> Unit = {}) {
        if (adsDisabled) {
            onDismissed()
            return
        }
        // v1.0.9 — 일별 cap (UTC epoch day 기준).
        // 60초 cap 만으론 1시간 60회 노출 가능 → 일 10회 이하로 보호.
        if (!EjectPrefs.canShowInterstitialToday(activity, MAX_INTERSTITIALS_PER_DAY)) {
            onDismissed()
            return
        }
        // Frequency cap: 직전에 띄운 지 INTERSTITIAL_MIN_INTERVAL_MS 이내이면 skip.
        val now = SystemClock.elapsedRealtime()
        if (lastInterstitialShownMs != 0L &&
            now - lastInterstitialShownMs < INTERSTITIAL_MIN_INTERVAL_MS) {
            onDismissed()
            return
        }
        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismissed()
                }
                override fun onAdFailedToShowFullScreenContent(error: AdError) {
                    interstitialAd = null
                    loadInterstitial(activity)
                    onDismissed()
                }
            }
            lastInterstitialShownMs = now
            EjectPrefs.recordInterstitialShown(activity) // v1.0.9 — 일별 카운터 +1
            ad.show(activity)
        } else {
            loadInterstitial(activity)
            onDismissed()
        }
    }

    // ── 보상형 광고 (Rewarded) ── v1.6.6 제거 ────────────────────────────
    //
    // 사용자 요청 (v1.6.6): "리워드를 줄만한게 없어 — 리워드 광고 제거". 기존 시스템:
    //   "무료 사용자가 caller 2번째 추가 시 RewardedAdDialog 로 30초 광고 시청 →
    //    1회 caller 추가 unlock". 보상 의미가 미미해 share-to-unlock 패턴으로 교체.
    //
    // 대체 동작: 무료 사용자 caller 추가 시도 → ShareToUnlockDialog (앱 공유 → 영구
    //   unlock). 코드: MainScreen.kt 의 showAddCaller 게이팅 + EjectPrefs.hasShared.
    //
    // 따라서 본 AdManager 의 rewarded 관련 코드 (loadRewarded/showRewarded/
    //   rewardedAd state + ADMOB_REWARDED_ID buildConfigField) 모두 제거.

    fun destroy() {
        _nativeAd.value?.destroy()
        _nativeAd.value = null
    }
}
