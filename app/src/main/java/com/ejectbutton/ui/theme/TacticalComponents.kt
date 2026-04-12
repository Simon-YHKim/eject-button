package com.ejectbutton.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

// ── Tactical Cockpit 재사용 컴포넌트 ────────────────────────────────────────
// 0dp 모서리, 단색 패널, 얇은 1dp ghost border, 희소 액센트.

/**
 * 전체 화면 배경에 깔리는 Micro-Grid 점 패턴.
 * 20dp 간격, 1dp 반경, TacticalOutline 8% 알파.
 */
fun Modifier.microGridBackground(
    spacing: Dp = 20.dp,
    dotRadius: Dp = 1.dp,
    color: Color = TacticalOutline,
    alpha: Float = 0.08f,
): Modifier = this.drawBehind {
    val spacingPx = spacing.toPx()
    val radiusPx  = dotRadius.toPx()
    val c = color.copy(alpha = alpha)
    var y = 0f
    while (y < size.height) {
        var x = 0f
        while (x < size.width) {
            drawCircle(color = c, radius = radiusPx, center = Offset(x, y))
            x += spacingPx
        }
        y += spacingPx
    }
}

/**
 * 섹션 헤더 — 좌측 4dp × 18dp cyan 바 + 대문자 라벨.
 */
@Composable
fun TacticalSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    accent: Color = TacticalCyan,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(18.dp)
                .background(accent)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text       = title.uppercase(),
            fontSize   = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            color      = TacticalOnVariant,
        )
    }
}

/**
 * Cyan LED 인디케이터 — 6dp 원형 + 대문자 라벨.
 */
@Composable
fun CyanLedIndicator(
    label: String,
    modifier: Modifier = Modifier,
    color: Color = TacticalCyan,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .background(color, shape = androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = color,
        )
    }
}

/**
 * 카드 패널 — TacticalContainer 배경 + 1dp ghost border + 16dp padding.
 */
@Composable
fun TacticalCard(
    modifier: Modifier = Modifier,
    background: Color = TacticalContainer,
    borderColor: Color = TacticalOutlineVar.copy(alpha = 0.4f),
    padding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(background, shape = RectangleShape)
            .border(1.dp, borderColor, RectangleShape)
            .padding(padding)
    ) {
        content()
    }
}

/**
 * 전체 화면 Tactical 배경 — base + micro grid.
 */
@Composable
fun TacticalBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TacticalBase)
            .microGridBackground()
    ) {
        content()
    }
}

/**
 * Hazard Stripe — 1dp TacticalOutlineVar 의 얇은 수평 바 2개
 * (상/하 분할 역할, 0dp 유지).
 */
@Composable
fun TacticalDividerBand(
    modifier: Modifier = Modifier,
    color: Color = TacticalLowest,
    height: Dp = 1.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(color)
    )
}
