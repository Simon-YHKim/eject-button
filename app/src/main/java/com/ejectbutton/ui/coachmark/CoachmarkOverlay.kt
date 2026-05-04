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
import androidx.compose.ui.graphics.drawscope.Stroke
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
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = null,
            ) { /* swallow taps */ }
            .drawWithContent {
                drawContent()
                drawRect(Color.Black.copy(alpha = 0.74f))
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

private fun placeTooltip(
    spot: Spot?, screenW: Float, screenH: Float, density: androidx.compose.ui.unit.Density,
): Placement {
    val sidePad = 16.dp
    val cardH = 220.dp
    val gap = 20.dp
    val rect = spot?.rect ?: return Placement(120.dp, sidePad)
    return with(density) {
        val centerY = rect.center.y
        when {
            centerY < screenH / 2 -> Placement(rect.bottom.toDp() + gap, sidePad)
            else -> Placement((rect.top.toDp() - gap - cardH).coerceAtLeast(80.dp), sidePad)
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
