package com.ejectbutton.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ejectbutton.data.LocalAppStrings
import com.ejectbutton.ui.theme.EjectBg
import com.ejectbutton.ui.theme.EjectCoral
import com.ejectbutton.ui.theme.EjectOnSurface
import com.ejectbutton.ui.theme.EjectOutlineVar
import com.ejectbutton.ui.theme.EjectSecondary
import com.ejectbutton.ui.theme.EjectSurface
import com.ejectbutton.ui.theme.EjectSurfaceMid

/**
 * 첫 실행 시 보여주는 사용 설명 튜토리얼.
 * 4 페이지 (Welcome / Command / Trigger / Systems) + 마지막 "Mayday" 확인 페이지.
 *
 * 마지막 페이지의 두 버튼:
 *  - [onDoneNoMore]  — 다시 보지 않음 (EjectPrefs.saveShowOnboarding(false)).
 *                      v1.2: stepCount 인자로 사용자가 도달한 페이지 수 (1..totalSteps)
 *                      를 함께 전달 → onboarding funnel 분석.
 *  - [onDoneOnceMore] — 다음 실행 시 또 띄움 (pref 유지). 호출부가 생략하면 no-op.
 *                       v1.2.2-test.2 부터: 시각 회귀 fix 로 인해 onboarding 안에서는
 *                       내부 index 0 복귀로 처리. 외부 콜백은 옵셔널 호환용으로 유지.
 *
 * 설정에서 "사용 설명 보기" 토글을 다시 켜면 재활성화된다.
 */
@Composable
fun OnboardingScreen(
    onDoneNoMore: (stepCount: Int) -> Unit,
    onDoneOnceMore: () -> Unit = {},
) {
    val strings = LocalAppStrings.current

    // v1.5.17 — onboarding 아이콘 매핑 갱신 (사용자 피드백 반영).
    //  page 1 (Welcome)   : ⏏  → 🚨  (사이렌, 브랜드 컨셉 = 비상)
    //  page 2 (Command)   : 🎯 → 🏃  (달리는 사람, "탈출" 메타포)
    //  page 3 (Trigger)   : 🚨 → 📞  (전화기, "가짜 통화 발사" 메타포)
    //  page 4 (Systems)   : ⚙ 유지   (앱 내 설정 아이콘 ⚙ 와 일관)
    //  page 5 (final)     : 🚨 → Image(R.drawable.ic_eject_button)
    //                       — OnboardingFinalContent 에서 별도 분기 (Mayday 페이지)
    val pages = remember(strings) {
        listOf(
            OnboardingPage(
                emoji = "🚨",
                title = strings.onboardingWelcomeTitle,
                body  = strings.onboardingWelcomeBody,
            ),
            OnboardingPage(
                emoji = "🏃",
                title = strings.onboardingCommandTitle,
                body  = strings.onboardingCommandBody,
            ),
            OnboardingPage(
                emoji = "📞",
                title = strings.onboardingTriggerTitle,
                body  = strings.onboardingTriggerBody,
            ),
            OnboardingPage(
                emoji = "⚙",
                title = strings.onboardingSystemsTitle,
                body  = strings.onboardingSystemsBody,
            ),
        )
    }
    val totalSteps = pages.size + 1 // +1 for final Mayday page
    var index by remember { mutableIntStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EjectBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(totalSteps) {
                // Round 11 — 누적 드래그 방식으로 전환. 드래그 중엔 아무 동작도
                // 하지 않고 손가락을 뗐을 때(onDragEnd) 한 번만 판정 → 빠른
                // 플릭이어도 무조건 한 페이지만 이동.
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart  = { totalDrag = 0f },
                    onDragCancel = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag < -60f && index < totalSteps - 1) {
                            index += 1
                        } else if (totalDrag > 60f && index > 0) {
                            index -= 1
                        }
                        totalDrag = 0f
                    },
                ) { _, dragAmount ->
                    totalDrag += dragAmount
                }
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top skip row (only shown on non-final pages)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                if (index < pages.size) {
                    TextButton(onClick = { index = pages.size }) {
                        Text(
                            strings.onboardingSkip,
                            color = EjectSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,
                        )
                    }
                } else {
                    Spacer(Modifier.height(40.dp))
                }
            }

            // Main content area — AnimatedContent gives slide transitions between pages
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = index,
                    transitionSpec = {
                        val goingForward = targetState > initialState
                        val dir = if (goingForward) 1 else -1
                        (slideInHorizontally(tween(260)) { w -> dir * w } +
                            fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(tween(260)) { w -> -dir * w } +
                                fadeOut(tween(180)))
                    },
                    label = "onboarding-slide",
                    modifier = Modifier.fillMaxSize(),
                ) { i ->
                    if (i < pages.size) {
                        OnboardingPageContent(pages[i])
                    } else {
                        // v1.2.2-test.2 — 두 버튼(옛썰! / 잘못들었습니다?)을 outer
                        // Column 의 bottom action area 로 hoist 했다. AnimatedContent
                        // 람다 내부에서 outer state(`index`)를 mutate 하면 transition
                        // 진행 중 일부 recomposition trigger 가 누락되는 회귀가 있어서
                        // (Test 1 — onRestart 무반응) 클릭 핸들러를 outer scope 로
                        // 옮겨 람다 indirection 을 제거했다. 여기엔 시각 요소만 남는다.
                        OnboardingFinalContent()
                    }
                }
            }

            // Progress dots
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(totalSteps) { dotIdx ->
                    val active = dotIdx == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (active) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) EjectCoral else EjectSurfaceMid),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Bottom action area — 페이지 별 버튼.
            // v1.2.2-test.2: final page 의 "옛썰!" / "잘못들었습니다?" 도 여기로 hoist.
            // 클릭 람다(`{ onDoneNoMore(totalSteps) }`, `{ index = 0 }`) 가
            // OnboardingScreen 스코프에서 직접 실행되므로 AnimatedContent 람다
            // capture 의 회귀 영향권 밖이다.
            if (index < pages.size) {
                Button(
                    onClick = { if (index < pages.size) index += 1 },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EjectCoral,
                        contentColor   = Color.White,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    Text(
                        strings.onboardingNext,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    )
                }
            } else {
                // Final page primary action — 온보딩 종료 + pref 저장.
                Button(
                    onClick = { onDoneNoMore(totalSteps) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EjectCoral,
                        contentColor   = Color.White,
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    Text(
                        strings.onboardingFinalDismiss,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                // Final page secondary action — 첫 페이지로 복귀.
                OutlinedButton(
                    onClick = { index = 0 },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    Text(
                        strings.onboardingFinalRepeat,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = EjectSecondary,
                        letterSpacing = 0.5.sp,
                    )
                }
            }
        }
    }
}

private data class OnboardingPage(
    val emoji: String,
    val title: String,
    val body: String,
)

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(136.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(EjectSurface)
                .border(1.dp, EjectOutlineVar, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center,
        ) {
            // v1.5.19 — page 4 (⚙ Systems) 만 앱 내 설정 아이콘 (Material Icons.Filled.Settings)
            // 로 분기. 이전엔 emoji "⚙" 가 시스템 폰트 글리프라 다른 step (사이렌/사람/전화기/
            // 앱 아이콘 픽토그램) 과 시각 톤 차이가 컸음. 사용자 피드백 "앱 내 설정 아이콘
            // 사용" 반영. tint 는 page 1~3 emoji 색과 비슷한 EjectOnSurface (정상 진하기).
            if (page.emoji == "⚙") {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = EjectOnSurface,
                )
            } else {
                Text(page.emoji, fontSize = 64.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text       = page.title,
            fontSize   = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = EjectOnSurface,
            letterSpacing = 1.sp,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text       = page.body,
            fontSize   = 15.sp,
            color      = EjectSecondary,
            fontWeight = FontWeight.Medium,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
    }
}

/**
 * v1.2.2-test.2 — 시각 요소만 (emoji + MAYDAY 타이틀 + 질문 텍스트).
 * 두 버튼은 OnboardingScreen 의 bottom action area 에서 직접 렌더한다.
 */
@Composable
private fun OnboardingFinalContent() {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // v1.5.17 — 마지막 페이지 아이콘 = 앱 아이콘 (사용자 자산 ic_eject_button) 으로 변경.
        // 이전 🚨 emoji 는 page 1 (Welcome) 으로 이동했고, 여기는 "비상탈출 완료 — 이제
        // 시작" 마무리 페이지라 앱 아이덴티티를 직접 노출하는 게 자연스럽다.
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(EjectCoral.copy(alpha = 0.12f))
                .border(2.dp, EjectCoral, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(
                    com.ejectbutton.R.drawable.ic_eject_button
                ),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text       = strings.onboardingFinalTitle,
            fontSize   = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = EjectCoral,
            letterSpacing = 2.sp,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text       = strings.onboardingFinalQuestion,
            fontSize   = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color      = EjectOnSurface,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
    }
}
