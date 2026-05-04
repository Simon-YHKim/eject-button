# Claude Design 통합 프롬프트 — Eject Button v1.5.1 (3개 영역 일괄)

> **Simon 사용법**: 이 파일 전체를 복사 → claude.ai 에 붙여넣기 → 한 세션에 3가지 디자인 답변 모두 받기.
> 산출물: 영역별 SVG mockup + 디자인 결정 + Compose 코드 권고.

---

# 0. 프로젝트 컨텍스트 (3개 영역 공통)

당신은 Android Compose 앱 **"Eject Button"** (한국어 명: **비상 탈출**) 의 디자인 컨설턴트입니다.
회식·어색한 자리·위험 상황에서 가짜 통화로 빠져나오는 fake-call 앱입니다.
**옆 사람 모르게 자연스럽게** 가 핵심 가치이고 7개 언어를 지원합니다 (en/ko/ja/zh-CN/zh-TW/es/hi).

현재 v1.5.0 까지 출시 완료 (코치마크 4-step 골격 푸시됨), v1.5.1 에서 다음 3가지를 동시에 개선합니다.

## 디자인 시스템 (3개 영역 공통, 절대 변경 금지)

### 컬러 토큰 (`ui/theme/`)
- `EjectCoral` ≈ `#FF6B6B` 류 빨강 — 브랜드 강조 / EJECT 버튼
- `EjectBg` — 라이트 = 거의 흰색 / 다크 = 짙은 네이비
- `EjectSurface` — 카드 배경 (라이트 = 미세 회색 / 다크 = 한 단계 밝은 네이비)
- `EjectSurfaceMid` — 비활성 dot, 보조 표면, BottomBar active 음영
- `EjectOnSurface` — 본문 텍스트
- `EjectSecondary` — 보조 텍스트 (회색)
- `EjectOutlineVar` — 외곽선 (얇은 회색)

### 톤 가드
- 시스템 기본 폰트 (Material 3 기본) — 별도 폰트 X
- 강조 = `FontWeight.ExtraBold` + `letterSpacing = 1.sp`
- 본문 = `FontWeight.Medium`
- 코너 라운드: 카드/다이얼로그 16~28dp, 버튼 16dp, 작은 칩 CircleShape, BottomBar `RoundedCornerShape(50)`
- 라이트/다크 모드 둘 다 자연스러워야 함 (v1.5.1 부터 라이트 디폴트)
- minSdk = 26 호환성 깨지 않기

### 절대 하지 말 것 (3개 영역 공통)
- ❌ 새 폰트 / 새 브랜드 컬러 도입
- ❌ 외부 SVG / 이미지 자산 의존 (Compose 네이티브 또는 Lottie 폴더 활용)
- ❌ blur effect 가 안 되는 minSdk 26 호환성 깨기
- ❌ Material NavigationBar / Material Tab 컴포넌트 도입 (현재 직접 작성한 톤 일관성)
- ❌ 영어 fallback (이미 7개 언어 strings 있음)

---

# 영역 A — 코치마크 4-Step 투어 시각 디자인 개선

## 현재 상태
v1.5.0 에서 4-step 코치마크가 메인 화면 위에 자동으로 뜨지만 두 가지 부족:
1. **Spotlight 좌표 미측정** — 현재는 fullscreen dim + 중앙 tooltip 만. 진짜 "버튼을 가리키는" a-ha 모먼트 없음.
2. **디자인 평범** — Card + 빨간 ring 의 단순 조합. 첫 사용자 비주얼 임팩트 부족.

## 4-Step 가리킬 영역

| Step | 가리킬 영역 | 화면 위치 | 크기 |
|---|---|---|---|
| 1 | 시나리오 선택 카드 (`CallerChips`, LazyRow) | 위에서 38% | width = 화면폭, height ≈ 70dp |
| 2 | 트리거 모드 토글 (`TriggerModeRow`) | 위에서 60% | width = 화면폭, height ≈ 56dp |
| 3 | EJECT 큰 빨간 버튼 (`EjectButton`) | 위에서 22% | 196dp × 196dp 원형 |
| 4 | ⚙ 설정 톱니 (`IconButton`) | 우상단 | 40dp × 40dp |

## 텍스트 (이미 7개 언어 다 추가됨, AppStrings.kt)
- Step 1: "시나리오를 골라요" / "어떤 가짜 통화가 올지 미리 정해두세요. 엄마, 아빠, 친구 — 자유롭게 추가할 수 있어요."
- Step 2: "트리거를 정해요" / "흔들기, 측면 버튼, 즉시, 지연 — 상황에 맞춰 골라보세요."
- Step 3: "이 버튼이 발사 스위치" / "이걸 누르면 가짜 통화가 시작돼요. 옆 사람 모르게 자연스럽게."
- Step 4: "더 자세히 알아보기" / "위장 아이콘, 언어, 사용 설명서 — 모두 여기에 있어요."

## 결정 요청

### A.1 Spotlight cutout 스타일
현재: 단순 라운드 사각형 cutout + 3dp EjectCoral stroke ring.
**옵션**:
- (a) Pulse animation — ring 이 호흡하듯 천천히 확장/축소
- (b) Glow halo — 라이트 = 코랄 그림자, 다크 = blur glow
- (c) Dotted/dashed ring — 카메라 셔터 감성
- (d) 본인 추천

원형 (Step 3 EJECT) vs 라운드 사각형 (Step 1, 2, 4) 별 처리도 답해 주세요.

### A.2 Tooltip Card 디자인
현재: 단순 EjectSurface 배경 + 코너 20dp + title (EjectCoral 18sp ExtraBold) + desc (EjectOnSurface 14sp Medium).
**개선 후보**: 화살표 꼬리(cutout 향함) / 단계별 아이콘 (Step 1=📇, 2=⚡, 3=🚨, 4=⚙) / 그라데이션 / "TIP" 라벨.

### A.3 Tooltip 배치 로직
- spotlight 가 화면 상반부 → tooltip 하단 중앙
- spotlight 가 하반부 → tooltip 상단
- spotlight 가 우측 (Step 4 ⚙) → tooltip 좌측 하단
- cutout 과 tooltip 사이 여백 권장 (16~32dp), 화살표 angle

### A.4 진행도 + 컨트롤
현재: 상단 "1/4" 텍스트 + 우측 "건너뛰기", 하단 "다음 →" 큰 버튼.
**후보**: dot indicator (4점) / "건너뛰기" under-emphasis / 진행도 막대.

## 영역 A 출력 요청
- SVG mockup 4장 (step 1~4) — 라이트 모드. 다크 모드는 컬러 매핑 텍스트로.
- 화면 크기 가정: 412 × 915 dp (Pixel 7 Pro)
- 디자인 결정 4개 (A.1~A.4) 별 채택 옵션 + 근거 (각 50~100자)
- Compose 코드 권고 — `CoachmarkOverlay.kt` 의 `drawWithContent` cutout/ring + Tooltip Card modifier 체인 + offset 계산 로직 diff

## 영역 A 참고 코드 (현재 구현)
```kotlin
.drawWithContent {
    drawContent()
    drawRect(color = Color.Black.copy(alpha = 0.78f))
    spot?.let {
        val pad = 8.dp.toPx()
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(it.offset.x - pad, it.offset.y - pad),
            size = Size(it.size.width + pad * 2, it.size.height + pad * 2),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            blendMode = BlendMode.Clear,
        )
        drawRoundRect(  // ring
            color = EjectCoral, ...,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}
```

---

# 영역 B — 가짜 incoming-call 화면 수락/거절 버튼 회귀 fix + 드래그 의도 재정립

## 현재 동작 (FakeIncomingCallScreenV2.kt v1.5.0)

```
┌─────────────────────────────────┐
│  caller name                    │
│  caller label                   │
│  ┌─[Call Assist pill]─┐         │
│                                 │
│  ◯ Accept(green)   ◯ Decline(red)
│      ↑ 72dp inner    ↑ 72dp inner
│   (200dp Box)     (200dp Box)   │
│                                 │
│  Send message                   │
└─────────────────────────────────┘
       ↑ Row(SpaceBetween) + horizontal padding 56dp
```

상호작용:
- **Tap** → 즉시 onAccept / onDecline
- **Drag** → 버튼 자체가 손가락 따라 움직임 (`Modifier.offset { IntOffset(offsetX, offsetY) }`) + Lottie `drag_confirm.json` 동심원 ring (progress = 거리/120px)
- 거리 ≥ 120px 에서 release → trigger, 미만이면 snap-back

## 문제

### B.1 위치 회귀
- `Row.padding(horizontal = 56.dp)` + 두 200dp Box → 합 **512dp 폭 필요**
- Pixel 7 (411dp) / Galaxy S22 (360dp) 에서 두 200dp Box **겹치거나 잘림**
- v1.4.4 의 Lottie `drag_confirm` 통합 시 ripple 영역 수용 위해 outer Box 200dp 로 키운 게 회귀 원인
- v1.4.4 이전엔 outer Box 가 더 작았을 것 (Lottie 도입 전)
- **요구**: v1.4.4 이전 위치/크기 회복 + Lottie ring 도 살리는 방안

### B.2 사용자 터치를 따라가는 애니메이션 (사용자 명시)
- 현재: 버튼이 손가락 따라 움직임 (`offsetX`, `offsetY` drag 누적)
- 사용자 명시: **"수락/거절 버튼들이 사용자 터치를 따라가지 않게 해"**
- 도입 의도: drag-to-confirm — 실수 trigger 방지. Lottie ring 으로 "확신 시각화"
- 문제: 버튼이 따라 움직이는 형태 = iOS / One UI 톤과 다름. iOS 잠금화면 슬라이드 처럼 "버튼 고정 + 별도 인디케이터 슬라이드" 가 자연스러움

## 결정 요청

### B.3 드래그 인텐트 시각화 — 어떤 패턴?

**옵션 A — Apple style 슬라이드 트랙**: 버튼은 절대 고정. 별도 horizontal 슬라이드 트랙 + 화살표 인디케이터가 손가락 따라감. → 끝까지 끌면 trigger.

**옵션 B — Lottie ring 만 활용 (현재의 50%)**: 버튼 고정. 손가락 위치 별도 추적해서 Lottie progress 만 컨트롤. `Modifier.offset` 만 제거.

**옵션 C — Long-press to confirm**: 드래그 자체 제거. Long-press (예: 800ms 홀드) 동안 ring 차오름. 가장 단순, 손 흔들려도 안전.

**옵션 D — 본인 추천**.

**각 옵션 장단점 + 권장 옵션 + 사용자 멘탈 모델 분석** (100~150자) 답해 주세요.

### B.4 위치/크기 회복 + Lottie ring 공존
- PulsingCallButton outer Box 크기 (96dp / 112dp / 144dp 중 권장)
- 두 버튼 사이 간격 (`Arrangement.SpaceBetween` 외 권고)
- Lottie ring 이 outer Box 보다 넓게 보이는 trick (예: `graphicsLayer { clip = false }`) 또는 ring 단순화

### B.5 Idle PulseRing 유지 여부
현재 idle 상태에서 ring 무한 반복. 유지 / 제거 / 톤 다운 중 권장.

## 영역 B 사이드 정보
- Accept = 녹색 `#22C55E`, Decline = 빨강 `#EF4444`
- 버튼 안 아이콘 = `Icons.Default.Call` / `Icons.Default.CallEnd`, 32dp white
- 배경 = 짙은 색조 그라데이션 4 blob (네이비/코랄/머스타드/라일락)
- caller name (32sp Bold white) + caller label (14sp 75% white) — Clarity 마스킹 적용
- "Call Assist" pill (One UI 8.5 톤)
- 하단 "Send message" 작은 핸들

## 영역 B 출력 요청
- SVG mockup 1~3장 (idle / mid-drag-or-confirm / triggered) — 412 × 915 dp
- 옵션 A/B/C/D 권장 + 근거
- 위치/크기 권고 (outer Box dp / horizontal padding / 두 버튼 간격)
- Compose 코드 권고 — `PulsingCallButton` 함수 변경 diff
  - **반드시 제거**: `.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }`
  - **유지/수정**: drag 인텐트 로직 (threshold + onTrigger), Lottie progress, Tap 즉시 trigger

## 영역 B 가드 (추가)
- ❌ One UI 8.5 incoming-call 톤 깨기
- ❌ Accept/Decline 컬러 변경 (녹/빨 글로벌 표준)
- ❌ Tap 즉시 trigger 제거 (위험 상황 빨리 받기 시나리오)
- ❌ Clarity 마스킹 깨기 (`callerName`/`callerLabel` 의 `.clarityMask()` 유지)

---

# 영역 C — BottomBar 인디케이터 부드러운 전환

## 현재 동작 (MainScreen.kt line 1743~1787, v1.5.0)

3-탭 BottomBar (COMMAND / HISTORY / SYSTEMS). 좌우 스와이프로 화면 전환 시
**메인 컨텐츠는 부드럽게 슬라이드**하지만, **BottomBar 의 active 음영 (회색, EjectSurfaceMid) 은 step function 처럼 jump**.

```kotlin
listOf(COMMAND, HISTORY, SYSTEMS).forEach { (screen, label) ->
    val isActive = screen == current
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(50))
            .background(if (isActive) EjectSurfaceMid else Color.Transparent)  // ← step
            .clickable { onSelect(screen) }
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            color = if (isActive) EjectOnSurface else EjectSecondary,         // ← step
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.SemiBold,
        )
    }
}
```

사용자 요구: **"on/off 보다는 부드럽게, 연속성 있게 넘어가게"**

## 결정 요청

### C.1 전환 패턴 — 어떤 게 가장 자연스러운가?

**옵션 1 — Color crossfade (단순)**: `animateColorAsState` 로 background/text 200~300ms 페이드. 코드 1~2줄 변경. 단점: "슬라이드" 느낌 X.

**옵션 2 — Sliding pill indicator (권장 후보)**: 회색 pill 별도 Box 1개로 만들어 active 위치로 `animateDpAsState` 슬라이드. iOS UISegmentedControl / Material 3 NavigationBar style. 인디케이터가 한 탭에서 다른 탭으로 미끄러짐.

**옵션 3 — Drag progress 동기화 (최고급)**: 화면 스와이프 누적 drag 비율 (0~1) 을 BottomBar 까지 hoist. 손가락 천천히 끌면 indicator 도 50% 위치. 가장 자연스러움. state hoisting 깊어짐.

**옵션 4 — Underline / dot indicator (대안)**: pill 배경 제거, 작은 underline (3dp 코랄) 또는 dot 만 슬라이드.

**우리는 옵션 2 를 1순위로 봅니다**. 본인 권장 답해 주세요.

### C.2 추가 결정
- Active 텍스트 weight (ExtraBold ↔ SemiBold) 도 부드럽게? `animateIntAsState` + `FontWeight(weight)` 보간
- Active 텍스트 color (EjectOnSurface ↔ EjectSecondary) 도 부드럽게? `animateColorAsState` 200ms
- Pill 코너/패딩/그림자 변경 여부

## 영역 C 출력 요청
- 권장 옵션 + 근거 (100~150자)
- Compose 코드 권고 — `BottomBar` 함수 변경 diff. Sliding pill 구현 (옵션 2 선택 시), `animateColorAsState`/`animateDpAsState`/`Animatable` 패턴, `onGloballyPositioned` 필요 여부
- (선택) Lottie 또는 SVG mockup 3프레임 (idle / mid-transition / settled)

## 영역 C 가드 (추가)
- ❌ EjectSurfaceMid / EjectOnSurface / EjectSecondary 외 새 컬러
- ❌ Material NavigationBar 컴포넌트 도입
- ❌ 코너 라운드 (`RoundedCornerShape(50)`) 변경
- ❌ 탭 개수 / 라벨 / 폰트 크기 변경
- ❌ shake/bounce 같은 과한 모션 (앱 톤 = 차분함)

## 영역 C 베이스라인 골격
```kotlin
@Composable
private fun BottomBar(current: AppScreen, ...) {
    val tabs = listOf(COMMAND, HISTORY, SYSTEMS)
    val activeIndex = tabs.indexOf(current)
    val animatedIndex by animateFloatAsState(
        targetValue = activeIndex.toFloat(),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "bottombar-pill",
    )
    BoxWithConstraints(...) {
        val tabWidth = (maxWidth - 12.dp) / tabs.size
        Box(
            modifier = Modifier
                .offset(x = tabWidth * animatedIndex + 6.dp)
                .width(tabWidth).height(40.dp)
                .clip(RoundedCornerShape(50))
                .background(EjectSurfaceMid),
        )
        Row(...) {
            tabs.forEachIndexed { idx, screen ->
                val activeFraction = ...  // animatedIndex 와 idx 거리
                Text(label, color = lerp(EjectSecondary, EjectOnSurface, activeFraction), ...)
            }
        }
    }
}
```
이 골격을 다듬거나 더 나은 구조 제시.

---

# 통합 출력 형식 (3 영역 한 번에)

```
## 영역 A — 코치마크
[SVG mockup 4장]
[A.1~A.4 결정 + 근거]
[Compose diff]

## 영역 B — 수락/거절 버튼
[SVG mockup 1~3장]
[B.3 옵션 권장 + 근거]
[B.4 위치/크기 + B.5 idle pulse]
[Compose diff]

## 영역 C — BottomBar
[권장 옵션 + 근거]
[Compose diff]
[(선택) SVG 3프레임]
```

답변은 한 번에 다 주세요. 각 영역은 독립적으로 채택/거절 가능해야 합니다.

---

**작성**: 2026-05-02 (v1.5.0 push 후 사용자 피드백 #1, #2, #6 → 통합)
**Simon → 답변 받은 뒤 → Claude Code 점검 프롬프트 (`PROMPT_v1.5.1_claude_code.md`) 와 함께 다음 세션에 전달**
