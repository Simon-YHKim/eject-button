# Eject Button — 디자인 리팩토링용 지식 전달 프롬프트

> **이 문서의 용도**: 다른 Claude 인스턴스(디자인 작업 전용)에게 "Eject Button" 앱의 디자인 시스템, 화면 구조, 모든 버튼/인터랙션의 역할을 한 번에 전달하기 위한 지식 패키지입니다. 이 문서를 그대로 디자인 클로드의 컨텍스트에 붙여 넣으면, 사전 탐색 없이 바로 리팩토링 제안 작업을 시작할 수 있습니다.
>
> **작성 기준 시점**: 저장소 `Simon-YHKim/eject-button`의 브랜치 `Eject_Button_app`, 최신 커밋 `10b7bbc` (v1.5.12 통합).

---

## 0. 디자인 클로드에게 — 작업 지시

당신은 안드로이드 앱 **Eject Button**의 디자인 리팩토링을 담당하게 됩니다. 이 문서는 코드를 다시 탐색하지 않아도 될 정도의 컨텍스트를 제공합니다. 작업 시 다음을 지켜주세요.

1. **Tactical Cockpit 디자인 언어를 보존**하되, 일관성/현대성/접근성을 강화하는 방향으로 제안합니다.
2. **하드코딩된 색상/사이즈는 토큰화**합니다. (`Theme.kt`, 또는 새 `Tokens.kt`/`Spacing.kt`로 이동)
3. **Compose 네이티브 패턴 우선**. XML 레이아웃은 사용하지 않습니다.
4. **라이트/다크 모드 양쪽 모두 검증**합니다. (특히 가짜 통화 화면의 그라데이션 이슈)
5. **7개 언어 모두에서 텍스트가 잘리지 않도록** 레이아웃 여유 폭을 확보합니다. (한국어, 일본어, 영어, 중국어 간체/번체, 스페인어, 힌디어)
6. 디자인 결정은 **DESIGN.md (또는 PR 설명)에 근거**와 함께 기록합니다.
7. **변경 전후 비교 가능한 형태**로 작업하세요. (Composable 단위로 commit, screenshot 첨부 권장)

---

## 1. 앱 정체성 — 무엇을 하는 앱인가

**Eject Button**은 사회적 회피 상황에서 **현실적인 가짜 수신 전화**를 발생시켜 사용자에게 자연스러운 탈출구를 제공하는 안드로이드 앱입니다.

### 핵심 가치 제안
- 어색한 자리(끝없는 회식, 어색한 소개팅, 불편한 영업 미팅, 부담스러운 가족 모임 등)에서 자연스럽게 빠져나올 수 있는 "탈출 버튼".
- **장난 앱이 아니라 사회적 안전 도구**로 포지셔닝됨 (PLAY_STORE_ASO.md 기준).

### 주요 기능
- **4가지 트리거 모드**: 즉시 탭 / 타이머 (10초/30초/1분/커스텀) / 흔들기 제스처 / 측면 볼륨 버튼 패턴
- **현실감 있는 수신 화면** (One UI 8.5 스타일, v1.5.12 기준)
- **시나리오(가짜 발신자) 커스터마이즈**: 엄마/상사/친구 프리셋 + 무제한 커스텀 (프리미엄)
- **인콜(통화 중) 화면**: 스크립트 힌트(프리미엄), 통화 시간, 종료
- **위장 런처 아이콘**: Calculator/Memo/Weather/Clock 4종 (강압 환경에서 앱을 숨김)
- **7개 언어 지원**: ko / en / ja / zh-CN / zh-TW / es / hi
- **히스토리 로그**: 가짜 통화 기록

### 수익 모델
프리미엄 일회성 결제 ($2.99 / ₩3,900 / ¥400) + AdMob 광고(배너 + 인터스티셜).
프리미엄 혜택: 광고 제거, 무제한 커스텀 발신자, 인콜 스크립트 힌트.

---

## 2. 기술 스택 / 아키텍처

| 항목 | 값 |
|------|----|
| 언어 | Kotlin 2.0.0 |
| AGP | 8.6.1 |
| UI 프레임워크 | **Jetpack Compose** (XML 레이아웃 미사용) |
| Material | Material3 (커스텀 ColorScheme + CompositionLocal 병용) |
| compileSdk / targetSdk | 35 (Android 15) |
| minSdk | 26 (Android 8) |
| JVM | 17 |
| 아키텍처 | **Single Activity + Composable Screens** (ViewModel 없음, state hoisting 위주) |

### 주요 라이브러리
- `androidx.compose.material3` / `material-icons-extended`
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `com.google.android.gms:play-services-ads` (AdMob)
- `com.android.billingclient:billing-ktx` (Play Billing)
- `com.airbnb.android:lottie-compose:6.4.0` (드래그 컨펌 링 애니메이션, v1.4.2+)
- Firebase (Analytics + Crashlytics) BoM
- `com.microsoft.clarity:clarity` (세션 녹화, 엄격한 마스킹)
- Google User Messaging Platform (UMP)
- Google Play Review API

### 포그라운드 서비스 (3종)
1. **FakeCallOverlayService** — 락스크린 위에 가짜 통화 화면 표시 (`SYSTEM_ALERT_WINDOW`)
2. **ShakeDetectionService** — 화면 OFF 상태에서 흔들기 감지 (`WAKE_LOCK`)
3. **ButtonWatchService** — 볼륨 키 패턴 감지 (MediaSession 기반)

### 소스 구조 (`app/src/main/java/com/ejectbutton/`)
```
EjectApplication.kt
MainActivity.kt
ads/AdManager.kt
analytics/{EjectAnalytics.kt, EjectClarity.kt}
billing/BillingManager.kt
consent/ConsentManager.kt
crash/CrashReportManager.kt
data/{AppStrings.kt, DecoyManager.kt, EjectPrefs.kt, Scenario.kt, SideButtonCommand.kt, PhoneNumberUtil.kt}
service/{FakeCallOverlayService.kt, ShakeDetectionService.kt, ButtonWatchService.kt,
         ButtonPatternDetector.kt, CountdownBus.kt, SideButtonTrigger.kt}
ui/
  call/{FakeIncomingCallScreenV2.kt, InCallScreenV2.kt,
        FakeIncomingCallScreen.kt(legacy), FakeInCallScreen.kt(legacy)}
  coachmark/{CoachmarkOverlay.kt, CoachmarkState.kt}
  main/{MainScreen.kt, OnboardingScreen.kt, SettingsScreen.kt,
        PremiumUpgradeDialog.kt, RewardedAdDialog.kt}
  theme/{Theme.kt, TacticalComponents.kt, LegacyCallTheme.kt}
  util/HistoryEntryParser.kt
geofence/GeofenceTransitionReceiver.kt
```

---

## 3. 디자인 시스템 — "Tactical Cockpit Interface"

전투기 계기판/지휘 콘솔에서 영감을 받은 **그레이스케일 기반 + 강조색 최소 사용** 디자인 언어. 모서리는 **모두 0dp (정사각형)**, 타이포는 **시스템 SansSerif + 좁은 자간**으로 정보 밀도를 높입니다.

### 3.1 컬러 토큰 (전체 `app/src/main/java/com/ejectbutton/ui/theme/Theme.kt`)

#### Dark Mode (기본)
```
// 표면 계층 (모두 회색)
TacticalLowest     #0C0E10  — 디바이더, 1dp 띠
TacticalBase       #121416  — 배경
TacticalLow        #1A1C1E  — container-low
TacticalContainer  #1E2022  — 카드 본체, 기본 surface
TacticalHigh       #282A2C  — container-high
TacticalHighest    #333537  — container-highest, housing
TacticalBright     #37393B  — surface-bright, press 상태

// 강조색 (사용 빈도 최소)
TacticalRed        #FFBAAC
TacticalRedDeep    #6A0008  — EJECT 버튼 바디
TacticalRedInv     #BA1A20  — 버튼 stroke, 주 강조색
TacticalRedFixed   #FFADAD
TacticalOnRed      #680008
TacticalYellow     #F8BD2A  — 위험 스트라이프, SIDE_BUTTON
TacticalCyan       #00DAF3  — LED, SHAKE 트리거, 섹션 바

// 텍스트 / 디바이더
TacticalOnSurface  #E2E2E5  — 본문 텍스트
TacticalOnVariant  #C6C6CB  — 보조 텍스트
TacticalOutline    #909095  — 약한 디바이더
TacticalOutlineVar #45474B  — 중간 디바이더
```

#### Light Mode (반전)
- 배경 `#F4F5F7`, surface `#FFFFFF`, onSurface `#1A1C1E`, secondary `#55575B`.
- 모든 토큰이 데이터 클래스 `EjectPalette` 안에 있고, `LocalEjectPalette` CompositionLocal로 런타임 주입됨.
- 접근자: `EjectRed`, `EjectBg`, `EjectSurface`, `EjectOnSurface`, `EjectSecondary`, `EjectCoral`, `EjectOutlineVar`, `EjectSurfaceMid` 등 (`@ReadOnlyComposable` getter).

#### ⚠ 주의 — 하드코딩 색상 (리팩토링 후보)
- `FakeIncomingCallScreenV2.kt` 그라데이션은 `Color(0xFF0F0A1A)`, `Color(0xFF1A1528)` 등 하드코딩 → **라이트 모드에서 부자연스러움**.
- `InCallScreenV2.kt`: `TileBg = Color(0xFF3C374B).copy(alpha = 0.55f)`, `RecTileBg = Color(0xFF1C1C22)` 파일 상단에 하드코딩.

### 3.2 타이포그래피 (`TacticalTypography` in Theme.kt)
- 폰트 패밀리: **System SansSerif** (커스텀 폰트 파일 없음, 폰트 리소스 디렉토리 사용 안 함)
- `bodySmall`은 System Monospace
- 좁은 자간(-0.02em ~ +0.18em)으로 "콕핏" 정보 밀도 연출

| 스타일 | 크기 | 굵기 | letter-spacing |
|--------|------|------|----------------|
| displayLarge | 48sp | Black | -0.02em |
| displayMedium | 40sp | ExtraBold | -0.02em |
| headlineLarge | 26sp | Bold | 0.02em |
| titleLarge | 18sp | Bold | 0.05em |
| titleMedium | 15sp | SemiBold | 0.08em |
| bodyLarge | 15sp | Normal | — |
| bodySmall (mono) | 12sp | Medium | 0.05em |
| labelSmall | 10sp | SemiBold | 0.18em |

### 3.3 셰이프
```kotlin
private val Square = RoundedCornerShape(0.dp)
// Material3 Shapes에 전역 적용 (TacticalShapes)
```
**예외**: 헤더/카드/배너 일부는 `RoundedCornerShape(12.dp ~ 16.dp)` 직접 사용 (특히 SettingsScreen). → 리팩토링 시 일관성 검토 필요.

### 3.4 드로어블 (15개, 모두 벡터 XML)
```
ic_decoy_calculator_bg/fg.xml      위장 1 (어두운 회색 + 그리드)
ic_decoy_memo_bg/fg.xml            위장 2 (연한 노랑 + 노트패드)
ic_decoy_weather_bg/fg.xml         위장 3 (하늘색 + 해/구름)
ic_decoy_clock_bg/fg.xml           위장 4 (어두운 녹색 + 시계)
ic_eject_mark.xml                  ⏏ 글리프, tint 가능
ic_eject_mark_red.xml              빨간 ⏏ (하드코딩 색)
ic_disguise_off.xml                위장 비활성 (가면 위 + ⏏ 아래)
ic_disguise_on.xml                 위장 활성 (⏏ 위 + 가면 떨어짐, -10° 기울기)
ic_launcher_background.xml         적응형 런처 배경
ic_launcher_foreground.xml         적응형 런처 전경
```
런처 아이콘은 **adaptive(bg+fg+monochrome)** 지원, Android 13+ Material You 호환.

### 3.5 위장 런처 별칭 메커니즘
`AndroidManifest.xml`에 4개 `<activity-alias>` 정의 → `PackageManager.setComponentEnabledSetting()`로 토글. 사용자는 홈스크린에서 일반 앱처럼 가장된 아이콘을 누름 → Eject Button이 실행됨. UI 재시작 없이 즉시 전환.

### 3.6 문자열 리소스
경로: `app/src/main/res/values{,-en,-ja,-zh-rCN,-zh-rTW,-es,-hi}/strings.xml`
- 기본 언어: 한국어 (`values/strings.xml`)
- 모든 UI 문자열은 `LocalAppStrings.current.{key}` 로 접근 (한 번에 7개 언어 자동 전환)

---

## 4. 화면 인벤토리 (Composable 단위)

### 4.1 OnboardingScreen.kt (357 lines)
첫 사용자에게만 노출되는 4페이지 스와이프 + 마지막 페이지 onboarding 완료.
- `EjectPrefs.coachmarkSeen == false` 일 때만 노출.
- 완료 후 MainScreen으로 이동하며 곧바로 CoachmarkOverlay 6단계 튜토리얼이 실행됨.

### 4.2 MainScreen.kt (2525 lines, 가장 큰 화면)
**Single Activity의 메인 진입점**. 내부에 3개 탭 (COMMAND / HISTORY / SYSTEMS).
주요 내부 Composable:
- `MainScreen` (134) — 진입점, 탭 상태 + 다이얼로그 호스팅
- `CommandContent` (861) — COMMAND 탭 본문
- `StitchTopBar` (1093) — 상단바 (위장 토글 + 설정 버튼)
- `SideButtonModeCard` (1265) — 측면 버튼 모드 표시 카드
- `HistoryContent` (1311) / `HistoryEntryCard` (1362) — HISTORY 탭
- `SystemsContent` (1429) — SYSTEMS 탭 (인라인 설정)
- `EjectButton` (1883) — 중앙 빨간 EJECT 버튼
- `CallerChips` (1966) — 시나리오(발신자) 칩 리스트
- `TriggerTimeRow` / `TriggerModeRow` / `TriggerChoiceRow` — 트리거 선택 UI
- `BottomBar` (2133) — 하단 탭 네비게이터
- `CustomDelayDialog` (2215) / `AddCallerDialog` (2245) — 다이얼로그
- `NativeAdCard` (2400) — AdMob 네이티브 광고 카드

### 4.3 SettingsScreen.kt (1448 lines)
SYSTEMS 탭의 외부 fullscreen 버전 (← 뒤로가기 버튼 포함).
- 동일한 설정 내용을 `ColumnScope.SettingsBodyInline` (1031~) 로 재사용 가능하게 분리.
- 다이얼로그: `SideButtonCommandPickerDialog`, `LanguagePickerDialog`, `HowToUseDialog`, `CustomCommandRecordingDialog`, 프리셋 복원 확인.
- 공용 컴포넌트: `EjectSectionHeader`, `EjectToggleCard`, `EjectLinkCard`.

### 4.4 FakeIncomingCallScreenV2.kt (351 lines)
v1.5.7+ 현행 가짜 수신 화면. One UI 8.5 스타일.
- 두 개의 `PulsingCallButton` (Accept 녹색 / Decline 빨강)
- 드래그 임계값 `300px` (v1.5.7에서 120→300으로 증가)
- 드래그 중 Lottie 동심원 링 (`animations/drag_confirm.json`) 표시
- 임계값 이상 드래그 후 release → 트리거; 미달 시 snap back
- 파스텔 라디얼 그라데이션 배경 (다크 보라/파랑, **하드코딩** → 리팩토링 후보)
- "Send message" 스와이프 힌트 (One UI 패턴)

### 4.5 InCallScreenV2.kt (493 lines)
사용자가 가짜 통화를 받아준 후의 통화 중 화면.
- 6개의 `ControlTile` (Speaker, Mute, Keypad, Add call, Video, Bluetooth 등)
- 1개의 `RecordingTile` (녹음 표시, 상태에 따라 색 변화)
- AI Assist 알약 (스크립트 힌트 노출, **프리미엄 전용**)
- 큰 빨간 종료 통화 버튼

### 4.6 CoachmarkOverlay.kt (295 lines) + CoachmarkState.kt (30 lines)
첫 사용 시 6단계 스포트라이트 튜토리얼.
- `CoachmarkHost` — 상태 기반 렌더링
- `CoachmarkOverlay` — 디밍 + 스포트라이트 컷아웃
- `TooltipCard` — 단계 안내 카드 (제목, 본문, "STEP n/6", 진행 도트, Skip/Next)
- Spotlight shape: `Circle` / `RoundRect` / `Capsule`

### 4.7 PremiumUpgradeDialog.kt (450 lines)
프리미엄 구매 ModalBottomSheet. Monthly/Annual 듀얼 플랜 카드 + Buy/Restore/Cancel 버튼.

### 4.8 RewardedAdDialog.kt (366 lines)
"광고 1회 시청 / Premium 영구 잠금 해제" 양자택일 시트. 커스텀 발신자 추가 등 프리미엄 기능 사용 시도 시 노출.

### 4.9 TacticalComponents.kt (171 lines)
공용 컴포넌트:
- `Modifier.microGridBackground()` — 그리드 배경 (콕핏 느낌)
- `TacticalSectionHeader`, `CyanLedIndicator`, `TacticalCard`, `TacticalBackground`, `TacticalDividerBand`

---

## 5. 버튼 / 인터랙션 상세 인벤토리 (60+ 요소)

각 요소: **위치 → 모양 → 활성 조건 → 탭 동작 → 결과 화면**.

### 5.1 OnboardingScreen — 4페이지 스와이프 튜토리얼

| 요소 | 위치 | 모양 | 동작 |
|------|------|------|------|
| **Skip TextButton** | 상단 우측 | 텍스트 링크 ("건너뛰기") | 마지막 페이지(완료)로 점프 (`index = pages.size`) |
| **진행 도트 (4개)** | 상단 중앙 | 8dp 원, 선택 시 EjectCoral | 시각적 진행도만 표시 (탭 불가) |
| **"다음" Button** | 페이지 하단 | 빨강 filled, 풀폭 | `index++` 또는 마지막 페이지에서 onboarding 완료 |
| **"다시 보기" Button** | 마지막 페이지 | 빨강 filled, 풀폭 | `index = 0` (1페이지로 회귀) |
| **"시작하기" OutlinedButton** | 마지막 페이지 | 빨강 outline | onboarding 완료 → MainScreen + Coachmark 트리거 |
| **수평 스와이프 제스처** | 전체 페이지 영역 | — | 좌/우로 페이지 전환 (HorizontalPager) |

### 5.2 MainScreen — StitchTopBar (모든 탭 공통 상단)

| 요소 | 위치 | 모양 | 동작 |
|------|------|------|------|
| **앱 타이틀** | 상단 좌측 | "EJECT BUTTON" 굵은 텍스트 | 비-인터랙티브 |
| **위장 토글 IconButton** | 상단 우측 (설정 옆) | `ic_disguise_off` (24dp) or `ic_disguise_on` (위장 활성 시) | (상태 1) 위장 picker 다이얼로그 열기 → 4개 옵션(계산기/메모/날씨/시계) 중 택1 → `PackageManager`로 런처 별칭 변경 + 스낵바 확인. (상태 2) 복구 확인 다이얼로그 → 확인 시 `DecoyManager.setActive(DEFAULT)` |
| **설정 IconButton** ⚙ | 상단 우측 끝 | gear 아이콘 (24dp) | `SettingsScreen` 풀스크린으로 전환 (COMMAND 탭에서 코치마크 step 4 spotlight) |

### 5.3 MainScreen — COMMAND 탭

| 요소 | 위치 | 모양 | 동작 |
|------|------|------|------|
| **카운트다운 배너** | 상단 (조건부) | EjectCoral 10% 배경, 16dp radius, 14sp 글씨 | `countdown > 0`일 때만 표시. "N초 후 가짜 전화 발생" 알림 |
| **사이드 버튼 대기 배너** | 상단 (조건부) | 동일 스타일 | `sideButtonStandby == true` & `countdown == 0`일 때. "측면 버튼을 누르면 발동" |
| **흔들기 대기 배너** | 상단 (조건부) | 동일 스타일 | `shakeStandby == true`일 때. "기기를 흔들면 발동" |
| **시나리오 칩 리스트 (CallerChips)** | 중상단 | 가로 스크롤 칩 (선택 시 EjectCoral 배경) | 탭 → 선택된 발신자(Scenario) 변경. 길게 누름 → 삭제(커스텀만). "+ 추가" 칩 → AddCallerDialog (프리미엄 게이트 가능) |
| **트리거 시간 Row** | 중앙 | 4개 segmented 버튼 (즉시 / 10초 / 30초 / 1분 / 커스텀) | 선택 시 `TimeChoice` 업데이트. 커스텀 탭 → `CustomDelayDialog` |
| **트리거 모드 Row** | 시간 아래 | 3개 segmented (TAP / SHAKE / SIDE BUTTON) | 선택 시 `ModeChoice` 업데이트. `SIDE BUTTON` 선택 시 `SideButtonModeCard` 노출 |
| **SideButtonModeCard** | 모드 Row 아래 (조건부) | 카드, "커맨드 설정 →" 링크 | 탭 → `SettingsScreen`의 측면 버튼 커맨드 설정으로 이동. `sideButtonNotice == true`이면 권한 안내 표시 |
| **EJECT 버튼 (중앙 빨강 원)** | 화면 중앙 (가장 큰 요소) | 232dp 원, EjectCoral 채우기 + 4dp stroke + 18dp shadow, 내부 ⏏ (108sp) + "탈출" (20sp), 호흡 펄스 (1→1.03 scale, 1.2초) | **(일반 모드)** 즉시 가짜 통화 시작 또는 카운트다운/대기 시작. **(취소 모드, `isCancelMode==true`)** ✕ + "취소" 표시, 탭 시 카운트다운/대기 취소 |
| **하단 광고 영역** | 화면 하단 | NativeAdCard | `isPremium == false`일 때만 노출 |

### 5.4 MainScreen — HISTORY 탭

| 요소 | 위치 | 모양 | 동작 |
|------|------|------|------|
| **HistoryEntryCard 리스트** | 본문 | 카드 (날짜/시간 + 발신자명 + 통화 길이) | 비-인터랙티브 (조회만) |
| **"히스토리 비우기" 액션** | (SettingsScreen 내) | 링크 카드 | 확인 다이얼로그 → `EjectPrefs.clearHistory()` |

### 5.5 MainScreen — SYSTEMS 탭 / SettingsScreen

| 요소 | 위치 | 모양 | 동작 |
|------|------|------|------|
| **← 뒤로가기** | 상단 좌측 (풀스크린 모드) | 36dp 정사각, RoundedCornerShape(12dp), EjectSurfaceMid 배경 | `onDismiss` → MainScreen으로 |
| **언어 선택 카드** | 상단 | 16dp radius 카드, 🌐 아이콘 + 현재 언어 nativeName + ›  | 탭 → `LanguagePickerDialog` (7개 옵션, RadioButton) |
| **테마 모드 세그먼티드** | 언어 아래 | 3분할 (LIGHT ☀ / SYSTEM ⚙ / DARK 🌙), 선택 시 EjectCoral 배경 + 흰 글씨 | 탭 → `ThemeMode` 변경 + 즉시 재구성 |
| **벨소리 ToggleCard** | | EjectToggleCard | 토글 시 `EjectPrefs.saveRingtone(value)` |
| **진동 ToggleCard** | | EjectToggleCard | 토글 시 `EjectPrefs.saveVibration(value)` |
| **햅틱 ToggleCard** | | EjectToggleCard | 토글 시 `EjectPrefs.saveHaptic(value)` |
| **튜토리얼 ToggleCard** | | EjectToggleCard | `showManualNext` 토글. ON → 다음 실행 시 Onboarding 재등장 |
| **측면 버튼 커맨드 카드** | | EjectLinkCard, 현재 커맨드명 표시 + › | 탭 → `SideButtonCommandPickerDialog` (사용자 정의 커맨드 녹화 옵션 포함) |
| **사용법 (How to Use)** | | EjectLinkCard | 탭 → `HowToUseDialog` (단계별 설명) |
| **프리미엄 카드** | (조건부) | EjectCoral 강조 카드 + "잠금 해제" CTA / "✓ Premium 활성" | 미구매 시 → `PremiumUpgradeDialog` (월/연 플랜 ModalBottomSheet) |
| **복원 구매 버튼** | | TextButton | `BillingManager.queryPurchases()` → `saveIsPremium` |
| **프리셋 복원 버튼** | | EjectLinkCard | 삭제한 mom/dad 프리셋 복원 + 완료 다이얼로그 |
| **개인정보 처리방침** | | EjectLinkCard | 외부 브라우저로 privacy-policy.html 오픈 |
| **이용약관** | | EjectLinkCard | 외부 브라우저로 약관 페이지 오픈 |
| **버전 정보** | 하단 | 단순 텍스트 ("v1.5.12") | 비-인터랙티브 (BuildConfig.VERSION_NAME) |

### 5.6 FakeIncomingCallScreenV2 (가짜 수신 화면)

| 요소 | 위치 | 모양 | 동작 |
|------|------|------|------|
| **발신자 이름** | 상단 중앙 | 큰 글씨 + 라벨 ("휴대전화") | 비-인터랙티브 |
| **"Call Assist" pill** | 발신자 이름 아래 | 알약형 작은 배지 | 비-인터랙티브 (접근성 라벨 표시) |
| **펄스 링 (idle)** | 두 버튼 주변 | EjectCoral/녹색 발산 애니메이션 (drag 중엔 사라짐) | 시각적 어트랙터 |
| **Lottie 드래그 오버레이** | 두 버튼 위 | 동심원 링, dragFraction(0~0.95)로 progress 제어 | 드래그 중 확장; 임계값(300px) 도달 시 색 강화 |
| **Accept PulsingCallButton (녹색)** | 화면 하단 우측 | 큰 녹색 원 (전화 받기 아이콘) | (탭) 즉시 `onAccept()`. (드래그 ≥300px) release 시 `onAccept()` → InCallScreenV2로 전환 |
| **Decline PulsingCallButton (빨강)** | 화면 하단 좌측 | 큰 빨강 원 (수화기 down 아이콘) | (탭) 즉시 `onDecline()`. (드래그 ≥300px) release 시 `onDecline()` → 통화 종료, MainScreen으로 |
| **"Send message" 스와이프 힌트** | 하단 (One UI) | 작은 위쪽 화살표 + 라벨 | 시각 안내만 (현재 액션 미구현) |

### 5.7 InCallScreenV2 (통화 중 화면)

| 요소 | 위치 | 모양 | 동작 |
|------|------|------|------|
| **발신자 이름 + 아바타** | 상단 | 큰 글씨 + 원형 아바타 | 비-인터랙티브 |
| **통화 시간 타이머** | 이름 아래 | 모노스페이스 시:분:초 | 1초마다 자동 증가 |
| **RecordingTile** | 첫 컨트롤 줄 | TileBg 또는 RecTileBg(녹화 시) | 탭 → 녹화 토글 (UI만, 실제 녹음 없음) |
| **ControlTile (Speaker)** | 컨트롤 그리드 | TileBg, 아이콘 + 라벨 | 탭 → 스피커 시각 토글 (실제 오디오 X) |
| **ControlTile (Mute)** | 컨트롤 그리드 | 동일 | 탭 → 음소거 시각 토글 |
| **ControlTile (Keypad)** | 컨트롤 그리드 | 동일 | 탭 → 키패드 시각 표시 |
| **ControlTile (Add call)** | 컨트롤 그리드 | 동일 | 탭 → 시각 효과만 |
| **ControlTile (Video)** | 컨트롤 그리드 | 동일 | 탭 → 시각 효과만 |
| **ControlTile (Bluetooth)** | 컨트롤 그리드 | 동일 | 탭 → 시각 효과만 |
| **AI Assist Pill** | 컨트롤 영역 위 | 알약 배지 + 작은 별 아이콘 | (Premium만) 탭 → 스크립트 힌트 캐러셀 표시. (Free) 탭 → `PremiumUpgradeDialog` |
| **종료 통화 버튼 (빨강)** | 하단 중앙 | 큰 빨강 원, 수화기 down 아이콘 | 탭 → `onEndCall()` → 히스토리 기록 + MainScreen으로 복귀 |
| **상단 상태 배지 (Signal/Battery)** | 시스템 status bar 위 | mock 신호/배터리 아이콘 | 비-인터랙티브 (현실감 연출) |

### 5.8 CoachmarkOverlay (6단계 튜토리얼)

| 요소 | 위치 | 모양 | 동작 |
|------|------|------|------|
| **디밍 배경** | 풀스크린 | 검정 85% alpha | 탭 무시 (Next/Skip만 작동) |
| **스포트라이트 컷아웃** | 현재 step 타겟 영역 | Circle / RoundRect / Capsule 셰이프 | 시각 강조만 (해당 영역은 디밍 제외) |
| **TooltipCard** | 스포트라이트 옆 | EjectSurface 배경, 16dp radius, 화살표 indicator | "STEP n" 배지 + 6개 진행 도트 + 제목 + 본문 + Skip TextButton + 다음 Button |
| **Skip TextButton** | 카드 하단 좌측 | 텍스트 링크 | `state.dismiss()` → 코치마크 종료 + `coachmarkSeen=true` 저장 |
| **다음 Button** | 카드 하단 우측 | 빨강 filled | (마지막 step 아닐 때) `state.next()`. (마지막) `state.dismiss()` + 종료 콜백 |

**6단계 구성**: ① 시나리오 칩 → ② 시간 선택 → ③ 모드 선택 → ④ 설정 ⚙ 버튼 → ⑤ EJECT 버튼 → ⑥ 위장 토글.

### 5.9 PremiumUpgradeDialog (ModalBottomSheet)

| 요소 | 동작 |
|------|------|
| **Sheet drag handle** | 위로 드래그 → 시트 dismiss |
| **Monthly Plan Card** | RadioButton + 가격 + 혜택 리스트. 탭 → `selectedPlan = MONTHLY` |
| **Annual Plan Card** (Recommended 배지) | RadioButton + 할인 가격 + 혜택. 탭 → `selectedPlan = ANNUAL` |
| **Buy Button** | 빨강 filled, 풀폭. 탭 → `BillingManager.launchBillingFlow(selectedPlan)` |
| **Restore TextButton** | 텍스트 링크. 탭 → `BillingManager.queryPurchases()` |
| **Cancel TextButton** | 텍스트 링크. 탭 → sheet 닫음 |

### 5.10 RewardedAdDialog (ModalBottomSheet)

| 요소 | 동작 |
|------|------|
| **Watch Ad Card** | RadioButton + "광고 1회 시청" 설명. 탭 → 선택 |
| **Premium Card** | RadioButton + "영구 잠금 해제". 탭 → 선택 |
| **Continue Button** | 선택에 따라 `AdManager.showRewardedAd()` 또는 `PremiumUpgradeDialog` 열기 |
| **Cancel TextButton** | sheet 닫음 |

### 5.11 다이얼로그 모음

| 다이얼로그 | 트리거 | 내용 |
|----------|-------|-----|
| **CustomDelayDialog** | 시간 row "커스텀" 탭 | 슬라이더 또는 숫자 입력 → 임의 초 설정 → "확인" → `customDelaySec` 업데이트 |
| **AddCallerDialog** | 시나리오 칩 "+ 추가" | TextField (발신자명), 옵션(전화번호/노트), 프리미엄 게이트 가능 |
| **SideButtonCommandPickerDialog** | SYSTEMS의 측면 버튼 카드 | 사전 정의 커맨드 리스트 + "커스텀 녹화" → `CustomCommandRecordingDialog` |
| **LanguagePickerDialog** | SYSTEMS의 언어 카드 | 7개 옵션 (네이티브 표기 + ISO 코드) RadioButton |
| **HowToUseDialog** | SYSTEMS의 사용법 카드 | 단계별 `HowToUseStep` 컴포넌트 (번호 + 텍스트) |
| **DisguisePickerDialog** | 상단바 위장 IconButton | 4개 옵션 (Calculator/Memo/Weather/Clock) 카드 |
| **UnmaskConfirmDialog** | 위장 활성 상태에서 위장 IconButton | "위장을 복구하시겠습니까?" 확인 |

---

## 6. 사용자 플로우 (End-to-End)

### Flow 1 — 첫 실행 (Onboarding)
앱 실행 → `EjectPrefs.coachmarkSeen == false` → OnboardingScreen (4페이지 스와이프) → "시작하기" → MainScreen + CoachmarkOverlay 6단계 → "Skip" 또는 마지막 "다음" → `coachmarkSeen = true` 저장.

### Flow 2 — 핵심 루프 (가짜 통화)
COMMAND 탭 → 시나리오 칩 선택 → 시간 선택 → 모드 선택 → EJECT 버튼 탭 → (지연/대기/즉시) → FakeIncomingCallScreenV2 (전화 받기 시작) → Accept 탭/드래그 → InCallScreenV2 → End Call → 히스토리 기록.

### Flow 3 — 발신자 커스터마이즈
COMMAND 탭 → "+ 추가" 칩 → (Free) RewardedAdDialog → 광고 시청 또는 프리미엄 구매 → AddCallerDialog → 이름/번호 입력 → 저장 → 칩 목록에 추가.

### Flow 4 — 위장 활성화
상단바 위장 IconButton → DisguisePickerDialog → 4개 중 1개 선택 → `DecoyManager.setActive(<Decoy>)` → 런처 별칭 즉시 변경 + 스낵바 안내 → 홈스크린의 아이콘이 즉시 가장됨.

### Flow 5 — 위장 복구
상단바 위장 IconButton (가면 상태) → UnmaskConfirmDialog → "확인" → `DecoyManager.setActive(DEFAULT)` → 런처 복구.

### Flow 6 — 프리미엄 구매
SYSTEMS 탭 → 프리미엄 카드 → PremiumUpgradeDialog (월/연 플랜) → Buy → Google Play Billing → `saveIsPremium(true)` → 광고 제거 + 기능 잠금 해제.

---

## 7. 디자인 강점 / 약점 (리팩토링 우선순위)

### Strengths (유지할 것)
1. **컬러 위계가 의도적**: 회색 8단계 + 빨강/노랑/시안만 강조. 시각 노이즈 최소.
2. **다크 모드 기본**: 배터리 효율 + "비상 도구" 무드.
3. **EjectPalette CompositionLocal로 런타임 테마 전환**: 라이트/다크 즉시 반영.
4. **위장 런처 별칭 메커니즘**: PackageManager 정공법, 액티비티 재시작 없이 작동.
5. **시스템 폰트만 사용**: 폰트 파일 관리 부담 없음, 7개 언어 안전.
6. **위장 아이콘 4종이 모두 커스텀 벡터**: 스톡 아이콘 미사용, 위화감 적음.

### Weaknesses (리팩토링 후보) — 우선순위 순
1. **하드코딩 색상 (Priority: HIGH)**
   - `FakeIncomingCallScreenV2.kt`의 그라데이션 (`#0F0A1A`, `#1A1528` 등)
   - `InCallScreenV2.kt`의 `TileBg`, `RecTileBg`
   - `ic_eject_mark_red.xml`의 하드코딩 빨강
   - **권장**: `EjectPalette`에 `callBackgroundGradient`, `tileBg`, `recTileBg` 토큰 추가 → 라이트/다크 모드 양쪽 보정.

2. **하드코딩 사이즈/매직 넘버 (Priority: MEDIUM)**
   - `dragThresholdPx = 300f` (FakeIncomingCallScreenV2)
   - `EjectButton` 232dp, ⏏ 108sp, "탈출" 20sp 등이 inline
   - **권장**: 새 파일 `ui/theme/Spacing.kt` (또는 `Tokens.kt`) 생성 → `buttonEjectSize`, `dragThresholdPx`, `displayIconSize` 등 추출.

3. **셰이프 일관성 (Priority: MEDIUM)**
   - 전역 셰이프는 `0.dp` (정사각) 인데, 실제 SettingsScreen은 12~16dp radius 사용.
   - **권장**: 셰이프 정책 결정 — "기능 카드는 12dp, 콕핏 chrome은 0dp" 같은 룰을 DESIGN.md에 명문화.

4. **레거시 가짜 통화 화면 정리 (Priority: LOW)**
   - `FakeIncomingCallScreen.kt` (v1 구버전) 와 `FakeInCallScreen.kt` (구버전 InCall) 가 아직 존재.
   - **권장**: 사용처 grep 후 삭제 또는 명확히 deprecated 표시.

5. **드로어블 디렉토리 평면 구조 (Priority: LOW)**
   - 15개가 모두 `drawable/`에 평면 배치.
   - **권장**: `drawable/launcher/`, `drawable/decoy/`, `drawable/ui/`, `drawable/icons/` 서브 디렉토리.

6. **라이트 모드 QA 미비 (Priority: HIGH)**
   - 가짜 통화 화면이 다크 전용으로 디자인됨 → 라이트 모드 사용자가 모드 전환 시 그라데이션이 부자연스러움.
   - CHANGELOG에 라이트 모드 통화 화면 QA 흔적 없음.
   - **권장**: 라이트/다크 양 모드 스크린샷 캡처 후 비교 PR.

7. **Material3 컴포넌트 저활용 (Priority: 결정 사항)**
   - 대부분 Box/Row/Column + 커스텀으로 구현. M3 Button/Card/Chip 미사용 (의도적).
   - 의도된 디자인 결정이지만, 신규 팀원 진입 비용 증가.
   - **권장**: 의도적 결정임을 DESIGN.md에 기록 + 커스텀 컴포넌트 라이브러리(`TacticalComponents.kt`) 확장.

---

## 8. 작업 시 참고 자료

### 필수 참고 파일
- `app/src/main/java/com/ejectbutton/ui/theme/Theme.kt` — 모든 컬러/타이포/셰이프 토큰
- `app/src/main/java/com/ejectbutton/ui/theme/TacticalComponents.kt` — 공용 컴포넌트
- `CHANGELOG.md` — v1.4.x ~ v1.5.12의 디자인 결정 기록
- `HANDOFF_v1.4.4.md` — 직전 세션 작업 컨텍스트
- `.claude/instincts/project-patterns.md` — 프로젝트 컨벤션
- `.claude/instincts/mistakes-learned.md` — 과거 실수 메모

### 디자인 결정이 기록된 위치
- **인라인 주석** (Theme.kt, MainScreen.kt에 한국어로 다수)
- **CHANGELOG.md** — 특히 v1.5.7 (One UI 스타일 도입), v1.5.12 (위장 토글 추가) 항목
- `docs/design/` — 일부 핸드오프 노트 (`bottombar-smooth-v1.5.1`, `coachmark-v1.5.1`, `incoming-call-fix-v1.5.1` 등)

### 디자인 결정이 기록되지 않은 위치 (이번에 채울 것)
- **DESIGN.md** — 아직 존재하지 않음. 디자인 시스템의 단일 진실의 원천으로 새로 만들 가치 있음.

---

## 9. 권장 작업 흐름 (디자인 클로드용)

1. 이 문서 + `Theme.kt` + `CHANGELOG.md` 최근 10 entries 읽기.
2. 리팩토링 후보 우선순위 (Section 7) 중 사용자와 1~2개 선택.
3. **DESIGN.md 초안 작성** — 시스템의 의도와 토큰을 명문화.
4. 토큰화 작업 (`Tokens.kt`/`Spacing.kt` 생성, 하드코딩 값 추출).
5. 라이트 모드 QA 진행 — `FakeIncomingCallScreenV2`, `InCallScreenV2` 라이트 모드 시각 검증.
6. 변경 사항을 Composable 단위로 atomic commit, Conventional Commits 형식 (`refactor(theme): extract call screen gradient to token`).
7. CHANGELOG에 디자인 결정 + 근거 추가.

---

**문서 끝.** 이 패키지로 충분하지 않은 부분이 발견되면 즉시 알려주세요. Eject Button 프로젝트의 디자인을 한 단계 끌어올리는 데 함께해주셔서 감사합니다.
