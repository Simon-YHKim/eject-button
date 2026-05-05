# Handoff: Drag-to-Confirm Lottie Animation (drag_confirm.json)

> **For Claude Code:** 이 폴더는 Eject Button Android 앱의
> 통화 수신 화면(IncomingCall)에 들어갈 **drag-to-confirm 애니메이션 에셋**과
> 그 통합 가이드입니다. One UI 8.5의 표준 통화 수신 패턴
> (동심원 ring이 grow하며 트리거)을 정확히 재현합니다.

---

## 🎯 무엇을 만드는가

비-iOS 디바이스 사용자에게 친숙한, **Galaxy One UI 8.5 표준 동작과 동일한**
"드래그하여 통화 받기 / 거절" UI를 Compose에서 구현합니다.

- 기준 사양: 1.6초 / 48-frame GIF로 캡처된 실제 One UI 8.5 동작 (frames 3–9)
- 정렬: outer ring의 **중심이 누른 버튼 중심**과 일치
- 좌우 대칭: accept/decline 양쪽 버튼에서 같은 JSON을 사용 — anchor만 다름

---

## 📂 이 폴더의 파일들

| 파일 | 역할 |
|---|---|
| **`drag_confirm.json`** | **✅ 실제 통합 대상** — 앱의 `assets/animations/` 또는 `res/raw/` 에 복사 |
| `drag-confirm-builder.js` | 위 JSON을 만든 빌더 스크립트. 향후 수정이 필요하면 이걸 고쳐서 `drag_confirm.json` 재생성 |
| `drag-confirm-isolation.html` | **단독 검증 페이지** — JSON을 transparent 캔버스 위에서 단독 재생. JSON에 회색 동심원 두 개만 들어있음을 시각적으로 증명 |
| `drag-confirm-preview.html` | **composition mockup** — Compose 통합 후 어떻게 보일지 시뮬레이션. 전화 버튼·아이콘·텍스트는 mockup이 그린 것이며 JSON에는 없음 |

> ⚠️ **Important:** `*.html` 파일들은 디자인 검증 / mockup용이며 **그대로 포팅하지 마세요**.
> 실제 앱에 들어가는 것은 `drag_confirm.json` 한 파일뿐입니다.

---

## 🎨 Fidelity

**High-fidelity Lottie asset.** 디자인 토큰(반경 / 알파 / 색상 / 키프레임)이
모두 확정되어 있고 JSON으로 export 완료. Compose 측에서는 이 JSON을 그대로 로드하고
`progress` 매개변수만 드래그 fraction과 연결하면 됩니다.

---

## 📐 애니메이션 사양

### 캔버스
- **크기**: 400 × 400dp (transparent)
- **fps**: 60
- **총 프레임**: 100 (op = 100)
- **progress 매핑**: 0.0 ↔ frame 0, 1.0 ↔ frame 100 (선형)
- **transparent canvas** — 배경 fill 없음. 모든 비-ring 영역은 투명.

### 레이어 (back→front)
| Layer | Color | Alpha (peak) | Radius (start → end) |
|---|---|---|---|
| **Outer** | rgb(200, 200, 200) | 30% | 100dp → 180dp |
| **Inner** | rgb(150, 150, 150) | 45% | 30dp → 160dp |

### 키프레임 (둘 다)

| Frame | Progress | Action |
|---|---|---|
| 0 | 0.00 | radius=start, opacity=0 |
| 5 | 0.05 | opacity=peak (페이드인 완료) |
| 95 | 0.95 | radius=거의 end, opacity=peak |
| 100 | 1.00 | radius=end, opacity=0 (페이드아웃) |

전 구간 **선형 easing** — Compose가 `progress`를 드래그 거리에서 직접 매핑하므로
Lottie 자체에는 easing을 넣지 않음. 사용자 손가락 움직임 = ring grow.

### 좌표계
- 두 layer의 anchor는 캔버스 중심 (200, 200) — 즉 **Lottie 호스트의 정중앙**
- Compose에서 **호스트의 정중앙이 누른 버튼 정중앙과 일치**하도록 배치하면 자동 정렬됨

---

## 🔧 Compose 통합 가이드

### 1. 에셋 배치
```
app/src/main/assets/animations/drag_confirm.json
```
(또는 프로젝트 컨벤션에 맞춰 `res/raw/drag_confirm.json` — 둘 다 OK)

### 2. 의존성 (이미 있을 가능성 높음)
```kotlin
implementation("com.airbnb.android:lottie-compose:6.4.0")
```

### 3. 핵심 컴포저블
```kotlin
@Composable
fun DragConfirmRipple(
    dragFraction: Float,           // 0f..1f, Compose가 드래그 거리에서 계산
    modifier: Modifier = Modifier, // 호출부에서 size + offset 지정
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("animations/drag_confirm.json")
    )
    LottieAnimation(
        composition = composition,
        progress = { dragFraction.coerceIn(0f, 1f) },
        modifier = modifier,
    )
}
```

### 4. 호출 위치 — 누른 버튼 중심 정렬

`Box`로 감싸서 ring이 **버튼 위에 오버레이**되게 합니다.
중요한 것은 **400×400dp 호스트의 정중앙이 버튼 정중앙과 일치**해야 한다는 것.
가장 간단한 방법: 버튼과 동일한 `Alignment`로 `Box` 안에 두기.

```kotlin
Box(contentAlignment = Alignment.Center) {
    // ring overlay는 버튼 뒤에 (z-순서 먼저)
    if (dragFraction > 0f) {
        DragConfirmRipple(
            dragFraction = dragFraction,
            modifier = Modifier.size(400.dp),
        )
    }
    // 실제 통화 버튼 (Compose가 그림)
    CallButton(
        type = CallButtonType.Accept,
        onClick = { /* ... */ },
        modifier = Modifier
            .size(64.dp)
            .draggable(...) { delta ->
                dragFraction = (dragFraction + delta / dragBudget).coerceIn(0f, 1f)
            },
    )
}
```

### 5. 양쪽 버튼 (accept / decline) 모두에 적용

같은 JSON, 같은 컴포저블을 두 번 호출. **하나만 활성화** — 사용자가 한쪽 버튼을 누르면
그 쪽 ring만 보임:

```kotlin
data class DragState(
    val side: Side? = null,    // null | Accept | Decline
    val fraction: Float = 0f,
)

// Accept 버튼 영역
Box(contentAlignment = Alignment.Center) {
    if (dragState.side == Side.Accept) {
        DragConfirmRipple(dragFraction = dragState.fraction, modifier = Modifier.size(400.dp))
    }
    AcceptButton(/* ... */)
}

// Decline 버튼 영역 — 동일 패턴
Box(contentAlignment = Alignment.Center) {
    if (dragState.side == Side.Decline) {
        DragConfirmRipple(dragFraction = dragState.fraction, modifier = Modifier.size(400.dp))
    }
    DeclineButton(/* ... */)
}
```

### 6. 트리거 로직

```kotlin
// 드래그 거리 budget — 두 버튼 중심 사이 거리에 맞춤
val dragBudgetPx = with(LocalDensity.current) { 240.dp.toPx() }

// 드래그 처리
.pointerInput(Unit) {
    detectDragGestures(
        onDragStart = { dragState = dragState.copy(side = Side.Accept) },
        onDrag = { _, drag ->
            val delta = drag.x.absoluteValue / dragBudgetPx
            dragState = dragState.copy(
                fraction = (dragState.fraction + delta).coerceIn(0f, 1f)
            )
            if (dragState.fraction >= 1f) {
                onAccept()
                dragState = DragState() // reset
            }
        },
        onDragEnd = {
            // 0으로 부드럽게 복귀
            scope.launch {
                animate(dragState.fraction, 0f, animationSpec = tween(220)) { v, _ ->
                    dragState = dragState.copy(fraction = v)
                }
                dragState = DragState()
            }
        },
    )
}
```

### 7. Compose-side 부가 효과 (옵션 — Lottie와 별개)

실제 One UI 동작에서는 ring 외에도:

- **누른 버튼**: progress에 비례해 점진적 페이드 (`alpha = 1f - dragFraction`)
- **반대편 버튼**: `progress > 0.85`부터만 짧게 페이드아웃

이는 Lottie가 아닌 Compose `Modifier.alpha(...)` 또는 `graphicsLayer { alpha = ... }`
로 구현. 이 두 효과는 `drag_confirm.json` 안에 **포함되지 않음** — 의도된 분리입니다.

---

## ✅ 검증 — JSON 내용물

`drag_confirm.json`에 들어있는 것:
- ✅ Layer 1: `Outer` shape layer — Ellipse (size animated) + Fill rgb(200,200,200)
- ✅ Layer 2: `Inner` shape layer — Ellipse (size animated) + Fill rgb(150,150,150)

**들어있지 않은 것 (Compose가 별도 렌더):**
- ✗ 통화 버튼 (accept/decline)
- ✗ 전화 아이콘
- ✗ 배경, status bar, 시계, 수신화면 텍스트
- ✗ 사용자 터치 트래킹 점 (reference GIF의 Android Show Touches artifact였음)

외부 검증 권장: `drag_confirm.json`을
[lottiefiles.com/preview-tool](https://lottiefiles.com/preview-tool)에 업로드 → transparent 배경 위에
회색 동심원 두 개만 보이면 정상.

---

## 🌐 다국어 / a11y 노트

이 애니메이션 자체에는 텍스트 없음 — 다국어 키 추가 불필요.
Compose 측에서 통화 버튼 contentDescription만 기존 패턴(앱 번들의 `accept` / `decline` 키)을 사용하면 됩니다.

---

## 🔁 향후 수정이 필요할 때

`drag-confirm-builder.js` 의 상수만 고치고 재생성:

```js
const INNER_R_START = 30;
const INNER_R_END   = 160;
const OUTER_R_START = 100;
const OUTER_R_END   = 180;
const INNER_ALPHA_PCT = 45;
const OUTER_ALPHA_PCT = 30;
const FADE_IN_END    = 5;     // p ≈ 0.05
const FADE_OUT_START = 95;    // p ≈ 0.95
```

브라우저 콘솔에서 빌더 스크립트 실행 → `JSON.stringify(buildDragConfirmLottie())` 결과를 저장하면 됩니다.
(또는 디자이너에게 다시 요청)

---

## 📎 통합 체크리스트

- [ ] `drag_confirm.json` 을 `assets/animations/` 에 복사
- [ ] `lottie-compose` 의존성 확인 (없다면 추가)
- [ ] `DragConfirmRipple` 컴포저블 추가 (`ui/incomingcall/components/` 권장 위치)
- [ ] `IncomingCallScreen` 의 accept/decline 버튼을 `Box(contentAlignment = Center)` 로 감싸고 ring overlay 추가
- [ ] 드래그 제스처 + `dragFraction` 상태 연결
- [ ] 두 버튼 중심 사이 거리를 `dragBudgetPx`로 설정
- [ ] 누른 버튼 alpha 페이드 (`1f - dragFraction`) 적용
- [ ] 반대편 버튼 alpha 페이드 (`p > 0.85` 시) 적용
- [ ] 트리거 시 햅틱 피드백 (`HapticFeedbackType.LongPress`) 추가
- [ ] 실기기에서 시각적 일치 확인 (참고용 reference GIF는 디자이너 보유)
