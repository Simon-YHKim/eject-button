package com.ejectbutton.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ejectbutton.BuildConfig
import com.ejectbutton.ads.AdManager
import com.ejectbutton.ui.theme.EjectSurface
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * v1.6.7 — 메인 화면 상시 Banner 광고.
 *
 * 이전 NativeAdCard (v1.5.1 ~ v1.6.6) 를 대체:
 *  - Native ad 는 광고주가 image/video 중 선택 → code-side video filter 로
 *    image 만 받았지만 AdMob validator 가 MediaView 사이즈만 보고 "video too
 *    small" warning. Banner 는 구조적으로 image 전용 → warning 0 + UX 동일.
 *  - Banner 는 AdMob 표준 layout (광고주 이미지 + 텍스트). 우리 custom layout
 *    (icon + headline + AD 뱃지) 적용 불가. 대신 hard error + validator
 *    warning 모두 회피 + 정식 image-only 보장.
 *
 * AdSize 결정:
 *  - getCurrentOrientationAnchoredAdaptiveBannerAdSize: 화면 너비에 맞춰
 *    AdMob 이 최적 사이즈 산출. 대부분 50~60dp 높이. 컴팩트 1행 유지.
 *  - BANNER (320×50) 대비 디바이스 너비 활용도 높음.
 *
 * v1.6.8 — Edge-to-edge layout. 모든 padding + RoundedCornerShape 제거.
 *  - 이전 (v1.6.7): outer padding(horizontal = 24.dp) [MainScreen]
 *      + inner padding(horizontal = 4.dp) [본 Composable] = 56dp 좁아진 컨테이너에
 *      화면 너비만큼 광고 요청 → CTA 우측 잘림 ("OPEN" 버튼 truncated).
 *      AdMob policy 상 truncated ad = violation 가능.
 *  - 현재: padding 0 → AdMob 에 전달한 screenWidthDp 와 실제 렌더 너비 일치.
 *      Adaptive Banner 의 정석 사용법 (Google 권장).
 *  - RoundedCornerShape 도 제거 (광고 컨텐츠 자체를 clip 하면 동일 policy 이슈).
 *
 * v1.6.9 — Activity lifecycle 연동 (수익 최적화).
 *  - AdMob 콘솔의 "Google 최적화" 자동 새로고침은 AdView 가 visible & resumed
 *    상태일 때만 동작. 이전 코드 (v1.6.7~v1.6.8) 는 onPause/onResume hook 없이
 *    AdView 단순 보관 → background 갔다가 돌아와도 refresh timing 깨질 수 있음.
 *  - LifecycleEventObserver 로 Activity 의 ON_PAUSE/ON_RESUME 이벤트를 AdView 에
 *    위임 → Google 최적화 refresh 가 정확히 작동 → 노출수 ↑ → 수익 ↑.
 *  - ON_DESTROY 시 AdView.destroy() 까지 호출해 메모리 누수 방지.
 *
 * Lifecycle:
 *  - DisposableEffect 로 LifecycleObserver 등록/해제 + AdView destroy.
 *  - AdManager.adsDisabled (premium / ads removed) true 면 AdView 자체 미생성.
 */
@Composable
fun BannerAdCard(modifier: Modifier = Modifier) {
    if (AdManager.adsDisabled) return
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val adView = remember {
        AdView(context).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            // 화면 너비 기반 adaptive 사이즈 (px → dp 변환은 AdMob 내부 처리).
            val widthDp = context.resources.configuration.screenWidthDp
            setAdSize(
                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
            )
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(lifecycleOwner, adView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE  -> adView.pause()
                Lifecycle.Event.ON_DESTROY -> adView.destroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // ON_DESTROY 가 이미 호출됐을 수 있지만 destroy() 는 idempotent.
            adView.destroy()
        }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(EjectSurface),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(factory = { adView })
    }
}
