# Claude Design 프롬프트 — 코치마크 4-Step 투어 (v1.5.1)

> **사용법**: 이 파일 전체를 복사해서 Claude (claude.ai) 또는 design-shotgun 도구에 붙여넣으세요.
> 산출물은 SVG mockup 4장 (step 1~4) + Compose 코드 권고안.

---

## 컨텍스트

당신은 Android Compose 앱 "Eject Button" (한국어 명: 비상 탈출) 의 디자인 컨설턴트입니다.
이 앱은 회식·어색한 자리·위험 상황에서 가짜 통화로 빠져나오는 fake-call 앱이고,
**옆 사람 모르게 자연스럽게** 가 핵심 가치입니다. 7개 언어를 지원합니다 (en/ko/ja/zh-CN/zh-TW/es/hi).

이 앱의 v1.5.0 에 4-step 코치마크 투어를 도입했지만 다음 두 가지가 부족합니다:

1. **Spotlight 좌표 미측정** — 현재는 fullscreen dim + 중앙 tooltip 만 표시. 진짜 "특정 버튼을 가리키는" a-ha 모먼트가 없음.
2. **디자인 자체가 평범** — Card + 빨간 ring 의 단순 조합. 첫 사용자가 "와, 똑똑하다" 라고 느낄 만한 비주얼 임팩트 부족.

**당신의 작업**: 4-step 코치마크 투어의 시각 디자인을 개선해 주세요.
**중요한 가드**: 기존 앱의 컬러/타이포/톤은 절대 망가뜨리지 마세요.

---

## 현재 디자인 시스템 (절대 변경 금지)

### 컬러 토큰 (`ui/theme/`)
- `EjectCoral` = `#FF6B6B` 류의 빨강 (브랜드 강조 / EJECT 버튼)
- `EjectBg` = 라이트 모드 = 거의 흰색 / 다크 모드 = 짙은 네이비
- `EjectSurface` = 카드 배경 (라이트 = 미세한 회색 / 다크 = 한 단계 밝은 네이비)
- `EjectSurfaceMid` = 비활성 dot, 보조 표면
- `EjectOnSurface` = 본문 텍스트 (라이트 = 거의 검정 / 다크 = 거의 흰색)
- `EjectSecondary` = 보조 텍스트 (회색)
- `EjectOutlineVar` = 외곽선 (얇은 회색)

### 타이포
- 모든 텍스트는 시스템 기본 폰트 (Material 3 기본). 별도 폰트 X.
- 강조 = `FontWeight.ExtraBold` + `letterSpacing = 1.sp`
- 본문 = `FontWeight.Medium`

### 코너 라운드
- 카드 / 다이얼로그: 16~28dp
- 버튼: 16dp
- 작은 칩: CircleShape
- spotlight cutout: 20dp

### 라이트/다크 모드
- 둘 다 지원. 디자인은 양쪽 모두 자연스러워야 함.
- v1.5.1 부터 디폴트는 라이트 (사용자 결정)

---

## 4-Step 코치마크 요구사항

각 step 은 메인 화면 (`MainScreen.kt` COMMAND 탭) 의 특정 버튼을 가리킵니다.

| Step | 가리킬 영역 | 화면 위치 (대략) | 버튼/카드 크기 (대략) |
|---|---|---|---|
| 1 | 시나리오 선택 카드 (`CallerChips`, LazyRow) | 화면 중간 (위에서 38%) | width = 화면폭, height = 70dp |
| 2 | 트리거 모드 토글 (`TriggerModeRow`) | 화면 중간 (위에서 60%) | width = 화면폭, height = 56dp |
| 3 | EJECT 큰 빨간 버튼 (`EjectButton`) | 화면 중상단 (위에서 22%) | 196dp × 196dp 원형 |
| 4 | ⚙ 설정 톱니 아이콘 (`IconButton`) | 화면 우상단 | 40dp × 40dp |

### 텍스트 (이미 7개 언어 다 추가됨 — `AppStrings.kt` 참조)
- Step 1: 제목 "시나리오를 골라요" / 설명 "어떤 가짜 통화가 올지 미리 정해두세요. 엄마, 아빠, 친구 — 자유롭게 추가할 수 있어요."
- Step 2: 제목 "트리거를 정해요" / 설명 "흔들기, 측면 버튼, 즉시, 지연 — 상황에 맞춰 골라보세요."
- Step 3: 제목 "이 버튼이 발사 스위치" / 설명 "이걸 누르면 가짜 통화가 시작돼요. 옆 사람 모르게 자연스럽게."
- Step 4: 제목 "더 자세히 알아보기" / 설명 "위장 아이콘, 언어, 사용 설명서 — 모두 여기에 있어요."

### 공통 요소
- 진행도 표시 (예: "1/4")
- "건너뛰기" (Skip) 텍스트 버튼 (우상단)
- "다음 →" / 마지막엔 "완료" (= "옛썰!" / "Copy that") 큰 버튼 (하단)

---

## 시각 디자인 요청

다음 4가지 디자인 결정을 해주세요:

### 1. Spotlight cutout 스타일
현재: 단순 라운드 사각형 cutout + 3dp EjectCoral stroke ring.
**개선 방향 후보**:
  - (a) Pulse animation — ring 이 호흡하듯 천천히 확장/축소
  - (b) Glow halo — 라이트 모드에선 코랄 그림자, 다크 모드에선 blur glow
  - (c) Dotted/dashed ring — 카메라 셔터 같은 감성
  - (d) 그 외 본인 추천

원형 (Step 3 EJECT) vs 라운드 사각형 (Step 1, 2, 4) 각각 어떻게 다르게 처리할지도 답해 주세요.

### 2. Tooltip Card 디자인
현재: 단순 EjectSurface 배경 + 코너 20dp + title (EjectCoral 18sp ExtraBold) + desc (EjectOnSurface 14sp Medium).
**개선 방향 후보**:
  - 화살표 (꼬리) 가 cutout 향함
  - 단계별 일러스트레이션 / 아이콘 (예: Step 1 = 📇, Step 2 = ⚡, Step 3 = 🚨, Step 4 = ⚙)
  - 카드 위에 그라데이션 또는 미세한 패턴
  - 본문 위에 "TIP" 라벨

### 3. Spotlight 위치별 Tooltip 배치 로직
원칙: tooltip 이 spotlight 와 겹치지 않게.
- spotlight 가 화면 상반부 → tooltip 은 하단 중앙 고정
- spotlight 가 화면 하반부 → tooltip 은 상단
- spotlight 가 화면 우측 (Step 4 ⚙) → tooltip 은 좌측 하단

cutout 과 tooltip 사이 여백 (16~32dp) 권장 값과 화살표 angle 도 답해 주세요.

### 4. 진행도 + 컨트롤 영역
현재: 상단 "1/4" 텍스트 + 우측 "건너뛰기" 텍스트 버튼, 하단 "다음 →" 큰 버튼.
**개선 후보**:
  - 진행도를 dot indicator (4개 점) 로 변경 — OnboardingScreen 의 progress dot 패턴과 통일
  - "건너뛰기" 를 살짝 흐리게 (under-emphasis)
  - "다음" 버튼 옆에 진행도 막대 (subtle)

---

## 출력 형식

가능하면 다음 모두를 답해 주세요:

1. **SVG mockup 4장** (step 1~4) — 라이트 모드. 다크 모드는 컬러 매핑만 적어주면 됨.
   - 화면 크기 가정: 412 × 915 (Pixel 7 Pro density-independent dp)
2. **디자인 결정 요약** — 위 4가지 항목별로 채택한 옵션 + 근거 (각 50~100자)
3. **Compose 코드 권고안** — `CoachmarkOverlay.kt` 에 적용할 변경 사항 (full file rewrite 가 아니라 diff 또는 함수 단위)
   - `drawWithContent` 안의 cutout/ring 처리
   - Tooltip Card 의 modifier 체인
   - Tooltip offset 계산 로직 (spot.offset.y 기반)

---

## 절대 하지 말 것 (가드)

- ❌ 새 폰트 도입 (시스템 기본 유지)
- ❌ EjectCoral 외의 새 브랜드 컬러 도입
- ❌ 외부 SVG/이미지 자산 의존 (Compose 네이티브 또는 Lottie 자산 폴더 활용)
- ❌ blur effect 가 안 되는 구버전 Android 호환성 깨기 (minSdk = 26)
- ❌ tooltip 텍스트를 영어로 fallback (이미 7개 언어 strings 있음)

---

## 빠른 참고 — 현재 v1.5.0 구현

```kotlin
// CoachmarkOverlay.kt 의 핵심 cutout 로직 (이미 구현됨, 참고용)
.drawWithContent {
    drawContent()
    drawRect(color = Color.Black.copy(alpha = 0.78f))  // dim
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
            color = EjectCoral,
            ...
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}
```

이 구조를 유지하되 ring/cutout 을 더 매력적으로, tooltip 을 더 정보감 있게 만드는 게 목표입니다.
