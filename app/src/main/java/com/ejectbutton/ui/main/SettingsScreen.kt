package com.ejectbutton.ui.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejectbutton.data.AppLanguage
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.ui.theme.*
import java.util.Locale

@Composable
fun SettingsScreen(
    currentLanguage: AppLanguage,
    isPremium: Boolean,
    onLanguageChange: (AppLanguage) -> Unit,
    onPurchasePremium: () -> Unit,
    onRestorePurchase: () -> Unit,
    premiumPrice: String?,
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

        // ── Premium Card ────────────────────────────────────────────────────
        if (!isPremium) {
            item {
                val displayPrice = premiumPrice ?: localizedFallbackPrice()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    EjectPrimaryContainer,
                                    EjectPrimaryContainer.copy(alpha = 0.92f),
                                )
                            )
                        )
                        .padding(24.dp),
                ) {
                    Column {
                        // ELITE TIER badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EjectCoral)
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Text(
                                "ELITE TIER",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 2.sp,
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        // Headline
                        Text(
                            "Sovereign Premium",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                        )
                        Spacer(Modifier.height(8.dp))
                        // Description
                        Text(
                            strings.premiumSubtitle,
                            fontSize = 14.sp,
                            color = EjectOnPrimaryContainer,
                        )
                        Spacer(Modifier.height(20.dp))
                        // Upgrade button
                        Button(
                            onClick = onPurchasePremium,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = EjectOnSurface,
                            ),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(
                                "Upgrade Now →",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        // Restore purchase
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                strings.premiumRestoreBtn,
                                fontSize = 12.sp,
                                color = EjectOnPrimaryContainer,
                                modifier = Modifier.clickable(onClick = onRestorePurchase),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }

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

        // ── Notifications & Sound ───────────────────────────────────────────
        item {
            SovereignSectionHeader(strings.settingsNotifications)
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
                SovereignToggleCard(
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
                SovereignToggleCard(
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
                SovereignToggleCard(
                    icon = "✋",
                    label = strings.settingsHaptic,
                    desc = null,
                    checked = haptic,
                    onCheckedChange = {
                        haptic = it
                        EjectPrefs.saveHaptic(ctx, it)
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── About ───────────────────────────────────────────────────────────
        item {
            SovereignSectionHeader(strings.settingsAbout)
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
                SovereignLinkCard(
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
                SovereignLinkCard(
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
                SovereignLinkCard(
                    icon = "🔒",
                    label = strings.settingsPrivacy,
                    onClick = {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://ejectbutton.app/privacy")))
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

// ── Sovereign design system components ──────────────────────────────────────

@Composable
private fun SovereignSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        color = EjectSecondary,
        letterSpacing = 3.sp,
    )
}

@Composable
private fun SovereignToggleCard(
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
private fun SovereignLinkCard(
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

/**
 * Google Play Billing에서 가격을 못 가져왔을 때
 * 기기 로케일 기반으로 근사치 가격 표시
 */
@Composable
private fun localizedFallbackPrice(): String {
    val country = Locale.getDefault().country  // ISO 3166-1 alpha-2
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
