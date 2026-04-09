package com.ejectbutton

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.ejectbutton.ads.AdManager
import com.ejectbutton.billing.BillingManager
import com.ejectbutton.data.AppLanguage
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.data.Scenario
import com.ejectbutton.data.strings
import com.ejectbutton.crash.CrashReportManager
import com.ejectbutton.service.FakeCallOverlayService
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.ejectbutton.service.ShakeDetectionService
import com.ejectbutton.ui.main.MainScreen
import com.ejectbutton.ui.theme.*
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    lateinit var billingManager: BillingManager
        private set

    private val multiPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {}

    private val overlayPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    private fun initClarity() {
        try {
            val projectId = BuildConfig.CLARITY_PROJECT_ID
            if (projectId.isNotBlank()) {
                Clarity.initialize(this, ClarityConfig(projectId))
            }
        } catch (_: Exception) {}
    }

    // 3번째 EJECT 사용 시 Play Store 리뷰 요청 (ASO 핵심 지표)
    private fun maybeRequestReview(ejectCount: Int) {
        if (ejectCount != 3) return
        try {
            val manager = ReviewManagerFactory.create(this)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(this, task.result)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 이전 크래시 로그가 있으면 자동으로 이메일 전송 화면 열기
        if (CrashReportManager.hasPendingReport(this)) {
            CrashReportManager.sendPendingReport(this)
        }

        // MS Clarity 초기화 (프로젝트 ID가 설정된 경우에만)
        initClarity()

        // AdMob 초기화
        AdManager.initialize(this)

        // 인앱 결제 초기화
        billingManager = BillingManager(this)
        billingManager.connect()

        // 최초 실행 시 권한 일괄 요청
        val prefs = getSharedPreferences("eject_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("perms_requested", false)) {
            prefs.edit().putBoolean("perms_requested", true).apply()
            val runtimePerms = buildList {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    add(Manifest.permission.POST_NOTIFICATIONS)
                add(Manifest.permission.READ_PHONE_STATE)
                add(Manifest.permission.READ_CONTACTS)
            }
            if (runtimePerms.isNotEmpty()) multiPermLauncher.launch(runtimePerms.toTypedArray())
        }

        if (!Settings.canDrawOverlays(this)) {
            overlayPermLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }

        setContent {
            EjectButtonTheme {
                // 앱 언어 상태 — 최상위에서 관리하여 전체 Composition 재구성
                var currentLanguage by remember {
                    mutableStateOf(EjectPrefs.loadLanguage(this@MainActivity))
                }
                val strings = currentLanguage.strings()

                CompositionLocalProvider(LocalAppStrings provides strings) {
                    var splashDone by remember { mutableStateOf(false) }
                    val isPremium by billingManager.isPremium.collectAsState()

                    LaunchedEffect(Unit) {
                        delay(2_000L)   // 2초 스플래시
                        splashDone = true
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // 메인 화면
                        AnimatedVisibility(
                            visible = splashDone,
                            enter   = fadeIn(tween(400)),
                        ) {
                            MainScreen(
                                currentLanguage  = currentLanguage,
                                isPremium        = isPremium,
                                onLanguageChange = { lang ->
                                    currentLanguage = lang
                                },
                                onPurchasePremium = {
                                    billingManager.launchPurchase(this@MainActivity)
                                },
                                onRestorePurchase = {
                                    billingManager.restorePurchases()
                                },
                                premiumPrice = billingManager.getPriceText(),
                                onEject = { scenario: Scenario, delayMs: Long ->
                                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                                        overlayPermLauncher.launch(
                                            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:$packageName"))
                                        )
                                        return@MainScreen
                                    }
                                    if (delayMs == -1L) {
                                        ShakeDetectionService.start(
                                            this@MainActivity,
                                            scenario.callerName,
                                            scenario.callerLabel,
                                            scenario.prompterHint,
                                        )
                                    } else {
                                        FakeCallOverlayService.start(
                                            this@MainActivity,
                                            scenario.callerName,
                                            scenario.callerLabel,
                                            scenario.prompterHint,
                                            delayMs,
                                        )
                                    }
                                    val count = EjectPrefs.incrementEjectCount(this@MainActivity)
                                    maybeRequestReview(count)
                                }
                            )
                        }

                        // 스플래시 (2초)
                        AnimatedVisibility(
                            visible = !splashDone,
                            exit    = fadeOut(tween(400)),
                        ) {
                            SplashScreen(strings.systemInitializing, strings.catchphrase)
                        }
                    }
                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        // 가짜 전화 종료 후 → 전면 광고 표시
        if (FakeCallOverlayService.showInterstitialOnNextResume) {
            FakeCallOverlayService.showInterstitialOnNextResume = false
            AdManager.showInterstitialIfReady(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        billingManager.destroy()
        AdManager.destroy()
    }
}

// ── 스플래시 화면 ─────────────────────────────────────────────────────────────

@Composable
private fun SplashScreen(initLabel: String, catchphrase: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -18f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ),
        label = "bounce",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EjectSurface),
        contentAlignment = Alignment.Center,
    ) {
        // 배경 장식 — 소프트 블러 서클
        Box(
            Modifier
                .size(280.dp)
                .offset(x = (-60).dp, y = (-120).dp)
                .background(EjectCoral.copy(alpha = 0.07f), androidx.compose.foundation.shape.CircleShape)
        )
        Box(
            Modifier
                .size(240.dp)
                .offset(x = 80.dp, y = 100.dp)
                .background(EjectSurfaceMid.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = offsetY.dp),
        ) {
            // ⏏ 아이콘 카드 (Stitch: 코랄 배경 rounded square)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .shadow(24.dp, RoundedCornerShape(28.dp), spotColor = EjectCoral.copy(0.3f))
                    .clip(RoundedCornerShape(28.dp))
                    .background(EjectCoral),
                contentAlignment = Alignment.Center,
            ) {
                Text("⏏", fontSize = 64.sp)
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text       = "EJECT BUTTON",
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = EjectCoral,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = catchphrase,
                fontSize  = 14.sp,
                color     = EjectSecondary,
                letterSpacing = 0.5.sp,
            )
        }

        // 하단 초기화 인디케이터
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .clip(RoundedCornerShape(50))
                .background(EjectSurfaceLow)
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot()
                Spacer(Modifier.width(10.dp))
                Text(initLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = EjectSecondary, letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun PulsingDot() {
    val inf = rememberInfiniteTransition(label = "dot")
    val alpha by inf.animateFloat(
        initialValue = 1f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        Modifier
            .size(8.dp)
            .background(EjectCoral.copy(alpha = alpha), androidx.compose.foundation.shape.CircleShape)
    )
}
