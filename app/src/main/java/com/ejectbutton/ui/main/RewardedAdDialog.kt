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
 * v1.1.0 — Rewarded Ad / Premium 선택 ModalBottomSheet.
 *
 * 비-프리미엄 사용자가 잠긴 기능(예: 추가 발신자, 사이드 버튼 트리거 등)을 누를 때 띄운다.
 * 두 가지 옵션을 라디오 카드로 제공:
 *  - "광고 보기" → 30초 보상형 광고 시청 후 1회 사용 권한
 *  - "프리미엄"  → 모든 잠긴 기능 영구 잠금 해제 (기본 선택)
 *
 * 설계 토큰 (dialogs.jsx 시안 그대로):
 *  - 24dp 둥근 코너 + drag handle (Material3 기본)
 *  - 카드 셀렉트 시 EjectCoral 테두리 2dp + 핑크 틴트 배경
 *  - "추천" 배지: 우상단 EjectCoral 라운드 칩
 *  - AdMob 디스클로저: footer 11sp / EjectSecondary
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RewardedAdDialog(
    onWatchAd: () -> Unit,
    onUpgradePremium: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val strings = LocalAppStrings.current
    // 기본 선택 = Premium (PRO). 사용자가 가장 자주 선택하길 기대하는 옵션.
    var selectPremium by rememberSaveable { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EjectSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        RewardedAdDialogBody(
            selectPremium = selectPremium,
            onSelectPremium = { selectPremium = true },
            onSelectAd = { selectPremium = false },
            onContinue = {
                if (selectPremium) onUpgradePremium() else onWatchAd()
            },
            onCancel = onDismiss,
            title = strings.rdTitle,
            subtitle = strings.rdSubtitle,
            adHead = strings.rdAdHead,
            adSub = strings.rdAdSub,
            adBadge = strings.rdAdBadge,
            proHead = strings.rdProHead,
            proSub = strings.rdProSub,
            proBadge = strings.rdProBadge,
            disclosure = strings.rdDisclosure,
            cancelLabel = strings.rdCancel,
            continueLabel = strings.rdContinue,
        )
    }
}

@Composable
private fun RewardedAdDialogBody(
    selectPremium: Boolean,
    onSelectPremium: () -> Unit,
    onSelectAd: () -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    title: String,
    subtitle: String,
    adHead: String,
    adSub: String,
    adBadge: String,
    proHead: String,
    proSub: String,
    proBadge: String,
    disclosure: String,
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

        // 옵션 1 — 광고 보기
        OptionCard(
            selected = !selectPremium,
            onSelect = onSelectAd,
            head = adHead,
            sub = adSub,
            badge = adBadge,
            badgeColor = EjectSurfaceMid,
            badgeTextColor = EjectSecondary,
            icon = "▶",
        )
        Spacer(Modifier.height(12.dp))

        // 옵션 2 — 프리미엄 (기본)
        OptionCard(
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

        // CTA: Continue
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

        Spacer(Modifier.height(8.dp))

        // AdMob 디스클로저 — Google Play 정책 준수.
        // 광고 옵션 선택 시에만 의미가 있으나 항상 노출하여 투명성 확보.
        Text(
            text = disclosure,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = EjectSecondary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun OptionCard(
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
    // 핑크 틴트 배경: 선택 시 EjectCoral.copy(alpha=0.08f), 비선택 시 EjectSurfaceMid
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
        // 라디오 인디케이터
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

        // 아이콘 박스
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

        // 텍스트
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

        // 배지
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
// 라이트/다크 × 한국어/영어 4종.

@Preview(name = "Light · EN", showBackground = true)
@Composable
private fun PreviewRewardedAdDialogLightEn() {
    PreviewWrapper(dark = false, language = AppLanguage.ENGLISH)
}

@Preview(name = "Dark · EN", showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewRewardedAdDialogDarkEn() {
    PreviewWrapper(dark = true, language = AppLanguage.ENGLISH)
}

@Preview(name = "Light · KO", showBackground = true)
@Composable
private fun PreviewRewardedAdDialogLightKo() {
    PreviewWrapper(dark = false, language = AppLanguage.KOREAN)
}

@Preview(name = "Dark · KO", showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewRewardedAdDialogDarkKo() {
    PreviewWrapper(dark = true, language = AppLanguage.KOREAN)
}

@Composable
private fun PreviewWrapper(dark: Boolean, language: AppLanguage) {
    val strings = remember(language) { language.strings() }
    EjectButtonTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
        androidx.compose.runtime.CompositionLocalProvider(LocalAppStrings provides strings) {
            // ModalBottomSheet 는 Preview 에서 부팅되지 않으므로 본문만 직접 호출.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EjectSurface),
            ) {
                RewardedAdDialogBody(
                    selectPremium = true,
                    onSelectPremium = {},
                    onSelectAd = {},
                    onContinue = {},
                    onCancel = {},
                    title = strings.rdTitle,
                    subtitle = strings.rdSubtitle,
                    adHead = strings.rdAdHead,
                    adSub = strings.rdAdSub,
                    adBadge = strings.rdAdBadge,
                    proHead = strings.rdProHead,
                    proSub = strings.rdProSub,
                    proBadge = strings.rdProBadge,
                    disclosure = strings.rdDisclosure,
                    cancelLabel = strings.rdCancel,
                    continueLabel = strings.rdContinue,
                )
            }
        }
    }
}
