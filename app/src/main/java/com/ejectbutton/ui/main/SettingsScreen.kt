package com.ejectbutton.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejectbutton.data.AppLanguage
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.data.SideButtonCommand
import com.ejectbutton.data.SideButtonStep
import com.ejectbutton.data.ThemeMode
import com.ejectbutton.service.ButtonWatchService
import com.ejectbutton.ui.theme.*
import java.util.Locale

@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    isPremium: Boolean,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onPurchasePremium: () -> Unit,
    onRestorePurchase: () -> Unit,
    premiumPrice: String?,
    // Round 31 — 사용자가 삭제한 mom/dad 프리셋을 복구. MainScreen 의 deletedPresetIds state
    // 와 EjectPrefs 양쪽을 같이 초기화해야 하므로 callback 으로 주입받는다.
    onRestorePresets: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val ctx = LocalContext.current

    var ringtone   by remember { mutableStateOf(EjectPrefs.loadRingtone(ctx)) }
    var vibration  by remember { mutableStateOf(EjectPrefs.loadVibration(ctx)) }
    var haptic     by remember { mutableStateOf(EjectPrefs.loadHaptic(ctx)) }
    var flash      by remember { mutableStateOf(EjectPrefs.loadFlash(ctx)) }
    // Round 9 — 다음 실행 시 튜토리얼을 다시 보여줄지 여부. 사용자가 끄면 OnboardingScreen 은
    // 더 이상 뜨지 않고, 여기서 다시 켜면 다음 앱 실행 때 튜토리얼이 되살아난다.
    var showManualNext by remember { mutableStateOf(EjectPrefs.loadShowOnboarding(ctx)) }
    var sideButtonCommand by remember {
        mutableStateOf(EjectPrefs.loadSideButtonCommand(ctx))
    }
    var showSideButtonPicker by remember { mutableStateOf(false) }
    var showHowToUse by remember { mutableStateOf(false) }
    var showLangPicker by remember { mutableStateOf(false) }

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

    if (showHowToUse) {
        HowToUseDialog(onDismiss = { showHowToUse = false })
    }

    if (showLangPicker) {
        LanguagePickerDialog(
            current  = currentLanguage,
            onSelect = { lang ->
                EjectPrefs.saveLanguage(ctx, lang.code)
                onLanguageChange(lang)
                showLangPicker = false
            },
            onDismiss = { showLangPicker = false },
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(EjectBg)
            .navigationBarsPadding(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
    ) {
        // ── Header ──────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(52.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EjectSurfaceMid)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("←", fontSize = 18.sp, color = EjectOnSurface)
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "SYSTEMS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EjectOnSurface,
                    letterSpacing = 3.sp,
                )
            }
            Spacer(Modifier.height(28.dp))
        }

        // Premium Card 는 SYSTEMS 탭으로 이동됐다.

        // ── Language ────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EjectSurface)
                    .clickable { showLangPicker = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Globe icon circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(EjectSurfaceMid),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🌐", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            strings.settingsLanguage,
                            fontSize = 15.sp,
                            color = EjectOnSurface,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            currentLanguage.nativeName,
                            fontSize = 14.sp,
                            color = EjectSecondary,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("›", fontSize = 20.sp, color = EjectSecondary)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Theme mode (3-way segmented picker: LIGHT / SYSTEM / DARK) ──────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EjectSurface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(EjectSurfaceMid),
                        contentAlignment = Alignment.Center,
                    ) {
                        val icon = when (themeMode) {
                            ThemeMode.LIGHT  -> "☀"
                            ThemeMode.DARK   -> "🌙"
                            ThemeMode.SYSTEM -> "⚙"
                        }
                        Text(icon, fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        strings.settingsTheme,
                        fontSize = 15.sp,
                        color = EjectOnSurface,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(12.dp))
                // LIGHT / SYSTEM / DARK 를 한 행에 세그먼트 형태로 배치.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val opts = listOf(
                        ThemeMode.LIGHT  to strings.themeLight,
                        ThemeMode.SYSTEM to strings.themeSystem,
                        ThemeMode.DARK   to strings.themeDark,
                    )
                    opts.forEach { (mode, label) ->
                        val isSel = themeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) EjectCoral else EjectSurfaceMid)
                                .clickable { onThemeModeChange(mode) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                label,
                                fontSize = 12.sp,
                                color = if (isSel) Color.White else EjectOnSurface,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Notifications & Sound ───────────────────────────────────────────
        item {
            EjectSectionHeader(strings.settingsNotifications)
            Spacer(Modifier.height(8.dp))
            // Grouped container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(EjectSurfaceLow)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Ringtone
                EjectToggleCard(
                    icon = "🔔",
                    label = strings.settingsRingtone,
                    desc = strings.settingsRingtoneDesc,
                    checked = ringtone,
                    onCheckedChange = {
                        ringtone = it
                        EjectPrefs.saveRingtone(ctx, it)
                    },
                )
                // Vibration
                EjectToggleCard(
                    icon = "📳",
                    label = strings.settingsVibration,
                    desc = null,
                    checked = vibration,
                    onCheckedChange = {
                        vibration = it
                        EjectPrefs.saveVibration(ctx, it)
                    },
                )
                // Haptic Feedback
                EjectToggleCard(
                    icon = "✋",
                    label = strings.settingsHaptic,
                    desc = null,
                    checked = haptic,
                    onCheckedChange = {
                        haptic = it
                        EjectPrefs.saveHaptic(ctx, it)
                    },
                )
                // Flash Alert
                EjectToggleCard(
                    icon = "💡",
                    label = strings.settingsFlash,
                    desc = strings.settingsFlashDesc,
                    checked = flash,
                    onCheckedChange = {
                        flash = it
                        EjectPrefs.saveFlash(ctx, it)
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Side Button Trigger ─────────────────────────────────────────────
        item {
            EjectSectionHeader(strings.settingSideButton)
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(EjectSurfaceLow)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Command picker row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EjectSurface)
                        .clickable { showSideButtonPicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EjectSurfaceMid),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("🎚️", fontSize = 18.sp)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    strings.settingSideButton,
                                    fontSize = 15.sp,
                                    color = EjectOnSurface,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    strings.settingSideButtonDesc,
                                    fontSize = 12.sp,
                                    color = EjectSecondary,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                sideButtonCommand.label(strings),
                                fontSize = 13.sp,
                                color = if (sideButtonCommand.isEnabled) EjectCoral else EjectSecondary,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("›", fontSize = 20.sp, color = EjectSecondary)
                        }
                    }
                }
                if (sideButtonCommand.isEnabled) {
                    // Warning banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EjectCoral.copy(alpha = 0.10f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Text(
                            strings.settingSideButtonWarning,
                            fontSize = 11.sp,
                            color = EjectCoral,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                // How-to-use link
                EjectLinkCard(
                    icon = "📘",
                    label = strings.howToUseTitle,
                    onClick = { showHowToUse = true },
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Tutorial (re-enable onboarding for next launch) ─────────────────
        item {
            EjectSectionHeader(strings.howToUseTitle)
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(EjectSurfaceLow)
                    .padding(8.dp),
            ) {
                EjectToggleCard(
                    icon  = "📖",
                    label = strings.settingsShowManual,
                    desc  = strings.settingsShowManualDesc,
                    checked = showManualNext,
                    onCheckedChange = {
                        showManualNext = it
                        EjectPrefs.saveShowOnboarding(ctx, it)
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Callers: restore default presets (Round 31) ────────────────────
        // 사용자가 메인 화면에서 mom/dad 칩의 X 를 눌러 soft-delete 한 프리셋을 한 번에 복원한다.
        item {
            EjectSectionHeader(strings.settingsRestorePresets)
            Spacer(Modifier.height(8.dp))
            EjectLinkCard(
                icon = "↩️",
                label = strings.settingsRestorePresets,
                onClick = onRestorePresets,
            )
            Spacer(Modifier.height(24.dp))
        }

        // ── About ───────────────────────────────────────────────────────────
        item {
            EjectSectionHeader(strings.settingsAbout)
            Spacer(Modifier.height(8.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(EjectSurfaceLow)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Share App
                EjectLinkCard(
                    icon = "📤",
                    label = strings.settingsShare,
                    onClick = {
                        val i = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Eject Button — ${strings.catchphrase}\nhttps://play.google.com/store/apps/details?id=com.ejectbutton")
                        }
                        ctx.startActivity(Intent.createChooser(i, strings.settingsShare))
                    }
                )
                // Rate on Store
                EjectLinkCard(
                    icon = "⭐",
                    label = strings.settingsRate,
                    onClick = {
                        runCatching {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=com.ejectbutton")))
                        }.onFailure {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.ejectbutton")))
                        }
                    }
                )
                // Privacy Policy
                EjectLinkCard(
                    icon = "🔒",
                    label = strings.settingsPrivacy,
                    onClick = {
                        // Round 29 — canonical URL is the live Cloudflare
                        // Workers deployment. The pages.dev alias I briefly
                        // switched to in round 28 is NOT live (ECONNREFUSED
                        // confirmed 2026-04-24) and would break Play review.
                        ctx.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://eject-button.hwanydanh.workers.dev/privacy-policy")))
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Version ─────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    strings.settingsVersion.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EjectSecondary.copy(alpha = 0.35f),
                    letterSpacing = 3.sp,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── Language picker dialog ──────────────────────────────────────────────────

@Composable
private fun LanguagePickerDialog(
    current: AppLanguage,
    onSelect: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settingsLanguage) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppLanguage.entries.forEach { lang ->
                    val isSelected = lang == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) EjectCoral.copy(0.08f) else Color.Transparent)
                            .clickable { onSelect(lang) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(lang.nativeName, fontSize = 15.sp,
                            color = if (isSelected) EjectRed else EjectOnSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = EjectCoral, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.dialogCancel) }
        },
        containerColor = EjectSurface,
    )
}

// ── Eject design system components ──────────────────────────────────────────

@Composable
private fun EjectSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = EjectSecondary,
        letterSpacing = 3.sp,
    )
}

@Composable
private fun EjectToggleCard(
    icon: String,
    label: String,
    desc: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EjectSurface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icon circle
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
                Column {
                    Text(
                        label,
                        fontSize = 15.sp,
                        color = EjectOnSurface,
                        fontWeight = FontWeight.Bold,
                    )
                    if (desc != null) {
                        Text(
                            desc,
                            fontSize = 12.sp,
                            color = EjectSecondary,
                        )
                    }
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor    = Color.White,
                    checkedTrackColor    = EjectOnSurface,
                    uncheckedThumbColor  = Color.White,
                    uncheckedTrackColor  = EjectSurfaceMid,
                )
            )
        }
    }
}

@Composable
private fun EjectLinkCard(
    icon: String,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EjectSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Icon circle
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
                    label,
                    fontSize = 15.sp,
                    color = EjectOnSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text("›", fontSize = 20.sp, color = EjectSecondary)
        }
    }
}

// ── Side button command picker ──────────────────────────────────────────────

internal fun SideButtonCommand.label(strings: com.ejectbutton.data.AppStrings): String =
    when (this) {
        SideButtonCommand.DISABLED        -> strings.cmdDisabled
        SideButtonCommand.VOL_UP_DOUBLE   -> strings.cmdVolUp2
        SideButtonCommand.VOL_UP_TRIPLE   -> strings.cmdVolUp3
        SideButtonCommand.VOL_DOWN_DOUBLE -> strings.cmdVolDown2
        SideButtonCommand.VOL_DOWN_TRIPLE -> strings.cmdVolDown3
        SideButtonCommand.CUSTOM          -> strings.cmdCustom
    }

@Composable
internal fun SideButtonCommandPickerDialog(
    current: SideButtonCommand,
    onSelect: (SideButtonCommand) -> Unit,
    onDismiss: () -> Unit,
) {
    val ctx = LocalContext.current
    val strings = LocalAppStrings.current
    var showCustomRecorder by remember { mutableStateOf(false) }

    if (showCustomRecorder) {
        CustomCommandRecordingDialog(
            initial = EjectPrefs.loadSideButtonCustomSequence(ctx),
            onDismiss = { showCustomRecorder = false },
            onSave = { seq ->
                EjectPrefs.saveSideButtonCustomSequence(ctx, seq)
                showCustomRecorder = false
                onSelect(SideButtonCommand.CUSTOM)
            },
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.settingSideButton) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                SideButtonCommand.entries.forEach { cmd ->
                    val isSelected = cmd == current
                    val isRecommended = cmd == SideButtonCommand.VOL_UP_DOUBLE
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) EjectCoral.copy(0.08f) else Color.Transparent)
                            .clickable {
                                if (cmd == SideButtonCommand.CUSTOM) showCustomRecorder = true
                                else onSelect(cmd)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                cmd.label(strings),
                                fontSize = 15.sp,
                                color = if (isSelected) EjectRed else EjectOnSurface,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            if (isRecommended) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EjectCoral)
                                        .padding(horizontal = 7.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        strings.cmdRecommended,
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp,
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = EjectCoral, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.dialogCancel) }
        },
        containerColor = EjectSurface,
    )
}

// ── How-to-use manual dialog ────────────────────────────────────────────────

@Composable
private fun HowToUseDialog(onDismiss: () -> Unit) {
    val strings = LocalAppStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                strings.howToUseTitle,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HowToUseStep("1", strings.howToUseStep1)
                HowToUseStep("2", strings.howToUseStep2)
                HowToUseStep("3", strings.howToUseStep3)
                HowToUseStep("4", strings.howToUseStep4)
                HowToUseStep("5", strings.howToUseStep5)
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(EjectCoral.copy(alpha = 0.10f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        strings.howToUseCaution,
                        fontSize = 12.sp,
                        color = EjectCoral,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.dialogConfirm, color = EjectCoral, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = EjectSurface,
    )
}

@Composable
private fun HowToUseStep(num: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(EjectCoral),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                num,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            fontSize = 14.sp,
            color = EjectOnSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

// ── 커스텀 커맨드 녹화 다이얼로그 ─────────────────────────────────────────────

@Composable
internal fun CustomCommandRecordingDialog(
    initial: List<SideButtonStep>,
    onDismiss: () -> Unit,
    onSave: (List<SideButtonStep>) -> Unit,
) {
    val strings = LocalAppStrings.current
    val ctx = LocalContext.current
    val activity = ctx as? com.ejectbutton.MainActivity

    var sequence by remember { mutableStateOf(initial) }

    // 다이얼로그 Window 안에서 볼륨키를 직접 가로채기 위한 포커스 요청자.
    // AlertDialog 는 별도 Window 를 사용하므로 MainActivity.onKeyDown 이
    // 호출되지 않는다. 대신 포커스된 Composable 의 onPreviewKeyEvent 를
    // 활용하면 다이얼로그 안에서 볼륨키 이벤트를 받을 수 있다.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    // 레거시 경로도 함께 설치해두어 다이얼로그 밖 포커스가 튀어도 이벤트가 유실되지 않도록.
    DisposableEffect(Unit) {
        activity?.recordingCallback = { step ->
            if (sequence.size < 5) sequence = sequence + step
        }
        onDispose { activity?.recordingCallback = null }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.customCommandTitle, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown &&
                            event.nativeKeyEvent.repeatCount == 0
                        ) {
                            when (event.nativeKeyEvent.keyCode) {
                                android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                                    if (sequence.size < 5) {
                                        sequence = sequence + SideButtonStep.UP
                                    }
                                    true
                                }
                                android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                                    if (sequence.size < 5) {
                                        sequence = sequence + SideButtonStep.DOWN
                                    }
                                    true
                                }
                                else -> false
                            }
                        } else {
                            // KeyUp 도 소비해 시스템 볼륨 UI 가 뜨지 않도록.
                            val kc = event.nativeKeyEvent.keyCode
                            kc == android.view.KeyEvent.KEYCODE_VOLUME_UP ||
                                kc == android.view.KeyEvent.KEYCODE_VOLUME_DOWN
                        }
                    },
            ) {
                Text(strings.customCommandHint, fontSize = 13.sp, color = EjectSecondary)

                // 녹화 상태 카드
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(EjectSurfaceMid)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                ) {
                    Column {
                        Text(
                            strings.customCommandRecording,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EjectCoral,
                            letterSpacing = 2.sp,
                        )
                        Spacer(Modifier.height(8.dp))
                        if (sequence.isEmpty()) {
                            Text(strings.customCommandEmpty, fontSize = 14.sp, color = EjectSecondary)
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                sequence.forEach { step ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EjectSurface)
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            if (step == SideButtonStep.UP) "▲ UP" else "▼ DOWN",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = EjectOnSurface,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 현재 저장값 표시
                if (initial.isNotEmpty()) {
                    Text(
                        "${strings.customCommandCurrent} " + initial.joinToString(" ") {
                            if (it == SideButtonStep.UP) "▲" else "▼"
                        },
                        fontSize = 11.sp,
                        color = EjectSecondary,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(sequence) },
                enabled = sequence.isNotEmpty(),
            ) {
                Text(
                    strings.customCommandSave,
                    color = if (sequence.isNotEmpty()) EjectCoral else EjectSecondary,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { sequence = emptyList() }) {
                    Text(strings.customCommandClear, color = EjectSecondary)
                }
                TextButton(onClick = onDismiss) {
                    Text(strings.dialogCancel)
                }
            }
        },
        containerColor = EjectSurface,
    )
}

/**
 * Google Play Billing 에서 가격을 못 가져왔을 때 (제품 미등록·디버그 빌드·오프라인)
 * 기기 로케일 기반으로 월 구독 근사치 가격을 표시한다.
 *
 * Round 18 — 일회성 가격에서 월 구독 (약 ₩1,900 / $1.99 수준) 로 전환.
 * premiumBuyBtn 포맷에 /월 접미사를 붙이므로 여기서는 통화 금액만 반환한다.
 */
@Composable
private fun localizedFallbackPrice(): String {
    val country = Locale.getDefault().country  // ISO 3166-1 alpha-2
    return when (country) {
        "KR" -> "₩1,900"
        "JP" -> "¥250"
        "CN", "TW", "HK" -> "¥9.9"
        "IN" -> "₹99"
        "MX" -> "MX\$29"
        "ES" -> "1,49 €"
        "GB" -> "£1.49"
        "DE", "FR", "IT", "NL" -> "1,49 €"
        "BR" -> "R\$6.90"
        "AU" -> "A\$1.99"
        "CA" -> "CA\$1.99"
        else -> "$1.99"
    }
}
