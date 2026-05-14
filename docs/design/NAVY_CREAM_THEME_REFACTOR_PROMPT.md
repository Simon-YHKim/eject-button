# Eject Button — Navy + Cream 라이트/다크 테마 교체 프롬프트 (v2, 개선판)

> **개선 메모**: v1 프롬프트의 톤 매핑과 코드 스니펫은 정확합니다. v2에서는 (1) 변경 의도/디자인 스토리, (2) 사전 영향 분석 단계, (3) 대비비 정량 검증, (4) 알파/그림자 부작용 점검, (5) 분할 커밋 전략을 보강했습니다. **변수명·구조·타이포·Shape·통화 화면은 v1과 동일하게 변경 금지**.

---

## 0. 변경 의도 — Why Navy + Cream

이 변경은 단순 컬러 스와프가 아니라 **브랜드 아이덴티티 재정렬**입니다. 코드 작업 전 이 의도를 이해해야 회색지대 결정을 내릴 수 있습니다.

- **앱 아이콘이 딥 네이비 + 크림으로 확정** → 앱 내부 UI가 아이콘과 톤 불일치 상태. 사용자가 홈스크린 아이콘 → 앱 진입 시 시각적 단절(red flash) 발생.
- **빨강은 "긴급/탈출"의 직관적 상징**이었지만, 동시에 **장난 앱/위협적 인상**으로 오해될 여지가 있었음 (PLAY_STORE_ASO.md의 포지셔닝: "장난 앱이 아닌 사회 안전 도구").
- **네이비 = 신뢰/도구/조용한 권위**. 크림 = 따뜻함/접근성. 두 색의 조합은 "조용히 작동하는 사회 안전 도구"라는 포지셔닝을 시각적으로 강화함.
- **빨강은 "error" 시맨틱에만 보존** (Material3 `error` 슬롯). 일반 강조 액션은 네이비로.

이 의도에 따라 변수명은 그대로 두되 (`TacticalRed*` → 네이비 값), 의미 불일치는 **2-d의 alias 옵션**으로 완화합니다.

---

## 1. 사전 영향 범위 분석 — 코드 작업 전 필수

테마 변경 전, 다음 grep으로 영향 범위를 먼저 잡습니다. **결과를 메모해두면 검증 단계가 빨라집니다.**

```bash
# (1) 하드코딩된 빨강 16진수
grep -rEn '0xFF(BA1A20|6A0008|A82430|FFB3AC|FFDAD6|680008|FFBAAC|FFADAD)' \
  app/src/main/java/com/ejectbutton/

# (2) 드로어블 XML 하드코딩 빨강
grep -rEn 'android:fillColor="#(BA1A20|6A0008|A82430|FFB3AC|FFDAD6|680008)"' \
  app/src/main/res/

# (3) EjectCoral / EjectRed 직접 참조 (간접 영향)
grep -rEn '\bEjectCoral\b|\bEjectRed\b' app/src/main/java/

# (4) alpha 곱셈으로 만든 배너/하이라이트 (가시성 재검증 필요)
grep -rEn '\.copy\(alpha\s*=' app/src/main/java/com/ejectbutton/ui/

# (5) Lottie 애니메이션 JSON에 빨강이 들어있는지
grep -rEn '"(0\.72[0-9]+|0\.85[0-9]+).+0\.10[0-9]+.+0\.12[0-9]+"' \
  app/src/main/assets/animations/ 2>/dev/null || echo "(빈 결과는 정상)"
```

**예상 영향 지점** (DESIGN_KNOWLEDGE_TRANSFER.md 기준):
- `EjectButton` (MainScreen.kt:1883) — `EjectCoral` 배경/stroke/shadow
- `CommandContent` 카운트다운/대기 배너 — `EjectCoral.copy(alpha = 0.1f)` 배경
- `StitchTopBar` 위장 IconButton 다이얼로그 확인 버튼 — `EjectCoral` 강조
- `SettingsScreen` 프리셋 복원 확인 다이얼로그 — `EjectCoral` 강조
- `CoachmarkOverlay` 진행 도트 — `EjectRed`로 현재 단계 표시
- `OnboardingScreen` 진행 도트 + "다음" / "다시 보기" 버튼
- `PremiumUpgradeDialog` Buy 버튼, "Recommended" 배지
- `RewardedAdDialog` Continue 버튼
- `ic_eject_mark_red.xml` 드로어블 (하드코딩 빨강)
- `ic_unmask.xml`, `ic_disguise_on/off.xml`의 빨강 잉크 (있다면)

---

## 2. 톤 매핑 (마스터 테이블)

### 2-a. 라이트 모드 (메인 변경)

| 역할 | 기존 (Cockpit Red) | 신규 (Navy + Cream) | 비고 |
|---|---|---|---|
| Primary — EJECT 버튼, 활성 칩, 모드 활성 | `#BA1A20` | `#1B2D4A` | 브랜드 시그니처 |
| Primary Deep — 입체 음영 | `#6A0008` | `#0A1525` | EjectButton shadow |
| Primary Soft | `#FFB3AC` | `#A8B5CC` | 비활성 상태 |
| Primary Fixed | `#FFDAD6` | `#E0E5F0` | 액센트 |
| On-Primary 텍스트 | `#680008` | `#0A1525` | navy ink |
| Background (앱 배경) | `#F4F5F7` cool gray | `#F2EDE0` warm cream | 톤 전환 핵심 |
| Surface (카드/표면) | `#FFFFFF` | `#FBF7EC` | 크림 톤 |
| Surface Low / Mid / Highest | `#EBECEF` / `#DDDEE2` / `#CFD0D5` | `#EDE7D4` / `#DFD8C2` / `#CFC6AC` | 단계 보존 |
| On-Surface (본문) | `#1A1C1E` | `#1B2D4A` (navy ink) | 잉크화 |
| Secondary (보조 텍스트) | `#55575B` | `#5A6478` (slate) | navy 친화 |
| Outline / OutlineVariant | `#909095` / `#C0C1C5` | `#9098A8` / `#D5D0BD` | warm tone |
| Primary Container (MAYDAY 카드) | `#1A1C1E` | `#0F1929` | 깊은 네이비 |
| On-Primary Container | `#C6C6CB` | `#D5DCE8` | 가독성 보존 |
| **Error (보존)** | `#BA1A20` | `#BA1A20` ⚠ **유지** | error 시맨틱 |
| **Error Container (보존)** | `#FFDAD6` | `#FFDAD6` ⚠ **유지** | |
| **TacticalCyan (보존)** | `#00DAF3` | `#00DAF3` ⚠ **유지** | SHAKE 시그니처 |
| **TacticalYellow (보존)** | `#F8BD2A` | `#F8BD2A` ⚠ **유지** | SIDE_BUTTON 시그니처 |

### 2-b. 다크 모드 (인버전 적용)

| 역할 | 기존 (Cockpit Dark) | 신규 (Navy Dark) | 비고 |
|---|---|---|---|
| Primary — EJECT 버튼 (CTA) | `#BA1A20` red | `#F2EDE0` **cream** | **인버전 — 다크 위 크림** |
| onPrimary (버튼 텍스트) | `#FFFFFF` | `#0A1525` | navy ink on cream |
| Background | `#121416` near-black | `#0A1525` deep navy | 본격 네이비 무드 |
| Surface (카드) | `#1E2022` | `#13223D` | navy surface |
| Surface Low / Mid / Highest | `#1A1C1E` / `#282A2C` / `#333537` | `#0F1A2E` / `#1F324F` / `#2A4063` | navy 단계 |
| onSurface (본문) | `#E2E2E5` cool gray | `#E5E0D0` warm cream | 톤 일관성 |
| Secondary | `#C6C6CB` | `#A8B0BD` slate | navy 친화 |
| Outline Variant | `#45474B` | `#3A4A66` | navy 라인 |
| Primary Container (MAYDAY) | `#0C0E10` | `#050A18` | 가장 깊은 네이비 |
| **Error (보존)** | `#FFB4A8` | `#FFB4A8` ⚠ **유지** | error 시맨틱 |

### 2-c. WCAG 대비비 사전 검증 (정량)

작업 전에 새 조합이 WCAG AA(텍스트 4.5:1, UI 컴포넌트 3:1)를 충족하는지 미리 계산해 두면 작업 중 우왕좌왕할 일이 없습니다.

| 조합 | 라이트 모드 | 다크 모드 | 기준 |
|---|---|---|---|
| 본문 텍스트 (onSurface ↔ surface) | `#1B2D4A` on `#FBF7EC` → **~13.8:1** ✓ | `#E5E0D0` on `#13223D` → **~11.2:1** ✓ | 4.5:1 |
| 본문 텍스트 (onBackground ↔ bg) | `#1B2D4A` on `#F2EDE0` → **~12.5:1** ✓ | `#E5E0D0` on `#0A1525` → **~13.6:1** ✓ | 4.5:1 |
| Primary CTA (onPrimary ↔ primary) | `#FFFFFF` on `#1B2D4A` → **~10.4:1** ✓ | `#0A1525` on `#F2EDE0` → **~13.8:1** ✓ | 3:1 |
| 보조 텍스트 (secondary ↔ surface) | `#5A6478` on `#FBF7EC` → **~5.9:1** ✓ | `#A8B0BD` on `#13223D` → **~6.4:1** ✓ | 4.5:1 |
| Outline (outline ↔ bg) | `#9098A8` on `#F2EDE0` → **~3.2:1** ✓ | `#3A4A66` on `#0A1525` → **~2.1:1** ⚠ | 3:1 |

> **⚠ 다크 모드 outlineVariant `#3A4A66`은 bg `#0A1525`에 대해 2.1:1로 UI 컴포넌트 기준 미달.** 디바이더로만 쓰는 경우 허용되지만, **클릭 가능한 윤곽선(card border, button border)에는 부적합**. 실측 후 필요 시 `#4A5F82`로 상향 검토.

### 2-d. (옵션) 변수명 alias — 의미 불일치 완화

`TacticalRed*` 변수에 네이비 값을 넣으면 6개월 후 유지보수 시 "왜 Red인데 네이비지?" 혼란이 발생합니다. **변수명을 바꾸지 말라**는 v1 제약을 지키면서, **신규 alias만 추가**하는 것이 안전합니다.

```kotlin
// Theme.kt 상단에 alias만 추가 (기존 변수는 그대로):
@Deprecated("Use BrandNavy* instead. Kept for backward compat.", ReplaceWith("BrandNavy"))
val TacticalRedInv: Color = BrandNavy

val BrandNavy        = Color(0xFF1B2D4A)
val BrandNavyDeep    = Color(0xFF0A1525)
val BrandNavySoft    = Color(0xFFA8B5CC)
val BrandNavyFixed   = Color(0xFFE0E5F0)
val BrandCream       = Color(0xFFF2EDE0)
val BrandCreamSoft   = Color(0xFFD8CDA9)
```

⚠ **결정 사항**: 사용자가 alias 추가 옵션 채택 여부를 결정해야 합니다. 기본은 **alias 미추가, 변수명 그대로 둠** (v1 원칙 준수).

---

## 3. Phase 1 — 라이트 모드 변경

### 3-a. `Theme.kt` — Top-level Tactical 색상 상수 값만 교체

```kotlin
// 변경 전:
val TacticalRed       = Color(0xFFFFB3AC)
val TacticalRedDeep   = Color(0xFF6A0008)
val TacticalRedInv    = Color(0xFFBA1A20)
val TacticalRedFixed  = Color(0xFFFFDAD6)
val TacticalOnRed     = Color(0xFF680008)

// 변경 후:
val TacticalRed       = Color(0xFFA8B5CC)   // soft navy (구 FFB3AC 위치)
val TacticalRedDeep   = Color(0xFF0A1525)   // deep navy (EJECT shadow)
val TacticalRedInv    = Color(0xFF1B2D4A)   // brand navy (primary CTA)
val TacticalRedFixed  = Color(0xFFE0E5F0)   // light navy accent
val TacticalOnRed     = Color(0xFF0A1525)   // on-navy ink
```

### 3-b. `Theme.kt` — `LightEjectPalette` 교체

```kotlin
private val LightEjectPalette = EjectPalette(
    red                = TacticalRedInv,
    coral              = TacticalRedInv,           // EjectCoral 체인의 진입점
    coralDim           = TacticalRedDeep,          // 그림자/음영
    bg                 = Color(0xFFF2EDE0),        // 기존 F4F5F7 → warm cream
    surface            = Color(0xFFFBF7EC),        // 기존 FFFFFF → cream surface
    surfaceLow         = Color(0xFFEDE7D4),
    surfaceMid         = Color(0xFFDFD8C2),
    surfaceHigh        = Color(0xFFDFD8C2),        // ⚠ Mid/High 동일값 — 의도된 단순화
    surfaceHighest     = Color(0xFFCFC6AC),
    onSurface          = Color(0xFF1B2D4A),        // navy ink
    secondary          = Color(0xFF5A6478),        // slate
    outlineVar         = Color(0xFFD5D0BD),
    secContainer       = Color(0xFFDFD8C2),
    primaryContainer   = Color(0xFF0F1929),        // deep navy MAYDAY card
    onPrimaryContainer = Color(0xFFD5DCE8),
    secondaryContainer = TacticalRedInv,
)
```

### 3-c. `Theme.kt` — `TacticalLightColors` ColorScheme 동기화

⚠ **v1의 6-c 이슈를 여기서 미리 처리합니다.** `error` 슬롯이 `TacticalRed*` 상수를 참조하면 1번에서 네이비로 바뀐 값을 그대로 받아 error 의미가 깨집니다. **error 4개 줄을 명시적 hex로 고정**:

```kotlin
private val TacticalLightColors = lightColorScheme(
    primary             = TacticalRedInv,                  // navy
    onPrimary           = Color.White,
    primaryContainer    = Color(0xFF0F1929),
    onPrimaryContainer  = Color(0xFFD5DCE8),
    secondary           = Color(0xFF5A6478),
    onSecondary         = Color.White,
    secondaryContainer  = Color(0xFFDFD8C2),
    onSecondaryContainer= Color(0xFF1B2D4A),
    tertiary            = TacticalCyan,                    // 유지 (SHAKE)
    onTertiary          = Color(0xFF1B2D4A),
    background          = Color(0xFFF2EDE0),
    onBackground        = Color(0xFF1B2D4A),
    surface             = Color(0xFFFBF7EC),
    onSurface           = Color(0xFF1B2D4A),
    surfaceVariant      = Color(0xFFDFD8C2),
    onSurfaceVariant    = Color(0xFF5A6478),
    outline             = Color(0xFF9098A8),
    outlineVariant      = Color(0xFFD5D0BD),
    // ── error 슬롯은 빨강으로 명시 고정 (TacticalRed* 참조 금지) ─────────────
    error               = Color(0xFFBA1A20),               // 빨강 = error
    onError             = Color.White,
    errorContainer      = Color(0xFFFFDAD6),
    onErrorContainer    = Color(0xFF680008),
)
```

### 3-d. 알파/그림자 시각 영향 점검 (가벼운 코드 변경 가능)

새 네이비 primary는 빨강보다 어두워서 **alpha 0.1 배너 배경의 가시성이 떨어집니다.**

```kotlin
// CommandContent (MainScreen.kt:917 부근) — 카운트다운 배너
// 변경 전: .background(EjectCoral.copy(alpha = 0.1f))
// 검증 결과 가시성 부족 시:
.background(EjectCoral.copy(alpha = 0.14f))   // 0.1 → 0.14 미세 상향
```

🔍 **검증 방법**: 라이트 모드에서 카운트다운 발동 → 배너 배경이 인지 가능한지 육안 확인. 안 보이면 alpha만 미세 조정 (값은 0.14 권장, 최대 0.18까지).

`EjectButton`의 shadow는 빨강 → 네이비로 바뀌면 발광감이 약해지지만, 이는 **의도된 시각 효과** (조용한 도구 무드). 별도 조정 불필요.

---

## 4. Phase 2 — 다크 모드 변경

### 4-a. `Theme.kt` — `DarkEjectPalette` 교체

```kotlin
private val DarkEjectPalette = EjectPalette(
    red                = Color(0xFFF2EDE0),   // ← cream CTA (라이트와 인버전)
    coral              = Color(0xFFF2EDE0),
    coralDim           = Color(0xFFD8CDA9),   // 어두운 크림 (그림자/음영)
    bg                 = Color(0xFF0A1525),   // deep navy
    surface            = Color(0xFF13223D),   // navy surface
    surfaceLow         = Color(0xFF0F1A2E),
    surfaceMid         = Color(0xFF1F324F),
    surfaceHigh        = Color(0xFF1F324F),
    surfaceHighest     = Color(0xFF2A4063),
    onSurface          = Color(0xFFE5E0D0),   // warm cream ink
    secondary          = Color(0xFFA8B0BD),
    outlineVar         = Color(0xFF3A4A66),   // ⚠ 클릭 윤곽선 용도면 #4A5F82 검토
    secContainer       = Color(0xFF1F324F),
    primaryContainer   = Color(0xFF050A18),   // deepest navy MAYDAY card
    onPrimaryContainer = Color(0xFFA8B0BD),
    secondaryContainer = Color(0xFFF2EDE0),
)
```

### 4-b. `Theme.kt` — `TacticalDarkColors` ColorScheme 동기화

```kotlin
private val TacticalDarkColors = darkColorScheme(
    primary             = Color(0xFFF2EDE0),   // cream CTA
    onPrimary           = Color(0xFF0A1525),   // navy on cream
    primaryContainer    = Color(0xFF050A18),
    onPrimaryContainer  = Color(0xFFE5E0D0),
    secondary           = Color(0xFFA8B0BD),
    onSecondary         = Color(0xFF0A1525),
    secondaryContainer  = Color(0xFF1F324F),
    onSecondaryContainer= Color(0xFFE5E0D0),
    tertiary            = TacticalCyan,                    // 유지 (SHAKE)
    onTertiary          = Color(0xFF0A1525),
    background          = Color(0xFF0A1525),
    onBackground        = Color(0xFFE5E0D0),
    surface             = Color(0xFF13223D),
    onSurface           = Color(0xFFE5E0D0),
    surfaceVariant      = Color(0xFF1F324F),
    onSurfaceVariant    = Color(0xFFA8B0BD),
    outline             = Color(0xFF6A7B96),
    outlineVariant      = Color(0xFF3A4A66),
    // ── error 슬롯 빨강 명시 고정 ──────────────────────────────────────
    error               = Color(0xFFFFB4A8),
    onError             = Color(0xFF680008),
    errorContainer      = Color(0xFF6A0008),
    onErrorContainer    = Color(0xFFFFDAD6),
)
```

---

## 5. 드로어블 XML — 하드코딩 빨강 정리

`grep` 결과(1번 사전 분석)를 기반으로 다음 매핑 적용:

| 기존 빨강 | 신규 네이비 | 의도 |
|---|---|---|
| `#BA1A20` | `#1B2D4A` | brand primary |
| `#6A0008` | `#0A1525` | deep accent |
| `#A82430` | `#1B2D4A` | 동일 매핑 |
| `#FFB3AC` | `#A8B5CC` | soft accent |
| `#FFDAD6` | `#E0E5F0` | fixed accent |
| `#680008` | `#0A1525` | on-primary |

**검증 필수 파일**:
- `app/src/main/res/drawable/ic_eject_mark_red.xml`
- `app/src/main/res/drawable/ic_unmask.xml` (존재 시)
- `app/src/main/res/drawable/ic_disguise_on.xml`
- `app/src/main/res/drawable/ic_disguise_off.xml`

**보존 색** (절대 변경 금지):
- 검정 잉크 (`#1A1A1A`, `#0E0E0F`) — 텍스트/글리프 본체
- 위장 아이콘 4종 (`ic_decoy_*`)의 고유 색상 — 위장 정체성 (계산기 회색, 메모 노랑, 날씨 하늘, 시계 녹색)
- Adaptive launcher 색상

---

## 6. 절대 건드리지 말 것 (Hard Boundary)

- **`FakeIncomingCallScreenV2.kt`** (가짜 수신 화면) — 한 줄도 수정 금지
- **`InCallScreenV2.kt`** (통화 중 화면) — 한 줄도 수정 금지
- **`TacticalCyan`** (`#00DAF3`, SHAKE 시그니처) — 유지
- **`TacticalYellow`** (`#F8BD2A`, SIDE_BUTTON 시그니처) — 유지
- **`TacticalTypography`**, **`TacticalShapes`** — 그대로
- **변수명, 함수명, 파일 경로, 임포트** — 그대로
- **error / errorContainer / onError / onErrorContainer 시맨틱** — 빨강 유지
- **Lottie 애니메이션 JSON** (`drag_confirm.json` 등) — 색상 인라인 변경 금지 (별도 작업)

### ⚠ 알려진 톤 부조화 (이번 작업에서 해결하지 않음, 후속 작업)

- **통화 화면 vs 메인 UI 톤 단절**: 라이트 모드에서 메인 UI는 크림이지만 `FakeIncomingCallScreenV2`는 다크 보라 그라데이션 유지 → 시각적 점프 발생. **이번 PR에서는 의도적으로 미해결**. 후속 PR (`refactor(call-screen): theme-aware gradient`)로 처리. CHANGELOG에 "Known: light mode call screen tone mismatch — tracked in #XXX" 표기.

---

## 7. 검증 (정량 + 정성)

### 7-a. 빌드 검증
1. `./gradlew compileDebugKotlin` — SUCCESSFUL
2. `./gradlew lintRelease` — error 0 유지
3. `./gradlew assembleDebug` — APK 빌드 통과

### 7-b. 라이트 모드 시각 검증 (에뮬레이터 또는 실기기)

다음 화면을 캡처하고 **변경 전 스크린샷과 나란히 비교**:

| 화면 | 체크 항목 |
|---|---|
| MainScreen COMMAND | 배경 warm cream / EJECT 버튼 deep navy + 흰 ⏏ / 시나리오 칩 활성 navy / 트리거 활성 navy |
| 카운트다운 발동 | 배너 가시성 (alpha 조정 필요 여부) |
| 위장 IconButton 다이얼로그 | 확인 버튼 navy, 4개 위장 옵션 색상 그대로 |
| HISTORY 탭 | 카드 배경 surface 톤, 텍스트 가독성 |
| SYSTEMS 탭 (인라인) | 카드 일관성, 테마 세그먼트의 활성 navy |
| SettingsScreen (풀스크린) | 언어/테마/측면버튼/사용법 카드 navy 일관성 |
| PremiumUpgradeDialog | Buy 버튼 navy, Recommended 배지 색 |
| CoachmarkOverlay | 진행 도트 활성 navy, Skip/Next 버튼 navy |
| MAYDAY 카드 (Settings) | 딥 네이비 배경 `#0F1929` + cream 텍스트 — 대비 충분 확인 |

### 7-c. 다크 모드 시각 검증

| 화면 | 체크 항목 |
|---|---|
| MainScreen COMMAND | 배경 deep navy / EJECT 버튼 cream + navy ⏏ — **달처럼 빛나는 효과** 확인 |
| SystemSettings 토글 | 다크 모드에서 cream CTA 가독성 |
| 모든 다이얼로그 | cream 버튼 + navy 텍스트 대비 |

### 7-d. 라이트/다크 토글 검증
- Settings → Theme → LIGHT / SYSTEM / DARK 즉시 전환
- 모든 화면이 즉시 재구성되며 깜빡임 없음 확인
- 시스템 다크 모드 토글에 따라 SYSTEM 옵션이 정확히 추종

### 7-e. 통화 화면 변경 없음 확인 (Hard Boundary)
- 가짜 수신 화면 호출 → 변경 전과 1px도 차이 없는지 확인 (의도된 톤 단절)
- 통화 중 화면 호출 → 변경 전과 동일

### 7-f. 7개 언어 텍스트 잘림 검증
- ko / en / ja / zh-CN / zh-TW / es / hi 각각에서 MainScreen COMMAND 탭만 확인
- 색 변경이 텍스트 잘림에 영향 줘서는 안 되지만, surface 톤 변경으로 텍스트 가독성에 미세 영향 가능

---

## 8. 분할 커밋 전략

한 PR에 다 넣지 말고 **3개 커밋으로 분할** (CodeReview/rollback 용이):

### Commit 1 — Light 모드만
```
feat(theme): light palette to Navy + Cream

- TacticalRed*/Inv/Deep/Fixed/On 상수값 네이비로 (변수명 보존)
- LightEjectPalette: cool gray → warm cream surface
- TacticalLightColors ColorScheme 동기화 (error 슬롯은 빨강 명시 고정)
- 알파 배너 가시성 미세 조정 (0.10 → 0.14, 필요 시)
- 다크 모드 / Cyan / Yellow / Typography / Shape / 통화 화면 unchanged
- 알려진 톤 부조화: 라이트 모드 통화 화면 — 후속 PR에서 해결

Refs: docs/design/NAVY_CREAM_THEME_REFACTOR_PROMPT.md
```

### Commit 2 — 드로어블 색 정리
```
refactor(drawable): hardcoded red → navy in vector assets

- ic_eject_mark_red.xml: BA1A20 → 1B2D4A
- ic_disguise_on/off.xml: 빨강 잉크 → 네이비 잉크
- 검정 텍스트 글리프 (#1A1A1A, #0E0E0F) 보존
- 위장 아이콘 4종 (ic_decoy_*) 고유색 보존
```

### Commit 3 — Dark 모드 인버전
```
feat(theme): dark palette to Navy BG + Cream CTA (inversion)

- DarkEjectPalette: near-black → deep navy (#0A1525)
- onSurface: cool gray → warm cream (#E5E0D0)
- Primary: red CTA → cream CTA (라이트와 인버전, "moon glow" 효과)
- TacticalDarkColors ColorScheme 동기화 (error 슬롯 빨강 유지)
- ⚠ outlineVariant #3A4A66 → 클릭 윤곽선 용도 시 #4A5F82 검토 필요
```

각 커밋 후 **빌드 + 시각 캡처** 확인하고 다음으로 진행. 문제 시 직전 커밋만 revert 가능.

---

## 9. 위험 요소 & Mitigation

| 위험 | Mitigation |
|---|---|
| Lottie `drag_confirm.json`에 빨강이 인라인 → 네이비 메인 + 빨강 링 부조화 | 작업 전 1번 grep으로 확인. 발견되면 후속 PR로 분리 |
| `outlineVariant #3A4A66` (다크)이 클릭 가능 윤곽선이면 대비 부족 | 4-a 주석대로 #4A5F82 상향 검토. 디바이더 용도만이면 유지 |
| `EjectCoral.copy(alpha = 0.1f)` 배너가 네이비에서 안 보임 | 3-d에 따라 alpha 0.14 미세 상향, 라이트 모드 실측 후 결정 |
| 코치마크 진행 도트가 네이비여서 시인성 저하 | 다크 모드에서 cream → 자연스러움. 라이트 모드에서 1번 시각 검증 시 확인 |
| 사용자가 빨강 = "탈출" 인지로 학습되어 있음 | CHANGELOG/PLAY_STORE 업데이트로 "신뢰감 강화 위해 톤 재정렬" 안내 |
| 통화 화면 톤 단절 (Section 6 알려진 이슈) | 이번 PR에서 미해결, 후속 PR 트래킹 |

---

## 10. 작업 흐름 요약

1. **사전 grep** (Section 1) → 영향 범위 메모
2. **Phase 1 작업** — `Theme.kt` 라이트 (3-a, 3-b, 3-c) → 빌드 → 시각 검증 → **Commit 1**
3. **드로어블 정리** (Section 5) → 빌드 → 시각 검증 → **Commit 2**
4. **Phase 2 작업** — `Theme.kt` 다크 (4-a, 4-b) → 빌드 → 시각 검증 → **Commit 3**
5. **전체 검증** (Section 7) — 라이트/다크/토글/통화 화면 변경 없음
6. **CHANGELOG.md 업데이트** — 디자인 결정 + 알려진 이슈 기록
7. **PR 생성** — 변경 전/후 스크린샷 첨부 (라이트/다크 핵심 화면 6개 정도)

---

## 11. 명세 외 결정 가이드 (회색지대용)

작업 중 명세에 없는 회색지대를 만나면 다음 원칙으로 결정:

1. **"빨강 = 긴급/에러"의 시맨틱은 보존, 그 외 모든 빨강은 네이비로**
2. **사용자 학습된 인지를 깨지 않을 것** — EJECT 버튼은 여전히 가장 강한 시각 강조로
3. **시스템 폰트와 정사각 셰이프는 절대 건드리지 않을 것** (Tactical Cockpit 핵심 정체성)
4. **결정이 애매하면 라이트 모드 우선 적용 → 실측 후 다크 모드 결정**
5. **불확실하면 작업을 중단하고 사용자에게 질문** — 잘못 진행하느니 멈추는 게 안전

---

**문서 끝.** 작업 시작 전 Section 1 (사전 grep) 결과를 메모로 남기면 검증 단계가 훨씬 가벼워집니다. 결정이 필요한 회색지대를 만나면 즉시 알려주세요.
