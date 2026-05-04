package com.ejectbutton.ui.coachmark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.ui.theme.EjectOnSurface
import com.ejectbutton.ui.theme.EjectOutlineVar
import com.ejectbutton.ui.theme.EjectRed
import com.ejectbutton.ui.theme.EjectSecondary
import com.ejectbutton.ui.theme.EjectSurface

// ────────────────────────────────────────────────────────────────────
//  CoachmarkOverlay — single-file, drop-in 4-step coachmark.
//  • Tri-stop EjectRed halo (works for circle + rounded rect targets)
//  • Tooltip card with directional placement
//  • 4-dot progress, ghost "건너뛰기", EjectRed primary pill
//  • Light + dark via theme tokens — no hard-coded mode branch needed
// ────────────────────────────────────────────────────────────────────

enum class SpotShape { Circle, RoundRect }

data class CoachmarkStep(
    val id: String,
    val title: String,
    val body: String,
    val primaryLabel: String,
)

data class Spot(
    val rect: Rect,
    val shape: SpotShape,
)

@Composable
fun CoachmarkHost(
    state: CoachmarkState,
    steps: List<CoachmarkStep>,
    onFinish: () -> Unit,
) {
    val visible = state.isActive && state.index in steps.indices
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        val step = steps[state.index]
        val spot = state.spotFor(step.id)
        CoachmarkOverlay(
            step = step,
            stepIndex = state.index, total = steps.size,
            spot = spot,
            onSkip = { state.dismiss(); onFinish() },
            onNext = {
                if (state.index == steps.lastIndex) { state.dismiss(); onFinish() }
                else state.next()
            },
        )
    }
}

@Composable
private fun CoachmarkOverlay(
    step: CoachmarkStep,
    stepIndex: Int, total: Int,
    spot: Spot?,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenW = with(density) { config.screenWidthDp.dp.toPx() }
    val screenH = with(density) { config.screenHeightDp.dp.toPx() }
    val pad = with(density) { 8.dp.toPx() }
    // EjectRed 가 @Composable getter (theme palette 분기) 라서 drawWithContent 람다 안에서
    // 직접 호출 불가 → composable scope 에서 한 번 읽어 local 로 lift.
    val haloColor = EjectRed
    val haloOffsets = listOf(
        Triple(0.dp,  2.5.dp, 1.0f),
        Triple(6.dp,  6.0.dp, 0.20f),
        Triple(12.dp, 10.0.dp, 0.08f),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            // v1.5.2 — BlendMode.Clear 가 dim 만 깎고 child content 는 살리려면
            // offscreen 컴포지팅 layer 가 필수. 디자인 zip 코드에서 누락된 부분 — 추가.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
            ) { /* swallow taps */ }
            .drawWithContent {
                drawContent()
                // v1.5.2 — dim alpha 0.74 → 0.62 로 약화. spotlight cutout 이 잘 작동하면
                // ring 안의 child (시나리오 카드 / EJECT 버튼 / ⚙ 등) 이 명확히 보이도록.
                drawRect(Color.Black.copy(alpha = 0.62f))
                spot?.let { s ->
                    if (s.shape == SpotShape.Circle) {
                        val r = (s.rect.maxDimension / 2) + pad
                        drawCircle(
                            color = Color.Transparent,
                            radius = r, center = s.rect.center,
                            blendMode = BlendMode.Clear,
                        )
                    } else {
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(s.rect.left - pad, s.rect.top - pad),
                            size = Size(s.rect.width + pad * 2, s.rect.height + pad * 2),
                            cornerRadius = CornerRadius(22.dp.toPx()),
                            blendMode = BlendMode.Clear,
                        )
                    }
                    haloOffsets.forEach { (off, stroke, alpha) ->
                        val offPx = off.toPx()
                        val strokePx = stroke.toPx()
                        if (s.shape == SpotShape.Circle) {
                            val r = (s.rect.maxDimension / 2) + pad + offPx
                            drawCircle(
                                color = haloColor.copy(alpha = alpha),
                                radius = r, center = s.rect.center,
                                style = Stroke(strokePx),
                            )
                        } else {
                            drawRoundRect(
                                color = haloColor.copy(alpha = alpha),
                                topLeft = Offset(s.rect.left - pad - offPx, s.rect.top - pad - offPx),
                                size = Size(
                                    s.rect.width + (pad + offPx) * 2,
                                    s.rect.height + (pad + offPx) * 2,
                                ),
                                cornerRadius = CornerRadius((22 + off.value).dp.toPx()),
                                style = Stroke(strokePx),
                            )
                        }
                    }
                }
            },
    ) {
        val placement = remember(spot, screenW, screenH) {
            placeTooltip(spot, screenW, screenH, density)
        }
        TooltipCard(
            step = step,
            stepIndex = stepIndex, total = total,
            placement = placement,
            onSkip = onSkip, onNext = onNext,
        )
    }
}

private data class Placement(val topDp: Dp, val sideDp: Dp)

/**
 * v1.5.2 — Tooltip 카드를 spotlight 와 절대 겹치지 않게 배치.
 *
 * 알고리즘:
 *  1) spotlight 의 위/아래 어디에 더 큰 여백이 있는지 계산
 *  2) 더 큰 여백 쪽에 카드 배치
 *  3) cardH 추정 = 180dp (실제는 더 작거나 비슷, 안전 margin)
 *  4) 카드가 화면 밖으로 나가지 않게 coerce
 *
 * 스크린샷 회귀 (v1.5.1) 의 원인:
 *  - 기존 220dp 가정 + 한 쪽만 보던 로직이라 spotlight 와 같은 영역 차지
 *  - 시나리오 카드가 dim 으로 가려져 ring 안에 무엇이 있는지 안 보였음 (graphicsLayer 누락)
 *  - 둘 다 fix 해서 "지시 (spotlight) + 팝업 (tooltip) 둘 다 잘 보이게" 보장
 */
private fun placeTooltip(
    spot: Spot?, screenW: Float, screenH: Float, density: androidx.compose.ui.unit.Density,
): Placement {
    val sidePad = 16.dp
    val cardH = 180.dp           // v1.5.1 의 220dp 보다 작게 — 카드 본문 더 컴팩트해짐
    val gap = 16.dp              // spotlight 와 카드 사이 안전 여백
    val minTop = 64.dp           // 화면 상단 안전 영역 (status bar)
    val rect = spot?.rect ?: return Placement(minTop + 56.dp, sidePad)

    return with(density) {
        val screenHDp = screenH.toDp()
        val spotTopDp = rect.top.toDp()
        val spotBotDp = rect.bottom.toDp()

        val spaceAbove = (spotTopDp - minTop - gap).coerceAtLeast(0.dp)
        val spaceBelow = (screenHDp - spotBotDp - gap).coerceAtLeast(0.dp)

        // 위/아래 중 cardH 가 들어갈 수 있는 큰 쪽 선택
        if (spaceBelow >= cardH || spaceBelow >= spaceAbove) {
            // 카드를 spotlight 아래에 배치
            Placement(spotBotDp + gap, sidePad)
        } else {
            // 카드를 spotlight 위에 배치 (위에서 cardH 만큼 빼서 top 결정)
            Placement((spotTopDp - gap - cardH).coerceAtLeast(minTop), sidePad)
        }
    }
}

@Composable
private fun TooltipCard(
    step: CoachmarkStep,
    stepIndex: Int, total: Int,
    placement: Placement,
    onSkip: () -> Unit,
    onNext: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Box(
        modifier = Modifier
            .padding(start = placement.sideDp, end = placement.sideDp, top = placement.topDp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(EjectSurface)
            .border(1.dp, EjectOutlineVar, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(RoundedCornerShape(11.dp)).background(EjectRed)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = "STEP ${stepIndex + 1}",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(total) { i ->
                        Box(
                            Modifier.size(7.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (i == stepIndex) EjectRed else EjectOutlineVar),
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(step.title, color = EjectOnSurface,
                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(8.dp))
            Text(step.body, color = EjectSecondary,
                fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp)
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = strings.coachmarkSkip,
                    color = EjectSecondary,
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onSkip() }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(14.dp)).background(EjectRed)
                        .clickable { onNext() }
                        .padding(horizontal = 22.dp, vertical = 11.dp),
                ) {
                    Text(step.primaryLabel, color = Color.White,
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.4.sp)
                }
            }
        }
    }
}
