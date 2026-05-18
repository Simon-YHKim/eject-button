package com.ejectbutton.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejectbutton.data.AppLanguage
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.data.ThemeMode
import com.ejectbutton.data.strings
import com.ejectbutton.ui.theme.EjectButtonTheme
import com.ejectbutton.ui.theme.EjectCoral
import com.ejectbutton.ui.theme.EjectOnSurface
import com.ejectbutton.ui.theme.EjectOutlineVar
import com.ejectbutton.ui.theme.EjectSecondary
import com.ejectbutton.ui.theme.EjectSurface
import com.ejectbutton.ui.theme.EjectSurfaceMid

/**
 * v1.6.6 — Share-to-unlock / Premium 선택 ModalBottomSheet.
 *
 * 비-프리미엄 + 아직 미공유 사용자가 caller 1명 초과로 추가하려 할 때 띄운다.
 * 이전 RewardedAdDialog (30초 광고 = 1회 unlock) 대체. 새 모델: 앱 공유 1회 = 영구 unlock.
 *
 * 두 가지 옵션을 라디오 카드로 제공:
 *  - "앱 공유하기" → ACTION_SEND intent + EjectPrefs.saveHasShared(true) → 영구 unlock
 *  - "프리미엄"   → 모든 잠긴 기능 영구 잠금 해제 (기본 선택)
 *
 * RewardedAdDialog 대비 변경:
 *  - "광고 보기" → "앱 공유" 라디오 카드
 *  - AdMob 디스클로저 footer 제거 (광고 아님)
 *  - 아이콘: ▶ → 💬 (공유 의미)
 *  - 기본 선택 = Premium 유지 (사용자가 가장 자주 선택할 옵션)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToUnlockDialog(
    onShareApp: () -> Unit,
    onUpgradePremium: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val strings = LocalAppStrings.current
    var selectPremium by rememberSaveable { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EjectSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        ShareToUnlockDialogBody(
            selectPremium = selectPremium,
            onSelectPremium = { selectPremium = true },
            onSelectShare = { selectPremium = false },
            onContinue = {
                if (selectPremium) onUpgradePremium() else onShareApp()
            },
            onCancel = onDismiss,
            title = strings.shareUnlockTitle,
            subtitle = strings.shareUnlockSubtitle,
            shareHead = strings.shareUnlockShareHead,
            shareSub = strings.shareUnlockShareSub,
            shareBadge = strings.shareUnlockShareBadge,
            proHead = strings.rdProHead,
            proSub = strings.rdProSub,
            proBadge = strings.rdProBadge,
            cancelLabel = strings.rdCancel,
            continueLabel = strings.rdContinue,
        )
    }
}

@Composable
private fun ShareToUnlockDialogBody(
    selectPremium: Boolean,
    onSelectPremium: () -> Unit,
    onSelectShare: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    title: String,
    subtitle: String,
    shareHead: String,
    shareSub: String,
    shareBadge: String,
    proHead: String,
    proSub: String,
    proBadge: String,
    cancelLabel: String,
    continueLabel: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = EjectOnSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = EjectSecondary,
        )
        Spacer(Modifier.height(20.dp))

        // 옵션 1 — 앱 공유 (이전 "광고 보기" 자리)
        ShareOptionCard(
            selected = !selectPremium,
            onSelect = onSelectShare,
            head = shareHead,
            sub = shareSub,
            badge = shareBadge,
            badgeColor = EjectSurfaceMid,
            badgeTextColor = EjectSecondary,
            icon = "💬",
        )
        Spacer(Modifier.height(12.dp))

        // 옵션 2 — 프리미엄 (기본 선택)
        ShareOptionCard(
            selected = selectPremium,
            onSelect = onSelectPremium,
            head = proHead,
            sub = proSub,
            badge = proBadge,
            badgeColor = EjectCoral,
            badgeTextColor = Color.White,
            icon = "★",
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EjectCoral,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(50),
        ) {
            Text(
                text = continueLabel,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            )
        }

        Spacer(Modifier.height(8.dp))

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = cancelLabel,
                fontSize = 13.sp,
                color = EjectSecondary,
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ShareOptionCard(
    selected: Boolean,
    onSelect: () -> Unit,
    head: String,
    sub: String,
    badge: String,
    badgeColor: Color,
    badgeTextColor: Color,
    icon: String,
) {
    val borderColor = if (selected) EjectCoral else EjectOutlineVar
    val borderWidth = if (selected) 2.dp else 1.dp
    val bg = if (selected) EjectCoral.copy(alpha = 0.08f) else EjectSurfaceMid

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(
                    width = 2.dp,
                    color = if (selected) EjectCoral else EjectOutlineVar,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(EjectCoral),
                )
            }
        }
        Spacer(Modifier.width(14.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (selected) EjectCoral.copy(alpha = 0.16f) else EjectSurface),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon, fontSize = 18.sp, color = EjectCoral)
        }
        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = head,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = EjectOnSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sub,
                fontSize = 12.sp,
                color = EjectSecondary,
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                text = badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = badgeTextColor,
                letterSpacing = 0.5.sp,
            )
        }
    }
}

// ─────────────────────────── Compose Previews ───────────────────────────────

@Preview(name = "Light · KO", showBackground = true)
@Composable
private fun PreviewShareToUnlockDialogLightKo() {
    PreviewWrapper(dark = false, language = AppLanguage.KOREAN)
}

@Preview(name = "Dark · EN", showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewShareToUnlockDialogDarkEn() {
    PreviewWrapper(dark = true, language = AppLanguage.ENGLISH)
}

@Composable
private fun PreviewWrapper(dark: Boolean, language: AppLanguage) {
    val strings = remember(language) { language.strings() }
    EjectButtonTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
        androidx.compose.runtime.CompositionLocalProvider(LocalAppStrings provides strings) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EjectSurface),
            ) {
                ShareToUnlockDialogBody(
                    selectPremium = true,
                    onSelectPremium = {},
                    onSelectShare = {},
                    onContinue = {},
                    onCancel = {},
                    title = strings.shareUnlockTitle,
                    subtitle = strings.shareUnlockSubtitle,
                    shareHead = strings.shareUnlockShareHead,
                    shareSub = strings.shareUnlockShareSub,
                    shareBadge = strings.shareUnlockShareBadge,
                    proHead = strings.rdProHead,
                    proSub = strings.rdProSub,
                    proBadge = strings.rdProBadge,
                    cancelLabel = strings.rdCancel,
                    continueLabel = strings.rdContinue,
                )
            }
        }
    }
}
