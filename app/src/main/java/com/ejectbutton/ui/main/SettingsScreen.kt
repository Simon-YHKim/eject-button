package com.ejectbutton.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejectbutton.data.AppLanguage
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.ui.theme.*

@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalAppStrings.current
    val ctx = LocalContext.current

    var ringtone   by remember { mutableStateOf(EjectPrefs.loadRingtone(ctx)) }
    var vibration  by remember { mutableStateOf(EjectPrefs.loadVibration(ctx)) }
    var haptic     by remember { mutableStateOf(EjectPrefs.loadHaptic(ctx)) }
    var showLangPicker by remember { mutableStateOf(false) }

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
        item {
            Spacer(Modifier.height(52.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(EjectSurfaceMid)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("←", fontSize = 18.sp, color = EjectOnSurface)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(strings.settingsTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EjectRed, letterSpacing = 1.sp)
                    Text("SYSTEMS", fontSize = 11.sp, color = EjectSecondary, letterSpacing = 2.sp)
                }
            }
            Spacer(Modifier.height(28.dp))
        }

        // ── 언어 ─────────────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(strings.settingsLanguage)
            Spacer(Modifier.height(8.dp))
            SettingsCard(
                onClick = { showLangPicker = true }
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(strings.settingsLanguage, fontSize = 15.sp, color = EjectOnSurface, fontWeight = FontWeight.Medium)
                        Text(currentLanguage.nativeName, fontSize = 13.sp, color = EjectSecondary)
                    }
                    Text("›", fontSize = 20.sp, color = EjectSecondary)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── 알림 & 소리 ────────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(strings.settingsNotifications)
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    ToggleRow(
                        label = strings.settingsRingtone,
                        desc  = strings.settingsRingtoneDesc,
                        checked = ringtone,
                        onCheckedChange = {
                            ringtone = it
                            EjectPrefs.saveRingtone(ctx, it)
                        },
                    )
                    HorizontalDivider(color = EjectSurfaceMid, thickness = 1.dp)
                    ToggleRow(
                        label = strings.settingsVibration,
                        desc  = strings.settingsVibrationDesc,
                        checked = vibration,
                        onCheckedChange = {
                            vibration = it
                            EjectPrefs.saveVibration(ctx, it)
                        },
                    )
                    HorizontalDivider(color = EjectSurfaceMid, thickness = 1.dp)
                    ToggleRow(
                        label = strings.settingsHaptic,
                        desc  = strings.settingsHapticDesc,
                        checked = haptic,
                        onCheckedChange = {
                            haptic = it
                            EjectPrefs.saveHaptic(ctx, it)
                        },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── 앱 정보 ────────────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(strings.settingsAbout)
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    SettingsLinkRow(
                        label = strings.settingsShare,
                        onClick = {
                            val i = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Eject Button — ${strings.catchphrase}\nhttps://play.google.com/store/apps/details?id=com.ejectbutton")
                            }
                            ctx.startActivity(Intent.createChooser(i, strings.settingsShare))
                        }
                    )
                    HorizontalDivider(color = EjectSurfaceMid, thickness = 1.dp)
                    SettingsLinkRow(
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
                    HorizontalDivider(color = EjectSurfaceMid, thickness = 1.dp)
                    SettingsLinkRow(
                        label = strings.settingsPrivacy,
                        onClick = {
                            ctx.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://ejectbutton.app/privacy")))
                        }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── 버전 정보 ─────────────────────────────────────────────────────────
        item {
            Box(
                Modifier
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
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ── 언어 선택 다이얼로그 ─────────────────────────────────────────────────────

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

// ── 공통 컴포넌트 ─────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = EjectSecondary,
        letterSpacing = 1.5.sp,
    )
}

@Composable
private fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val mod = Modifier
        .fillMaxWidth()
        .shadow(1.dp, RoundedCornerShape(16.dp))
        .clip(RoundedCornerShape(16.dp))
        .background(EjectSurface)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(horizontal = 20.dp, vertical = 16.dp)

    Column(modifier = mod, content = content)
}

@Composable
private fun ToggleRow(
    label: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, fontSize = 15.sp, color = EjectOnSurface, fontWeight = FontWeight.Medium)
            Text(desc, fontSize = 12.sp, color = EjectSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor    = Color.White,
                checkedTrackColor    = EjectCoral,
                uncheckedThumbColor  = Color.White,
                uncheckedTrackColor  = EjectSurfaceMid,
            )
        )
    }
}

@Composable
private fun SettingsLinkRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, color = EjectOnSurface, fontWeight = FontWeight.Medium)
        Text("›", fontSize = 20.sp, color = EjectSecondary)
    }
}
