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
// v1.6.0: 브랜드 톤을 빨강 → 네이비로 재정렬 (앱 아이콘 = Deep Navy + Cream).
// 변수명은 호환을 위해 유지하되 값은 네이비. error 시맨틱은 ColorScheme.error
// 슬롯에 빨강 hex 명시 고정 (TacticalRed* 참조 금지).
val TacticalRed       = Color(0xFFA8B5CC)  // soft navy (구 FFB3AC)
val TacticalRedDeep   = Color(0xFF0A1525)  // deep navy — EJECT 음영
val TacticalRedInv    = Color(0xFF1B2D4A)  // brand navy — primary CTA
val TacticalRedFixed  = Color(0xFFE0E5F0)  // light navy accent
val TacticalOnRed     = Color(0xFF0A1525)  // on-navy ink
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

// Dark mode 톤 — v1.6.0: Cockpit Dark → Navy Dark + Cream CTA (라이트와 인버전).
// 라이트 모드의 deep-navy CTA 가 다크 모드에서 cream 으로 반전되며 "달처럼 빛나는"
// 시각 효과를 의도. 다크 배경 자체도 near-black → deep navy 로 톤 정렬.
private val DarkEjectPalette = EjectPalette(
    red                = Color(0xFFF2EDE0),   // cream CTA (라이트와 인버전)
    coral              = Color(0xFFF2EDE0),
    coralDim           = Color(0xFFD8CDA9),   // 어두운 크림 — 그림자/음영
    bg                 = Color(0xFF0A1525),   // deep navy background
    surface            = Color(0xFF13223D),   // navy surface (card body)
    surfaceLow         = Color(0xFF0F1A2E),
    surfaceMid         = Color(0xFF1F324F),
    surfaceHigh        = Color(0xFF1F324F),
    surfaceHighest     = Color(0xFF2A4063),
    onSurface          = Color(0xFFE5E0D0),   // warm cream ink
    secondary          = Color(0xFFA8B0BD),   // slate
    outlineVar         = Color(0xFF3A4A66),   // navy line (디바이더 전용)
    secContainer       = Color(0xFF1F324F),
    primaryContainer   = Color(0xFF050A18),   // deepest navy — MAYDAY 카드
    onPrimaryContainer = Color(0xFFA8B0BD),
    secondaryContainer = Color(0xFFF2EDE0),   // cream
)

// Light mode 톤 — v1.6.0: Cool gray → Warm cream + Navy ink.
// 앱 아이콘(Deep Navy + Cream)과 톤 정렬. 강조색은 위 TacticalRed* 상수가 네이비로 변경됨.
private val LightEjectPalette = EjectPalette(
    red                = TacticalRedInv,
    coral              = TacticalRedInv,
    coralDim           = TacticalRedDeep,
    bg                 = Color(0xFFF2EDE0),  // page background — warm cream
    surface            = Color(0xFFFBF7EC),  // card body — soft cream
    surfaceLow         = Color(0xFFEDE7D4),  // module bg
    surfaceMid         = Color(0xFFDFD8C2),  // container high
    surfaceHigh        = Color(0xFFDFD8C2),
    surfaceHighest     = Color(0xFFCFC6AC),  // housing
    onSurface          = Color(0xFF1B2D4A),  // primary text — navy ink
    secondary          = Color(0xFF5A6478),  // secondary text — slate
    outlineVar         = Color(0xFFD5D0BD),  // dividers — warm beige
    secContainer       = Color(0xFFDFD8C2),
    primaryContainer   = Color(0xFF0F1929),  // MAYDAY 카드 — deep navy
    onPrimaryContainer = Color(0xFFD5DCE8),  // cream-tinted text on deep navy
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

// Dark mode 용 ColorScheme — v1.6.0: Navy BG + Cream CTA 인버전.
private val TacticalDarkColors = darkColorScheme(
    primary             = Color(0xFFF2EDE0),    // cream CTA
    onPrimary           = Color(0xFF0A1525),    // deep navy on cream
    primaryContainer    = Color(0xFF050A18),    // deepest navy
    onPrimaryContainer  = Color(0xFFE5E0D0),
    secondary           = Color(0xFFA8B0BD),    // slate
    onSecondary         = Color(0xFF0A1525),
    secondaryContainer  = Color(0xFF1F324F),
    onSecondaryContainer= Color(0xFFE5E0D0),
    tertiary            = TacticalCyan,         // 보존 (SHAKE 시그니처)
    onTertiary          = Color(0xFF0A1525),
    background          = Color(0xFF0A1525),    // deep navy background
    onBackground        = Color(0xFFE5E0D0),
    surface             = Color(0xFF13223D),    // navy surface
    onSurface           = Color(0xFFE5E0D0),
    surfaceVariant      = Color(0xFF1F324F),
    onSurfaceVariant    = Color(0xFFA8B0BD),
    outline             = Color(0xFF6A7B96),
    outlineVariant      = Color(0xFF3A4A66),
    // ── error 슬롯: 빨강 명시 고정 — error 시맨틱 보존 ─────────────────────
    error               = Color(0xFFFFB4A8),
    onError             = Color(0xFF680008),
    errorContainer      = Color(0xFF6A0008),
    onErrorContainer    = Color(0xFFFFDAD6),
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

// Light mode 용 ColorScheme — v1.6.0: Navy + Cream 톤 정렬.
// 앱 내부 화면은 위의 [LightEjectPalette] 를 통해 팔레트 전체가 반전된다.
// error 슬롯은 TacticalRed* 참조 대신 빨강 hex 로 명시 고정 — error 시맨틱 보존.
private val TacticalLightColors = lightColorScheme(
    primary             = TacticalRedInv,           // navy
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFF0F1929),        // deep navy
    onPrimaryContainer  = Color(0xFFD5DCE8),
    secondary           = Color(0xFF5A6478),        // slate
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFDFD8C2),        // warm beige
    onSecondaryContainer= Color(0xFF1B2D4A),
    tertiary            = TacticalCyan,             // 보존 (SHAKE 시그니처)
    onTertiary          = Color(0xFF1B2D4A),
    background          = Color(0xFFF2EDE0),        // warm cream
    onBackground        = Color(0xFF1B2D4A),
    surface             = Color(0xFFFBF7EC),
    onSurface           = Color(0xFF1B2D4A),
    surfaceVariant      = Color(0xFFDFD8C2),
    onSurfaceVariant    = Color(0xFF5A6478),
    outline             = Color(0xFF9098A8),
    outlineVariant      = Color(0xFFD5D0BD),
    // ── error 슬롯: 빨강 명시 고정 (TacticalRed* 참조 금지) ─────────────────
    error               = Color(0xFFBA1A20),
    onError             = Color.White,
    errorContainer      = Color(0xFFFFDAD6),
    onErrorContainer    = Color(0xFF680008),
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
