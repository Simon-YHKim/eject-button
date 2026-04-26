package com.ejectbutton

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.ejectbutton.data.ThemeMode
import com.ejectbutton.data.strings
import com.ejectbutton.crash.CrashReportManager
import com.ejectbutton.data.SideButtonCommand
import com.ejectbutton.data.SideButtonStep
import com.ejectbutton.service.ButtonPatternDetector
import com.ejectbutton.service.ButtonWatchService
import com.ejectbutton.service.FakeCallOverlayService
import com.ejectbutton.service.SideButtonTrigger
import com.microsoft.clarity.Clarity
import com.microsoft.clarity.ClarityConfig
import com.ejectbutton.analytics.EjectAnalytics
import com.ejectbutton.service.ShakeDetectionService
import com.ejectbutton.ui.main.MainScreen
import com.ejectbutton.ui.main.OnboardingScreen
import com.ejectbutton.ui.theme.*
import androidx.compose.foundation.border
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    lateinit var billingManager: BillingManager
        private set

    /**
     * Foreground 상태에서 볼륨 키 패턴을 감지.
     * Background 진입 시 [ButtonWatchService] 의 ContentObserver 가 이어받음.
     */
    private val foregroundDetector = ButtonPatternDetector(
        command = SideButtonCommand.DISABLED,
        doubleWindowMs = ButtonPatternDetector.FOREGROUND_DOUBLE_WINDOW_MS,
        tripleWindowMs = ButtonPatternDetector.FOREGROUND_TRIPLE_WINDOW_MS,
    ) {
        SideButtonTrigger.fire(this)
    }

    /**
     * 커스텀 커맨드 녹화 다이얼로그가 활성일 때 설정하는 콜백.
     * null 이 아니면 onKeyDown 이 볼륨 키 이벤트를 패턴 감지기 대신 이쪽으로 전달한다.
     */
    @Volatile var recordingCallback: ((SideButtonStep) -> Unit)? = null

    private val multiPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        // v1.1.5 — Clarity funnel: 각 권한 grant/deny 결과를 개별 이벤트로 발사.
        // permission name 이 너무 길어 키로 쓰기 어려우니 마지막 dot 이후만 잘라서 사용.
        results.forEach { (permission, granted) ->
            val shortName = permission.substringAfterLast('.').lowercase()
            com.ejectbutton.analytics.EjectClarity.permissionResult(shortName, granted)
        }
    }

    private val overlayPermLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {}

    /**
     * Round 18 — 최초 실행 & 튜토리얼을 끝냈을 때 한 번만 호출.
     * 런타임 퍼미션 (알림/전화상태/연락처) + 오버레이 퍼미션을 연속으로 요청.
     * pref "perms_requested" 로 재요청을 막는다.
     */
    private fun requestInitialPermissionsIfNeeded() {
        // v1.0.10 — raw SharedPreferences 호출에서 EjectPrefs 함수로 이관.
        // PREF 이름·키 이름 동일하므로 기존 사용자 데이터 그대로 호환.
        if (EjectPrefs.loadPermsRequested(this)) return
        EjectPrefs.savePermsRequested(this, true)

        val runtimePerms = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.READ_PHONE_STATE)
            add(Manifest.permission.READ_CONTACTS)
        }
        if (runtimePerms.isNotEmpty()) {
            multiPermLauncher.launch(runtimePerms.toTypedArray())
        }

        if (!Settings.canDrawOverlays(this)) {
            overlayPermLauncher.launch(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
    }

    private fun initClarity() {
        try {
            val projectId = BuildConfig.CLARITY_PROJECT_ID
            if (projectId.isNotBlank()) {
                Clarity.initialize(this, ClarityConfig(projectId))
            }
        } catch (_: Exception) {}
    }

    // EJECT 3회 이상 사용했고 아직 요청하지 않았을 때 Play Store 리뷰 요청 (ASO 핵심 지표).
    // Round 31 — 기존엔 count==3 정확 일치라 한 번 지나면 영원히 안 떴다. 이제 count>=3
    // + KEY_REVIEW_REQUESTED 플래그로 제어 → 3회째 이후 아무 때나 한 번은 트리거되며
    // 트리거 후 flag 로 재요청을 막는다. launchReviewFlow 는 Play 의 쿼터 정책에 따라
    // 실제 UI 를 띄울지 결정하지만 내부에서는 "시도했다" 를 기록하는 게 핵심.
    private fun maybeRequestReview(ejectCount: Int) {
        if (ejectCount < 3) return
        if (EjectPrefs.loadReviewRequested(this)) return
        try {
            val manager = ReviewManagerFactory.create(this)
            manager.requestReviewFlow().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(this, task.result)
                    EjectPrefs.saveReviewRequested(this, true)
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * 상태표시줄/네비게이션바 색을 현재 (다크/라이트) 테마에 맞춰 재적용.
     * Compose 트리의 SideEffect 에서 호출되므로 themeMode 변경 또는 시스템
     * 다크모드 토글에 따라 즉시 반영된다.
     */
    private fun applySystemBars(useDark: Boolean) {
        if (useDark) {
            val dark = android.graphics.Color.parseColor("#191C1E")
            enableEdgeToEdge(
                statusBarStyle     = SystemBarStyle.dark(dark),
                navigationBarStyle = SystemBarStyle.dark(dark),
            )
        } else {
            // light scrim 은 반투명 흰색, darkScrim 은 API<29 폴백용 흑색
            val lightScrim = android.graphics.Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
            val darkScrim  = android.graphics.Color.argb(0x80, 0x1B, 0x1B, 0x1B)
            enableEdgeToEdge(
                statusBarStyle     = SystemBarStyle.light(lightScrim, darkScrim),
                navigationBarStyle = SystemBarStyle.light(lightScrim, darkScrim),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // v1.1.5 — FLAG_SECURE: Eject Button 핵심 가치는 "옆사람에게 들키지 않는 탈출".
        // 이 플래그가 없으면 가해자가 Recents (최근 앱) 멀티태스킹 화면에서 시나리오/
        // 발신자 이름/SETTINGS 내용을 다 볼 수 있음 → 위기 사용자 안전 직격.
        // 또한 스크린샷·화면녹화도 시스템에서 차단되어 의도치 않은 PII 유출 방지.
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
        )

        // 상태표시줄/네비게이션바 색상은 아래 setContent 안에서 themeMode 에
        // 반응해 [applySystemBars] 로 동적으로 설정한다. 여기서는 edge-to-edge
        // 모드로 진입만 시키고, 실제 색상은 Compose 가 그려지기 직전에 적용.
        enableEdgeToEdge()

        // 이전 크래시 로그가 있으면 자동으로 이메일 전송 화면 열기
        if (CrashReportManager.hasPendingReport(this)) {
            CrashReportManager.sendPendingReport(this)
        }

        // MS Clarity 초기화 (프로젝트 ID가 설정된 경우에만)
        initClarity()

        // Firebase Analytics 초기화 — 이벤트 트래킹은 EjectAnalytics 헬퍼를 통해 호출.
        EjectAnalytics.init(this)

        // AdMob 초기화
        AdManager.initialize(this)

        // 인앱 결제 초기화
        billingManager = BillingManager(this)
        billingManager.connect()

        // Round 18 — 권한 요청은 onCreate 가 아니라 '튜토리얼 이후' 에 실행.
        // 컴포지션 트리 안의 LaunchedEffect 에서 showOnboarding 이 false 로 바뀐 순간
        // 아래 [requestInitialPermissionsIfNeeded] 를 호출한다.

        setContent {
            // 테마 모드 상태 — Settings 에서 변경 가능
            var themeMode by remember {
                mutableStateOf(EjectPrefs.loadThemeMode(this@MainActivity))
            }
            // themeMode / 시스템 다크모드 변경에 맞춰 상태표시줄/네비바 색 재적용
            val systemDark = isSystemInDarkTheme()
            val useDarkBars = when (themeMode) {
                ThemeMode.LIGHT  -> false
                ThemeMode.DARK   -> true
                ThemeMode.SYSTEM -> systemDark
            }
            SideEffect {
                applySystemBars(useDarkBars)
            }
            EjectButtonTheme(themeMode = themeMode) {
                // 앱 언어 상태 — 최상위에서 관리하여 전체 Composition 재구성
                var currentLanguage by remember {
                    mutableStateOf(EjectPrefs.loadLanguage(this@MainActivity))
                }
                val strings = currentLanguage.strings()

                CompositionLocalProvider(LocalAppStrings provides strings) {
                    var splashDone by remember { mutableStateOf(false) }
                    val isPremium by billingManager.isPremium.collectAsState()
                    // 첫 실행 튜토리얼: pref 값이 true 일 때 MainScreen 대신 OnboardingScreen 을 보여준다.
                    // 사용자가 '더이상의 설명은 필요 없다' 를 누르면 false 로 저장.
                    // '다음번에... 한번만... 더...' 를 누르면 값을 유지 (다음 실행에 다시 표시).
                    var showOnboarding by remember {
                        mutableStateOf(EjectPrefs.loadShowOnboarding(this@MainActivity))
                    }
                    // Round 11 — 배터리 최적화 제외 안내 다이얼로그.
                    // 백그라운드/락스크린에서 사이드 버튼·흔들기 서비스가 Doze 모드에
                    // 먹히지 않으려면 제외 설정이 필요하다. 첫 진입 후 한 번만 물어본다.
                    var showBatteryOptDialog by remember { mutableStateOf(false) }

                    // 프리미엄 구매/복원 시 광고 로더/슬롯을 즉시 비운다.
                    LaunchedEffect(isPremium) {
                        AdManager.setPremium(this@MainActivity, isPremium)
                    }

                    LaunchedEffect(Unit) {
                        delay(2_000L)   // 2초 스플래시
                        splashDone = true
                    }

                    // Round 18 — 튜토리얼이 끝나고 메인이 뜨는 순간에 한 번만:
                    //   1) 런타임 권한 + 오버레이 권한 요청 (requestInitialPermissionsIfNeeded)
                    //   2) 배터리 최적화 제외 다이얼로그
                    // 런치 순서: 스플래시 → 튜토리얼 → 권한 → 배터리. 튜토리얼을
                    // 먼저 보여주면 사용자가 맥락을 이해한 상태에서 권한 허용 판단.
                    LaunchedEffect(splashDone, showOnboarding) {
                        if (splashDone && !showOnboarding) {
                            requestInitialPermissionsIfNeeded()
                            val pm = getSystemService(POWER_SERVICE) as? PowerManager
                            val exempt = pm?.isIgnoringBatteryOptimizations(packageName) ?: true
                            val alreadyAsked = EjectPrefs.loadBatteryOptAsked(this@MainActivity)
                            if (!exempt && !alreadyAsked) {
                                showBatteryOptDialog = true
                            }
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        // 튜토리얼 (최초 실행 / 사용자가 설정에서 재활성화한 경우)
                        AnimatedVisibility(
                            visible = splashDone && showOnboarding,
                            enter   = fadeIn(tween(400)),
                            exit    = fadeOut(tween(250)),
                        ) {
                            OnboardingScreen(
                                onDoneNoMore   = {
                                    EjectPrefs.saveShowOnboarding(this@MainActivity, false)
                                    EjectAnalytics.logOnboardingDone(skipFurther = true)
                                    showOnboarding = false
                                },
                                onDoneOnceMore = {
                                    // pref 유지 — 다음 실행에 다시 표시
                                    EjectPrefs.saveShowOnboarding(this@MainActivity, true)
                                    EjectAnalytics.logOnboardingDone(skipFurther = false)
                                    showOnboarding = false
                                },
                            )
                        }

                        // 메인 화면
                        AnimatedVisibility(
                            visible = splashDone && !showOnboarding,
                            enter   = fadeIn(tween(400)),
                        ) {
                            MainScreen(
                                currentLanguage  = currentLanguage,
                                isPremium        = isPremium,
                                themeMode        = themeMode,
                                onThemeModeChange = { mode ->
                                    themeMode = mode
                                    EjectPrefs.saveThemeMode(this@MainActivity, mode)
                                },
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
                                    val mode = when {
                                        delayMs == -1L -> "shake"
                                        delayMs == 0L  -> "button_now"
                                        else            -> "button_delayed"
                                    }
                                    val delaySec = if (delayMs > 0) (delayMs / 1000).toInt() else 0
                                    EjectAnalytics.logEjectFired(mode, delaySec, scenario.id)
                                    // v1.1.0 — Clarity 정성 funnel: 가짜 통화 화면이
                                    // 실제로 뜨는 시점에 별도 이벤트 발사 (logEjectFired 는
                                    // "버튼 탭" 시점이라 둘 다 분리해서 마커로 남김).
                                    com.ejectbutton.analytics.EjectClarity.fakeCallStarted(
                                        scenarioId  = scenario.id,
                                        callerName  = scenario.callerName,
                                        mode        = mode,
                                    )
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

                        // Round 11 — 배터리 최적화 제외 요청 다이얼로그
                        if (showBatteryOptDialog) {
                            AlertDialog(
                                onDismissRequest = {
                                    EjectPrefs.saveBatteryOptAsked(this@MainActivity, true)
                                    showBatteryOptDialog = false
                                },
                                title = {
                                    Text(
                                        strings.batteryOptTitle,
                                        fontWeight = FontWeight.Bold,
                                    )
                                },
                                text = { Text(strings.batteryOptMsg) },
                                confirmButton = {
                                    TextButton(onClick = {
                                        EjectPrefs.saveBatteryOptAsked(this@MainActivity, true)
                                        showBatteryOptDialog = false
                                        // 시스템 배터리 최적화 제외 요청 화면 열기.
                                        // 일부 OEM 은 ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 에
                                        // 바로 토글 가능한 확인창을 띄우고, 일부는 앱 전체 목록으로
                                        // 이동시킨다. 실패 시 앱 상세 설정으로 폴백.
                                        runCatching {
                                            startActivity(
                                                Intent(
                                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                                    Uri.parse("package:$packageName"),
                                                )
                                            )
                                        }.onFailure {
                                            runCatching {
                                                startActivity(
                                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                )
                                            }
                                        }
                                    }) {
                                        Text(
                                            strings.batteryOptGrant,
                                            color = EjectCoral,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = {
                                        EjectPrefs.saveBatteryOptAsked(this@MainActivity, true)
                                        showBatteryOptDialog = false
                                    }) {
                                        Text(strings.batteryOptLater)
                                    }
                                },
                            )
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
        // 사이드 버튼 트리거 설정에 맞춰 detector 와 watch service 동기화
        foregroundDetector.command = EjectPrefs.loadSideButtonCommand(this)
        foregroundDetector.customSequence = EjectPrefs.loadSideButtonCustomSequence(this)
        ButtonWatchService.reconcile(this)
    }

    /**
     * Foreground 상태에서 볼륨 키 입력을 가로채 패턴 감지.
     * 패턴이 일치하지 않으면 super 를 호출해 일반 볼륨 조절이 동작하도록 한다.
     * 일치하면 true 를 반환해 시스템 볼륨 변화를 차단한다.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // 녹화 모드가 활성이면 볼륨 키는 녹화 콜백으로만 전달
        val rec = recordingCallback
        if (rec != null && event.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP   -> { rec(SideButtonStep.UP);   return true }
                KeyEvent.KEYCODE_VOLUME_DOWN -> { rec(SideButtonStep.DOWN); return true }
            }
        }
        if (!foregroundDetector.command.isEnabled || event.repeatCount != 0) {
            return super.onKeyDown(keyCode, event)
        }
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                if (foregroundDetector.command.isVolumeUp) {
                    foregroundDetector.onVolumeUp()
                    true
                } else super.onKeyDown(keyCode, event)
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                if (foregroundDetector.command.isVolumeDown) {
                    foregroundDetector.onVolumeDown()
                    true
                } else super.onKeyDown(keyCode, event)
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    /**
     * onKeyDown 에서 true 를 반환했을 때 KEYCODE_VOLUME_*  의 KeyUp 도 같이
     * consume 해야 시스템이 볼륨 패널을 띄우지 않는다.
     */
    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (recordingCallback != null &&
            (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return true
        }
        if (!foregroundDetector.command.isEnabled) {
            return super.onKeyUp(keyCode, event)
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && foregroundDetector.command.isVolumeUp) return true
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN && foregroundDetector.command.isVolumeDown) return true
        return super.onKeyUp(keyCode, event)
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalBase)
            .microGridBackground(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ⏏ icon panel — machined crimson square (0dp corners, ghost border)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .background(TacticalRedDeep)
                    .border(1.dp, TacticalOutlineVar.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("⏏", fontSize = 64.sp, color = TacticalOnSurface)
            }

            Spacer(Modifier.height(32.dp))

            Text(
                text       = "EJECT BUTTON",
                fontSize   = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = TacticalOnSurface,
                letterSpacing = 3.sp,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text      = catchphrase,
                fontSize  = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                color     = TacticalOnVariant,
            )
        }

        // Bottom initializing indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDot()
                Spacer(Modifier.width(10.dp))
                Text(initLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                    color = TacticalCyan, letterSpacing = 2.sp)
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
            .background(TacticalCyan.copy(alpha = alpha), androidx.compose.foundation.shape.CircleShape)
    )
}
