# Claude Design 프롬프트 — BottomBar 인디케이터 부드러운 전환 (v1.5.1)

> **사용법**: 이 파일 전체를 복사해서 Claude (claude.ai) 또는 design-shotgun 에 붙여넣으세요.
> 산출물은 SVG 또는 lottie 또는 Compose 코드 권고.

---

## 컨텍스트

Android Compose 앱 "Eject Button" (한국어 명: 비상 탈출). 메인 화면 하단에 3-탭 BottomBar 가 있습니다.
- `COMMAND` (명령) — 가짜 통화 발사
- `HISTORY` (탈출 기록)
- `SYSTEMS` (설정)

사용자가 좌우로 화면을 스와이프하면 `AnimatedContent` + `slideInHorizontally` 로
**메인 컨텐츠는 부드럽게 슬라이드**하지만, **BottomBar 의 active 탭 회색 음영은 step function 처럼 jump** 합니다.

사용자 요구: "on/off 보다는 부드럽게, 연속성 있게 넘어가게"

---

## 현재 동작 (v1.5.0 — `MainScreen.kt` line 1743~1787)

```kotlin
Row(
    // ... pill background, shadow, border
) {
    listOf(COMMAND, HISTORY, SYSTEMS).forEach { (screen, label) ->
        val isActive = screen == current  // ← boolean step
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(50))
                .background(if (isActive) EjectSurfaceMid else Color.Transparent)  // ← 갑자기 on/off
                .clickable { onSelect(screen) }
                .padding(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Text(
                text = label,
                color = if (isActive) EjectOnSurface else EjectSecondary,         // ← 갑자기 on/off
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
            )
        }
    }
}
```

스와이프 / 탭 → `currentScreen` state 가 즉시 다음 값으로 변경 → `if (isActive) EjectSurfaceMid else Color.Transparent` 가 컬러를 0ms 안에 jump → 회색 음영이 한 탭에서 사라지고 다음 탭에 즉시 나타남.

---

## 당신의 작업

### 핵심 결정 — 어떤 전환 패턴이 가장 자연스러운가?

**옵션 1 — Color crossfade (단순)**:
- `animateColorAsState` 로 background / text color 을 200~300ms 페이드.
- 인디케이터가 한 탭에서 사라지면서 다른 탭에 동시에 나타남 (cross-fade).
- 장점: 코드 변경 최소 (1~2줄).
- 단점: "슬라이드 = 인디케이터가 한 탭에서 다른 탭으로 미끄러져 이동" 느낌 X.

**옵션 2 — Sliding pill indicator (권장 후보)**:
- 회색 pill 을 별도 Box 1개로 만들어, 3-탭 중 active 탭 위치로 `animateDpAsState` 또는 `Animatable<Float>` 로 슬라이드.
- iOS UISegmentedControl 식 / Material 3 의 NavigationBar style.
- 장점: 사용자가 보고 싶어 한 "연속성" 명확. 인디케이터가 한 탭에서 다른 탭으로 이동.
- 단점: 코드 구조 약간 변경 (Layout 측정 필요).

**옵션 3 — Drag progress 동기화 (최고급)**:
- 화면 스와이프의 누적 drag 비율 (0~1) 을 BottomBar 까지 hoist.
- 사용자가 손가락으로 천천히 끌면 indicator 도 50% 위치.
- 장점: 화면 슬라이드와 완전 동기. iOS Music app 같은 느낌.
- 단점: state hoisting 깊어짐 (drag totalDrag → MainScreen → BottomBar). v1.5.1 에 진행하기엔 위험.

**옵션 4 — Underline / dot indicator (대안 디자인)**:
- pill 배경 제거, 작은 underline (3dp 코랄 라인) 또는 dot 만 슬라이드.
- 더 미니멀. 현재 EjectSurfaceMid 음영 사용 안 함.

각 옵션의 **장단점 + 권장 옵션**을 답해 주세요. 우리는 **옵션 2** 를 1순위로 보고 있습니다.

### 추가 디자인 결정

1. **Active 텍스트 weight 변경 (ExtraBold ↔ SemiBold) 도 부드럽게?**
   - 현재 `FontWeight` 는 step. `animateIntAsState` 로 weight 보간 가능 (`FontWeight(weight)` 직접 생성). 권장 여부?
2. **Active 텍스트 color 변경 (EjectOnSurface ↔ EjectSecondary) 도 부드럽게?**
   - `animateColorAsState` 로 200ms 권장 여부?
3. **Pill 의 코너 라운드 / 패딩 / 그림자**는 그대로 유지? 아니면 active 시 살짝 강조 (예: shadow elevation 1dp → 2dp)?

---

## 출력 형식

1. **권장 옵션 + 근거** (옵션 1~4 중) — 100~150자
2. **Compose 코드 권고안** — `BottomBar` 함수 변경 diff
   - Sliding pill indicator 구현 (옵션 2 선택 시)
   - `animateColorAsState` / `animateDpAsState` / `Animatable` 사용 패턴
   - layout 측정 (`onGloballyPositioned`) 필요 여부
3. **(선택)** Lottie 또는 SVG mockup — idle / mid-transition / settled 3프레임

---

## 절대 하지 말 것 (가드)

- ❌ EjectSurfaceMid / EjectOnSurface / EjectSecondary 외 새 컬러 도입
- ❌ Material NavigationBar 컴포넌트 도입 (현재는 직접 작성한 pill row 톤이 브랜드 일관성)
- ❌ 코너 라운드 (현재 RoundedCornerShape(50)) 변경 — pill 톤 유지
- ❌ 탭 개수 / 라벨 / 폰트 크기 변경
- ❌ shake/bounce 같은 과한 모션 (앱 톤 = 차분함)

---

## 참고 — 옵션 2 의 골격 (제안 baseline)

```kotlin
// pseudo-code, 검증된 코드 아님

@Composable
private fun BottomBar(current: AppScreen, ...) {
    val tabs = listOf(COMMAND, HISTORY, SYSTEMS)
    val activeIndex = tabs.indexOf(current)

    // 슬라이드 위치 (0f ~ tabCount-1)
    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "bottombar-pill",
    )

    BoxWithConstraints(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        // ... pill background
    ) {
        val tabWidth = (maxWidth - 12.dp) / tabs.size  // 6dp 양옆 padding
        // 슬라이딩 pill (인디케이터)
        Box(
            modifier = Modifier
                .offset(x = tabWidth * animatedIndex + 6.dp)
                .width(tabWidth)
                .height(40.dp)  // 텍스트 + padding 높이
                .clip(RoundedCornerShape(50))
                .background(EjectSurfaceMid),
        )
        // 텍스트 row (음영 위에 얹힘)
        Row(...) {
            tabs.forEachIndexed { idx, screen ->
                val activeFraction = ... // animatedIndex 와 idx 의 거리 기반
                Text(label, color = lerp(EjectSecondary, EjectOnSurface, activeFraction), ...)
            }
        }
    }
}
```

이 골격을 다듬어서 권고해 주세요. 또는 더 나은 구조가 있으면 그걸 제시하세요.
