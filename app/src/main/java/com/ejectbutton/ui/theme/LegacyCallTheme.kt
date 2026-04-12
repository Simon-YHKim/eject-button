package com.ejectbutton.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 가짜 전화 화면 전용 격리 테마.
 *
 * 전화 화면 (FakeIncomingCallScreen / FakeInCallScreen) 은 본래 모든 색상·
 * 모양을 하드코딩으로 그려내도록 설계되어 있다. Tactical Cockpit 테마 적용
 * 이후에도 전화 화면만큼은 원본 모습을 그대로 유지하기 위해, Material3
 * 기본값 (라이트 컬러 스킴 + 기본 Typography + 기본 Shapes) 만으로 래핑한다.
 *
 * 이렇게 하면 새 Tactical 팔레트/0dp Shapes/Typography 가 전화 화면에 간접
 * 적용되는 경로 (Text 기본 color, Button/Card 모양, letterSpacing 등) 가
 * 전부 차단된다.
 */
@Composable
fun LegacyCallTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(),
        content     = content,
    )
}
