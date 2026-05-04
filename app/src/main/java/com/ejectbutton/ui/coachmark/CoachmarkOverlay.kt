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
            // v1.5.6 — drawWithContent 순서 변경: dim 먼저 → cutout → drawContent (tooltip).
            // 이전: drawContent() → dim → cutout → tooltip 도 dim 위에 보임... 아니, drawContent
            // 가 children (= tooltip) 그리고 그 위에 dim 그려짐. 결과적으로 tooltip 도 어두워짐.
            // 새 순서: dim 그림 → cutout (spotlight transparent) → drawContent() (tooltip 등).
            // 이러면 tooltip 은 dim 위에 그려져 항상 명확히 보이고, spotlight 영역만 child 보임.
            .drawWithContent {
                // 1. 전체 dim
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
                // v1.5.6 — 마지막에 children (TooltipCard + 진행도 + Skip + 다음 버튼) 그림.
                // 이러면 dim/cutout 위에 tooltip 이 항상 명확히 보임 (사용자 #2 fix).
                drawContent()
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
 * v1.5.6 — Tooltip 카드를 spotlight 와 절대 겹치지 않게 배치 + 화면 비율 기반.
 *
 * 알고리즘:
 *  1) cardH/gap/sidePad/minTop 모두 화면 height 비율 기반 (작은 폰/큰 폰 자동 적응)
 *  2) spotlight 위/아래 여백 비교 후 더 큰 쪽에 배치
 *  3) 카드 화면 밖 coerce
 */
private fun placeTooltip(
    spot: Spot?, screenW: Float, screenH: Float, density: androidx.compose.ui.unit.Density,
): Placement {
    return with(density) {
        val screenHDp = screenH.toDp()
        // v1.5.6 — 화면 비율 기반 (다른 폰 자동 적응):
        val sidePad = (screenW.toDp() * 0.04f).coerceIn(12.dp, 24.dp)   // 좌우 4% (12~24dp)
        val cardH   = (screenHDp * 0.22f).coerceIn(150.dp, 240.dp)      // 카드 ≈ 화면 22% (150~240)
        val gap     = (screenHDp * 0.02f).coerceIn(12.dp, 24.dp)        // spot 과 카드 간격 ≈ 2%
        val minTop  = (screenHDp * 0.08f).coerceIn(56.dp, 96.dp)        // 상단 안전 영역 ≈ 8%

        val rect = spot?.rect ?: return@with Placement(minTop + 56.dp, sidePad)
        val spotTopDp = rect.top.toDp()
        val spotBotDp = rect.bottom.toDp()

        val spaceAbove = (spotTopDp - minTop - gap).coerceAtLeast(0.dp)
        val spaceBelow = (screenHDp - spotBotDp - gap).coerceAtLeast(0.dp)

        if (spaceBelow >= cardH || spaceBelow >= spaceAbove) {
            Placement(spotBotDp + gap, sidePad)
        } else {
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
