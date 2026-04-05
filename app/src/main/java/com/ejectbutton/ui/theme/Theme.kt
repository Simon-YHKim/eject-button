package com.ejectbutton.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Stitch (5) 디자인 시스템 토큰 ────────────────────────────────────────────

// Primary
val EjectRed          = Color(0xFFB9051B)  // primary  — 딥 레드 (텍스트 강조, 탭 활성)
val EjectCoral        = Color(0xFFFF766E)  // primary-container — 코랄 (EJECT 버튼, 선택 트리거 배경)
val EjectCoralDim     = Color(0xFFA40015)  // primary-dim — 어두운 레드

// Surface hierarchy (Stitch "inverted lift" — 카드가 배경보다 밝음)
val EjectBg           = Color(0xFFF7F6F4)  // surface / background
val EjectSurface      = Color(0xFFFFFFFF)  // surface-container-lowest — 카드
val EjectSurfaceLow   = Color(0xFFF1F1EF)  // surface-container-low
val EjectSurfaceMid   = Color(0xFFE8E8E6)  // surface-container
val EjectSurfaceHigh  = Color(0xFFE2E2E0)  // surface-container-high
val EjectSurfaceHighest = Color(0xFFDDDDDB) // surface-container-highest / surface-variant

// Text
val EjectOnSurface    = Color(0xFF2E2F2E)  // on-surface — 기본 텍스트 (pure black 금지)
val EjectSecondary    = Color(0xFF5A5C5C)  // secondary / on-surface-variant — 보조 텍스트
val EjectOutlineVar   = Color(0xFFADADAB)  // outline-variant — 비활성 테두리

// Semantic
val EjectSecContainer = Color(0xFFE2E2E2)  // secondary-container — 칩 비선택 배경

private val LightColors = lightColorScheme(
    primary          = EjectRed,
    secondary        = EjectSecondary,
    background       = EjectBg,
    surface          = EjectSurface,
    surfaceVariant   = EjectSurfaceHighest,
    onPrimary        = Color.White,
    onBackground     = EjectOnSurface,
    onSurface        = EjectOnSurface,
    onSurfaceVariant = EjectSecondary,
    outline          = Color(0xFF767775),
    outlineVariant   = EjectOutlineVar,
)

@Composable
fun EjectButtonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
