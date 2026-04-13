package com.ejectbutton.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.ejectbutton.data.ThemeMode
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// ── Tactical Cockpit 디자인 시스템 토큰 ────────────────────────────────────────
// 전투기 계기판 기반의 Machined Cockpit Interface 팔레트.
// 색상 희소성: Red/Yellow/Cyan 은 경고/강조에만 — 나머지는 무채색 그레이.

// Surface hierarchy — 심도감은 Surface 레이어로만 표현
val TacticalLowest    = Color(0xFF0C0E10)  // 최하위 — 구분선 역할 (1dp band)
val TacticalBase      = Color(0xFF121416)  // background
val TacticalLow       = Color(0xFF1A1C1E)  // container-low
val TacticalContainer = Color(0xFF1E2022)  // container (card body)
val TacticalHigh      = Color(0xFF282A2C)  // container-high
val TacticalHighest   = Color(0xFF333537)  // container-highest (housing)
val TacticalBright    = Color(0xFF37393B)  // surface-bright (press hit)

// Accents — 희소 사용
val TacticalRed       = Color(0xFFFFB3AC)
val TacticalRedDeep   = Color(0xFF6A0008)  // EJECT 버튼 바디
val TacticalRedInv    = Color(0xFFBA1A20)
val TacticalRedFixed  = Color(0xFFFFDAD6)
val TacticalOnRed     = Color(0xFF680008)
val TacticalYellow    = Color(0xFFF8BD2A)  // hazard stripes / SIDE_BUTTON
val TacticalCyan      = Color(0xFF00DAF3)  // LED / SHAKE / section bar

// Text / outline
val TacticalOnSurface = Color(0xFFE2E2E5)
val TacticalOnVariant = Color(0xFFC6C6CB)
val TacticalOutline   = Color(0xFF909095)
val TacticalOutlineVar= Color(0xFF45474B)

// ── Legacy 토큰 alias — 기존 화면 파일이 참조하는 이름을 그대로 보존 ───────────
// 이름은 유지하되 값을 Tactical 팔레트로 remap 해서 전 화면을 다크 테마화.
val EjectRed            = TacticalRedInv
val EjectCoral          = TacticalRedInv          // EJECT crimson
val EjectCoralDim       = TacticalRedDeep

val EjectBg             = TacticalBase
val EjectSurface        = TacticalContainer       // cards
val EjectSurfaceLow     = TacticalLow             // modules
val EjectSurfaceMid     = TacticalHigh            // container
val EjectSurfaceHigh    = TacticalHigh
val EjectSurfaceHighest = TacticalHighest

val EjectOnSurface    = TacticalOnSurface
val EjectSecondary    = TacticalOnVariant
val EjectOutlineVar   = TacticalOutlineVar

val EjectSecContainer       = TacticalHigh
val EjectPrimaryContainer   = TacticalLowest        // PRO 배너 다크 바탕
val EjectOnPrimaryContainer = TacticalOnVariant
val EjectSecondaryContainer = TacticalRedInv

private val TacticalDarkColors = darkColorScheme(
    primary             = TacticalRedInv,
    onPrimary           = Color.White,
    primaryContainer    = TacticalLowest,
    onPrimaryContainer  = TacticalOnVariant,
    secondary           = TacticalOnVariant,
    onSecondary         = TacticalBase,
    secondaryContainer  = TacticalHigh,
    onSecondaryContainer= TacticalOnSurface,
    tertiary            = TacticalCyan,
    onTertiary          = TacticalBase,
    background          = TacticalBase,
    onBackground        = TacticalOnSurface,
    surface             = TacticalContainer,
    onSurface           = TacticalOnSurface,
    surfaceVariant      = TacticalHigh,
    onSurfaceVariant    = TacticalOnVariant,
    outline             = TacticalOutline,
    outlineVariant      = TacticalOutlineVar,
    error               = TacticalRed,
    onError             = TacticalOnRed,
    errorContainer      = TacticalRedDeep,
    onErrorContainer    = TacticalRedFixed,
)

// ── Tactical Typography — Space Grotesk 의존성 없이 시스템 폰트 + 타이트한
// letter-spacing 과 FontWeight 로 "cockpit" 느낌 재현 ───────────────────────
private val Mono = FontFamily.Monospace
private val San  = FontFamily.SansSerif

val TacticalTypography = Typography(
    displayLarge  = TextStyle(fontFamily = San,  fontWeight = FontWeight.Black,     fontSize = 48.sp, letterSpacing = (-0.02).em),
    displayMedium = TextStyle(fontFamily = San,  fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, letterSpacing = (-0.02).em),
    displaySmall  = TextStyle(fontFamily = San,  fontWeight = FontWeight.ExtraBold, fontSize = 32.sp, letterSpacing = 0.02.em),
    headlineLarge = TextStyle(fontFamily = San,  fontWeight = FontWeight.Bold,      fontSize = 26.sp, letterSpacing = 0.02.em),
    headlineMedium= TextStyle(fontFamily = San,  fontWeight = FontWeight.Bold,      fontSize = 22.sp, letterSpacing = 0.02.em),
    headlineSmall = TextStyle(fontFamily = San,  fontWeight = FontWeight.Bold,      fontSize = 18.sp, letterSpacing = 0.05.em),
    titleLarge    = TextStyle(fontFamily = San,  fontWeight = FontWeight.Bold,      fontSize = 18.sp, letterSpacing = 0.05.em),
    titleMedium   = TextStyle(fontFamily = San,  fontWeight = FontWeight.SemiBold,  fontSize = 15.sp, letterSpacing = 0.08.em),
    titleSmall    = TextStyle(fontFamily = San,  fontWeight = FontWeight.SemiBold,  fontSize = 13.sp, letterSpacing = 0.1.em),
    labelLarge    = TextStyle(fontFamily = San,  fontWeight = FontWeight.SemiBold,  fontSize = 13.sp, letterSpacing = 0.12.em),
    labelMedium   = TextStyle(fontFamily = San,  fontWeight = FontWeight.SemiBold,  fontSize = 11.sp, letterSpacing = 0.15.em),
    labelSmall    = TextStyle(fontFamily = San,  fontWeight = FontWeight.SemiBold,  fontSize = 10.sp, letterSpacing = 0.18.em),
    bodyLarge     = TextStyle(fontFamily = San,  fontWeight = FontWeight.Normal,    fontSize = 15.sp),
    bodyMedium    = TextStyle(fontFamily = San,  fontWeight = FontWeight.Normal,    fontSize = 14.sp),
    bodySmall     = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Medium,    fontSize = 12.sp, letterSpacing = 0.05.em),
)

// ── 0dp 모서리 전역 강제 — 둥근 모서리 금지 ─────────────────────────────────
// Material3 Shapes 는 CornerBasedShape 만 허용 → RoundedCornerShape(0.dp) 로 구현.
private val Square = RoundedCornerShape(0.dp)
private val TacticalShapes = Shapes(
    extraSmall = Square,
    small      = Square,
    medium     = Square,
    large      = Square,
    extraLarge = Square,
)

// Light mode 용 ColorScheme — Tactical Cockpit 은 기본 다크 디자인이므로
// 라이트 모드는 Material3 기본 lightColorScheme 기반으로 최소한의 대비만 제공.
// (앱 내부 팔레트는 여전히 하드코딩된 다크 토큰을 사용 — 이는 의도된 디자인이며
// 라이트 모드는 Material3 컴포넌트 기본값에만 영향을 준다.)
private val TacticalLightColors = lightColorScheme(
    primary        = TacticalRedInv,
    onPrimary      = Color.White,
    background     = Color(0xFFF7F7F8),
    onBackground   = Color(0xFF1A1C1E),
    surface        = Color(0xFFFFFFFF),
    onSurface      = Color(0xFF1A1C1E),
)

@Composable
fun EjectButtonTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) TacticalDarkColors else TacticalLightColors,
        typography  = TacticalTypography,
        shapes      = TacticalShapes,
        content     = content,
    )
}
