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
import androidx.activity.compose.rememberLauncherForActivityResult
// v1.6.11 — In-App Update API (Flexible flow).
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ejectbutton.ads.AdManager
import com.ejectbutton.billing.BillingManager
import com.ejectbutton.data.AppLanguage
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.data.PermissionGate
import com.ejectbutton.data.Scenario
import com.ejectbutton.data.ThemeMode
import com.ejectbutton.data.TriggerMode
import com.ejectbutton.data.strings
import com.ejectbutton.crash.CrashReportManager
import com.ejectbutton.data.SideButtonCommand
import com.ejectbutton.data.SideButtonStep
import com.ejectbutton.service.ButtonPatternDetector
import com.ejectbutton.service.ButtonWatchService
import com.ejectbutton.service.FakeCallOverlayService
import com.ejectbutton.service.SideButtonTrigger
import com.ejectbutton.analytics.EjectAnalytics
import com.ejectbutton.service.ShakeDetectionService
import com.ejectbutton.ui.main.MainScreen
import com.ejectbutton.ui.main.OnboardingScreen
import com.ejectbutton.ui.theme.*
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

    // v1.6.11 — In-App Update (Flexible flow). 사용자가 Play Store 갱신을 안 켜뒀거나
    // 미루는 경우에도 앱이 자체적으로 백그라운드 다운로드 → "재시작" 스낵바로 적용 유도.
    //   - FLEXIBLE: 사용자가 앱 사용을 계속할 수 있는 비강제 모드. 다운로드 완료 시
    //     [updateDownloaded] state 가 true 가 되고 Compose 트리가 Snackbar 표시.
    //   - 디버그/사이드로드 환경은 startUpdateFlow 가 즉시 실패 — 로그만 남기고 무시.
    //   - 다음 출시(v1.6.12+) 부터 실제 trigger. v1.6.11 자체는 코드 ship 만 함.
    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(applicationContext)
    }

    private val updateDownloaded = mutableStateOf(false)

    private val installStateListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            updateDownloaded.value = true
        }
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* 거부 시 그냥 패스. 다음 onResume 에서 재시도 안 함 — 사용자 선택 존중 */ }

    private fun checkForFlexibleUpdate() {
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            info,
                            updateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        )
                    }.onFailure { e ->
                        android.util.Log.w("MainActivity", "startUpdateFlow failed", e)
                    }
                }
            }
            .addOnFailureListener { e ->
                // 디버그 빌드, 사이드로드 APK, Play Store 미설치 디바이스 등에서 발생.
                android.util.Log.d("MainActivity", "appUpdateInfo not available: ${e.message}")
            }
    }

    private fun completeFlexibleUpdate() {
        runCatching { appUpdateManager.completeUpdate() }
    }

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
        // v1.4.2 — FLAG_SECURE 임시 비활성화 (Closed Testing 사용자가 스크린샷
        // 으로 버그 신고 / UX 피드백 가능하게). production 출시 전 v1.5+ 에서
        // debug 빌드에만 비활성화 + release 빌드는 다시 활성화하는 토글로 재구성 예정.
        //
        // 원래 의도 (재활성화 시 복원):
        // FLAG_SECURE: Eject Button 핵심 가치 = "옆사람에게 들키지 않는 탈출".
        // 이 플래그가 없으면 가해자가 Recents (최근 앱) 멀티태스킹 화면에서
        // 시나리오/발신자 이름/SETTINGS 내용 노출. 또한 스크린샷·화면녹화 차단.
        // (현재 비활성 — 테스터 편의 우선)

        // 상태표시줄/네비게이션바 색상은 아래 setContent 안에서 themeMode 에
        // 반응해 [applySystemBars] 로 동적으로 설정한다. 여기서는 edge-to-edge
        // 모드로 진입만 시키고, 실제 색상은 Compose 가 그려지기 직전에 적용.
        enableEdgeToEdge()

        // 이전 크래시 로그가 있으면 자동으로 이메일 전송 화면 열기
        if (CrashReportManager.hasPendingReport(this)) {
            CrashReportManager.sendPendingReport(this)
        }

        // Firebase Analytics 초기화 — 이벤트 트래킹은 EjectAnalytics 헬퍼를 통해 호출.
        EjectAnalytics.init(this)

        // v1.2 — UMP (GDPR) consent 흐름 먼저, 그 후 AdMob 초기화.
        //   - EEA 사용자: consent 다이얼로그 → 동의/거부 결과에 따라 광고 personalize 결정.
        //   - non-EEA 사용자: 즉시 canRequestAds=true → AdMob 초기화.
        //   - consent 거부해도 non-personalized 광고는 송출 가능.
        com.ejectbutton.consent.ConsentManager.gather(this) { canRequestAds ->
            if (canRequestAds) {
                AdManager.initialize(this)
            } else {
                android.util.Log.w("MainActivity", "Consent not granted — AdMob not initialized this session")
            }
        }

        // 인앱 결제 초기화
        billingManager = BillingManager(this)
        billingManager.connect()

        // v1.6.11 — In-App Update Flexible flow. Listener 등록 후 첫 체크.
        // 결과는 [updateDownloaded] state 로 Compose 트리에 전달돼 Snackbar 표시.
        appUpdateManager.registerListener(installStateListener)
        checkForFlexibleUpdate()

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
                    // v1.3.0 — INAPP 광고 제거 일회성 unlock 상태. premium 과 별도.
                    val isAdsRemoved by billingManager.isAdsRemoved.collectAsState()
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

                    // v1.6.10 — EJECT 시점 권한 게이트. MainScreen 의 모든 arm/fire 분기가
                    // 이 콜백을 통해 들어온다. 미충족 권한이 있으면 [PermissionGateDialog] 가
                    // 떠서 사용자에게 다시 안내하고, 모두 grant 된 뒤에야 [pendingProceed] 가
                    // 실행돼 실제 arm/fire 가 일어난다. 거부 시 pendingProceed 는 폐기되어
                    // 다음 EJECT 누름에 다시 재확인됨 (사용자 요청 spec).
                    var pendingProceed by remember { mutableStateOf<(() -> Unit)?>(null) }
                    var pendingMode by remember { mutableStateOf<TriggerMode?>(null) }
                    var pendingDelayMs by remember { mutableStateOf(0L) }
                    val ensurePermissions: (TriggerMode, Long, () -> Unit) -> Unit =
                        { mode, delayMs, proceed ->
                            if (PermissionGate.missing(this@MainActivity, mode, delayMs).isEmpty()) {
                                proceed()
                            } else {
                                pendingMode = mode
                                pendingDelayMs = delayMs
                                pendingProceed = proceed
                            }
                        }

                    // 프리미엄 구매/복원 시 광고 로더/슬롯을 즉시 비운다.
                    LaunchedEffect(isPremium) {
                        AdManager.setPremium(this@MainActivity, isPremium)
                    }
                    // v1.3.0 — 광고 제거 일회성 구매/복원 시 광고 즉시 파기.
                    LaunchedEffect(isAdsRemoved) {
                        AdManager.setAdsRemoved(this@MainActivity, isAdsRemoved)
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
                                onDoneNoMore = { stepCount ->
                                    EjectPrefs.saveShowOnboarding(this@MainActivity, false)
                                    EjectAnalytics.logOnboardingDone(
                                        skipFurther = true,
                                        stepCount = stepCount,
                                    )
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
                                isAdsRemoved = isAdsRemoved,
                                onPurchaseRemoveAds = {
                                    billingManager.launchPurchaseRemoveAds(this@MainActivity)
                                },
                                removeAdsPrice = billingManager.getRemoveAdsPriceText(),
                                ensurePermissions = ensurePermissions,
                                onEject = { scenario: Scenario, delayMs: Long ->
                                    // v1.6.10 — overlay 권한은 MainScreen 이 ensurePermissions 로
                                    // 사전에 검증하므로 여기서는 별도 체크 불필요. 안전망 차원에서
                                    // canDrawOverlays 가 의외로 false 면 그냥 fire 시도 (Service 가
                                    // SecurityException 으로 fallback) 보다는 사용자에게 다시
                                    // 안내하는 게 낫지만, 게이트가 fail-safe 로 동작하므로 직접
                                    // 도달 가능성은 거의 없다.
                                    if (delayMs == -1L) {
                                        ShakeDetectionService.start(
                                            this@MainActivity,
                                            scenario.callerName,
                                            scenario.callerLabel,
                                            scenario.prompterHint,
                                            scenarioId = scenario.id,
                                            mode = "shake",
                                        )
                                    } else {
                                        FakeCallOverlayService.start(
                                            this@MainActivity,
                                            scenario.callerName,
                                            scenario.callerLabel,
                                            scenario.prompterHint,
                                            delayMs,
                                            scenario.id,
                                            if (delayMs == 0L) "button_now" else "button_delayed",
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
                                    // v1.2 — Conversion event: 첫 EJECT/시나리오 사용 1회만 발사.
                                    if (!EjectPrefs.isFirstEjectLogged(this@MainActivity)) {
                                        EjectAnalytics.logFirstEjectFired(mode, scenario.id)
                                        EjectPrefs.markFirstEjectLogged(this@MainActivity)
                                    }
                                    if (!EjectPrefs.isFirstScenarioLogged(this@MainActivity)) {
                                        EjectAnalytics.logScenarioFirstUse(scenario.id)
                                        EjectPrefs.markFirstScenarioLogged(this@MainActivity)
                                    }
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

                        // v1.6.10 — pre-arm permission gate (EJECT 시점에 필수 권한 부족하면 등장).
                        // pendingProceed 는 onAllGranted 호출 시 한 번만 실행 후 초기화.
                        val proceedSnap = pendingProceed
                        val modeSnap = pendingMode
                        if (proceedSnap != null && modeSnap != null) {
                            PermissionGateDialog(
                                mode = modeSnap,
                                delayMs = pendingDelayMs,
                                onAllGranted = {
                                    pendingProceed = null
                                    pendingMode = null
                                    proceedSnap()
                                },
                                onCancel = {
                                    pendingProceed = null
                                    pendingMode = null
                                },
                            )
                        }

                        // v1.6.11 — In-App Update Snackbar.
                        // [updateDownloaded] 가 true 가 되면 한 번 띄우고 dismiss/action 후 더는
                        // 안 띄움 (snackbarHandled 플래그). 다음 앱 lifecycle (재실행) 에 다시.
                        val snackbarHostState = remember { SnackbarHostState() }
                        val downloaded by updateDownloaded
                        var snackbarHandled by remember { mutableStateOf(false) }
                        LaunchedEffect(downloaded) {
                            if (downloaded && !snackbarHandled) {
                                snackbarHandled = true
                                val result = snackbarHostState.showSnackbar(
                                    message = strings.updateDownloadedMsg,
                                    actionLabel = strings.updateRestartBtn,
                                    duration = SnackbarDuration.Indefinite,
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    completeFlexibleUpdate()
                                }
                            }
                        }
                        SnackbarHost(
                            hostState = snackbarHostState,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                // enableEdgeToEdge() 가 활성이라 system nav bar 위에
                                // 정렬되도록 inset 적용. 없으면 snackbar 가 nav bar 뒤에 깔림.
                                .windowInsetsPadding(WindowInsets.navigationBars)
                                .padding(bottom = 16.dp),
                        )
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

        // v1.6.11 — Flexible update 가 백그라운드 다운로드 완료된 채로 앱을 떠났다가
        // 돌아왔을 때 (예: 사용자가 잠시 다른 앱 쓰는 사이 다운로드 마무리) Snackbar 가
        // 다시 뜨도록 install status 재평가.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                updateDownloaded.value = true
            }
        }
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
        // v1.6.11 — In-App Update listener 누수 방지.
        runCatching { appUpdateManager.unregisterListener(installStateListener) }
    }
}

// ── 스플래시 화면 ─────────────────────────────────────────────────────────────

@Composable
private fun SplashScreen(initLabel: String, catchphrase: String) {
    val strings = LocalAppStrings.current
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
            // v1.5.14 — launcher 아이콘과 동일한 비주얼 (빨강 배경 + 흰 비상구 + 빨강 인물).
            // adaptive icon 의 둥근 사각형 마스크 (28dp corner)와 같은 형태로 클립.
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xFFB71720)), // EmergencyRed (launcher background)
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.mipmap.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                // v1.5.2 — 스플래시 brand label 도 다국어
                text       = strings.appBrandLabel,
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

// ── v1.6.10 — Pre-arm permission gate dialog ─────────────────────────────────
//
// EJECT 누른 시점에 [PermissionGate.missing] 이 비어있지 않을 때 노출. 다이얼로그가 떠 있는
// 동안:
//   1. ON_RESUME 마다 missing 재계산 → 모두 grant 되면 즉시 [onAllGranted] 호출 후 dismiss.
//   2. "허용하기" 클릭 시 첫 번째 미충족 권한의 시스템 intent 를 launch. 사용자가 시스템 화면에서
//      돌아오면 (1) 의 ON_RESUME 훅이 missing 을 다시 평가하므로 자동으로 다음 권한으로 진행
//      하거나 모두 충족 시 onAllGranted.
//   3. "다음에" 클릭 시 [onCancel] — 다이얼로그만 닫고 다음 EJECT 누름에 다시 재확인됨.
//
// Notif permission 은 RequestPermission contract, 나머지(overlay / battery opt)는 settings
// activity 이므로 StartActivityForResult contract 를 별도로 보유. 두 launcher 모두 결과 자체는
// 무시하고 ON_RESUME 재평가에 위임.
@Composable
private fun PermissionGateDialog(
    mode: TriggerMode,
    delayMs: Long,
    onAllGranted: () -> Unit,
    onCancel: () -> Unit,
) {
    val ctx = LocalContext.current
    val strings = LocalAppStrings.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 초기 진입 + 매 ON_RESUME 마다 재계산.
    var missing by remember(mode, delayMs) {
        mutableStateOf(PermissionGate.missing(ctx, mode, delayMs))
    }

    DisposableEffect(lifecycleOwner, mode, delayMs) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                missing = PermissionGate.missing(ctx, mode, delayMs)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 게이트 통과 시점에 한 번만 onAllGranted 호출. LaunchedEffect 안에서 호출해
    // recomposition 중 부수효과가 발생하지 않도록 한다.
    LaunchedEffect(missing) {
        if (missing.isEmpty()) onAllGranted()
    }

    if (missing.isEmpty()) return

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result 자체는 무시; ON_RESUME 재평가로 처리 */ }

    val settingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* result 자체는 무시; ON_RESUME 재평가로 처리 */ }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(strings.permGateTitle, fontWeight = FontWeight.Bold) },
        text = { Text(strings.permGateBody) },
        confirmButton = {
            TextButton(onClick = {
                when (missing.first()) {
                    PermissionGate.Req.OVERLAY -> {
                        settingsLauncher.launch(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${ctx.packageName}"),
                            )
                        )
                    }
                    PermissionGate.Req.POST_NOTIFICATIONS -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                    PermissionGate.Req.BATTERY_OPT -> {
                        runCatching {
                            settingsLauncher.launch(
                                Intent(
                                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                    Uri.parse("package:${ctx.packageName}"),
                                )
                            )
                        }.onFailure {
                            runCatching {
                                settingsLauncher.launch(
                                    Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                )
                            }
                        }
                    }
                }
            }) {
                Text(
                    strings.permGateBtnGrant,
                    color = EjectCoral,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(strings.permGateBtnCancel)
            }
        },
    )
}
