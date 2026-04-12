package com.ejectbutton.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ── Eject Utility 디자인 시스템 토큰 ────────────────────────────────────────────

// Primary
val EjectRed          = Color(0xFF000000)  // primary — structural black
val EjectCoral        = Color(0xFFB6191A)  // crimson — EJECT button only
val EjectCoralDim     = Color(0xFF93000B)  // dark crimson

// Surface hierarchy
val EjectBg           = Color(0xFFF7F9FB)  // surface / background
val EjectSurface      = Color(0xFFFFFFFF)  // surface-container-lowest — cards
val EjectSurfaceLow   = Color(0xFFF2F4F6)  // surface-container-low — modules
val EjectSurfaceMid   = Color(0xFFECEEF0)  // surface-container
val EjectSurfaceHigh  = Color(0xFFE6E8EA)  // surface-container-high
val EjectSurfaceHighest = Color(0xFFE0E3E5) // surface-container-highest

// Text
val EjectOnSurface    = Color(0xFF191C1E)  // on-surface — primary text
val EjectSecondary    = Color(0xFF44474C)  // on-surface-variant — secondary text
val EjectOutlineVar   = Color(0xFFC4C6CC)  // outline-variant

// Semantic
val EjectSecContainer = Color(0xFFE6E8EA)  // secondary-container — chips unselected
val EjectPrimaryContainer   = Color(0xFF101B30)  // dark bg for PRO badge
val EjectOnPrimaryContainer = Color(0xFF79849D)
val EjectSecondaryContainer = Color(0xFFD9352F)  // secondary container

private val LightColors = lightColorScheme(
    primary             = EjectRed,
    secondary           = EjectSecondary,
    secondaryContainer  = EjectSecondaryContainer,
    background          = EjectBg,
    surface             = EjectSurface,
    surfaceVariant      = EjectSurfaceHighest,
    onPrimary           = Color.White,
    onBackground        = EjectOnSurface,
    onSurface           = EjectOnSurface,
    onSurfaceVariant    = EjectSecondary,
    outline             = Color(0xFF767775),
    outlineVariant      = EjectOutlineVar,
    primaryContainer    = EjectPrimaryContainer,
    onPrimaryContainer  = EjectOnPrimaryContainer,
)

@Composable
fun EjectButtonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content,
    )
}
