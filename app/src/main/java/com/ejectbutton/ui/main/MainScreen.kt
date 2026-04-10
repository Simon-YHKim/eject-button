package com.ejectbutton.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.ejectbutton.data.Urgency
import com.ejectbutton.data.defaultScenarios
import com.ejectbutton.ui.theme.*
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import android.widget.ImageView
import android.widget.TextView
import android.view.LayoutInflater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

private enum class AppScreen { COMMAND, HISTORY, SYSTEMS }

@Composable
fun MainScreen(
    currentLanguage: AppLanguage,
    isPremium: Boolean,
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

    var selectedScenario by remember(strings) { mutableStateOf(localizedDefaults[0]) }
    var selectedTrigger  by remember { mutableStateOf(TriggerMode.IMMEDIATE) }
    var customDelaySec   by remember { mutableIntStateOf(60) }
    var showCustomDialog by remember { mutableStateOf(false) }
    var showAddCaller    by remember { mutableStateOf(false) }
    var customCallers    by remember { mutableStateOf(EjectPrefs.loadScenarios(ctx)) }
    var history          by remember { mutableStateOf(EjectPrefs.loadHistory(ctx)) }

    // 딜레이 카운트다운
    var countdownEnd by remember { mutableLongStateOf(0L) }
    var countdown    by remember { mutableIntStateOf(0) }
    LaunchedEffect(countdownEnd) {
        if (countdownEnd <= 0L) return@LaunchedEffect
        while (true) {
            val remaining = ((countdownEnd - System.currentTimeMillis()) / 1000 + 1).toInt()
            countdown = remaining.coerceAtLeast(0)
            if (countdown == 0) break
            delay(300L)
        }
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
            onLanguageChange  = onLanguageChange,
            onPurchasePremium = onPurchasePremium,
            onRestorePurchase = onRestorePurchase,
            premiumPrice      = premiumPrice,
            onDismiss         = { showSettings = false },
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EjectBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // 메인 컨텐츠 (하단 바 + 광고 영역만큼 패딩)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
        ) {
            when (currentScreen) {
                AppScreen.COMMAND -> CommandContent(
                    selectedScenario = selectedScenario,
                    selectedTrigger  = selectedTrigger,
                    customDelaySec   = customDelaySec,
                    allCallers       = localizedDefaults + customCallers,
                    customCallerIds  = customCallers.map { it.id }.toSet(),
                    countdown        = countdown,
                    onSelectCaller   = { selectedScenario = it },
                    onDeleteCaller   = { toDelete ->
                        val updated = customCallers.filter { it.id != toDelete.id }
                        customCallers = updated
                        EjectPrefs.saveScenarios(ctx, updated)
                        if (selectedScenario.id == toDelete.id) selectedScenario = localizedDefaults[0]
                    },
                    onSelectTrigger  = { trigger ->
                        selectedTrigger = trigger
                        if (trigger == TriggerMode.CUSTOM) showCustomDialog = true
                    },
                    onAddCaller      = { showAddCaller = true },
                    onSettingsTap    = { showSettings = true },
                    onEject          = {
                        val delayMs = when (selectedTrigger) {
                            TriggerMode.IMMEDIATE -> 0L
                            TriggerMode.AFTER_10S -> 10_000L
                            TriggerMode.AFTER_30S -> 30_000L
                            TriggerMode.AFTER_1MIN -> 60_000L
                            TriggerMode.SHAKE     -> -1L
                            TriggerMode.CUSTOM    -> customDelaySec * 1000L
                        }
                        if (delayMs > 0L) countdownEnd = System.currentTimeMillis() + delayMs

                        if (EjectPrefs.loadHaptic(ctx)) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        val triggerLabel = when (selectedTrigger) {
                            TriggerMode.IMMEDIATE -> strings.triggerNow
                            TriggerMode.AFTER_10S -> strings.trigger10s
                            TriggerMode.AFTER_30S -> strings.trigger30s
                            TriggerMode.AFTER_1MIN -> strings.trigger1min
                            TriggerMode.SHAKE     -> strings.triggerShake
                            TriggerMode.CUSTOM    -> "${customDelaySec}s"
                        }
                        val entry = "${SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(Date())} · ${selectedScenario.emoji}${selectedScenario.name} · $triggerLabel"
                        EjectPrefs.addHistory(ctx, entry)
                        history = EjectPrefs.loadHistory(ctx)
                        onEject(selectedScenario, delayMs)
                    },
                )
                AppScreen.HISTORY -> HistoryContent(history = history)
                AppScreen.SYSTEMS -> SystemsContent(
                    onClearHistory = {
                        EjectPrefs.clearHistory(ctx)
                        history = emptyList()
                    },
                    onSettingsTap = { showSettings = true },
                )
            }
        }

        // 하단 고정 영역: 광고 + 탭바
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
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
    selectedTrigger: TriggerMode,
    customDelaySec: Int,
    allCallers: List<Scenario>,
    customCallerIds: Set<String>,
    countdown: Int,
    onSelectCaller: (Scenario) -> Unit,
    onDeleteCaller: (Scenario) -> Unit,
    onSelectTrigger: (TriggerMode) -> Unit,
    onAddCaller: () -> Unit,
    onSettingsTap: () -> Unit,
    onEject: () -> Unit,
) {
    val strings = LocalAppStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        // 헤더 (좌: 앱명, 우: 설정 아이콘)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "⏏ EJECT BUTTON",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    color = EjectCoral, letterSpacing = 0.5.sp,
                )
                Text(strings.catchphrase, fontSize = 12.sp, color = EjectSecondary)
            }
            IconButton(onClick = onSettingsTap) {
                Icon(Icons.Default.Settings, contentDescription = strings.settingsTitle,
                    tint = EjectSecondary, modifier = Modifier.size(22.dp))
            }
        }

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
                    fontSize = 15.sp, color = EjectRed, fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // EJECT 버튼 (Stitch: 256dp, 8dp 코랄 테두리, soft-glow)
        EjectButton(onClick = onEject)
        Spacer(Modifier.height(12.dp))
        Text(strings.noEscapeLabel, fontSize = 14.sp, color = EjectSecondary, fontWeight = FontWeight.Medium)

        Spacer(Modifier.weight(1f))

        // 발신자 섹션
        SectionHeader(strings.callerPresets, strings.sectionCaller)
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

        // 트리거 섹션
        SectionHeader(strings.triggerTimer, strings.sectionDelay)
        Spacer(Modifier.height(12.dp))
        TriggerGrid(
            selected       = selectedTrigger,
            customDelaySec = customDelaySec,
            onSelect       = onSelectTrigger,
        )

        Spacer(Modifier.height(28.dp))
    }
}

// ─── HISTORY 탭 ──────────────────────────────────────────────────────────────

@Composable
private fun HistoryContent(history: List<String>) {
    val strings = LocalAppStrings.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(strings.historyTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EjectRed, letterSpacing = 1.sp)
        Text(strings.historySubtitle, fontSize = 13.sp, color = EjectSecondary)
        Spacer(Modifier.height(24.dp))

        if (history.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                Text(strings.historyEmpty, fontSize = 14.sp, color = EjectOutlineVar)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { entry ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(EjectSurface)
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                    ) {
                        Text(entry, fontSize = 13.sp, color = EjectOnSurface)
                    }
                }
            }
        }
    }
}

// ─── SYSTEMS 탭 ──────────────────────────────────────────────────────────────

@Composable
private fun SystemsContent(
    onClearHistory: () -> Unit,
    onSettingsTap: () -> Unit,
) {
    val strings = LocalAppStrings.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(16.dp))
        Text(strings.systemsTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EjectRed, letterSpacing = 1.sp)
        Text(strings.systemsSubtitle, fontSize = 13.sp, color = EjectSecondary)
        Spacer(Modifier.height(28.dp))

        // 설정 바로가기
        SettingsRowCard(label = strings.settingsTitle, onClick = onSettingsTap)
        Spacer(Modifier.height(10.dp))
        SettingsRowCard(label = "🗑  ${strings.settingsClearHistory}", onClick = onClearHistory)

        Spacer(Modifier.height(16.dp))

        // 앱 정보
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(EjectSurface)
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Column {
                Text("⏏  Eject Button", fontSize = 15.sp, color = EjectOnSurface, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(strings.settingsVersion, fontSize = 12.sp, color = EjectSecondary)
            }
        }
    }
}

@Composable
private fun SettingsRowCard(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(EjectSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 15.sp, color = EjectOnSurface, fontWeight = FontWeight.Medium)
            Text("›", fontSize = 20.sp, color = EjectSecondary)
        }
    }
}

// ─── 공통 컴포넌트 ────────────────────────────────────────────────────────────

@Composable
private fun EjectButton(onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "s",
    )

    Box(
        modifier = Modifier
            .size(210.dp)
            .scale(scale)
            .shadow(
                elevation    = 20.dp,
                shape        = CircleShape,
                ambientColor = EjectCoral.copy(alpha = 0.3f),
                spotColor    = EjectCoral.copy(alpha = 0.3f),
            )
            .clip(CircleShape)
            .background(EjectSurface)
            .border(8.dp, EjectCoral, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⏏", fontSize = 72.sp)
            Spacer(Modifier.height(2.dp))
            Text("EJECT", fontSize = 12.sp, color = EjectCoral, fontWeight = FontWeight.ExtraBold, letterSpacing = 6.sp)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(callers) { caller ->
            val isSelected = caller.id == selected.id
            val isCustom   = caller.id in customIds

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    // 선택: 흰색 bg + 2dp 코랄 테두리 / 비선택: #e2e2e2 bg (Stitch HTML 참고)
                    .background(if (isSelected) EjectSurface else EjectSecContainer)
                    .then(if (isSelected) Modifier.border(2.dp, EjectCoral, RoundedCornerShape(50)) else Modifier)
                    .clickable { onSelect(caller) }
                    .padding(start = 16.dp, end = if (isCustom) 8.dp else 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = "${caller.emoji} ${caller.name}",
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color    = if (isSelected) EjectCoral else EjectSecondary,
                )
                if (isCustom) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector     = Icons.Default.Close,
                        contentDescription = "Delete",
                        tint            = EjectOutlineVar,
                        modifier        = Modifier.size(16.dp).clickable { onDelete(caller) },
                    )
                }
            }
        }

        // + 추가 버튼
        item {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(EjectSurfaceLow)
                    .border(1.5.dp, EjectOutlineVar, RoundedCornerShape(50))
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Add, "Add", tint = EjectSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(strings.addCallerBtn, fontSize = 13.sp, color = EjectSecondary)
            }
        }
    }
}

@Composable
private fun TriggerGrid(
    selected: TriggerMode,
    customDelaySec: Int,
    onSelect: (TriggerMode) -> Unit,
) {
    val strings = LocalAppStrings.current
    val row1 = listOf(
        TriggerMode.IMMEDIATE to strings.triggerNow,
        TriggerMode.AFTER_10S to strings.trigger10s,
        TriggerMode.AFTER_30S to strings.trigger30s,
    )
    val row2 = listOf(
        TriggerMode.AFTER_1MIN to strings.trigger1min,
        TriggerMode.SHAKE      to "📳 ${strings.triggerShake}",
        TriggerMode.CUSTOM     to "${strings.triggerCustom} ${customDelaySec}s",
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(row1, row2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (mode, label) ->
                    val isSel = mode == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            // 선택: 코랄 채우기 + 흰 텍스트 / 비선택: 회색 bg (발신자 칩과 동일)
                            .background(if (isSel) EjectCoral else EjectSecContainer)
                            .clickable { onSelect(mode) }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = label,
                            fontSize   = 13.sp,
                            color      = if (isSel) Color.White else EjectSecondary,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(korean: String, english: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(korean, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EjectSecondary,
            letterSpacing = 1.sp)
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
            .padding(horizontal = 24.dp)
            .shadow(4.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
            .background(EjectSurface)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        listOf(
            AppScreen.COMMAND to strings.tabCommand,
            AppScreen.HISTORY to strings.tabHistory,
            AppScreen.SYSTEMS to strings.tabSystems,
        ).forEach { (screen, label) ->
            val isActive = screen == current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (isActive) EjectCoral.copy(0.1f) else Color.Transparent)
                    .clickable { onSelect(screen) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = label,
                    fontSize   = 11.sp,
                    color      = if (isActive) EjectCoral else EjectSecondary,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    letterSpacing = 1.sp,
                )
            }
        }

        // 프리미엄 버튼 (무료 사용자만)
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(EjectCoral)
                    .clickable(onClick = onPremiumTap)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = strings.premiumBadge,
                    fontSize   = 11.sp,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
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
    var showSearch    by remember { mutableStateOf(false) }
    var searchQuery   by remember { mutableStateOf("") }
    var contacts      by remember { mutableStateOf(listOf<String>()) }
    var searchGranted by remember {
        mutableStateOf(ctx.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        searchGranted = granted; if (granted) showSearch = true
    }

    LaunchedEffect(showSearch, searchQuery) {
        if (!showSearch || !searchGranted) return@LaunchedEffect
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
                while (it.moveToNext() && result.size < 30) {
                    val name = it.getString(idx) ?: continue
                    if (seen.add(name)) result.add(name)
                }
            }
            result
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.dialogAddCaller) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = callerName,
                    onValueChange = { callerName = it },
                    label = { Text(strings.dialogCallerName) },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchGranted) showSearch = !showSearch
                            else permLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }) {
                            Icon(Icons.Default.Search, strings.dialogSearch)
                        }
                    }
                )
                if (showSearch) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text(strings.dialogSearch) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 180.dp)) {
                        items(contacts) { name ->
                            Text(
                                text = name,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { callerName = name; showSearch = false }
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
            }) { Text(strings.dialogAdd) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.dialogCancel) } },
        containerColor = EjectSurface,
    )
}

// ─── 네이티브 광고 ──────────────────────────────────────────────────────────

@Composable
private fun NativeAdCard(ad: NativeAd, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(EjectSurface)
            .shadow(1.dp, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // 광고 표시 + 헤드라인
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ad.icon?.drawable?.let { icon ->
                    AndroidView(
                        factory = { ctx -> ImageView(ctx).apply { setImageDrawable(icon) } },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    ad.headline?.let { headline ->
                        Text(
                            headline,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EjectOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    ad.body?.let { body ->
                        Text(
                            body,
                            fontSize = 11.sp,
                            color = EjectSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // 광고 라벨
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(EjectSecondary.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("AD", fontSize = 9.sp, color = EjectSecondary, fontWeight = FontWeight.Bold)
                }
            }

            // CTA 버튼
            ad.callToAction?.let { cta ->
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(EjectCoral.copy(alpha = 0.1f))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(cta, fontSize = 12.sp, color = EjectCoral, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
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
