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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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
    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false
    private var lastInterstitialShownMs: Long = 0L
    @Volatile private var rewardedLoading: Boolean = false

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
        if (rewardedAd == null) {
            loadRewarded(context)
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
            rewardedAd = null
        } else if (isInitialized) {
            if (_nativeAd.value == null) loadNativeAd(context)
            if (interstitialAd == null) loadInterstitial(context)
            if (rewardedAd == null) loadRewarded(context)
        }
    }

    // ── 네이티브 광고 ────────────────────────────────────────────────────────

    fun loadNativeAd(context: Context) {
        if (adsDisabled) return
        val adLoader = AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_ID)
            .forNativeAd { ad ->
                if (adsDisabled) {
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
            .withNativeAdOptions(NativeAdOptions.Builder().build())
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

    // ── 보상형 광고 (Rewarded) — v1.1.0 ────────────────────────────────────
    //
    // RewardedAdDialog 에서 "광고 보기" 선택 시 30초 영상 광고 노출 후 onRewarded
    // 콜백으로 1회 사용 권한을 부여한다. AdMob 정책상 사용자가 명시적으로 옵트인
    // 한 경우에만 표시 가능 (=> RewardedAdDialog 의 "광고 보기" 라디오 선택 + Continue).
    //
    // load 는 멱등하게 동작하되 동시 다중 load 를 막기 위해 [rewardedLoading] 플래그
    // 로 in-flight 가드. 실패해도 사용자가 다시 RewardedAdDialog 를 띄우면 자동 재시도.

    fun loadRewarded(context: Context) {
        if (adsDisabled) return
        if (rewardedAd != null || rewardedLoading) return
        rewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    if (adsDisabled) return
                    rewardedAd = ad
                }
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    rewardedAd = null
                    Log.d("AdManager", "Rewarded failed: ${error.message}")
                }
            }
        )
    }

    /**
     * 보상형 광고 표시.
     *  @param onRewarded 사용자가 광고를 끝까지 시청하고 보상 조건을 만족했을 때 호출.
     *  @param onDismissed 광고 종료(보상 여부 무관) 또는 표시 실패 시 호출. UI 후속 처리용.
     *
     * Premium 사용자는 광고 시청 없이 즉시 onRewarded + onDismissed 양쪽 호출 (잠긴
     * 기능을 그냥 통과). 광고가 아직 로드 안 돼 있으면 onDismissed 만 호출하고 다음 회차
     * 를 위해 백그라운드에서 재로드 시작.
     */
    fun showRewarded(
        activity: Activity,
        onRewarded: () -> Unit,
        onDismissed: () -> Unit = {},
    ) {
        if (adsDisabled) {
            onRewarded()
            onDismissed()
            return
        }
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded(activity)
            onDismissed()
            return
        }
        var rewarded = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded(activity)
                if (rewarded) onRewarded()
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadRewarded(activity)
                onDismissed()
            }
        }
        ad.show(activity) { _ ->
            // RewardItem 의 amount/type 은 우리 모델에서 무의미 (1회 = 잠금 해제 1회).
            rewarded = true
        }
    }

    fun destroy() {
        _nativeAd.value?.destroy()
        _nativeAd.value = null
    }
}
