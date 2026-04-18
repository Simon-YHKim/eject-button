package com.ejectbutton.ui.main

import android.Manifest
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.ejectbutton.ads.AdManager
import com.ejectbutton.data.AppLanguage
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.data.Scenario
import com.ejectbutton.data.TriggerMode
import com.ejectbutton.data.SideButtonCommand
import com.ejectbutton.data.Urgency
import com.ejectbutton.data.defaultScenarios
import com.ejectbutton.service.ButtonWatchService
import com.ejectbutton.service.CountdownBus
import com.ejectbutton.service.FakeCallOverlayService
import com.ejectbutton.service.ShakeDetectionService
import android.provider.Settings as AndroidSettings
import com.ejectbutton.ui.theme.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
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
    onEject: (scenario: Scenario, delayMs: Long) -> Unit,
) {
    val ctx     = LocalContext.current
    val strings = LocalAppStrings.current
    val haptic  = LocalHapticFeedback.current

    var currentScreen    by remember { mutableStateOf(AppScreen.COMMAND) }
    var showSettings     by remember { mutableStateOf(false) }
    var showPremiumSheet by remember { mutableStateOf(false) }

    // 언어에 따라 기본 발신자 이름 로컬화
    val localizedDefaults = remember(strings) {
        defaultScenarios.map { s ->
            when (s.id) {
                "mom" -> s.copy(name = strings.callerMom, callerName = strings.callerMom)
                "dad" -> s.copy(name = strings.callerDad, callerName = strings.callerDad)
                else  -> s
            }
        }
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

    // Round 9 — 모드 선택만으로 자동 arm 하지 않는다.
    // 사용자가 EJECT 를 눌러야 각 모드가 실제로 대기 상태로 들어간다 (onEject 참고).
    // 모드를 전환할 때는 직전 모드의 남아있는 arm 상태만 정리한다.
    DisposableEffect(selectedMode) {
        when (selectedMode) {
            ModeChoice.SHAKE -> {
                // SHAKE 로 전환 시점: 과거 사이드 arm 이 남아있을 수 있으니 해제.
                EjectPrefs.saveSideButtonArmed(ctx, false)
                ButtonWatchService.reconcile(ctx)
            }
            ModeChoice.SIDE_BUTTON -> {
                // SIDE_BUTTON 로 전환 시점: 과거 shake arm 해제.
                ShakeDetectionService.stop(ctx)
            }
            ModeChoice.BUTTON -> {
                // BUTTON 로 전환: 두 백그라운드 모드 모두 해제.
                ShakeDetectionService.stop(ctx)
                EjectPrefs.saveSideButtonArmed(ctx, false)
                ButtonWatchService.reconcile(ctx)
            }
        }
        onDispose { }
    }

    // 사이드 버튼 트리거 명령 — 메인 화면 카드에서도 변경 가능
    var sideButtonCommand by remember {
        mutableStateOf(EjectPrefs.loadSideButtonCommand(ctx))
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
    var customCallers    by remember { mutableStateOf(EjectPrefs.loadScenarios(ctx)) }
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
            // 무료 사용자는 커스텀 발신자 1명까지만
            AlertDialog(
                onDismissRequest = { showAddCaller = false },
                title = { Text(strings.premiumTitle) },
                text = { Text(strings.premiumMaxCallersMsg, color = EjectSecondary) },
                confirmButton = {
                    TextButton(onClick = { showAddCaller = false; showPremiumSheet = true }) {
                        Text(strings.premiumBadge, color = EjectCoral, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCaller = false }) {
                        Text(strings.dialogCancel)
                    }
                },
                containerColor = EjectSurface,
            )
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
            showPremiumSheet         -> showPremiumSheet = false
            showAddCaller            -> showAddCaller = false
            showCustomDialog         -> showCustomDialog = false
            showSideButtonPicker     -> showSideButtonPicker = false
            showSettings             -> showSettings = false
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

    // 설정 화면
    if (showSettings) {
        SettingsScreen(
            currentLanguage   = currentLanguage,
            isPremium         = isPremium,
            themeMode         = themeMode,
            onThemeModeChange = onThemeModeChange,
            onLanguageChange  = onLanguageChange,
            onPurchasePremium = onPurchasePremium,
            onRestorePurchase = onRestorePurchase,
            premiumPrice      = premiumPrice,
            onDismiss         = {
                showSettings = false
                // 설정에서 사이드 버튼 트리거를 켜고/끈 결과 반영
                sideButtonCommand = EjectPrefs.loadSideButtonCommand(ctx)
            },
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EjectBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            // Round 9 — COMMAND ↔ HISTORY ↔ SYSTEMS 탭을 좌/우 스와이프로 이동.
            // 임계값 ±18f: 가볍게 밀기만 해도 전환되도록.
            .pointerInput(Unit) {
                detectHorizontalDragGestures { _, dragAmount ->
                    val screens = AppScreen.entries
                    val cur = screens.indexOf(currentScreen)
                    if (dragAmount < -18f && cur < screens.size - 1) {
                        currentScreen = screens[cur + 1]
                    } else if (dragAmount > 18f && cur > 0) {
                        currentScreen = screens[cur - 1]
                    }
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
                    allCallers       = localizedDefaults + customCallers,
                    customCallerIds  = customCallers.map { it.id }.toSet(),
                    countdown        = countdown,
                    sideButtonCommand = sideButtonCommand,
                    sideButtonStandby = sideButtonStandby,
                    shakeStandby      = shakeStandby,
                    sideButtonNotice  = sideButtonNotice,
                    onDismissNotice   = { sideButtonNotice = false },
                    onOpenSettingsForSideButton = {
                        sideButtonNotice = false
                        showSettings = true
                    },
                    onOpenSideButtonPicker = { showSideButtonPicker = true },
                    onSelectCaller   = { selectedScenario = it },
                    onDeleteCaller   = { toDelete ->
                        val updated = customCallers.filter { it.id != toDelete.id }
                        customCallers = updated
                        EjectPrefs.saveScenarios(ctx, updated)
                        if (selectedScenario.id == toDelete.id) selectedScenario = localizedDefaults[0]
                    },
                    onSelectTime     = { time ->
                        selectedTime = time
                        if (time == TimeChoice.CUSTOM) showCustomDialog = true
                    },
                    onSelectMode     = { mode ->
                        selectedMode = mode
                    },
                    onAddCaller      = { showAddCaller = true },
                    onSettingsTap    = { showSettings = true },
                    onEject          = handleEject@{
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
                            ShakeDetectionService.start(
                                ctx,
                                selectedScenario.callerName,
                                selectedScenario.callerLabel,
                                selectedScenario.prompterHint,
                                shakeDelayMs,
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
                        onEject(selectedScenario, delayMs)
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
                )
                AppScreen.HISTORY -> HistoryContent(
                    history = history,
                    onSettingsTap = { showSettings = true },
                )
                AppScreen.SYSTEMS -> SystemsContent(
                    isPremium         = isPremium,
                    premiumPrice      = premiumPrice,
                    onUpgradePremium  = { showPremiumSheet = true },
                    onClearHistory    = {
                        EjectPrefs.clearHistory(ctx)
                        history = emptyList()
                        showHistoryClearedDialog = true
                    },
                    onSettingsTap     = { showSettings = true },
                )
            }
            }
        }

        // 하단 고정 영역: 광고 + 탭바.
        // EjectBg 불투명 배경으로 스크롤 컨텐츠가 비쳐 보이지 않도록 한다.
        // (과거엔 배경이 투명해 SideButtonModeCard 같은 하단 콘텐츠가
        //  '애매하게' 가려 보였던 문제를 해결)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(EjectBg),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 네이티브 광고 (무료 사용자만)
            if (!isPremium) {
                val nativeAd by AdManager.nativeAd.collectAsState()
                nativeAd?.let { ad ->
                    NativeAdCard(ad = ad, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                }
                Spacer(Modifier.height(8.dp))
            }

            BottomBar(
                current = currentScreen,
                isPremium = isPremium,
                onSelect = { currentScreen = it },
                onPremiumTap = { showPremiumSheet = true },
            )
            Spacer(Modifier.height(12.dp))
        }
    }
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
        Spacer(Modifier.height(14.dp))
        StitchTopBar(onSettingsTap = onSettingsTap)

        Spacer(Modifier.height(24.dp))

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
                    .background(EjectCoral.copy(alpha = 0.1f))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    String.format(strings.countdownFmt, countdown),
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
                    .background(EjectCoral.copy(alpha = 0.1f))
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
                    .background(EjectCoral.copy(alpha = 0.1f))
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
        EjectButton(
            isCancelMode = isCancelMode,
            onClick = { if (isCancelMode) onCancel() else onEject() },
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text          = strings.noEscapeLabel,
            fontSize      = 13.sp,
            color         = EjectSecondary,
            fontWeight    = FontWeight.Medium,
        )

        Spacer(Modifier.height(24.dp))

        // 발신자 섹션
        SectionHeader(strings.sectionCaller)
        Spacer(Modifier.height(12.dp))
        CallerChips(
            callers   = allCallers,
            selected  = selectedScenario,
            customIds = customCallerIds,
            onSelect  = onSelectCaller,
            onDelete  = onDeleteCaller,
            onAdd     = onAddCaller,
        )

        Spacer(Modifier.height(24.dp))

        // 트리거 시간 행 (즉시/10초/커스텀)
        SectionHeader(strings.sectionDelay)
        Spacer(Modifier.height(12.dp))
        TriggerTimeRow(
            selected       = selectedTime,
            customDelaySec = customDelaySec,
            onSelect       = onSelectTime,
        )

        Spacer(Modifier.height(24.dp))

        // 트리거 모드 행 (버튼/흔들기/사이드)
        SectionHeader(strings.sectionTriggerMode)
        Spacer(Modifier.height(12.dp))
        TriggerModeRow(
            selected = selectedMode,
            onSelect = onSelectMode,
        )

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

@Composable
private fun StitchTopBar(onSettingsTap: () -> Unit) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text          = "⏏ EJECT BUTTON",
                fontSize      = 20.sp,
                fontWeight    = FontWeight.ExtraBold,
                color         = EjectCoral,
                letterSpacing = 0.5.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text     = strings.catchphrase,
                fontSize = 12.sp,
                color    = EjectSecondary,
            )
        }
        IconButton(onClick = onSettingsTap, modifier = Modifier.size(40.dp)) {
            Icon(
                Icons.Default.Settings,
                contentDescription = strings.settingsTitle,
                tint               = EjectOnSurface,
                modifier           = Modifier.size(24.dp),
            )
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
    // 엔트리 형식: "MM/dd HH:mm · 👤Name · trigger"
    val parts = entry.split(" · ")
    val timestamp  = parts.getOrNull(0) ?: ""
    val callerPart = parts.getOrNull(1) ?: ""
    val trigger    = parts.getOrNull(2) ?: ""

    // 이모지와 이름 분리
    val emoji = callerPart.firstOrNull()?.toString() ?: "👤"
    val name  = if (callerPart.length > 1) callerPart.substring(1).trim() else ""

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
    onUpgradePremium: () -> Unit,
    onClearHistory: () -> Unit,
    onSettingsTap: () -> Unit,
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(14.dp))
        StitchTopBar(onSettingsTap = onSettingsTap)
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
                            "ELITE TIER",
                            fontSize      = 10.sp,
                            fontWeight    = FontWeight.ExtraBold,
                            color         = Color.White,
                            letterSpacing = 2.sp,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Eject Mayday",
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
            // 프리미엄 사용자 배지
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(EjectPrimaryContainer)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint               = EjectCoral,
                    modifier           = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    strings.premiumBadge,
                    fontSize      = 13.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    color         = Color.White,
                    letterSpacing = 2.sp,
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // 그룹 1: 빠른 작업
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(EjectSurfaceLow)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SystemsRow(icon = "⚙", label = strings.settingsTitle, onClick = onSettingsTap)
            SystemsRow(icon = "🗑", label = strings.settingsClearHistory, onClick = onClearHistory)
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
                    text       = "EJECT BUTTON",
                    fontSize   = 14.sp,
                    color      = EjectOnSurface,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = strings.settingsVersion,
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
            Text(icon, fontSize = 18.sp)
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

    // 취소 모드: 채워진 코랄 원 + 흰색 X 아이콘. 일반 모드: 흰 원 + 코랄 테두리.
    Box(
        modifier = Modifier
            .size(260.dp)
            .scale(scale),
        contentAlignment = Alignment.Center,
    ) {
        // Outer halo
        Box(
            modifier = Modifier
                .size(252.dp)
                .clip(CircleShape)
                .background(EjectCoral.copy(alpha = 0.10f))
        )
        // Main circle
        Box(
            modifier = Modifier
                .size(232.dp)
                .shadow(
                    elevation    = 18.dp,
                    shape        = CircleShape,
                    ambientColor = EjectCoral.copy(alpha = 0.35f),
                    spotColor    = EjectCoral.copy(alpha = 0.35f),
                )
                .clip(CircleShape)
                .background(if (isCancelMode) EjectCoral else EjectSurface)
                .border(4.dp, EjectCoral, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isCancelMode) {
                    Text(
                        "✕",
                        fontSize   = 72.sp,
                        color      = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        strings.dialogCancel.uppercase(Locale.getDefault()),
                        fontSize      = 13.sp,
                        color         = Color.White,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 4.sp,
                    )
                } else {
                    Text(
                        "⏏",
                        fontSize   = 72.sp,
                        color      = EjectOnSurface,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "EJECT",
                        fontSize      = 13.sp,
                        color         = EjectCoral,
                        fontWeight    = FontWeight.ExtraBold,
                        letterSpacing = 4.sp,
                    )
                }
            }
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
) {
    val strings = LocalAppStrings.current

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(callers) { caller ->
            val isSelected = caller.id == selected.id
            val isCustom   = caller.id in customIds

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    // 선택: 크림슨 bg + 흰 텍스트 / 비선택: 서피스 bg + 외곽선
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
                    text       = "${caller.emoji}  ${caller.name}",
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

        // + 추가 버튼
        item {
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
}

@Composable
private fun TriggerTimeRow(
    selected: TimeChoice,
    customDelaySec: Int,
    onSelect: (TimeChoice) -> Unit,
) {
    val strings = LocalAppStrings.current
    val items = listOf(
        TimeChoice.IMMEDIATE to strings.triggerNow,
        TimeChoice.AFTER_10S to strings.trigger10s,
        TimeChoice.CUSTOM    to if (selected == TimeChoice.CUSTOM && customDelaySec > 0)
            "${strings.triggerCustom} (${customDelaySec}s)" else strings.triggerCustom,
    )
    TriggerChoiceRow(items = items, selectedKey = selected, onSelect = onSelect)
}

@Composable
private fun TriggerModeRow(
    selected: ModeChoice,
    onSelect: (ModeChoice) -> Unit,
) {
    val strings = LocalAppStrings.current
    val items = listOf(
        ModeChoice.BUTTON      to strings.triggerModeButton,
        ModeChoice.SHAKE       to strings.triggerShake,
        ModeChoice.SIDE_BUTTON to strings.triggerSideButton,
    )
    TriggerChoiceRow(items = items, selectedKey = selected, onSelect = onSelect)
}

@Composable
private fun <T> TriggerChoiceRow(
    items: List<Pair<T, String>>,
    selectedKey: T,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun BottomBar(
    current: AppScreen,
    isPremium: Boolean,
    onSelect: (AppScreen) -> Unit,
    onPremiumTap: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(8.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(EjectSurface.copy(alpha = 0.96f))
            .border(1.dp, EjectOutlineVar.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        listOf(
            AppScreen.COMMAND to strings.tabCommand,
            AppScreen.HISTORY to strings.tabHistory,
            AppScreen.SYSTEMS to strings.tabSystems,
        ).forEach { (screen, label) ->
            val isActive = screen == current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isActive) EjectSurfaceMid else Color.Transparent)
                    .clickable { onSelect(screen) }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text          = label,
                    fontSize      = 11.sp,
                    color         = if (isActive) EjectOnSurface else EjectSecondary,
                    fontWeight    = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                )
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

@Composable
private fun AddCallerDialog(onDismiss: () -> Unit, onConfirm: (Scenario) -> Unit) {
    val strings = LocalAppStrings.current
    val ctx = LocalContext.current
    var callerName    by remember { mutableStateOf("") }
    var searchQuery   by remember { mutableStateOf("") }
    var contacts      by remember { mutableStateOf(listOf<String>()) }
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
            val result = mutableListOf<String>()
            val cursor = ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                if (searchQuery.isNotBlank()) "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?" else null,
                if (searchQuery.isNotBlank()) arrayOf("%$searchQuery%") else null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
            )
            cursor?.use {
                val idx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val seen = mutableSetOf<String>()
                while (it.moveToNext() && result.size < 50) {
                    val name = it.getString(idx) ?: continue
                    if (seen.add(name)) result.add(name)
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
                    modifier = Modifier.fillMaxWidth(),
                )
                if (searchGranted) {
                    // 연락처 검색
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(strings.dialogSearch) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    // 연락처 리스트 — 처음부터 표시
                    LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                        items(contacts) { name ->
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { callerName = name }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                color = EjectOnSurface,
                            )
                            HorizontalDivider(color = EjectSurfaceMid)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (callerName.isNotBlank()) {
                    onConfirm(Scenario(
                        id           = "custom_${System.currentTimeMillis()}",
                        emoji        = "👤",
                        name         = callerName,
                        callerName   = callerName,
                        callerLabel  = strings.callerMobile,
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
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply {
                    marginEnd = dp(8)
                }
                scaleType = ImageView.ScaleType.FIT_CENTER
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

            val iconDrawable = ad.icon?.drawable
            if (iconDrawable != null) {
                iconView?.setImageDrawable(iconDrawable)
                iconView?.visibility = android.view.View.VISIBLE
            } else {
                iconView?.visibility = android.view.View.GONE
            }
            headlineView?.text = ad.headline ?: ad.body ?: ""

            // impression + click 집계를 위한 필수 호출.
            adView.setNativeAd(ad)
        },
    )
}

// ─── 프리미엄 업그레이드 다이얼로그 ─────────────────────────────────────────

@Composable
private fun PremiumUpgradeDialog(
    price: String?,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalAppStrings.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = EjectCoral,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(strings.premiumTitle, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(strings.premiumSubtitle, color = EjectSecondary, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                PremiumFeatureRow(strings.premiumFeature1)
                PremiumFeatureRow(strings.premiumFeature2)
                PremiumFeatureRow(strings.premiumFeature3)
            }
        },
        confirmButton = {
            Button(
                onClick = onBuy,
                colors = ButtonDefaults.buttonColors(containerColor = EjectCoral),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    String.format(strings.premiumBuyBtn, price ?: localizedFallbackPrice()),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onRestore) {
                Text(strings.premiumRestoreBtn, color = EjectSecondary, fontSize = 13.sp)
            }
        },
        containerColor = EjectSurface,
    )
}

@Composable
private fun PremiumFeatureRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(EjectCoral, CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 15.sp, color = EjectOnSurface)
    }
}

@Composable
private fun localizedFallbackPrice(): String {
    val country = Locale.getDefault().country
    return when (country) {
        "KR" -> "₩4,500"
        "JP" -> "¥500"
        "CN", "TW", "HK" -> "¥19.9"
        "IN" -> "₹249"
        "MX" -> "MX\$59"
        "ES" -> "2,99 €"
        "GB" -> "£2.49"
        "DE", "FR", "IT", "NL" -> "2,99 €"
        "BR" -> "R\$14.90"
        "AU" -> "A\$4.49"
        "CA" -> "CA\$3.99"
        else -> "$2.99"
    }
}
