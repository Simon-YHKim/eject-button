package com.ejectbutton.ads

import android.app.Activity
import android.content.Context
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

    private var interstitialAd: InterstitialAd? = null
    private var isInitialized = false

    private val _nativeAd = MutableStateFlow<NativeAd?>(null)
    val nativeAd: StateFlow<NativeAd?> = _nativeAd

    fun initialize(context: Context) {
        if (!isInitialized) {
            MobileAds.initialize(context) {}
            isInitialized = true
            loadInterstitial(context)
            loadNativeAd(context)
            return
        }
        // SDK 는 이미 초기화돼 있지만, 웜 스타트로 Activity 가 재생성돼
        // 이전 세션의 onDestroy() 에서 네이티브 광고가 파기된 상태일 수 있다.
        // 이 경우 광고 슬롯이 비어 있다면 다시 로드해서 메인 화면에 노출되도록 한다.
        if (_nativeAd.value == null) {
            loadNativeAd(context)
        }
        if (interstitialAd == null) {
            loadInterstitial(context)
        }
    }

    // ── 네이티브 광고 ────────────────────────────────────────────────────────

    fun loadNativeAd(context: Context) {
        val adLoader = AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_ID)
            .forNativeAd { ad ->
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
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
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
