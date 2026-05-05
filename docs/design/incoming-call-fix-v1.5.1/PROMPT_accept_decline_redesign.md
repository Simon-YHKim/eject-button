# Claude Design 프롬프트 — 수락/거절 버튼 회귀 fix + 드래그 의도 재정립 (v1.5.1)

> **사용법**: 이 파일 전체를 복사해서 Claude (claude.ai) 또는 design-shotgun 에 붙여넣으세요.
> 산출물은 SVG mockup 1~3장 (idle / mid-drag / triggered) + Compose 코드 권고안.

---

## 컨텍스트

당신은 Android Compose 앱 "Eject Button" (한국어 명: 비상 탈출) 의 디자인 컨설턴트입니다.
이 앱은 회식·어색한 자리·위험 상황에서 가짜 통화로 빠져나오는 fake-call 앱이고,
**옆 사람 모르게 자연스럽게** 가 핵심 가치입니다.

현재 v1.5.0 의 가짜 incoming-call 화면 (`FakeIncomingCallScreenV2.kt`) 의
**수락 (Accept) / 거절 (Decline) 버튼 두 개에 두 가지 회귀 + 의도 misalignment** 가 있습니다.

---

## 현재 동작 (v1.4.4 ~ v1.5.0)

```
┌─────────────────────────────────┐
│  caller name                    │
│  caller label                   │
│                                 │
│  ┌─[Call Assist pill]─┐         │
│                                 │
│  ◯ Accept(green)   ◯ Decline(red)
│      ↑ 72dp          ↑ 72dp     │
│   (200dp Box)     (200dp Box)   │
│                                 │
│  Send message                   │
└─────────────────────────────────┘
       ↑ Row(SpaceBetween) + horizontal padding 56dp
       ↑ 두 PulsingCallButton
```

**상호작용**:
- **Tap** → 즉시 onAccept / onDecline 발동 (가짜 통화 받음 / 끊음)
- **Drag** → 버튼 자체가 손가락을 따라 움직임 (`Modifier.offset { IntOffset(offsetX, offsetY) }`)
  - 드래그 중엔 200dp Box 안에 Lottie `drag_confirm.json` 동심원 ring 이 progress = `dragFraction` (거리/120px 비율) 으로 표시
  - 거리 ≥ 120px 이면 onTrigger() 발동, 아니면 snap-back

---

## 문제

### 문제 1: 위치가 이상하다 (사용자 보고)
- `Row(modifier.padding(horizontal = 56.dp))` + 두 버튼이 각 200dp Box → 합 = 200 + 200 + 56*2 = **512dp 폭 필요**.
- 일반 폰 (Pixel 7: 411dp / Galaxy S22: 360dp 폭) 에서 두 200dp Box 가 **겹치거나 잘림**.
- v1.4.4 의 Lottie `drag_confirm` 통합 시 ripple 영역 수용 위해 PulsingCallButton 의 outer Box 를 200dp 로 키운 것이 회귀 원인.
- v1.4.4 이전엔 PulsingCallButton 의 outer Box 가 더 작았을 것 (Lottie 도입 전).

**요구**: v1.4.4 이전 위치/크기로 회복하되, Lottie ring 도 살리는 방안 제시.

### 문제 2: 사용자 터치를 따라가는 애니메이션 (사용자가 명시적으로 원치 않음)
- 현재: 버튼이 손가락 따라 움직임 (`offsetX`, `offsetY` 가 drag 양 누적).
- 사용자 명시: **"수락/거절 버튼들이 사용자 터치를 따라가지 않게 해"**

**드래그 도입 의도 (v1.4.3 commit 메시지에서 추정)**:
- "drag-to-confirm" — 실수 trigger 방지. 즉 사용자가 자신의 드래그 의도를 분명히 해야 trigger 됨.
- Lottie `drag_confirm.json` 동심원 ring 으로 "확신의 시각화" 유도.

**문제는 시각화 방식**: 버튼이 따라 움직이는 형태 = 모바일 OS 기본 dialer (iOS / One UI) 와 톤이 다름. iOS 잠금화면 슬라이드 처럼 "버튼은 고정, **다른 인디케이터** (예: 화살표 / 트레일 / 트랙) 가 슬라이드" 가 더 자연스러움.

---

## 당신의 작업

### 핵심 결정 — 드래그 인텐트 시각화를 어떻게 바꿀 것인가?

**옵션 A — Apple style 슬라이드 트랙**:
  - 버튼은 절대 고정. 버튼 옆 또는 아래 별도의 horizontal 슬라이드 트랙 + 화살표 인디케이터가 손가락 따라감.
  - "→" 화살표를 끝까지 끌면 trigger.

**옵션 B — Lottie ring 만 활용 (현재의 50% 채택)**:
  - 버튼 고정. 손가락 위치를 별도로 추적해서 Lottie progress 만 컨트롤.
  - 현재의 dragFraction (거리/threshold) 를 그대로 사용하되, `Modifier.offset` 만 제거.

**옵션 C — Long-press to confirm**:
  - 드래그 자체를 제거. Long-press (예: 800ms 홀드) 동안 ring 이 차오르고 끝까지 차면 trigger.
  - 가장 단순. 손이 흔들려도 안전.

**옵션 D — 본인 추천**.

각 옵션의 **장단점 + 권장 옵션**을 답해 주세요.

### 위치/크기 회복 + Lottie ring 공존
- PulsingCallButton 의 outer Box 크기를 어디까지 줄일지 (예: 96dp / 112dp / 144dp).
- 두 버튼 사이 간격 (`Arrangement.SpaceBetween` 외 권고 패턴).
- Lottie `drag_confirm.json` ring 이 outer Box 보다 더 넓게 보이게 하는 trick (예: `Modifier.size(200.dp).graphicsLayer { clip = false }`) 또는 ring 자체를 단순화.

### Idle 상태 PulseRing 유지 여부
현재: 드래그 중이 아니면 idle pulse (확장-페이드 ring) 가 무한 반복.
- 유지가 좋은가? 끄는 게 좋은가? 톤이 너무 산만하지 않은지 판단.

### 사이드 정보
- Accept = 녹색 (`#22C55E`)
- Decline = 빨강 (`#EF4444`)
- 버튼 안 아이콘 = `Icons.Default.Call` (수락) / `Icons.Default.CallEnd` (거절), 32dp white
- 배경 = 짙은 색조의 그라데이션 4 blob (네이비/코랄/머스타드/라일락)
- 위에 caller name (32sp Bold white) + caller label (14sp 75% white) — Clarity 마스킹 적용됨
- "Call Assist" pill (One UI 8.5 톤)
- 하단 "Send message" 작은 핸들

---

## 출력 형식

1. **SVG mockup 1~3장** — idle / mid-drag (or mid-confirm) / triggered.
   - 화면 크기 가정: 412 × 915 (Pixel 7 Pro)
2. **드래그 인텐트 옵션 권장** — A/B/C/D 중 선택 + 근거 (100~150자) + 사용자 멘탈 모델 분석
3. **위치/크기 권고** — outer Box dp / horizontal padding dp / 두 버튼 사이 간격
4. **Compose 코드 권고안** — `FakeIncomingCallScreenV2.kt` 의 `PulsingCallButton` 함수 변경 diff
   - **반드시 제거할 것**: line 233 의 `.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }` (사용자 명시 요구)
   - **유지/수정할 것**: drag 인텐트 로직 (threshold + onTrigger), Lottie progress 연동, Tap 즉시 trigger

---

## 절대 하지 말 것 (가드)

- ❌ One UI 8.5 incoming call 톤 깨기 (사용자가 v1.4.x 부터 일관되게 추구한 디자인 방향)
- ❌ Accept/Decline 컬러 변경 (녹색/빨강 = 글로벌 표준)
- ❌ 새 dependency 도입 (Lottie 는 이미 있음, 그 외 X)
- ❌ Tap 즉시 trigger 제거 (드래그/롱프레스만 가능하게 만들면 핵심 사용성 망가짐 — 위험 상황에서 빨리 통화 받기 시나리오)
- ❌ Clarity 마스킹 깨기 (`callerName` / `callerLabel` 의 `.clarityMask()` 유지)

---

## 참고 코드 (v1.5.0 현재)

### Layout (line 132~159)
```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 56.dp),  // ← 좁힐 여지
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    PulsingCallButton(
        icon = Icons.Default.Call, color = Color(0xFF22C55E),
        contentDescription = strings.accept, onTrigger = onAccept,
        // ... pulse / dragThreshold 파라미터들
    )
    PulsingCallButton(
        icon = Icons.Default.CallEnd, color = Color(0xFFEF4444),
        contentDescription = strings.decline, onTrigger = onDecline,
        // ...
    )
}
```

### PulsingCallButton 핵심 (line 192~282)
```kotlin
var offsetX by remember { mutableStateOf(0f) }
var offsetY by remember { mutableStateOf(0f) }
var triggered by remember { mutableStateOf(false) }
val dragging = offsetX != 0f || offsetY != 0f
val distance = sqrt(offsetX * offsetX + offsetY * offsetY)
val dragFraction = (distance / dragThresholdPx).coerceIn(0f, 1f)

Box(modifier = Modifier.size(200.dp), ...) {  // ← 200dp 가 회귀 원인
    // Lottie drag_confirm overlay (드래그 중일 때만)
    if (dragging && !triggered) {
        LottieAnimation(progress = { dragFraction }, modifier = Modifier.size(200.dp))
    }
    // Idle pulse (드래그 안 할 때)
    if (!dragging && !triggered) { for (i in 0 until pulseLayers) PulseRing(...) }

    // 실제 버튼
    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }  // ← 제거 대상
            .size(72.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { ... }  // Tap → 즉시 trigger
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { _, drag -> offsetX += drag.x; offsetY += drag.y },
                    onDragEnd = { if (distance ≥ threshold) onTrigger() else snap-back },
                    ...
                )
            },
    ) { Icon(icon, ...) }
}
```

이 구조를 회귀 fix 후 의도에 맞게 재설계해 주세요.
