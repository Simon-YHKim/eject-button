package com.ejectbutton.ui.call

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.ejectbutton.data.LocalAppStrings
import com.microsoft.clarity.modifiers.clarityMask
import kotlin.math.roundToInt
import kotlin.math.sqrt

// ──────────────────────────────────────────────────────────────────────────
//  FakeIncomingCallScreenV2
//  One UI 8.5 style (April 2026) incoming call screen.
//
//  INTERACTIONS (both supported, matching One UI 8.5):
//   • Tap   — instantly triggers onAccept / onDecline
//   • Drag  — drag the button past threshold, triggers on release;
//             button follows finger while dragging and snaps back if
//             released below threshold.
//
//  VISUALS:
//   • Pastel radial-blob gradient background
//   • Centered caller info, "Call Assist" pill, Accept / Decline buttons,
//     and "Send message" swipe hint
//   • Continuous pulse rings emitted from each call button
//
//  All copy is pulled from LocalAppStrings, so every language you already
//  support (en / ko / zh-CN / zh-TW / ja / es / hi) is covered automatically.
// ──────────────────────────────────────────────────────────────────────────

private val IncomingGradientBase = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF0F0A1A), Color(0xFF1A1528), Color(0xFF2B2140),
        Color(0xFF3D2B4A), Color(0xFF5A4352), Color(0xFF8A7A6A),
    )
)

@Composable
fun FakeIncomingCallScreenV2(
    callerName: String,
    callerLabel: String,
    onDecline: () -> Unit,
    onAccept: () -> Unit,
    pulseDurationMs: Int = 1800,
    pulseMaxScale: Float = 2.2f,
    pulseLayers: Int = 2,
    pulseColor: Color = Color.White.copy(alpha = 0.22f),
    dragThresholdPx: Float = 120f,
) {
    val strings = LocalAppStrings.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(IncomingGradientBase)
            .drawBehind {
                val w = size.width
                val h = size.height
                drawBlob(Color(0xFF5A6EB4).copy(alpha = 0.55f), Offset(w * -0.05f, h * 0.05f), w * 0.55f)
                drawBlob(Color(0xFFD2826E).copy(alpha = 0.50f), Offset(w * 1.05f, h * 0.55f), w * 0.55f)
                drawBlob(Color(0xFFDCC88C).copy(alpha = 0.45f), Offset(w * 0.95f, h * 1.05f), w * 0.50f)
                drawBlob(Color(0xFF968CB4).copy(alpha = 0.40f), Offset(w * -0.05f, h * 1.05f), w * 0.55f)
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(56.dp))

            Text(strings.incomingCallLabel, fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.72f))

            Spacer(Modifier.height(52.dp))

            // v1.0.1 — Clarity 마스킹: callerName 은 사용자가 등록한 실제 contact 이름
            // (예: "전남편") 일 수 있어 PII. callerLabel 도 실제 전화번호 ("010-XXXX-XXXX")
            // 또는 관계 라벨일 수 있음. Microsoft Clarity 가 ComposeView 녹화 시 노출되지
            // 않도록 마스킹 처리. 화면 다른 부분의 heatmap 분석은 유지됨.
            Text(callerName, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.clarityMask())
            Spacer(Modifier.height(10.dp))
            Text(callerLabel, fontSize = 14.sp, color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.clarityMask())

            Spacer(Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0x8C1E1E28))
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Icon(Icons.Default.AutoAwesome, null, tint = Color.White,
                    modifier = Modifier.size(14.dp))
                Text(strings.callAssist, fontSize = 14.sp, color = Color.White,
                    fontWeight = FontWeight.Medium)
            }

            Spacer(Modifier.height(52.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PulsingCallButton(
                    icon = Icons.Default.Call,
                    color = Color(0xFF22C55E),
                    contentDescription = strings.accept,
                    onTrigger = onAccept,
                    pulseDurationMs = pulseDurationMs,
                    pulseMaxScale = pulseMaxScale,
                    pulseLayers = pulseLayers,
                    pulseColor = pulseColor,
                    dragThresholdPx = dragThresholdPx,
                )
                PulsingCallButton(
                    icon = Icons.Default.CallEnd,
                    color = Color(0xFFEF4444),
                    contentDescription = strings.decline,
                    onTrigger = onDecline,
                    pulseDurationMs = pulseDurationMs,
                    pulseMaxScale = pulseMaxScale,
                    pulseLayers = pulseLayers,
                    pulseColor = pulseColor,
                    dragThresholdPx = dragThresholdPx,
                )
            }

            Spacer(Modifier.height(36.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(56.dp).height(2.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.6f))
                )
                Spacer(Modifier.height(10.dp))
                Text(strings.sendMessage, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f))
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun PulsingCallButton(
    icon: ImageVector,
    color: Color,
    contentDescription: String,
    onTrigger: () -> Unit,
    pulseDurationMs: Int,
    pulseMaxScale: Float,
    pulseLayers: Int,
    pulseColor: Color,
    dragThresholdPx: Float,
) {
    // Drag state
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var triggered by remember { mutableStateOf(false) }
    val dragging = offsetX != 0f || offsetY != 0f
    val distance = sqrt(offsetX * offsetX + offsetY * offsetY)

    // v1.4.3 — drag fraction (0.0 ~ 1.0) for Lottie progress.
    val dragFraction = (distance / dragThresholdPx).coerceIn(0f, 1f)

    Box(
        modifier = Modifier.size(200.dp),  // larger box to accommodate Lottie ripple
        contentAlignment = Alignment.Center,
    ) {
        // v1.4.3 — Lottie drag-to-confirm rings overlay (drag 중일 때만)
        if (dragging && !triggered) {
            val lottieComp by rememberLottieComposition(
                LottieCompositionSpec.Asset("animations/drag_confirm.json")
            )
            LottieAnimation(
                composition = lottieComp,
                progress = { dragFraction },
                modifier = Modifier.size(200.dp),
            )
        }

        // Pulses only when idle (looks cleaner mid-drag)
        if (!dragging && !triggered) {
            for (i in 0 until pulseLayers) {
                PulseRing(
                    durationMs = pulseDurationMs,
                    maxScale = pulseMaxScale,
                    color = pulseColor,
                    delayFraction = i.toFloat() / pulseLayers,
                )
            }
        }

        val interaction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                // v1.5.2 — 사용자 명시: "사용자 터치 따라가지 않게 + 버튼 고정시키고 애니메이션만 작동".
                // 기존의 .offset { IntOffset(offsetX, offsetY) } 제거. 버튼은 절대 고정.
                // 드래그 인텐트는 dragFraction (= 거리/threshold) → Lottie ring progress 만 시각화.
                // 위치/크기 (72dp / 200dp outer Box / Row padding 56dp) 는 사용자 명시로 그대로.
                .size(72.dp)
                .clip(CircleShape)
                .background(color)
                // Tap — 즉시 trigger (위험 상황 빠른 받기 시나리오 유지)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                ) {
                    if (!triggered) {
                        triggered = true
                        onTrigger()
                    }
                }
                // Drag — 의도 추적만 (offsetX/Y 누적은 유지하지만 .offset 으로 그리지 않음).
                // 거리 ≥ threshold 면 release 시 trigger, 아니면 reset.
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, drag ->
                            if (!triggered) {
                                change.consume()
                                offsetX += drag.x
                                offsetY += drag.y
                            }
                        },
                        onDragEnd = {
                            if (!triggered) {
                                val dist = sqrt(offsetX * offsetX + offsetY * offsetY)
                                if (dist >= dragThresholdPx) {
                                    triggered = true
                                    onTrigger()
                                } else {
                                    offsetX = 0f; offsetY = 0f
                                }
                            }
                        },
                        onDragCancel = {
                            if (!triggered) { offsetX = 0f; offsetY = 0f }
                        }
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

@Composable
private fun PulseRing(
    durationMs: Int,
    maxScale: Float,
    color: Color,
    delayFraction: Float,
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val raw by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulsePhase",
    )
    val phase = ((raw + delayFraction) % 1f)
    val scale = 1f + (maxScale - 1f) * phase
    val alpha = (0.55f * (1f - phase)).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(color)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBlob(
    color: Color, center: Offset, radius: Float,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}
