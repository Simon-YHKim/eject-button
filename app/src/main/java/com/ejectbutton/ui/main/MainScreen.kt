package com.ejectbutton.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlin.math.abs
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.res.stringResource
import com.ejectbutton.BuildConfig
import com.ejectbutton.R
import com.ejectbutton.ads.AdManager
import com.ejectbutton.data.AppLanguage
import com.ejectbutton.data.DecoyManager
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.data.Scenario
import com.ejectbutton.data.TriggerMode
import com.ejectbutton.data.SideButtonCommand
import com.ejectbutton.data.Urgency
import com.ejectbutton.data.defaultScenarios
import com.ejectbutton.data.formatPhoneWithHyphens
import com.ejectbutton.data.localizedDefaultScenarios
import com.ejectbutton.data.randomKoreanMobileLabel
import com.ejectbutton.data.withRuntimeCallerLabel
import com.ejectbutton.service.ButtonWatchService
import com.ejectbutton.service.CountdownBus
import com.ejectbutton.service.FakeCallOverlayService
import com.ejectbutton.service.ShakeDetectionService
import com.ejectbutton.ui.coachmark.CoachmarkHost
import com.ejectbutton.ui.coachmark.CoachmarkState
import com.ejectbutton.ui.coachmark.CoachmarkStep
import com.ejectbutton.ui.coachmark.SpotShape
import com.ejectbutton.ui.coachmark.rememberCoachmarkState
import android.provider.Settings as AndroidSettings
import com.ejectbutton.ui.theme.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.microsoft.clarity.modifiers.clarityMask
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private enum class AppScreen { COMMAND, HISTORY, SYSTEMS }

// 트리거 UI를 시간 × 모드 두 축으로 분리.
// 시간: IMMEDIATE / AFTER_10S / CUSTOM
// 모드: BUTTON(기본 EJECT 버튼 탭) / SHAKE / SIDE_BUTTON
private enum class TimeChoice { IMMEDIATE, AFTER_10S, CUSTOM }
private enum class ModeChoice { BUTTON, SHAKE, SIDE_BUTTON }

private fun TriggerMode.toTimeChoice(): TimeChoice = when (this) {
    TriggerMode.IMMEDIATE   -> TimeChoice.IMMEDIATE
    TriggerMode.AFTER_10S   -> TimeChoice.AFTER_10S
    TriggerMode.AFTER_30S   -> TimeChoice.AFTER_10S   // 레거시 값은 가장 가까운 새 옵션으로 폴백
    TriggerMode.AFTER_1MIN  -> TimeChoice.CUSTOM
    TriggerMode.CUSTOM      -> TimeChoice.CUSTOM
    TriggerMode.SHAKE       -> TimeChoice.IMMEDIATE
    TriggerMode.SIDE_BUTTON -> TimeChoice.IMMEDIATE
}

private fun TriggerMode.toModeChoice(): ModeChoice = when (this) {
    TriggerMode.SHAKE       -> ModeChoice.SHAKE
    TriggerMode.SIDE_BUTTON -> ModeChoice.SIDE_BUTTON
    else                    -> ModeChoice.BUTTON
}

private fun composeTrigger(time: TimeChoice, mode: ModeChoice): TriggerMode = when (mode) {
    ModeChoice.SHAKE       -> TriggerMode.SHAKE
    ModeChoice.SIDE_BUTTON -> TriggerMode.SIDE_BUTTON
    ModeChoice.BUTTON      -> when (time) {
        TimeChoice.IMMEDIATE -> TriggerMode.IMMEDIATE
        TimeChoice.AFTER_10S -> TriggerMode.AFTER_10S
        TimeChoice.CUSTOM    -> TriggerMode.CUSTOM
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    currentLanguage: AppLanguage,
    isPremium: Boolean,
    themeMode: com.ejectbutton.data.ThemeMode,
    onThemeModeChange: (com.ejectbutton.data.ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onPurchasePremium: () -> Unit,
    onRestorePurchase: () -> Unit,
    premiumPrice: String?,
    // v1.3.0 — INAPP 광고 제거 일회성 (eject_remove_ads_lifetime)
    isAdsRemoved: Boolean = false,
    onPurchaseRemoveAds: () -> Unit = {},
    removeAdsPrice: String? = null,
    onEject: (scenario: Scenario, delayMs: Long) -> Unit,
) {
    val ctx     = LocalContext.current
    val strings = LocalAppStrings.current
    val haptic  = LocalHapticFeedback.current

    var currentScreen    by remember { mutableStateOf(AppScreen.COMMAND) }
    var showPremiumSheet by remember { mutableStateOf(false) }
    // v1.1.0 — Rewarded Ad sheet. 비-Premium 사용자가 잠긴 기능을 누르면 띄운다.
    // [pendingRewardedAction] 은 광고 시청 완료(또는 Premium) 시 실행할 후속 동작.
    var showRewardedSheet by remember { mutableStateOf(false) }
    var pendingRewardedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // v1.2 — 언어별 mom/dad 매핑은 Scenario.kt 의 localizedDefaultScenarios 헬퍼로 통일.
    // 같은 로직이 SideButtonTrigger / MainScreen 두 곳에 중복돼 있던 것을 단일 출처로 리팩터.
    val localizedDefaults = remember(strings) {
        localizedDefaultScenarios(strings)
    }

    // 사이드 버튼 트리거가 백그라운드에서 발동될 때도 같은 선택을 사용하기 위해
    // selectedScenario / selectedTrigger / customDelaySec 를 EjectPrefs 와 동기화한다.
    val initialScenario = remember(strings) {
        val savedId = EjectPrefs.loadSelectedScenarioId(ctx)
        val all = localizedDefaults + EjectPrefs.loadScenarios(ctx)
        savedId?.let { id -> all.firstOrNull { it.id == id } } ?: localizedDefaults[0]
    }
    val initialTrigger = remember {
        runCatching {
            TriggerMode.valueOf(EjectPrefs.loadSelectedTrigger(ctx) ?: TriggerMode.IMMEDIATE.name)
        }.getOrDefault(TriggerMode.IMMEDIATE)
    }
    val initialTime = remember {
        val saved = EjectPrefs.loadSelectedTimeChoice(ctx)
        runCatching { TimeChoice.valueOf(saved ?: "") }.getOrDefault(initialTrigger.toTimeChoice())
    }
    var selectedScenario by remember(strings) { mutableStateOf(initialScenario) }
    var selectedTime     by remember { mutableStateOf(initialTime) }
    var selectedMode     by remember { mutableStateOf(initialTrigger.toModeChoice()) }
    val selectedTrigger  = composeTrigger(selectedTime, selectedMode)
    var customDelaySec   by remember { mutableIntStateOf(EjectPrefs.loadCustomDelaySec(ctx)) }

    LaunchedEffect(selectedScenario.id) {
        EjectPrefs.saveSelectedScenarioId(ctx, selectedScenario.id)
    }
    LaunchedEffect(selectedTrigger) {
        EjectPrefs.saveSelectedTrigger(ctx, selectedTrigger.name)
    }
    LaunchedEffect(selectedTime) {
        EjectPrefs.saveSelectedTimeChoice(ctx, selectedTime.name)
    }
    LaunchedEffect(customDelaySec) {
        EjectPrefs.saveCustomDelaySec(ctx, customDelaySec)
    }

    // 사이드 버튼 트리거 명령 — 메인 화면 카드에서도 변경 가능
    var sideButtonCommand by remember {
        mutableStateOf(EjectPrefs.loadSideButtonCommand(ctx))
    }
    // v1.1.4 — SETTINGS 탭 안 SettingsBodyInline 에서 sideButtonCommand 가 변경되면
    // EjectPrefs 에는 즉시 저장되지만 MainScreen 의 state 는 동기화되지 않는다.
    // 사용자가 다시 COMMAND 탭으로 이동할 때 prefs 값을 다시 읽어 메인 카드 표시를 맞춘다.
    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.COMMAND) {
            sideButtonCommand = EjectPrefs.loadSideButtonCommand(ctx)
        }
    }
    var showSideButtonPicker by remember { mutableStateOf(false) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showAddCaller    by remember { mutableStateOf(false) }
    // SIDE_BUTTON 트리거 상태: 대기 중 배지 + 미설정 경고 배너
    var sideButtonStandby by remember { mutableStateOf(false) }
    var sideButtonNotice  by remember { mutableStateOf(false) }
    // Round 9 — SHAKE arm 상태 (EJECT 탭으로 arm 된 상태).
    var shakeStandby      by remember { mutableStateOf(false) }
    // Round 9 — SIDE_BUTTON arm 직후 한 번만 띄우는 "볼륨 커맨드 안내" 팝업.
    var showSideVolumeHint by remember { mutableStateOf(false) }

    // v1.5.1 — 코치마크 4-step 투어 (디자인 적용판).
    // CoachmarkState 가 register-spot / index / isActive 모두 관리.
    // 첫 진입 시 EjectPrefs.loadCoachmarkSeen 이 false 면 LaunchedEffect 가 start().
    // OnboardingScreen 직후에만 의미가 있으므로 COMMAND 탭에서만 자동 트리거.
    val coachmark = rememberCoachmarkState()
    // v1.5.6 — 5-step (시나리오 → 타이밍 → 모드 → EJECT → ⚙).
    // 신규 step "timing" 추가 (TriggerTimeRow). 마지막 step (settings) primary 만 "옛썰!".
    val coachmarkSteps = remember(strings) {
        listOf(
            CoachmarkStep(
                id = "scenario",
                title = strings.coachmarkStep1Title,
                body  = strings.coachmarkStep1Desc,
                primaryLabel = strings.coachmarkNext,
            ),
            CoachmarkStep(
                id = "timing",
                title = strings.coachmarkStepTimingTitle,
                body  = strings.coachmarkStepTimingDesc,
                primaryLabel = strings.coachmarkNext,
            ),
            CoachmarkStep(
                id = "trigger",
                title = strings.coachmarkStep2Title,
                body  = strings.coachmarkStep2Desc,
                primaryLabel = strings.coachmarkNext,
            ),
            CoachmarkStep(
                id = "eject",
                title = strings.coachmarkStep3Title,
                body  = strings.coachmarkStep3Desc,
                primaryLabel = strings.coachmarkNext,
            ),
            // v1.5.15 — Step 5: 위장 토글 IconButton spotlight (변경: settings 보다 먼저).
            // TopBar 좌측 위장(⏏) → 우측 설정(⚙) 시각 동선이 자연스럽다.
            // 위장은 옵션적 기능이라 먼저 짧게 안내하고, 마지막에 settings(본부 정비)에서
            // 모든 설정의 진입점을 보여주며 투어를 마무리한다.
            CoachmarkStep(
                id = "disguise",
                title = strings.coachmarkStepDisguiseTitle,
                body  = strings.coachmarkStepDisguiseDesc,
                primaryLabel = strings.coachmarkNext,
            ),
            // v1.5.15 — Step 6 (마지막): 본부 정비 = 모든 설정의 진입점.
            // 마지막 step 이라 primary 라벨에 onboardingFinalDismiss ("받았다." 등) 사용.
            CoachmarkStep(
                id = "settings",
                title = strings.coachmarkStep4Title,
                body  = strings.coachmarkStep4Desc,
                primaryLabel = strings.onboardingFinalDismiss,
            ),
        )
    }
    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.COMMAND &&
            !EjectPrefs.loadCoachmarkSeen(ctx) &&
            !coachmark.isActive
        ) {
            coachmark.start()
        }
    }

    // Round 11 — 모드 전환 = 자동 취소.
    // 현재 진행 중인 countdown / 서비스 / arm 플래그를 모두 정리.
    // 사용자가 EJECT 취소 버튼을 누르지 않고 모드만 바꿔도 안전하게 clean slate 로
    // 돌아가도록. 새 모드에서 arm 하려면 EJECT 를 다시 눌러야 한다.
    // (모든 state var 가 선언된 뒤에 위치해야 Kotlin 이 캡처할 수 있음 — 위로 올려두면 컴파일 에러)
    DisposableEffect(selectedMode) {
        CountdownBus.clear()
        FakeCallOverlayService.stop(ctx)
        ShakeDetectionService.stop(ctx)
        EjectPrefs.saveSideButtonArmed(ctx, false)
        ButtonWatchService.reconcile(ctx)
        sideButtonStandby = false
        shakeStandby      = false
        onDispose { }
    }

    var customCallers    by remember { mutableStateOf(EjectPrefs.loadScenarios(ctx)) }
    // Round 30 — 사용자가 숨긴 프리셋 (mom/dad) id 집합. Settings > "프리셋 복원" 에서 초기화.
    var deletedPresetIds by remember { mutableStateOf(EjectPrefs.loadDeletedPresetIds(ctx)) }
    // Round 32 — 실수로 X 를 눌러 발신자가 바로 삭제되는 걸 막기 위한 확인 팝업.
    // 사용자가 "예" 를 눌러야 실제 삭제가 실행됨.
    var pendingDeleteCaller by remember { mutableStateOf<Scenario?>(null) }
    var history          by remember { mutableStateOf(EjectPrefs.loadHistory(ctx)) }
    // 탈출 기록 초기화 후 확인 팝업
    var showHistoryClearedDialog by remember { mutableStateOf(false) }
    // 뒤로가기(Command 탭) 종료 확인 팝업
    var showExitConfirmDialog    by remember { mutableStateOf(false) }

    // 딜레이 카운트다운 — EJECT 탭 / SHAKE / SIDE_BUTTON 어디서 시작하든 공유 버스 구독
    val countdownEnd by CountdownBus.endMs.collectAsState()
    var countdown    by remember { mutableIntStateOf(0) }
    LaunchedEffect(countdownEnd) {
        if (countdownEnd <= 0L) { countdown = 0; return@LaunchedEffect }
        while (true) {
            val remaining = ((countdownEnd - System.currentTimeMillis()) / 1000 + 1).toInt()
            countdown = remaining.coerceAtLeast(0)
            if (countdown == 0) break
            delay(300L)
        }
    }

    if (showSideButtonPicker) {
        SideButtonCommandPickerDialog(
            current   = sideButtonCommand,
            onSelect  = { cmd ->
                sideButtonCommand = cmd
                EjectPrefs.saveSideButtonCommand(ctx, cmd)
                ButtonWatchService.reconcile(ctx)
                showSideButtonPicker = false
            },
            onDismiss = { showSideButtonPicker = false },
        )
    }

    if (showCustomDialog) {
        CustomDelayDialog(
            initial  = customDelaySec,
            onDismiss = { showCustomDialog = false },
            onConfirm = { sec -> customDelaySec = sec; showCustomDialog = false },
        )
    }

    if (showAddCaller) {
        if (!isPremium && customCallers.size >= 1) {
            // v1.1.0 — 무료 사용자가 발신자 1명을 초과하려 하면 RewardedAdDialog
            // 로 두 가지 옵션 제시 (광고 1회 / Premium 영구). 기존 AlertDialog 의
            // 단방향 "Upgrade only" CTA 대비 사용자 옵션 폭이 넓어진다.
            showAddCaller = false
            pendingRewardedAction = {
                // 광고 시청 보상으로 1회 발신자 추가 권한이 부여되면 다시 다이얼로그를 띄운다.
                showAddCaller = true
            }
            showRewardedSheet = true
        } else {
            AddCallerDialog(
                onDismiss = { showAddCaller = false },
                onConfirm = { caller ->
                    val updated = customCallers + caller
                    customCallers = updated
                    EjectPrefs.saveScenarios(ctx, updated)
                    selectedScenario = caller
                    showAddCaller = false
                }
            )
        }
    }

    // Round 32 — 호출 대상 삭제 확인. 실수로 X 를 눌러 즉시 삭제되는 걸 막는다.
    pendingDeleteCaller?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCaller = null },
            title = { Text(strings.deleteCallerTitle, fontWeight = FontWeight.Bold) },
            text  = { Text(strings.deleteCallerMsg.format(target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    val presetIds = defaultScenarios.map { it.id }.toSet()
                    if (target.id in presetIds) {
                        val updated = deletedPresetIds + target.id
                        deletedPresetIds = updated
                        EjectPrefs.saveDeletedPresetIds(ctx, updated)
                    } else {
                        val updated = customCallers.filter { it.id != target.id }
                        customCallers = updated
                        EjectPrefs.saveScenarios(ctx, updated)
                    }
                    if (selectedScenario.id == target.id) {
                        val remaining = localizedDefaults.filterNot { it.id in deletedPresetIds || it.id == target.id } +
                                customCallers.filter { it.id != target.id }
                        selectedScenario = remaining.firstOrNull() ?: localizedDefaults[0]
                    }
                    pendingDeleteCaller = null
                }) { Text(strings.dialogYes, color = EjectCoral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCaller = null }) {
                    Text(strings.dialogNo)
                }
            },
            containerColor = EjectSurface,
        )
    }

    // 기록 초기화 확인 팝업
    if (showHistoryClearedDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryClearedDialog = false },
            title            = { Text(strings.settingsClearHistory, fontWeight = FontWeight.Bold) },
            text             = { Text(strings.historyClearedMsg) },
            confirmButton    = {
                TextButton(onClick = { showHistoryClearedDialog = false }) {
                    Text(
                        strings.dialogConfirm,
                        color      = EjectCoral,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            containerColor = EjectSurface,
        )
    }

    // 종료 확인 팝업 (Command 탭에서 뒤로가기 시)
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title            = { Text(strings.exitConfirmTitle, fontWeight = FontWeight.Bold) },
            text             = { Text(strings.exitConfirmMsg) },
            confirmButton    = {
                TextButton(onClick = {
                    showExitConfirmDialog = false
                    (ctx as? android.app.Activity)?.finish()
                }) {
                    Text(
                        strings.dialogYes,
                        color      = EjectCoral,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text(strings.dialogNo)
                }
            },
            containerColor = EjectSurface,
        )
    }

    // Round 9 — 사이드 모드 arm 직후 "백그라운드에서 볼륨 커맨드를 누르세요" 안내 팝업.
    if (showSideVolumeHint) {
        AlertDialog(
            onDismissRequest = { showSideVolumeHint = false },
            title            = { Text(strings.sideModeArmedTitle, fontWeight = FontWeight.Bold) },
            text             = { Text(strings.sideModeArmedMsg) },
            confirmButton    = {
                TextButton(onClick = { showSideVolumeHint = false }) {
                    Text(
                        strings.dialogConfirm,
                        color      = EjectCoral,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            containerColor = EjectSurface,
        )
    }

    // 뒤로가기 처리 (최우선)
    // 우선순위: 종료 다이얼로그 열림 → 닫기 / 설정 화면 열림 → 닫기 /
    //          Command 이외 탭 → Command 로 이동 / Command 탭 → 종료 확인 팝업
    BackHandler(enabled = true) {
        when {
            showExitConfirmDialog    -> showExitConfirmDialog = false
            showHistoryClearedDialog -> showHistoryClearedDialog = false
            showSideVolumeHint       -> showSideVolumeHint = false
            showRewardedSheet        -> { showRewardedSheet = false; pendingRewardedAction = null }
            showPremiumSheet         -> showPremiumSheet = false
            showAddCaller            -> showAddCaller = false
            showCustomDialog         -> showCustomDialog = false
            showSideButtonPicker     -> showSideButtonPicker = false
            currentScreen != AppScreen.COMMAND -> currentScreen = AppScreen.COMMAND
            else                     -> showExitConfirmDialog = true
        }
    }

    // 프리미엄 업그레이드 다이얼로그
    if (showPremiumSheet) {
        PremiumUpgradeDialog(
            price     = premiumPrice,
            onBuy     = { onPurchasePremium(); showPremiumSheet = false },
            onRestore = { onRestorePurchase(); showPremiumSheet = false },
            onDismiss = { showPremiumSheet = false },
        )
    }

    // v1.1.0 — Rewarded Ad / Premium 선택 시트.
    if (showRewardedSheet) {
        RewardedAdDialog(
            onWatchAd = {
                showRewardedSheet = false
                val activity = ctx as? android.app.Activity
                val action = pendingRewardedAction
                if (activity != null) {
                    AdManager.showRewarded(
                        activity = activity,
                        onRewarded = {
                            // 광고 끝까지 시청 → pendingAction 실행 (예: AddCallerDialog 재오픈)
                            action?.invoke()
                            pendingRewardedAction = null
                        },
                        onDismissed = {
                            // 보상 없이 끝났을 때만 pending 정리. 보상 받은 경우 onRewarded 가 먼저 정리.
                            if (pendingRewardedAction === action) pendingRewardedAction = null
                        },
                    )
                } else {
                    pendingRewardedAction = null
                }
            },
            onUpgradePremium = {
                showRewardedSheet = false
                pendingRewardedAction = null
                showPremiumSheet = true
            },
            onDismiss = {
                showRewardedSheet = false
                pendingRewardedAction = null
            },
        )
    }

    // v1.1.4 — SettingsScreen 별도 진입 제거. SETTINGS 탭으로 이동하면 본문이
    // 직접 SettingsBodyInline 을 emit. SettingsScreen() composable 자체는
    // 호환을 위해 SettingsScreen.kt 에 남아있지만 더 이상 호출되지 않는다.

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EjectBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            // Round 11 — COMMAND ↔ HISTORY ↔ SYSTEMS 탭 스와이프.
            // 드래그 누적 거리를 저장했다가 onDragEnd 시점에 단 한 번만 판정해
            // 어떤 속도/길이의 제스처에서도 항상 "한 번 스와이프 = 한 탭 이동" 보장.
            //
            // v1.5.1 — 코치마크 활성 시 차단. coachmark.isActive 를 pointerInput key 로 두어
            // 코치마크 시작/종료 시 제스처 핸들러가 새로 매칭되도록 강제.
            .pointerInput(coachmark.isActive) {
                if (coachmark.isActive) return@pointerInput
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart  = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f },
                    onDragEnd = {
                        val screens = AppScreen.entries
                        val cur = screens.indexOf(currentScreen)
                        // 60f 는 의도한 스와이프만 통과시키는 실측 임계값
                        // (너무 낮으면 스크롤 중의 횡 흔들림이 탭 이동으로 오인됨).
                        if (totalDrag < -60f && cur < screens.size - 1) {
                            currentScreen = screens[cur + 1]
                        } else if (totalDrag > 60f && cur > 0) {
                            currentScreen = screens[cur - 1]
                        }
                        totalDrag = 0f
                    },
                ) { _, dragAmount ->
                    totalDrag += dragAmount
                }
            },
    ) {
        // 메인 컨텐츠 (하단 바 + 광고 영역만큼 패딩).
        // 광고 카드 ~34dp + spacer 8dp + BottomBar ~56dp + spacer 12dp ≈ 110dp
        // 정도만 필요하므로 여유 10dp 포함 120dp 로 축소. 과거 170dp 는 너무
        // 많이 잡아먹어서 standby/countdown 배너가 뜨면 모드 섹션이 잘렸다.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val goingRight = targetState.ordinal > initialState.ordinal
                    val dir = if (goingRight) 1 else -1
                    (slideInHorizontally(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                    ) { w -> dir * w } + fadeIn(tween(180))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                    ) { w -> -dir * w } + fadeOut(tween(180)))
                },
                label = "screen-slide",
            ) { screen ->
            when (screen) {
                AppScreen.COMMAND -> CommandContent(
                    selectedScenario = selectedScenario,
                    selectedTime     = selectedTime,
                    selectedMode     = selectedMode,
                    customDelaySec   = customDelaySec,
                    // Round 30 — 프리셋도 삭제 가능하므로 deletedPresetIds 로 필터링.
                    // customCallerIds 는 "이름은 그대로지만 **삭제 가능한** id 집합" 의미로 확장 — 모든 표시 caller 포함.
                    allCallers       = (localizedDefaults.filterNot { it.id in deletedPresetIds } + customCallers),
                    customCallerIds  = (localizedDefaults.filterNot { it.id in deletedPresetIds } + customCallers).map { it.id }.toSet(),
                    countdown        = countdown,
                    sideButtonCommand = sideButtonCommand,
                    sideButtonStandby = sideButtonStandby,
                    shakeStandby      = shakeStandby,
                    sideButtonNotice  = sideButtonNotice,
                    onDismissNotice   = { sideButtonNotice = false },
                    onOpenSettingsForSideButton = {
                        sideButtonNotice = false
                        currentScreen = AppScreen.SYSTEMS
                    },
                    onOpenSideButtonPicker = { showSideButtonPicker = true },
                    onSelectCaller   = { scenario ->
                        selectedScenario = scenario
                        // v1.1.5 — Clarity funnel: 시나리오 선택 이벤트 (custom vs preset 구분).
                        val type = if (scenario.id in setOf("mom", "dad")) "preset" else "custom"
                        com.ejectbutton.analytics.EjectClarity.scenarioSelected(type)
                    },
                    onDeleteCaller   = { toDelete ->
                        // Round 32 — 즉시 삭제하지 않고 확인 다이얼로그 표시. 실제 삭제는 "예" 버튼 onClick.
                        pendingDeleteCaller = toDelete
                    },
                    onSelectTime     = { time ->
                        // Round 12 — 프리미엄 게이팅 임시 해제 (사용자 테스트용).
                        // Play Console 에 제품 등록 + 수익 플로우 준비되면 다시 건다.
                        selectedTime = time
                        if (time == TimeChoice.CUSTOM) showCustomDialog = true
                    },
                    onSelectMode     = { mode ->
                        // Round 12 — 프리미엄 게이팅 임시 해제.
                        selectedMode = mode
                        com.ejectbutton.analytics.EjectAnalytics.logModeChanged(mode.name)
                    },
                    onAddCaller      = { showAddCaller = true },
                    onSettingsTap    = { currentScreen = AppScreen.SYSTEMS },
                    onEject          = handleEject@{
                        // Round 12 — 프리미엄 게이팅 임시 해제 (사용자 테스트용).

                        // Round 9 — SIDE_BUTTON 모드: EJECT 탭으로 arm + 볼륨 안내 팝업.
                        if (selectedTrigger == TriggerMode.SIDE_BUTTON) {
                            val cmd = EjectPrefs.loadSideButtonCommand(ctx)
                            if (!cmd.isEnabled) {
                                sideButtonNotice = true
                                return@handleEject
                            }
                            // 오버레이 권한이 없으면 arm 해도 트리거 시 가짜 전화가 뜨지 않으므로
                            // 권한 설정 화면을 띄우고 arm 은 보류.
                            if (!AndroidSettings.canDrawOverlays(ctx)) {
                                EjectPrefs.saveSideButtonArmed(ctx, false)
                                ButtonWatchService.reconcile(ctx)
                                runCatching {
                                    ctx.startActivity(
                                        android.content.Intent(
                                            AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${ctx.packageName}"),
                                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                                android.widget.Toast.makeText(
                                    ctx,
                                    strings.sideButtonOverlayRequired,
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                                return@handleEject
                            }
                            EjectPrefs.saveSelectedScenarioId(ctx, selectedScenario.id)
                            EjectPrefs.saveSelectedTrigger(ctx, selectedTrigger.name)
                            EjectPrefs.saveSideButtonArmed(ctx, true)
                            ButtonWatchService.reconcile(ctx)
                            sideButtonStandby    = true
                            showSideVolumeHint   = true
                            if (EjectPrefs.loadHaptic(ctx)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            return@handleEject
                        }

                        // Round 9 — SHAKE 모드: EJECT 탭으로 ShakeDetectionService arm.
                        if (selectedTrigger == TriggerMode.SHAKE) {
                            if (!AndroidSettings.canDrawOverlays(ctx)) {
                                runCatching {
                                    ctx.startActivity(
                                        android.content.Intent(
                                            AndroidSettings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            android.net.Uri.parse("package:${ctx.packageName}"),
                                        ).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                                android.widget.Toast.makeText(
                                    ctx,
                                    strings.sideButtonOverlayRequired,
                                    android.widget.Toast.LENGTH_LONG,
                                ).show()
                                return@handleEject
                            }
                            EjectPrefs.saveSelectedScenarioId(ctx, selectedScenario.id)
                            EjectPrefs.saveSelectedTrigger(ctx, selectedTrigger.name)
                            val shakeDelayMs = when (selectedTime) {
                                TimeChoice.IMMEDIATE -> 0L
                                TimeChoice.AFTER_10S -> 10_000L
                                TimeChoice.CUSTOM    -> customDelaySec * 1000L
                            }
                            // v1.2 — SHAKE armed 시점에 runtime caller label 을 결정해서
                            // service intent 에 직접 박는다. ShakeDetectionService 가
                            // 자체적으로 또 무작위화하지 않도록 발사 직전 한 번만 샘플링.
                            val shakeScenario = selectedScenario.withRuntimeCallerLabel()
                            ShakeDetectionService.start(
                                ctx,
                                shakeScenario.callerName,
                                shakeScenario.callerLabel,
                                shakeScenario.prompterHint,
                                shakeDelayMs,
                                scenarioId = shakeScenario.id,
                                mode = "shake",
                            )
                            shakeStandby = true
                            if (EjectPrefs.loadHaptic(ctx)) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            return@handleEject
                        }

                        val delayMs = when (selectedTrigger) {
                            TriggerMode.IMMEDIATE -> 0L
                            TriggerMode.AFTER_10S -> 10_000L
                            TriggerMode.AFTER_30S -> 30_000L
                            TriggerMode.AFTER_1MIN -> 60_000L
                            TriggerMode.SHAKE     -> -1L
                            TriggerMode.SIDE_BUTTON -> 0L // handled above
                            TriggerMode.CUSTOM    -> customDelaySec * 1000L
                        }
                        CountdownBus.start(delayMs)

                        if (EjectPrefs.loadHaptic(ctx)) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        val triggerLabel = when (selectedTrigger) {
                            TriggerMode.IMMEDIATE -> strings.triggerNow
                            TriggerMode.AFTER_10S -> strings.trigger10s
                            TriggerMode.AFTER_30S -> strings.trigger30s
                            TriggerMode.AFTER_1MIN -> strings.trigger1min
                            TriggerMode.SHAKE     -> strings.triggerShake
                            TriggerMode.SIDE_BUTTON -> strings.triggerSideButton
                            TriggerMode.CUSTOM    -> "${customDelaySec}s"
                        }
                        val entry = "${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())} · ${selectedScenario.emoji}${selectedScenario.name} · $triggerLabel"
                        EjectPrefs.addHistory(ctx, entry)
                        history = EjectPrefs.loadHistory(ctx)
                        // Round 30 / v1.2 — mom/dad 프리셋은 isRandomPhone=true 이므로 실제
                        // 통화 직전 매번 새로운 무작위 번호로 callerLabel 갱신. 커스텀 발신자는
                        // 입력한 번호 유지. 인라인 분기 → Scenario.withRuntimeCallerLabel() 헬퍼.
                        val scenarioToSend = selectedScenario.withRuntimeCallerLabel()
                        onEject(scenarioToSend, delayMs)
                    },
                    onCancel = {
                        // countdown 중이면 진행 중인 가짜 전화 서비스도 함께 종료.
                        // SHAKE / SIDE_BUTTON arm 상태도 모두 해제 (Round 9).
                        FakeCallOverlayService.stop(ctx)
                        CountdownBus.clear()
                        sideButtonStandby = false
                        shakeStandby      = false
                        ShakeDetectionService.stop(ctx)
                        EjectPrefs.saveSideButtonArmed(ctx, false)
                        ButtonWatchService.reconcile(ctx)
                    },
                    coachmark = coachmark,
                )
                AppScreen.HISTORY -> HistoryContent(
                    history = history,
                    onSettingsTap = { currentScreen = AppScreen.SYSTEMS },
                )
                AppScreen.SYSTEMS -> SystemsContent(
                    isPremium         = isPremium,
                    premiumPrice      = premiumPrice,
                    isAdsRemoved      = isAdsRemoved,
                    removeAdsPrice    = removeAdsPrice,
                    onPurchaseRemoveAds = onPurchaseRemoveAds,
                    currentLanguage   = currentLanguage,
                    themeMode         = themeMode,
                    onUpgradePremium  = { showPremiumSheet = true },
                    onClearHistory    = {
                        EjectPrefs.clearHistory(ctx)
                        history = emptyList()
                        showHistoryClearedDialog = true
                    },
                    onLanguageChange  = { lang ->
                        // Round 31 → v1.1.4 — 인라인 Language 선택. EjectPrefs 저장은
                        // SettingsBodyInline 내부 LanguagePickerDialog 가 책임지고,
                        // 여기서는 MainScreen 의 currentLanguage state 만 갱신.
                        onLanguageChange(lang)
                    },
                    onThemeModeChange = { mode -> onThemeModeChange(mode) },
                    // Round 31 — 프리셋 복원 callback. MainScreen 의 state 와 EjectPrefs 동기화.
                    onRestorePresets  = {
                        deletedPresetIds = emptySet()
                        EjectPrefs.clearDeletedPresetIds(ctx)
                    },
                    // v1.5.12 — "사용 설명서" 클릭 시 코치마크 강제 재시작.
                    //   EjectPrefs.saveCoachmarkSeen(false) → COMMAND 탭 진입 시 자동 트리거 다시 가능
                    //   coachmark.start() → SETTINGS 탭에서는 spotlight target 이 없으므로,
                    //     COMMAND 로 자동 이동 후 시작하도록 currentScreen 도 함께 변경.
                    onShowGuide = {
                        EjectPrefs.saveCoachmarkSeen(ctx, false)
                        currentScreen = AppScreen.COMMAND
                        coachmark.start()
                    },
                )
            }
            }
        }

        // 하단 고정 영역: 탭바(위) + 광고(아래).
        // v1.6.1 — 사용자 피드백: 탭바와 광고 위치 교체. 광고를 BottomBar 위가
        //  아니라 아래로 옮겨서, 탭(출동/기록/설정) 가까이 손가락이 닿는
        //  자리에 광고가 끼어드는 것을 방지. 광고는 화면 최하단에 안정적으로
        //  앉아 있고, 사용자의 1차 컨트롤(탭)이 위로 올라가 조작 흐름이
        //  자연스러워진다.
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(EjectBg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BottomBar(
                current = currentScreen,
                isPremium = isPremium,
                onSelect = { currentScreen = it },
                onPremiumTap = { showPremiumSheet = true },
            )

            // 네이티브 광고 (무료 사용자만) — 탭바 아래 위치
            if (!isPremium) {
                Spacer(Modifier.height(8.dp))
                val nativeAd by AdManager.nativeAd.collectAsState()
                nativeAd?.let { ad ->
                    NativeAdCard(ad = ad, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
        }

    }

    // v1.5.7 round 3 — CoachmarkHost를 outer Box 밖 (root composition 레벨) 으로 이동.
    // 이전 위치: outer Box (statusBarsPadding 적용) 안 → overlay 좌표계가 status bar 만큼 시프트
    //   → register(boundsInRoot, status bar 포함 좌표) 와 mismatch (≈130px) 로 ring off-by-one.
    // 새 위치: root level → overlay도 boundsInRoot와 같은 window root 좌표계 → ring 정확.
    CoachmarkHost(
        state = coachmark,
        steps = coachmarkSteps,
        onFinish = { EjectPrefs.saveCoachmarkSeen(ctx, true) },
    )
}

// ─── COMMAND 탭 ──────────────────────────────────────────────────────────────

@Composable
private fun CommandContent(
    selectedScenario: Scenario,
    selectedTime: TimeChoice,
    selectedMode: ModeChoice,
    customDelaySec: Int,
    allCallers: List<Scenario>,
    customCallerIds: Set<String>,
    countdown: Int,
    sideButtonCommand: SideButtonCommand,
    sideButtonStandby: Boolean,
    shakeStandby: Boolean,
    sideButtonNotice: Boolean,
    onDismissNotice: () -> Unit,
    onOpenSettingsForSideButton: () -> Unit,
    onOpenSideButtonPicker: () -> Unit,
    onSelectCaller: (Scenario) -> Unit,
    onDeleteCaller: (Scenario) -> Unit,
    onSelectTime: (TimeChoice) -> Unit,
    onSelectMode: (ModeChoice) -> Unit,
    onAddCaller: () -> Unit,
    onSettingsTap: () -> Unit,
    onEject: () -> Unit,
    onCancel: () -> Unit,
    coachmark: CoachmarkState,
) {
    val strings = LocalAppStrings.current
    val isCancelMode = countdown > 0 || sideButtonStandby || shakeStandby

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // v1.6.1 — 한 화면 fit: top spacer 14→8, 섹션 간 spacer 24→16 으로 일괄
        //   축소. EJECT 버튼 80% 축소(260→208dp)와 합쳐 ≈84dp 절약 → 광고+
        //   네비 영역(≈100dp)이 들어와도 스크롤 없이 한 화면에 모든 컨트롤
        //   (출동 버튼·캐릭터·타이밍·트리거 모드) 노출.
        // v1.6.2 — 와이프 피드백: v1.6.1 의 spacing 이 너무 좁아 시각 답답함.
        //   8→12 / 10→14 / 16→20 일괄 +4dp 증가. EJECT 버튼은 208dp 유지 +
        //   광고 크기 유지 (그대로) → 한 화면 fit 도 유지하며 breathing room 확보.
        Spacer(Modifier.height(12.dp))
        StitchTopBar(
            onSettingsTap = onSettingsTap,
            // v1.5.1 — 코치마크 Step 4 spotlight 등록 (라운드 사각형 — IconButton 자체가 정사각형이라 RoundRect 로).
            // v1.6.2 — 와이프 피드백: TopBar IconButton 의 spotlight 가 너무 크게 표시.
            //   register rect 자체를 80% 로 deflate 해서 spotlight 가 element 면적에
            //   더 fit 되도록. 두 IconButton 이 동일 사이즈 (40dp) 라서 deflated
            //   결과도 동일 → step 5(disguise)/6(settings) spotlight 크기 균일.
            onSettingsBoundsChanged = { rect ->
                coachmark.register("settings", shrinkRect(rect, 0.8f), SpotShape.RoundRect)
            },
            // v1.5.12 — 코치마크 Step 6 (위장 토글) spotlight 등록.
            onDisguiseBoundsChanged = { rect ->
                coachmark.register("disguise", shrinkRect(rect, 0.8f), SpotShape.RoundRect)
            },
        )

        Spacer(Modifier.height(20.dp))

        // 카운트다운 배너
        AnimatedVisibility(
            visible = countdown > 0,
            enter   = fadeIn() + slideInVertically(),
            exit    = fadeOut() + slideOutVertically(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EjectCoral.copy(alpha = 0.14f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    // v1.0.10 — Locale.US 명시. 미지정 시 기본 로케일이 적용되어
                    // 일부 언어 (AR/HI/FA 등) 에서 숫자 표기가 예상과 다르게 출력될 수 있다.
                    // 카운트다운 숫자는 의도상 항상 0-9 ASCII 로 표시되어야 한다.
                    String.format(java.util.Locale.US, strings.countdownFmt, countdown),
                    fontSize = 14.sp, color = EjectCoral, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }

        // 사이드 버튼 대기 중 배너
        AnimatedVisibility(
            visible = sideButtonStandby && countdown == 0,
            enter   = fadeIn() + slideInVertically(),
            exit    = fadeOut() + slideOutVertically(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EjectCoral.copy(alpha = 0.14f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    strings.sideButtonStandby,
                    fontSize = 14.sp, color = EjectCoral, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }

        // Round 9 — 흔들기 대기 중 배너 (EJECT 탭으로 arm 된 상태).
        AnimatedVisibility(
            visible = shakeStandby && countdown == 0,
            enter   = fadeIn() + slideInVertically(),
            exit    = fadeOut() + slideOutVertically(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EjectCoral.copy(alpha = 0.14f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    strings.shakeNotifTitle,
                    fontSize = 14.sp, color = EjectCoral, fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
            }
        }

        // 사이드 버튼 미설정 경고 다이얼로그
        if (sideButtonNotice) {
            AlertDialog(
                onDismissRequest = onDismissNotice,
                title = { Text(strings.settingSideButton, fontWeight = FontWeight.Bold) },
                text  = { Text(strings.sideButtonNotConfigured) },
                confirmButton = {
                    TextButton(onClick = onOpenSettingsForSideButton) {
                        Text(strings.settingsTitle, color = EjectCoral, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissNotice) { Text(strings.dialogCancel) }
                },
                containerColor = EjectSurface,
            )
        }

        // EJECT 버튼 — countdown/standby 중엔 취소 버튼으로 변신
        // v1.5.1 — 코치마크 Step 3 spotlight 등록 (원형).
        Box(
            modifier = Modifier.onGloballyPositioned { coords ->
                // v1.5.15 — EJECT 버튼이 v1.5.13 부터 둥근 사각형 픽토그램(ic_eject_button)
                // 으로 바뀌었기 때문에 spotlight 도 RoundRect 로 같이 맞춰야 시각적으로 자연스럽다.
                // 기존 Circle 은 v1.5.0~v1.5.12 (빨간 원형 + ⏏ 글리프) 시절의 잔재.
                coachmark.register("eject", coords.boundsInRoot(), SpotShape.RoundRect)
            },
        ) {
            EjectButton(
                isCancelMode = isCancelMode,
                onClick = { if (isCancelMode) onCancel() else onEject() },
            )
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text          = strings.noEscapeLabel,
            fontSize      = 13.sp,
            color         = EjectSecondary,
            fontWeight    = FontWeight.Medium,
        )

        Spacer(Modifier.height(20.dp))

        // v1.5.7 라운드 1 — SectionHeader 를 register Box 밖으로.
        // 이전 wrap 패턴: ring = 헤더 + 12dp + chips (헤더가 ring 안에 통째 보였음).
        // 변경: SectionHeader 외부 / Box(register) 가 chips 만 wrap → ring ≈ chips 영역.
        SectionHeader(strings.sectionCaller)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    coachmark.register("scenario", coords.boundsInRoot(), SpotShape.RoundRect)
                }
                .fillMaxWidth(),
        ) {
            CallerChips(
                callers   = allCallers,
                selected  = selectedScenario,
                customIds = customCallerIds,
                onSelect  = onSelectCaller,
                onDelete  = onDeleteCaller,
                onAdd     = onAddCaller,
            )
        }

        Spacer(Modifier.height(20.dp))

        // v1.5.6 — 신규 step "timing" — wrap 패턴 (v1.5.5 와 동일).
        SectionHeader(strings.sectionDelay)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    coachmark.register("timing", coords.boundsInRoot(), SpotShape.RoundRect)
                }
                .fillMaxWidth(),
        ) {
            TriggerTimeRow(
                selected       = selectedTime,
                customDelaySec = customDelaySec,
                onSelect       = onSelectTime,
            )
        }

        Spacer(Modifier.height(20.dp))

        // v1.5.6 — 트리거 모드 — wrap 패턴 (v1.5.5 와 동일).
        SectionHeader(strings.sectionTriggerMode)
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    coachmark.register("trigger", coords.boundsInRoot(), SpotShape.RoundRect)
                }
                .fillMaxWidth(),
        ) {
            TriggerModeRow(
                selected = selectedMode,
                onSelect = onSelectMode,
            )
        }

        // 사이드 모드 선택 시에만 사이드 버튼 설정 카드 노출
        if (selectedMode == ModeChoice.SIDE_BUTTON) {
            Spacer(Modifier.height(16.dp))
            SideButtonModeCard(
                current = sideButtonCommand,
                onClick = onOpenSideButtonPicker,
            )
        }

        Spacer(Modifier.height(28.dp))
    }
}

/**
 * v1.6.2 — Deflate a Compose Rect to a percentage of its original size, keeping
 *   the center. Used by coachmark register sites where the underlying element's
 *   bounding box is larger than the visual hit-target and the spotlight should
 *   hug the icon more tightly (e.g. TopBar IconButton steps 5 + 6).
 *
 *   factor = 0.8f → spotlight width/height shrinks to 80% of the original rect,
 *   center stays put. With CoachmarkOverlay's existing `pad = 8dp` baseline, the
 *   net cutout area lands at ~80% of the old spotlight.
 */
private fun shrinkRect(rect: androidx.compose.ui.geometry.Rect, factor: Float): androidx.compose.ui.geometry.Rect {
    val cx = rect.center.x
    val cy = rect.center.y
    val w = rect.width * factor
    val h = rect.height * factor
    return androidx.compose.ui.geometry.Rect(
        left = cx - w / 2f,
        top  = cy - h / 2f,
        right  = cx + w / 2f,
        bottom = cy + h / 2f,
    )
}

@Composable
private fun StitchTopBar(
    onSettingsTap: () -> Unit,
    showSettingsIcon: Boolean = true,
    // v1.5.1 — 코치마크 Step 4 (⚙ 설정 spotlight) 등록용. COMMAND 탭에서만 전달.
    onSettingsBoundsChanged: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
    // v1.5.12 — 코치마크 Step 6 (위장 토글 spotlight) 등록용. COMMAND 탭에서만 전달.
    onDisguiseBoundsChanged: ((androidx.compose.ui.geometry.Rect) -> Unit)? = null,
) {
    val strings = LocalAppStrings.current
    val ctx = LocalContext.current

    // v1.5.12 — 위장 토글 IconButton. Settings 왼쪽에 항상 노출되며 두 상태로 토글:
    //   STATE 1 (isDisguised=false, 일반 모드)
    //     drawable: ic_disguise_off (가면 위 + ⏏ 아래)
    //     contentDescription: actionDisguiseOn ("앱 위장")
    //     onClick: 위장 picker 다이얼로그 → 4개 옵션 (계산기/메모/날씨/시계) 중 선택
    //   STATE 2 (isDisguised=true, 위장 모드)
    //     drawable: ic_disguise_on (⏏ 위 + 가면 떨어짐, 기울어짐)
    //     contentDescription: actionUnmask ("위장 복구")
    //     onClick: 복구 확인 다이얼로그 → 확인 시 DecoyManager.setActive(ctx, DEFAULT)
    //   상태 변경 시 currentDecoy state 업데이트 → drawable + onClick 자동 토글.
    var currentDecoy by remember { mutableStateOf(EjectPrefs.loadDecoy(ctx)) }
    val isDisguised = currentDecoy != DecoyManager.Decoy.DEFAULT
    var showUnmaskDialog by remember { mutableStateOf(false) }
    var showDisguisePicker by remember { mutableStateOf(false) }

    // ── 위장 복구 확인 다이얼로그 ───────────────────────────────────────────────
    if (showUnmaskDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUnmaskDialog = false },
            title = { Text(strings.unmaskConfirmTitle, fontWeight = FontWeight.Bold) },
            text  = { Text(strings.unmaskConfirmBody) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        DecoyManager.setActive(ctx, DecoyManager.Decoy.DEFAULT)
                        currentDecoy = DecoyManager.Decoy.DEFAULT
                        showUnmaskDialog = false
                    },
                ) {
                    Text(
                        strings.unmaskConfirmCta,
                        color      = EjectCoral,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showUnmaskDialog = false }) {
                    Text(strings.dialogCancel, color = EjectSecondary)
                }
            },
            containerColor = EjectSurface,
        )
    }

    // ── 위장 picker 다이얼로그 (일반 → 위장 활성화) ──────────────────────────────
    if (showDisguisePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDisguisePicker = false },
            title = { Text(strings.settingsDecoy, fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    Text(
                        strings.settingsDecoyDesc,
                        fontSize = 13.sp,
                        color    = EjectSecondary,
                    )
                    Spacer(Modifier.height(16.dp))
                    val options = listOf(
                        DecoyManager.Decoy.CALCULATOR to strings.decoyLabelCalculator,
                        DecoyManager.Decoy.MEMO       to strings.decoyLabelMemo,
                        DecoyManager.Decoy.WEATHER    to strings.decoyLabelWeather,
                        DecoyManager.Decoy.CLOCK      to strings.decoyLabelClock,
                    )
                    options.forEach { (decoy, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    DecoyManager.setActive(ctx, decoy)
                                    currentDecoy = decoy
                                    showDisguisePicker = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // v1.5.20 — 🎭 placeholder emoji prefix → 사용자가 v1.5.16에 추가한
                            // 신규 decoy launcher 아이콘 (Calculator/Memo/Weather/Clock) 노출.
                            // picker dialog 안에서 옵션이 실제 어떻게 보이는지 시각 매핑.
                            val iconRes = when (decoy) {
                                DecoyManager.Decoy.CALCULATOR -> com.ejectbutton.R.mipmap.ic_decoy_calculator_foreground
                                DecoyManager.Decoy.MEMO       -> com.ejectbutton.R.mipmap.ic_decoy_memo_foreground
                                DecoyManager.Decoy.WEATHER    -> com.ejectbutton.R.mipmap.ic_decoy_weather_foreground
                                DecoyManager.Decoy.CLOCK      -> com.ejectbutton.R.mipmap.ic_decoy_clock_foreground
                                else                          -> com.ejectbutton.R.drawable.ic_eject_button
                            }
                            Image(
                                painter = painterResource(iconRes),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showDisguisePicker = false }) {
                    Text(strings.dialogCancel, color = EjectSecondary)
                }
            },
            containerColor = EjectSurface,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // v1.5.15 — 레거시 ⏏ 글리프 (Text "⏏") 를 v1.5.13 EmergencyRed 픽토그램
            // (ic_eject_button) 으로 교체. EJECT 메인 버튼과 시각적으로 일치시켜
            // 브랜드 정체성 강화. 28dp 사이즈로 brandLabel 옆 leading icon.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_eject_button),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    // v1.5.2 — strings.appBrandLabel 사용. 7개 언어 자동 분기 (한국어 = "비상 탈출" 등).
                    text          = strings.appBrandLabel,
                    fontSize      = 20.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = EjectCoral,
                    letterSpacing = 0.5.sp,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text     = strings.catchphrase,
                fontSize = 12.sp,
                color    = EjectSecondary,
            )
        }
        // v1.5.12 — 위장 토글 IconButton. 항상 노출, drawable 만 상태에 따라 변경.
        IconButton(
            onClick  = {
                if (isDisguised) showUnmaskDialog = true
                else             showDisguisePicker = true
            },
            modifier = Modifier
                .size(40.dp)
                .onGloballyPositioned { coords ->
                    onDisguiseBoundsChanged?.invoke(coords.boundsInRoot())
                },
        ) {
            Icon(
                painter = painterResource(
                    id = if (isDisguised) R.drawable.ic_disguise_on
                         else              R.drawable.ic_disguise_off,
                ),
                contentDescription = if (isDisguised) strings.actionUnmask
                                     else              strings.actionDisguiseOn,
                // tint Unspecified → vector drawable 의 hard-coded fillColor (deep red + ink) 그대로 노출.
                // 메인 EJECT 버튼 톤과 동일해 시그니처 역할.
                tint               = androidx.compose.ui.graphics.Color.Unspecified,
                modifier           = Modifier.size(24.dp),
            )
        }
        // v1.1.5 — SETTINGS 탭에서는 톱니바퀴를 숨김 (이미 SETTINGS 안이라 의미 X).
        // COMMAND/HISTORY 탭에서만 톱니바퀴를 보여 SETTINGS 로 점프 가능하게.
        if (showSettingsIcon) {
            IconButton(
                onClick = onSettingsTap,
                modifier = Modifier
                    .size(40.dp)
                    .onGloballyPositioned { coords ->
                        onSettingsBoundsChanged?.invoke(coords.boundsInRoot())
                    },
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = strings.settingsTitle,
                    tint               = EjectOnSurface,
                    modifier           = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun SideButtonModeCard(
    current: SideButtonCommand,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clarityMask()
            .clip(RoundedCornerShape(18.dp))
            .background(EjectSurface)
            .border(1.dp, EjectOutlineVar, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EjectSurfaceMid),
            contentAlignment = Alignment.Center,
        ) {
            Text("🎚", fontSize = 18.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                current.label(strings),
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                color      = if (current.isEnabled) EjectCoral else EjectOnSurface,
            )
            Text(
                strings.settingSideButtonDesc,
                fontSize = 11.sp,
                color    = EjectSecondary,
            )
        }
        Text("›", fontSize = 22.sp, color = EjectSecondary, fontWeight = FontWeight.Bold)
    }
}

// ─── HISTORY 탭 ──────────────────────────────────────────────────────────────

@Composable
private fun HistoryContent(
    history: List<String>,
    onSettingsTap: () -> Unit,
) {
    val strings = LocalAppStrings.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(14.dp))
        StitchTopBar(onSettingsTap = onSettingsTap)
        Spacer(Modifier.height(24.dp))
        Text(
            text          = strings.historyTitle.uppercase(),
            fontSize      = 22.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = EjectOnSurface,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = strings.historySubtitle,
            fontSize   = 12.sp,
            color      = EjectSecondary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(24.dp))

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("⏳", fontSize = 36.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        strings.historyEmpty,
                        fontSize      = 12.sp,
                        color         = EjectSecondary,
                        fontWeight    = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { entry ->
                    HistoryEntryCard(entry)
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: String) {
    // Parse via the unit-tested helper in ui.util.HistoryEntryParser — the
    // surrogate-pair emoji split lives there so we can regression-test it.
    val parsed = com.ejectbutton.ui.util.parseHistoryEntry(entry)
    val timestamp  = parsed.timestamp
    val emoji      = parsed.emoji
    val name       = parsed.name
    val trigger    = parsed.trigger
    // Fall back to the raw caller segment only when parser returned no name.
    val callerPart = if (name.isEmpty()) "$emoji".trim() else name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(EjectSurface)
            .border(1.dp, EjectOutlineVar.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 이모지 아바타 원
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(EjectSurfaceMid),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = name.ifEmpty { callerPart },
                fontSize   = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = EjectOnSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text       = timestamp,
                fontSize   = 11.sp,
                color      = EjectSecondary,
                fontWeight = FontWeight.Medium,
            )
        }
        // 트리거 배지
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(EjectSurfaceMid)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text          = trigger.uppercase(),
                fontSize      = 9.sp,
                color         = EjectOnSurface,
                fontWeight    = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            )
        }
    }
}

// ─── SYSTEMS 탭 ──────────────────────────────────────────────────────────────

@Composable
private fun SystemsContent(
    isPremium: Boolean,
    premiumPrice: String?,
    // v1.3.0 — INAPP 광고 제거 일회성
    isAdsRemoved: Boolean = false,
    removeAdsPrice: String? = null,
    onPurchaseRemoveAds: () -> Unit = {},
    currentLanguage: AppLanguage,
    themeMode: com.ejectbutton.data.ThemeMode,
    onUpgradePremium: () -> Unit,
    onClearHistory: () -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onThemeModeChange: (com.ejectbutton.data.ThemeMode) -> Unit,
    onRestorePresets: () -> Unit,
    // v1.5.12 — 사용 설명서 (코치마크 다시 보기) 콜백.
    onShowGuide: () -> Unit = {},
) {
    // v1.1.4 — SETTINGS 탭. 별도 SettingsScreen 진입 제거.
    // SettingsScreen 본문 전체 (Language / Theme / Notifications / SideButton /
    // Tutorial / RestorePresets / About) 를 SettingsBodyInline 으로 인라인 표시.
    // Premium card + Quick Actions(Wipe Mission Log) + Version 카드는 그대로 유지.
    val strings = LocalAppStrings.current
    val ctx = LocalContext.current
    // v1.2.0 — 위장 아이콘 다이얼로그 상태.
    var showDecoyDialog by remember { mutableStateOf(false) }
    var currentDecoy by remember { mutableStateOf(EjectPrefs.loadDecoy(ctx)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        // 톱니 아이콘은 이 화면 자체가 SETTINGS 이므로 의미 없음 → no-op.
        // v1.1.5 — SETTINGS 탭 자체에서는 톱니바퀴 숨김 (이미 여기 있는데 의미 X).
        StitchTopBar(onSettingsTap = {}, showSettingsIcon = false)
        Spacer(Modifier.height(24.dp))
        Text(
            text          = strings.systemsTitle.uppercase(),
            fontSize      = 22.sp,
            fontWeight    = FontWeight.ExtraBold,
            color         = EjectOnSurface,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text       = strings.systemsSubtitle,
            fontSize   = 12.sp,
            color      = EjectSecondary,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.height(24.dp))

        // ── Premium Card (무료 사용자만) ────────────────────────────────────
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                EjectPrimaryContainer,
                                EjectPrimaryContainer.copy(alpha = 0.92f),
                            )
                        )
                    )
                    .padding(24.dp),
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EjectCoral)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text(
                            // v1.5.5 — strings.premiumBadge ("MAYDAY" 등) 사용
                            strings.premiumBadge,
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            color         = Color.White,
                            letterSpacing = 2.sp,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        // v1.5.5 — strings.premiumTitle (한국어 = "탈출 Mayday" 등)
                        strings.premiumTitle,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        strings.premiumSubtitle,
                        fontSize = 14.sp,
                        color    = EjectOnPrimaryContainer,
                    )
                    // v1.5.9 — 프리미엄 혜택 리스트.
                    // v1.6.2 — premiumFeature3 (통화중 대사 힌트 / In-call briefing
                    //   cues) 제거. 해당 기능 미지원 상태인데 결제 페이지에서
                    //   "잠금 해제됨" 으로 표시되어 사용자에게 거짓 약속 소지.
                    //   premiumFeature1/2 (광고 제거 / 무제한 호출 대상) 만 노출.
                    Spacer(Modifier.height(16.dp))
                    listOf(
                        strings.premiumFeature1,
                        strings.premiumFeature2,
                    ).forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Text(
                                "✓",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EjectCoral,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                feature,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick  = onUpgradePremium,
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = EjectCoral,
                            contentColor   = Color.White,
                        ),
                        shape    = RoundedCornerShape(50),
                    ) {
                        val displayPrice = premiumPrice ?: localizedFallbackPrice()
                        Text(
                            "${strings.premiumBuyBtn}  ·  $displayPrice",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        } else {
            // v1.5.12 — 프리미엄 사용자 카드 (구독 중).
            // 이전엔 단순 ⭐ + "MAYDAY" 텍스트 한 줄 배지였음. 사용자 요청으로 풍부한
            // "구독 중 카드" 로 변경 — features 활성 표시 + Play 스토어 구독 관리 진입점.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                EjectPrimaryContainer,
                                EjectPrimaryContainer.copy(alpha = 0.92f),
                            )
                        )
                    )
                    .padding(24.dp),
            ) {
                Column {
                    // 활성 배지 — 코랄 배경 + ✓ 아이콘
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(EjectCoral)
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            strings.premiumActiveBadge,
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            color         = Color.White,
                            letterSpacing = 2.sp,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        strings.premiumActiveTitle,
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = Color.White,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        strings.premiumActiveSubtitle,
                        fontSize = 14.sp,
                        color    = EjectOnPrimaryContainer,
                    )
                    // 활성 features 리스트 (premium 카드와 동일 키 재사용, ✓ 아이콘으로 활성 강조)
                    // v1.6.2 — premiumFeature3 제거 (위 카드와 동일 이유).
                    Spacer(Modifier.height(16.dp))
                    listOf(
                        strings.premiumFeature1,
                        strings.premiumFeature2,
                    ).forEach { feature ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp),
                        ) {
                            Text(
                                "✓",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EjectCoral,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                feature,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    // 구독 관리 버튼 — Play 스토어 Subscriptions 페이지로 deep link.
                    //   url 패턴: https://play.google.com/store/account/subscriptions?sku=<sku>&package=<pkg>
                    //   sku 가 없어도 일반 subscriptions 페이지로 fallback 가능.
                    OutlinedButton(
                        onClick  = {
                            val url = "https://play.google.com/store/account/subscriptions?package=${ctx.packageName}"
                            runCatching {
                                ctx.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(url),
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                        ),
                        border   = androidx.compose.foundation.BorderStroke(1.dp, EjectCoral),
                        shape    = RoundedCornerShape(50),
                    ) {
                        Text(
                            strings.premiumManageBtn,
                            fontWeight = FontWeight.Bold,
                            fontSize   = 14.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // v1.1.4 — SettingsScreen 본문 inline. (Language / Theme / Notifications /
        // SideButton / Tutorial / RestorePresets / About 7개 섹션)
        SettingsBodyInline(
            currentLanguage   = currentLanguage,
            themeMode         = themeMode,
            onThemeModeChange = onThemeModeChange,
            onLanguageChange  = onLanguageChange,
            onRestorePresets  = onRestorePresets,
        )

        // 빠른 작업: 히스토리 정리 + 친구 추천.
        // v1.2.0 — "친구에게 추천" 행 추가. 사용자가 EJECT 한 번 성공한 후 settings
        // 들어와서 share 가능. ACTION_SEND intent → Play Store URL 자동 포함.
        // viral loop 의 핵심 — 회식 탈출 use case 는 단톡방에 자연스럽게 share 됨.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(EjectSurfaceLow)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SystemsRow(icon = "🗑", label = strings.settingsClearHistory, onClick = onClearHistory)
            // v1.5.9 — "광고 제거" 일회성 결제 버튼 제거. 광고 제거는 프리미엄 업그레이드
            // 카드의 혜택(premiumFeature1)으로 통합 — 사용자가 두 결제 옵션 중 선택해야
            // 하던 혼란 제거. 이미 광고 제거 결제한 사용자는 isAdsRemoved=true 가 그대로
            // 유지되어 광고 비활성 상태로 계속 사용 가능 (BillingManager 결제 path 유지).
            if (!isPremium && isAdsRemoved) {
                SystemsRow(icon = "✅", label = strings.settingsRemoveAdsActive, onClick = {})
            }
            // v1.2.0 — 위장 아이콘 토글. 다이얼로그에서 5개 옵션 중 선택.
            // v1.5.22 — Settings row prefix 가 placeholder 🎭 였던 것을 현재 활성화된
            // decoy 아이콘 (또는 DEFAULT 시 메인 EmergencyRed) 으로 교체. 사용자가
            // "위장 = 가면" 이라는 추상적 메타포 대신 "지금 내가 어떤 위장으로 설정돼
            // 있나"를 한 눈에 알 수 있도록. picker dialog 안의 아이콘 + top-bar 의
            // 위장 IconButton 과 시각 시그니처 통일.
            val decoyRowIconRes = when (currentDecoy) {
                DecoyManager.Decoy.CALCULATOR -> R.mipmap.ic_decoy_calculator_foreground
                DecoyManager.Decoy.MEMO       -> R.mipmap.ic_decoy_memo_foreground
                DecoyManager.Decoy.WEATHER    -> R.mipmap.ic_decoy_weather_foreground
                DecoyManager.Decoy.CLOCK      -> R.mipmap.ic_decoy_clock_foreground
                else                          -> R.drawable.ic_disguise_off
            }
            SystemsRow(iconRes = decoyRowIconRes, label = strings.settingsDecoy, onClick = {
                currentDecoy = EjectPrefs.loadDecoy(ctx)
                showDecoyDialog = true
            })
            // v1.5.12 — 사용 설명서 (코치마크 다시 보기). 코치마크 step 4 desc 가 안내하는 메뉴.
            SystemsRow(icon = "📖", label = strings.settingsManual, onClick = onShowGuide)
            SystemsRow(icon = "💬", label = strings.settingsShare, onClick = {
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(
                        android.content.Intent.EXTRA_TEXT,
                        strings.shareMessageBody
                    )
                }
                runCatching {
                    ctx.startActivity(
                        android.content.Intent.createChooser(shareIntent, strings.settingsShare)
                    )
                }
            })
        }

        // v1.2.0 — 위장 아이콘 선택 다이얼로그.
        // 5 개 옵션 (Eject Button / 계산기 / 메모장 / 날씨 / 시계) 라디오 선택.
        // 선택 즉시 DecoyManager.setActive() 가 PackageManager 토글 + EjectPrefs 저장.
        if (showDecoyDialog) {
            AlertDialog(
                onDismissRequest = { showDecoyDialog = false },
                title = {
                    Text(strings.settingsDecoy, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column {
                        Text(
                            strings.settingsDecoyDesc,
                            fontSize = 13.sp,
                            color = EjectSecondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        val options = listOf(
                            DecoyManager.Decoy.DEFAULT to strings.settingsDecoyDefault,
                            DecoyManager.Decoy.CALCULATOR to strings.decoyLabelCalculator,
                            DecoyManager.Decoy.MEMO to strings.decoyLabelMemo,
                            DecoyManager.Decoy.WEATHER to strings.decoyLabelWeather,
                            DecoyManager.Decoy.CLOCK to strings.decoyLabelClock,
                        )
                        options.forEach { (decoy, label) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        currentDecoy = decoy
                                        DecoyManager.setActive(ctx, decoy)
                                        showDecoyDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                RadioButton(
                                    selected = currentDecoy == decoy,
                                    onClick = null,
                                )
                                Spacer(Modifier.width(8.dp))
                                // v1.5.20 — Settings 의 위장 picker 다이얼로그도 첫 번째 picker
                                // (StitchTopBar) 와 시각 일관성: 옵션 옆에 실제 decoy 아이콘 노출.
                                // DEFAULT 옵션은 메인 앱 아이콘 (ic_eject_button) 으로.
                                val iconRes = when (decoy) {
                                    DecoyManager.Decoy.CALCULATOR -> com.ejectbutton.R.mipmap.ic_decoy_calculator_foreground
                                    DecoyManager.Decoy.MEMO       -> com.ejectbutton.R.mipmap.ic_decoy_memo_foreground
                                    DecoyManager.Decoy.WEATHER    -> com.ejectbutton.R.mipmap.ic_decoy_weather_foreground
                                    DecoyManager.Decoy.CLOCK      -> com.ejectbutton.R.mipmap.ic_decoy_clock_foreground
                                    else                          -> com.ejectbutton.R.drawable.ic_eject_button
                                }
                                Image(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(label, fontSize = 15.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDecoyDialog = false }) {
                        Text(strings.dialogCancel)
                    }
                },
                containerColor = EjectSurface,
            )
        }

        Spacer(Modifier.height(16.dp))

        // 앱 정보 카드
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(EjectSurface)
                .border(1.dp, EjectOutlineVar.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(EjectCoral),
                contentAlignment = Alignment.Center,
            ) {
                Text("⏏", fontSize = 22.sp, color = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    // v1.5.2 — 앱 정보 카드도 다국어 brand label 사용
                    text       = strings.appBrandLabel,
                    fontSize   = 14.sp,
                    color      = EjectOnSurface,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    // v1.5.12 — BuildConfig.VERSION_NAME 으로 동적 표시.
                    //   이전: strings.settingsVersion 하드코딩 ("v1.0 · ...") → 실제 v1.5.x 와 mismatch.
                    //   이제: CI 가 -PversionNameOverride 로 주입한 실제 버전이 자동 반영.
                    text       = "v" + BuildConfig.VERSION_NAME + "  ·  " + strings.catchphrase,
                    fontSize   = 11.sp,
                    color      = EjectSecondary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun SystemsRow(icon: String, label: String, onClick: () -> Unit) {
    SystemsRowScaffold(label = label, onClick = onClick) {
        Text(icon, fontSize = 18.sp)
    }
}

/**
 * v1.5.22 — SystemsRow overload with a drawable resource prefix instead of
 * an emoji string. Used by the 위장 (decoy) row in the Settings tab so the
 * preview reflects the currently-active disguise icon (계산기/메모/날씨/시계
 * 4종 또는 기본 EmergencyRed 픽토그램) — same visual signature as the picker
 * dialog opened from this row and the top-bar disguise IconButton.
 *
 * Why an overload, not a nullable parameter:
 *   – Existing 8 call sites pass `icon = "<emoji>"` and we don't want to
 *     touch any of them.
 *   – Keeps the call site type-safe: `SystemsRow(iconRes = R.drawable.x, …)`
 *     vs `SystemsRow(icon = "🎭", …)` are visually distinct in source.
 */
@Composable
private fun SystemsRow(iconRes: Int, label: String, onClick: () -> Unit) {
    SystemsRowScaffold(label = label, onClick = onClick) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun SystemsRowScaffold(
    label: String,
    onClick: () -> Unit,
    iconSlot: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(EjectSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(EjectSurfaceMid),
            contentAlignment = Alignment.Center,
        ) {
            iconSlot()
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text       = label,
            fontSize   = 14.sp,
            color      = EjectOnSurface,
            fontWeight = FontWeight.SemiBold,
            modifier   = Modifier.weight(1f),
        )
        Text("›", fontSize = 22.sp, color = EjectSecondary, fontWeight = FontWeight.Bold)
    }
}

// v1.1.4 — InlineLanguageRow / InlineThemeCard 는 SettingsBodyInline (SettingsScreen.kt)
// 안에 동일 디자인이 들어갔으므로 제거됨.

// ─── 공통 컴포넌트 ────────────────────────────────────────────────────────────

@Composable
private fun EjectButton(
    isCancelMode: Boolean = false,
    onClick: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s",
    )

    // v1.5.13 — EJECT 버튼 비주얼: 런처 아이콘과 통일된 비상구 픽토그램 비트맵 사용.
    // 일반 모드(EJECT): 라운드 스퀘어 비트맵 이미지 (red bg + 흰 문 + 빨간 figure).
    // 취소 모드(CANCEL): 기존 빨간 원 + ✕ UI 유지 (취소는 단순 정지 메타포).
    //
    // v1.6.1 — 와이프 피드백: 출동 화면의 모든 콘텐츠가 스크롤 없이 한 화면에
    //   보이도록 EJECT 버튼을 기존 대비 80% 로 축소. 260/252/232dp 의 outer/halo/
    //   inner triplet → 208/202/186dp. Cancel ✕ Text fontSize 72sp → 58sp,
    //   letterSpacing 4sp → 3sp 도 동일 비율 축소. ContentScale.Fit 라서 픽토
    //   그램 자체는 새 사이즈에 맞게 자동으로 다운스케일됨.
    Box(
        modifier = Modifier
            .size(208.dp)
            .scale(scale),
        contentAlignment = Alignment.Center,
    ) {
        if (isCancelMode) {
            // Cancel 모드: 기존 빨간 원 ✕
            Box(
                modifier = Modifier
                    .size(202.dp)
                    .clip(CircleShape)
                    .background(EjectCoral.copy(alpha = 0.10f))
            )
            Box(
                modifier = Modifier
                    .size(186.dp)
                    .shadow(
                        elevation    = 14.dp,
                        shape        = CircleShape,
                        ambientColor = EjectCoral.copy(alpha = 0.35f),
                        spotColor    = EjectCoral.copy(alpha = 0.35f),
                    )
                    .clip(CircleShape)
                    .background(EjectCoral)
                    .border(3.dp, EjectCoral, CircleShape)
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "✕",
                        fontSize   = 58.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        strings.dialogCancel.uppercase(Locale.getDefault()),
                        fontSize      = 11.sp,
                        color         = Color.White,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 3.sp,
                    )
                }
            }
        } else {
            // EJECT 모드: 신규 비상구 픽토그램 비트맵 (런처 아이콘과 통일).
            // 라운드 스퀘어 형태로 사용자가 앱 아이콘과 즉시 연관 인식하도록.
            val haloShape = androidx.compose.foundation.shape.RoundedCornerShape(40.dp)
            Box(
                modifier = Modifier
                    .size(202.dp)
                    .clip(haloShape)
                    .background(EjectCoral.copy(alpha = 0.10f))
            )
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    id = com.ejectbutton.R.drawable.ic_eject_button
                ),
                contentDescription = strings.ejectButtonLabel,
                modifier = Modifier
                    .size(186.dp)
                    .shadow(
                        elevation    = 14.dp,
                        shape        = haloShape,
                        ambientColor = EjectCoral.copy(alpha = 0.35f),
                        spotColor    = EjectCoral.copy(alpha = 0.35f),
                    )
                    .clip(haloShape)
                    .clickable(onClick = onClick),
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            )
        }
    }
}

@Composable
private fun CallerChips(
    callers: List<Scenario>,
    selected: Scenario,
    customIds: Set<String>,
    onSelect: (Scenario) -> Unit,
    onDelete: (Scenario) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,  // v1.5.6 — 코치마크 register 용
) {
    val strings = LocalAppStrings.current

    // v1.5.7 round 2 — LazyRow -> Row + horizontalScroll.
    // Hypothesis: LazyRow lazy measure -> first layout pass register Box height stale ->
    //   subsequent register Boxes' boundsInRoot cached one step too high -> ring off-by-one.
    // Row + horizontalScroll = eager measure -> accurate height from first pass.
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        callers.forEach { caller ->
            val isSelected = caller.id == selected.id
            val isCustom   = caller.id in customIds

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) EjectCoral else EjectSurface)
                    .then(
                        if (!isSelected)
                            Modifier.border(1.dp, EjectOutlineVar, RoundedCornerShape(50))
                        else Modifier
                    )
                    .clickable { onSelect(caller) }
                    .padding(start = 16.dp, end = if (isCustom) 8.dp else 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text       = caller.emoji + "  " + caller.name,
                    fontSize   = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color      = if (isSelected) Color.White else EjectOnSurface,
                )
                if (isCustom) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector        = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint               = if (isSelected) Color.White.copy(alpha = 0.6f) else EjectOutlineVar,
                        modifier           = Modifier.size(15.dp).clickable { onDelete(caller) },
                    )
                }
            }
        }

        // + add button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(EjectSurface)
                .border(1.dp, EjectOutlineVar, RoundedCornerShape(50))
                .clickable(onClick = onAdd)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Add, "Add", tint = EjectOnSurface, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.addCallerBtn, fontSize = 13.sp, color = EjectOnSurface, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TriggerTimeRow(
    selected: TimeChoice,
    customDelaySec: Int,
    onSelect: (TimeChoice) -> Unit,
    modifier: Modifier = Modifier,  // v1.5.6 — 코치마크 register 용
) {
    val strings = LocalAppStrings.current
    val items = listOf(
        TimeChoice.IMMEDIATE to strings.triggerNow,
        TimeChoice.AFTER_10S to strings.trigger10s,
        TimeChoice.CUSTOM    to if (selected == TimeChoice.CUSTOM && customDelaySec > 0)
            "${strings.triggerCustom} (${customDelaySec}s)" else strings.triggerCustom,
    )
    TriggerChoiceRow(items = items, selectedKey = selected, onSelect = onSelect, modifier = modifier)
}

@Composable
private fun TriggerModeRow(
    selected: ModeChoice,
    onSelect: (ModeChoice) -> Unit,
    modifier: Modifier = Modifier,  // v1.5.6 — 코치마크 register 용
) {
    val strings = LocalAppStrings.current
    val items = listOf(
        ModeChoice.BUTTON      to strings.triggerModeButton,
        ModeChoice.SHAKE       to strings.triggerShake,
        ModeChoice.SIDE_BUTTON to strings.triggerSideButton,
    )
    TriggerChoiceRow(items = items, selectedKey = selected, onSelect = onSelect, modifier = modifier)
}

@Composable
private fun <T> TriggerChoiceRow(
    items: List<Pair<T, String>>,
    selectedKey: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,  // v1.5.6 — 코치마크 register 용
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (key, label) ->
            val isSel = key == selectedKey
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isSel) EjectCoral else EjectSurfaceMid)
                    .clickable { onSelect(key) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = label,
                    fontSize   = 12.sp,
                    color      = if (isSel) Color.White else EjectOnSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text       = title,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Bold,
            color      = EjectSecondary,
        )
    }
}

/**
 * v1.5.3 — Sliding pill indicator (사용자 피드백 #6).
 *
 * 회귀 (v1.5.0~v1.5.2): `if (isActive) EjectSurfaceMid else Color.Transparent` 가 boolean step
 * 으로 작동해서, 화면 스와이프 / 탭 전환 시 회색 음영이 한 탭에서 사라지면서 다른 탭에 즉시 jump.
 *
 * fix: pill 을 별도 Box 로 추출 → `animateFloatAsState` 로 slide 위치 보간.
 *      text color / fontWeight 도 active fraction 비례해 lerp.
 *      iOS UISegmentedControl / Material3 NavigationBar 톤.
 *
 * 코너 RoundedCornerShape(50), shadow, border 등 기존 디자인 시스템 그대로 유지.
 */
@Composable
private fun BottomBar(
    current: AppScreen,
    isPremium: Boolean,
    onSelect: (AppScreen) -> Unit,
    onPremiumTap: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val tabs = listOf(
        AppScreen.COMMAND to strings.tabCommand,
        AppScreen.HISTORY to strings.tabHistory,
        AppScreen.SYSTEMS to strings.tabSystems,
    )
    val activeIndex = tabs.indexOfFirst { it.first == current }.coerceAtLeast(0)
    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "bottombar-pill-position",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(8.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(EjectSurface.copy(alpha = 0.96f))
            .border(1.dp, EjectOutlineVar.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(vertical = 6.dp, horizontal = 6.dp),
    ) {
        // 슬라이딩 pill — 한 탭에서 다른 탭으로 부드럽게 이동.
        // BoxWithConstraints 의 maxWidth = 6+6dp horizontal padding 빼고 남은 가용 폭 (3 탭 동등 분할).
        // v1.5.4 — fillMaxHeight() 회귀 fix: BoxWithConstraints 가 wrap-content 인 상황에서
        // child 의 fillMaxHeight 가 unbounded 로 측정되어 BottomBar 전체 height 가
        // 화면 거의 전체로 확장됐던 문제. 명시 height(40.dp) 로 안정화 (Row padding 12+12 +
        // text 11sp ≈ 37dp 와 일치, 약간 여유).
        val tabWidth = maxWidth / tabs.size
        val pillHeight = 40.dp
        Box(
            modifier = Modifier
                .offset(x = tabWidth * animatedIndex)
                .width(tabWidth)
                .height(pillHeight)
                .clip(RoundedCornerShape(50))
                .background(EjectSurfaceMid),
        )
        // 텍스트 row — pill 위에 layered. 각 탭의 active fraction 으로 color/weight 보간.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEachIndexed { idx, (screen, label) ->
                val activeFrac = (1f - abs(animatedIndex - idx)).coerceIn(0f, 1f)
                val textColor = lerp(EjectSecondary, EjectOnSurface, activeFrac)
                // FontWeight 보간: SemiBold(600) ↔ ExtraBold(800)
                val weightInt = (600 + (800 - 600) * activeFrac).toInt()
                // v1.5.6 — clickable area 와 sliding pill area 정확히 일치 (호버/클릭 음영 크기 통일).
                // 3 탭 모두 weight(1f) + height(pillHeight) → tabWidth × 40dp 동일.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(pillHeight)
                        .clip(RoundedCornerShape(50))
                        .clickable { onSelect(screen) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text          = label,
                        fontSize      = 11.sp,
                        color         = textColor,
                        fontWeight    = FontWeight(weightInt),
                        letterSpacing = 1.5.sp,
                    )
                }
            }
        }
    }
}

// ─── 다이얼로그 ───────────────────────────────────────────────────────────────

@Composable
private fun CustomDelayDialog(initial: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val strings = LocalAppStrings.current
    var text by remember { mutableStateOf(initial.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.dialogCustomDelay) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) text = it },
                label = { Text(strings.dialogCustomDelayLabel) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull()?.coerceIn(1, 9999) ?: initial) }) {
                Text(strings.dialogConfirm)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.dialogCancel) } },
        containerColor = EjectSurface,
    )
}

// Round 31 — 연락처 항목을 이름+번호 쌍으로 들고 있어야 (1) 리스트에서 "이름 / 번호" 둘 다 보이고
// (2) 선택 시 가짜 수신 화면 callerLabel 에 실제 번호를 박을 수 있다.
// 예전 구현은 이름만 읽고 callerLabel 을 "휴대전화" 리터럴로 채워 버그.
private data class ContactEntry(val name: String, val phone: String)

@Composable
private fun AddCallerDialog(onDismiss: () -> Unit, onConfirm: (Scenario) -> Unit) {
    val strings = LocalAppStrings.current
    val ctx = LocalContext.current
    var callerName    by remember { mutableStateOf("") }
    // Round 31 — 연락처에서 고른 실제 번호. 수동 입력시에는 빈 문자열 → fallback "휴대전화" 유지.
    var selectedPhone by remember { mutableStateOf("") }
    var searchQuery   by remember { mutableStateOf("") }
    var contacts      by remember { mutableStateOf(listOf<ContactEntry>()) }
    var searchGranted by remember {
        mutableStateOf(ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        searchGranted = granted
    }

    // 다이얼로그 열릴 때 권한이 없으면 즉시 요청
    LaunchedEffect(Unit) {
        if (!searchGranted) permLauncher.launch(Manifest.permission.READ_CONTACTS)
    }

    // 권한이 허용되면 즉시 연락처 로드 & 쿼리 변경에 반응
    LaunchedEffect(searchGranted, searchQuery) {
        if (!searchGranted) return@LaunchedEffect
        contacts = withContext(Dispatchers.IO) {
            val result = mutableListOf<ContactEntry>()
            val cursor = ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                ),
                if (searchQuery.isNotBlank()) "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?" else null,
                if (searchQuery.isNotBlank()) arrayOf("%$searchQuery%") else null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
            )
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx  = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val seen = mutableSetOf<String>()
                // Round 14 — 전체 연락처 표시. 과거엔 50개에서 잘라 "스크롤 해도
                // 더 없음" 처럼 보였던 UX 버그를 수정. LazyColumn 이 viewport
                // 바깥 아이템을 지연 렌더링하므로 수천 개도 부드럽게 처리된다.
                //
                // Round 31 — 이름+번호 쌍으로 중복 제거 (같은 이름에 번호 여러 개 있는 케이스 지원).
                while (it.moveToNext()) {
                    val name  = it.getString(nameIdx) ?: continue
                    val phone = it.getString(numIdx)?.trim().orEmpty()
                    val key = "$name|$phone"
                    if (seen.add(key)) result.add(ContactEntry(name, phone))
                }
            }
            result
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.dialogAddCaller, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 발신자 이름 — 돋보기 없음
                OutlinedTextField(
                    value = callerName,
                    onValueChange = { callerName = it },
                    label = { Text(strings.dialogCallerName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().clarityMask(),
                )
                if (searchGranted) {
                    // 연락처 검색
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(strings.dialogSearch) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().clarityMask(),
                    )
                    // 연락처 리스트 — 처음부터 표시. Round 31 — 이름 밑에 번호도 보여서 사용자가
                    // 어느 번호가 가짜 수신 화면에 뜰지 미리 확인 가능.
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(contacts) { entry ->
                            // Round 32 — 주소록 번호를 국가별 하이픈 포맷으로 보여서 사용자가
                            // 어떤 모양이 실제 가짜 수신 화면에 뜰지 미리 확인 가능하게.
                            val formatted = remember(entry.phone) {
                                formatPhoneWithHyphens(entry.phone, Locale.getDefault().country)
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clarityMask()
                                    .clickable {
                                        callerName = entry.name
                                        selectedPhone = formatted
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                            ) {
                                Text(
                                    text = entry.name,
                                    fontSize = 14.sp,
                                    color = EjectOnSurface,
                                )
                                if (formatted.isNotBlank()) {
                                    Text(
                                        text = formatted,
                                        fontSize = 12.sp,
                                        color = EjectSecondary,
                                    )
                                }
                            }
                            HorizontalDivider(color = EjectSurfaceMid)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (callerName.isNotBlank()) {
                    // Round 31 — 사용자가 주소록 항목을 탭해서 `selectedPhone` 이 채워졌다면
                    // 실제 번호를 "폰 010-XXXX-XXXX" 형태로 가짜 수신 화면에 박는다.
                    // 비어 있으면(이름만 직접 입력한 경우) 기존 fallback "휴대전화" 라벨.
                    val label = if (selectedPhone.isNotBlank())
                        "폰 $selectedPhone"
                    else
                        strings.callerMobile
                    onConfirm(Scenario(
                        id           = "custom_${System.currentTimeMillis()}",
                        emoji        = "👤",
                        name         = callerName,
                        callerName   = callerName,
                        callerLabel  = label,
                        preSmsText   = "",
                        prompterHint = "",
                        urgency      = Urgency.NORMAL,
                    ))
                }
            }) { Text(strings.dialogAdd, color = EjectCoral, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.dialogCancel) } },
        containerColor = EjectSurface,
    )
}

// ─── 네이티브 광고 (compact banner) ─────────────────────────────────────────
// AdMob 정책: 네이티브 광고 에셋(headline/icon/body/CTA) 은 반드시
// [NativeAdView] 의 자식으로 배치되고 `setHeadlineView` 등으로 등록된 뒤
// `setNativeAd(ad)` 가 호출돼야 한다. 그래야 impression/click 이 집계되고
// 사용자의 탭이 광고주 페이지로 넘어간다. 이 규칙을 어기면 계정 정지 사유.
//
// 따라서 한 줄 배너이긴 하지만 NativeAdView 안에 LinearLayout (icon +
// headline + AD 뱃지) 를 구성하고 각 View 를 NativeAdView 의 슬롯에 바인드한다.

@SuppressLint("SetTextI18n") // "AD" is required ad disclosure label per Google AdMob policy — must not be localized
@Composable
private fun NativeAdCard(ad: NativeAd, modifier: Modifier = Modifier) {
    val surfaceColor   = EjectSurface.toArgb()
    val onSurfaceColor = EjectOnSurface.toArgb()
    val secondaryColor = EjectSecondary.toArgb()

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            fun dp(value: Int): Int = (value * ctx.resources.displayMetrics.density).toInt()

            val adView = NativeAdView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }

            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(6), dp(10), dp(6))
                // 둥근 모서리 + 배경색을 프로그램매틱하게 그린 GradientDrawable
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(12).toFloat()
                    setColor(surfaceColor)
                }
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
            }

            val iconView = ImageView(ctx).apply {
                // v1.5.19 — AdMob native ad validator 32×32dp 정책 강화.
                //
                // v1.5.17: layoutParams 22dp → 32dp 만 변경 → validator 여전히 violation.
                //
                // 진단:
                //  (1) ScaleType.FIT_CENTER 가 작은 raster icon 을 view 안에 비율 유지
                //      해서 그리니까 rendered content 가 32dp 미만으로 측정됨.
                //  (2) update 람다에서 drawable null 일 때 view.GONE → 0×0 violation.
                //  (3) layoutParams 만으론 measure 시 view 가 줄어들 수 있음.
                //
                // v1.5.19 다중 안전장치:
                //  (a) 48dp 로 상향 (32dp 마진 안에서 보수적으로).
                //  (b) minimumWidth/Height 명시 → measure 시 절대 줄어들지 않음.
                //  (c) ScaleType.CENTER_INSIDE → image 가 view 안에서 비율 유지하며 fit,
                //      view 자체 사이즈는 layoutParams 그대로.
                //  (d) adjustViewBounds = false → image bitmap 따라 view 가 줄어들지 X.
                //  (e) visibility = VISIBLE 유지 (update 람다에서 GONE 안 하고
                //      placeholder drawable 사용).
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48)).apply {
                    marginEnd = dp(10)
                }
                minimumWidth = dp(48)
                minimumHeight = dp(48)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                adjustViewBounds = false
            }

            val headlineView = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1f,
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(onSurfaceColor)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val badgeView = TextView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                ).apply { marginStart = dp(8) }
                text = "AD"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 8f)
                setTextColor(secondaryColor)
                setPadding(dp(5), dp(1), dp(5), dp(1))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = dp(3).toFloat()
                    // 15% alpha 보조색
                    setColor((secondaryColor and 0x00FFFFFF) or 0x26000000)
                }
            }

            row.addView(iconView)
            row.addView(headlineView)
            row.addView(badgeView)
            adView.addView(row)

            adView.iconView     = iconView
            adView.headlineView = headlineView
            adView
        },
        update = { adView ->
            val iconView     = adView.iconView as? ImageView
            val headlineView = adView.headlineView as? TextView

            // v1.5.19 — drawable null 일 때도 view 를 GONE 으로 바꾸지 않는다.
            // GONE 으로 바꾸면 view 사이즈가 0×0 으로 측정되어 AdMob native ad
            // validator 가 "Resolution less than 32x32dp" 경고를 띄운다 (정책상
            // iconView 등록된 view 는 항상 32dp 이상이어야 한다). drawable 이
            // 비어있는 ad 의 경우 앱 아이콘 placeholder 로 보여주고 view 사이즈를
            // 48dp 로 유지한다.
            val iconDrawable = ad.icon?.drawable
            if (iconDrawable != null) {
                iconView?.setImageDrawable(iconDrawable)
            } else {
                iconView?.setImageResource(com.ejectbutton.R.drawable.ic_eject_button)
            }
            iconView?.visibility = android.view.View.VISIBLE
            headlineView?.text = ad.headline ?: ad.body ?: ""

            // impression + click 집계를 위한 필수 호출.
            adView.setNativeAd(ad)
        },
    )
}

// ─── 프리미엄 업그레이드 다이얼로그 ─────────────────────────────────────────
// v1.1.0 — PremiumUpgradeDialog 는 PremiumUpgradeDialog.kt 로 이전.
// 듀얼 플랜 (월/연) ModalBottomSheet 로 교체됨.
// PremiumFeatureRow 는 더 이상 사용되지 않아 제거 (Round 30 까지의 단일 플랜 잔재).

/**
 * Round 18 — 월 구독 근사치 fallback. Google Play Billing 이 아직 제품을
 * 못 받아왔을 때만 쓰인다 (제품 미등록·오프라인·디버그).
 *
 * Round 30 — premiumBuyBtn 은 더 이상 placeholder 를 포함하지 않고 "업그레이드 — 월"
 * 같은 고정 라벨이다. 가격은 여기서 반환한 금액을 UI 레이어에서 별도 점-구분자로 concat.
 */
@Composable
private fun localizedFallbackPrice(): String {
    // v1.6.2 — 구독료 인상 (₩1,900 → ₩3,000) + 14국 PPP 재책정.
    //   Play Console 의 실제 product 가격은 Simon 이 직접 설정해야 하며,
    //   이 fallback 은 BillingClient 가 아직 product 못 받아왔을 때 (제품 미등록·
    //   오프라인·디버그) 만 쓰는 임시 표시. 실제 결제 가격과 100% 일치하도록
    //   Play Console 도 같이 갱신 필요. PPP (Purchasing Power Parity) 기준:
    //   한국 ₩3,000 = US $2.49 ≈ €2.29 ≈ £1.99 ≈ ¥350 (JP) ≈ ¥15 (CN).
    val country = Locale.getDefault().country
    return when (country) {
        "KR" -> "₩3,000"
        "JP" -> "¥350"
        "CN" -> "¥15"
        "TW" -> "NT\$70"
        "HK" -> "HK\$18"
        "IN" -> "₹149"
        "MX" -> "MX\$45"
        "ES" -> "2,29 €"
        "GB" -> "£1.99"
        "DE", "FR", "IT", "NL" -> "2,29 €"
        "BR" -> "R\$10.90"
        "AU" -> "A\$3.49"
        "CA" -> "CA\$3.29"
        "ID" -> "Rp35.000"
        else -> "\$2.49"
    }
}