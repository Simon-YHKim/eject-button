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
import androidx.compose.runtime.CompositionLocalProvider
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
 * v1.1.0 — Premium 업그레이드 듀얼 플랜 ModalBottomSheet.
 *
 * 두 개의 플랜 카드를 제공:
 *  - 월간 (Monthly): 기본 가격 표시
 *  - 연간 (Annual, 기본 선택): "17% 할인" + "가장 인기" 배지
 *
 * 디자인 토큰 (dialogs.jsx 시안 그대로):
 *  - 24dp 둥근 코너 + drag handle
 *  - 선택 카드: 2dp EjectCoral 테두리 + 핑크 틴트 배경
 *  - "가장 인기" 배지: 카드 우상단 EjectCoral 라운드 칩
 *  - "17% 할인" 인라인 칩: 가격 우측 작은 라운드 배지
 *  - CTA: 50% 라운드 버튼, EjectCoral fill, 흰 글자
 *  - footer 11sp: "Cancel anytime in Play Store..." (Google 정책 준수)
 *
 * 결제 connection: BillingManager 가 현재 monthly 단일 상품만 지원하므로
 * onSelectAnnual / onSelectMonthly 모두 동일한 onPurchase 로 라우팅한다.
 * 추후 yearly product 추가 시 BillingManager 에서 plan 분기.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumUpgradeDialog(
    price: String?,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val strings = LocalAppStrings.current
    // 기본 선택 = ANNUAL (가장 인기). 사용자가 가장 자주 선택하길 기대.
    var selectAnnual by rememberSaveable { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EjectSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        PremiumUpgradeBody(
            // 월간 가격: BillingManager 가 아직 못 받아왔거나 (null) 디버그 빌드면 strings.prMonthlyPrice 사용.
            monthlyPrice = price ?: strings.prMonthlyPrice,
            annualPrice = strings.prAnnualPrice,
            annualAvg = strings.prAnnualAvg,
            selectAnnual = selectAnnual,
            onSelectAnnual = { selectAnnual = true },
            onSelectMonthly = { selectAnnual = false },
            onBuy = onBuy,
            onRestore = onRestore,
            onCancel = onDismiss,
            title = strings.prTitle,
            subtitle = strings.prSubtitle,
            monthlyLabel = strings.prMonthly,
            monthlyPer = strings.prMonthlyPer,
            monthlyNote = strings.prMonthlyNote,
            annualLabel = strings.prAnnual,
            annualPer = strings.prAnnualPer,
            annualNote = strings.prAnnualNote,
            saveLabel = strings.prSave,
            bestLabel = strings.prBest,
            cta = strings.prCta,
            restoreLabel = strings.prRestore,
            cancelLabel = strings.prCancel,
            footer = strings.prFooter,
        )
    }
}

@Composable
private fun PremiumUpgradeBody(
    monthlyPrice: String,
    annualPrice: String,
    annualAvg: String,
    selectAnnual: Boolean,
    onSelectAnnual: () -> Unit,
    onSelectMonthly: () -> Unit,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onCancel: () -> Unit,
    title: String,
    subtitle: String,
    monthlyLabel: String,
    monthlyPer: String,
    monthlyNote: String,
    annualLabel: String,
    annualPer: String,
    annualNote: String,
    saveLabel: String,
    bestLabel: String,
    cta: String,
    restoreLabel: String,
    cancelLabel: String,
    footer: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
    ) {
        // 헤더: 별 아이콘 + 타이틀
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(EjectCoral.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text("★", fontSize = 18.sp, color = EjectCoral)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = EjectOnSurface,
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = EjectSecondary,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // 플랜 카드 1 — 월간
        PlanCard(
            selected = !selectAnnual,
            onSelect = onSelectMonthly,
            label = monthlyLabel,
            price = monthlyPrice,
            per = monthlyPer,
            note = monthlyNote,
            saveBadge = null,
            popularBadge = null,
        )
        Spacer(Modifier.height(12.dp))

        // 플랜 카드 2 — 연간 (기본)
        PlanCard(
            selected = selectAnnual,
            onSelect = onSelectAnnual,
            label = annualLabel,
            price = annualPrice,
            per = annualPer,
            note = "$annualAvg · $annualNote",
            saveBadge = saveLabel,
            popularBadge = bestLabel,
        )

        Spacer(Modifier.height(20.dp))

        // CTA: 구독 시작
        Button(
            onClick = {
                // v1.1.0 — Clarity 마커: 실제 결제 launch 직전 (BillingManager.launchPurchase
                // 보다 한 단계 위 — 사용자가 명시적으로 결제 의향을 표시한 시점).
                // selectAnnual 분기로 sku 추정 — BillingManager 가 실제로는 monthly 단일 상품만
                // 지원하므로 양쪽 모두 monthly 로 fallback (yearly 상품 추가 시 분기 변경).
                com.ejectbutton.analytics.EjectClarity.premiumUpgradeClicked(
                    sku = if (selectAnnual) "eject_premium_annual" else "eject_premium_monthly"
                )
                onBuy()
            },
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
                text = cta,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            )
        }

        Spacer(Modifier.height(8.dp))

        // 복원 + 취소 한 행
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onRestore) {
                Text(
                    text = restoreLabel,
                    fontSize = 13.sp,
                    color = EjectSecondary,
                )
            }
            TextButton(onClick = onCancel) {
                Text(
                    text = cancelLabel,
                    fontSize = 13.sp,
                    color = EjectSecondary,
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Google Play 정책 준수 footer.
        Text(
            text = footer,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            color = EjectSecondary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PlanCard(
    selected: Boolean,
    onSelect: () -> Unit,
    label: String,
    price: String,
    per: String,
    note: String,
    saveBadge: String?,
    popularBadge: String?,
) {
    val borderColor = if (selected) EjectCoral else EjectOutlineVar
    val borderWidth = if (selected) 2.dp else 1.dp
    val bg = if (selected) EjectCoral.copy(alpha = 0.08f) else EjectSurfaceMid

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // 우상단 "가장 인기" 배지
        if (popularBadge != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EjectCoral)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = popularBadge,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
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

            // 라벨 + 가격 + 노트
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EjectOnSurface,
                        letterSpacing = 0.5.sp,
                    )
                    if (saveBadge != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EjectCoral.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = saveBadge,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = EjectCoral,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = price,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EjectOnSurface,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = per,
                        fontSize = 13.sp,
                        color = EjectSecondary,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note,
                    fontSize = 11.sp,
                    color = EjectSecondary,
                )
            }
        }
    }
}

// ─────────────────────────── Compose Previews ───────────────────────────────
// 라이트/다크 × 한국어/영어 4종.

@Preview(name = "Light · EN", showBackground = true)
@Composable
private fun PreviewPremiumLightEn() {
    PreviewPremiumWrapper(dark = false, language = AppLanguage.ENGLISH)
}

@Preview(name = "Dark · EN", showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewPremiumDarkEn() {
    PreviewPremiumWrapper(dark = true, language = AppLanguage.ENGLISH)
}

@Preview(name = "Light · KO", showBackground = true)
@Composable
private fun PreviewPremiumLightKo() {
    PreviewPremiumWrapper(dark = false, language = AppLanguage.KOREAN)
}

@Preview(name = "Dark · KO", showBackground = true, backgroundColor = 0xFF111111)
@Composable
private fun PreviewPremiumDarkKo() {
    PreviewPremiumWrapper(dark = true, language = AppLanguage.KOREAN)
}

@Composable
private fun PreviewPremiumWrapper(dark: Boolean, language: AppLanguage) {
    val strings = remember(language) { language.strings() }
    EjectButtonTheme(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT) {
        CompositionLocalProvider(LocalAppStrings provides strings) {
            Box(modifier = Modifier.fillMaxWidth().background(EjectSurface)) {
                PremiumUpgradeBody(
                    monthlyPrice = strings.prMonthlyPrice,
                    annualPrice = strings.prAnnualPrice,
                    annualAvg = strings.prAnnualAvg,
                    selectAnnual = true,
                    onSelectAnnual = {},
                    onSelectMonthly = {},
                    onBuy = {},
                    onRestore = {},
                    onCancel = {},
                    title = strings.prTitle,
                    subtitle = strings.prSubtitle,
                    monthlyLabel = strings.prMonthly,
                    monthlyPer = strings.prMonthlyPer,
                    monthlyNote = strings.prMonthlyNote,
                    annualLabel = strings.prAnnual,
                    annualPer = strings.prAnnualPer,
                    annualNote = strings.prAnnualNote,
                    saveLabel = strings.prSave,
                    bestLabel = strings.prBest,
                    cta = strings.prCta,
                    restoreLabel = strings.prRestore,
                    cancelLabel = strings.prCancel,
                    footer = strings.prFooter,
                )
            }
        }
    }
}
