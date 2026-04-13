package com.ejectbutton.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
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

// ── Eject Palette — 라이트/다크 모드 간 전환 가능한 토큰 집합 ─────────────────
// 기존에는 다크 톤이 하드코딩되어 테마 전환이 UI 에 반영되지 않았다.
// 이제 [EjectPalette] 에 모든 토큰을 집약하고 [LocalEjectPalette] 로 공급한다.
// 파일 아래쪽의 top-level `val Eject*` 은 `@Composable @ReadOnlyComposable`
// getter 로 바뀌어 현재 테마의 팔레트를 반환한다 — 호출 측 코드는 변경이 없다.

data class EjectPalette(
    val red: Color,
    val coral: Color,
    val coralDim: Color,

    val bg: Color,
    val surface: Color,
    val surfaceLow: Color,
    val surfaceMid: Color,
    val surfaceHigh: Color,
    val surfaceHighest: Color,

    val onSurface: Color,
    val secondary: Color,
    val outlineVar: Color,

    val secContainer: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondaryContainer: Color,
)

private val DarkEjectPalette = EjectPalette(
    red                = TacticalRedInv,
    coral              = TacticalRedInv,
    coralDim           = TacticalRedDeep,
    bg                 = TacticalBase,
    surface            = TacticalContainer,
    surfaceLow         = TacticalLow,
    surfaceMid         = TacticalHigh,
    surfaceHigh        = TacticalHigh,
    surfaceHighest     = TacticalHighest,
    onSurface          = TacticalOnSurface,
    secondary          = TacticalOnVariant,
    outlineVar         = TacticalOutlineVar,
    secContainer       = TacticalHigh,
    primaryContainer   = TacticalLowest,
    onPrimaryContainer = TacticalOnVariant,
    secondaryContainer = TacticalRedInv,
)

// Light mode 톤 — 라이트 배경/어두운 텍스트로 반전. 강조색 (coral/red) 은 공통.
private val LightEjectPalette = EjectPalette(
    red                = TacticalRedInv,
    coral              = TacticalRedInv,
    coralDim           = TacticalRedDeep,
    bg                 = Color(0xFFF4F5F7),  // page background
    surface            = Color(0xFFFFFFFF),  // card body
    surfaceLow         = Color(0xFFEBECEF),  // module bg
    surfaceMid         = Color(0xFFDDDEE2),  // container high
    surfaceHigh        = Color(0xFFDDDEE2),
    surfaceHighest     = Color(0xFFCFD0D5),  // housing
    onSurface          = Color(0xFF1A1C1E),  // primary text
    secondary          = Color(0xFF55575B),  // secondary text
    outlineVar         = Color(0xFFC0C1C5),  // dividers
    secContainer       = Color(0xFFDDDEE2),
    primaryContainer   = Color(0xFF1A1C1E),  // PRO 배너 는 라이트 모드에서도 강한 대비 유지
    onPrimaryContainer = Color(0xFFC6C6CB),
    secondaryContainer = TacticalRedInv,
)

val LocalEjectPalette = staticCompositionLocalOf { DarkEjectPalette }

// ── Legacy top-level 토큰 — 이제는 현재 팔레트에서 읽어오는 getter ────────────
// 호출 지점은 모두 @Composable 문맥이므로 기존 `EjectBg` 사용은 그대로 동작한다.
val EjectRed: Color            @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.red
val EjectCoral: Color          @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.coral
val EjectCoralDim: Color       @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.coralDim

val EjectBg: Color             @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.bg
val EjectSurface: Color        @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.surface
val EjectSurfaceLow: Color     @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.surfaceLow
val EjectSurfaceMid: Color     @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.surfaceMid
val EjectSurfaceHigh: Color    @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.surfaceHigh
val EjectSurfaceHighest: Color @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.surfaceHighest

val EjectOnSurface: Color      @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.onSurface
val EjectSecondary: Color      @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.secondary
val EjectOutlineVar: Color     @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.outlineVar

val EjectSecContainer: Color       @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.secContainer
val EjectPrimaryContainer: Color   @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.primaryContainer
val EjectOnPrimaryContainer: Color @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.onPrimaryContainer
val EjectSecondaryContainer: Color @Composable @ReadOnlyComposable get() = LocalEjectPalette.current.secondaryContainer

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

// Light mode 용 ColorScheme — Material3 컴포넌트 기본값에 최소한의 대비만 제공.
// 앱 내부 화면은 위의 [LightEjectPalette] 를 통해 팔레트 전체가 반전된다.
private val TacticalLightColors = lightColorScheme(
    primary             = TacticalRedInv,
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFF1A1C1E),
    onPrimaryContainer  = Color(0xFFE2E2E5),
    secondary           = Color(0xFF55575B),
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFDDDEE2),
    onSecondaryContainer= Color(0xFF1A1C1E),
    tertiary            = TacticalCyan,
    onTertiary          = Color(0xFF1A1C1E),
    background          = Color(0xFFF4F5F7),
    onBackground        = Color(0xFF1A1C1E),
    surface             = Color(0xFFFFFFFF),
    onSurface           = Color(0xFF1A1C1E),
    surfaceVariant      = Color(0xFFDDDEE2),
    onSurfaceVariant    = Color(0xFF55575B),
    outline             = Color(0xFF909095),
    outlineVariant      = Color(0xFFC0C1C5),
    error               = TacticalRedInv,
    onError             = Color.White,
    errorContainer      = TacticalRedFixed,
    onErrorContainer    = TacticalOnRed,
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
    val palette = if (useDark) DarkEjectPalette else LightEjectPalette
    CompositionLocalProvider(LocalEjectPalette provides palette) {
        MaterialTheme(
            colorScheme = if (useDark) TacticalDarkColors else TacticalLightColors,
            typography  = TacticalTypography,
            shapes      = TacticalShapes,
            content     = content,
        )
    }
}
