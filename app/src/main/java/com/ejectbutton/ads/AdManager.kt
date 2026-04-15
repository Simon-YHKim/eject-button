package com.ejectbutton.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.ejectbutton.BuildConfig
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

    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false
    private var lastInterstitialShownMs: Long = 0L

    /**
     * 프리미엄(광고 제거) 사용자 여부. true 이면 네이티브/전면 모두 로드하지 않고
     * 이미 로드된 광고는 파기한다.
     */
    @Volatile private var isPremium: Boolean = false

    private val _nativeAd = MutableStateFlow<NativeAd?>(null)
    val nativeAd: StateFlow<NativeAd?> = _nativeAd

    fun initialize(context: Context) {
        if (!isInitialized) {
            MobileAds.initialize(context) {}
            isInitialized = true
        }
        if (isPremium) return
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
        if (premium) {
            _nativeAd.value?.destroy()
            _nativeAd.value = null
            interstitialAd = null
        } else if (isInitialized) {
            if (_nativeAd.value == null) loadNativeAd(context)
            if (interstitialAd == null) loadInterstitial(context)
        }
    }

    // ── 네이티브 광고 ────────────────────────────────────────────────────────

    fun loadNativeAd(context: Context) {
        if (isPremium) return
        val adLoader = AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_ID)
            .forNativeAd { ad ->
                if (isPremium) {
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
        if (isPremium) return
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    if (isPremium) return
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
        if (isPremium) {
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
            ad.show(activity)
        } else {
            loadInterstitial(activity)
            onDismissed()
        }
    }

    fun destroy() {
        _nativeAd.value?.destroy()
        _nativeAd.value = null
    }
}
