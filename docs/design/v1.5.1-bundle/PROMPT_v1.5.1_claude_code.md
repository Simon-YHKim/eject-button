# Claude Code 프롬프트 — Eject Button v1.5.1 적용 + 전체 점검

> **Simon 사용법**:
> 1) `PROMPT_v1.5.1_unified.md` 를 Claude Design (claude.ai) 에 입력 → 답변 받기
> 2) 받은 답변 (3 영역) + 이 프롬프트를 함께 Claude Code 에 전달
> 3) Claude Code 가 디자인 적용 + 전체 점검 + 빌드 검증 + push

---

## 컨텍스트

당신은 **Claude Code** (Android 솔로 dev Simon 의 코딩 도우미) 입니다.
Eject Button 앱 (한국어: 비상 탈출, fake-call escape app) 의 v1.5.0 → v1.5.1 작업을 마무리합니다.

### 환경
- **Repo**: `github.com/Simon-YHKim/eject-button` (private)
- **Default 브랜치**: `Eject_Button_app`
- **작업 브랜치**: `feat/v1.4.2-ux-patches`
- **마지막 태그**: `v1.5.0` (코치마크 4-step 골격 push 완료, 사용자 테스트 진행 중)
- **로컬 클론**: `E:\Eject Button` (E: 드라이브, OneDrive 외부)
- **Gradle/Kotlin**: AGP 8.6.1, Kotlin 2.0.0, compileSdk = 35, minSdk = 26
- **Workflow 제약**: workspace bash 는 read-only git ops 만. mutate 는 사용자 PowerShell 으로 paste-ready

### 메모리 / 핸드오프 우선 읽기
1. `MEMORY.md` 인덱스 → user_profile / project_eject_button / feedback_onedrive_git / reference_github_noreply
2. `E:\Eject Button\HANDOFF_v1.5.0.md` — v1.5.0 골격 상세 + 9순위 (#3, #4, #5 코드 fix 가이드)

---

## 입력

### A) 위 디자인 답변
이 프롬프트와 함께 첨부될 Claude Design 의 답변 3 영역:
1. **영역 A — 코치마크 4-step 시각 디자인** (SVG mockup 4장 + A.1~A.4 결정 + Compose diff)
2. **영역 B — 수락/거절 버튼 회귀 fix + 드래그 의도** (SVG 1~3장 + B.3 옵션 + B.4 위치 + Compose diff)
3. **영역 C — BottomBar 부드러운 전환** (권장 옵션 + Compose diff + 선택 SVG)

### B) v1.5.0 push 후 사용자 6가지 피드백
| # | 항목 | 처리 |
|---|---|---|
| 1 | 코치마크 디자인 개선 | 영역 A 답변 적용 |
| 2 | 수락/거절 버튼 위치 회귀 + "터치 따라가지 않게" | 영역 B 답변 적용 |
| 3 | 코치마크 진행 중 메인 화면 스와이프 차단 | 코드 fix (HANDOFF 9순위 가이드) |
| 4 | 시스템 테마 디폴트 = LIGHT | 코드 fix (HANDOFF 9순위 가이드) |
| 5 | "Command/History/Eject" 등 영문 표현 7개 언어로 다국어화 | 코드 fix (hard-coded 검색 + AppStrings 추가) |
| 6 | BottomBar 인디케이터 부드러운 전환 | 영역 C 답변 적용 |

---

## 작업 — 다음 순서로 처리

### 1. 사전 점검 (5분)
- `git status` + `git log --oneline -10` 으로 v1.5.0 상태 확인
- `gh release view v1.5.0` 으로 push 확인
- `HANDOFF_v1.5.0.md` 읽고 9순위 코드 fix 가이드 숙지
- 디자인 답변 3개 영역 받았는지 확인 (없으면 Simon 에게 요청)

### 2. 디자인 답변 적용 (영역 A → B → C)

#### 영역 A — 코치마크 시각 디자인 + spotlight 좌표 측정
**수정 파일**:
- `app/src/main/java/com/ejectbutton/ui/onboarding/CoachmarkOverlay.kt` — 디자인 답변의 `drawWithContent` 내부 cutout/ring/glow 패턴 + Tooltip Card 디자인 + offset 계산 로직 적용
- `app/src/main/java/com/ejectbutton/ui/main/MainScreen.kt` — 4개 메인 영역 (`CallerChips` / `TriggerModeRow` / `EjectButton` / 우상단 `IconButton`) 에 `Modifier.onGloballyPositioned` 추가:
  ```kotlin
  import androidx.compose.ui.layout.onGloballyPositioned
  import androidx.compose.ui.layout.positionInWindow
  import com.ejectbutton.ui.onboarding.CoachmarkSpotlight

  var scenarioSpot   by remember { mutableStateOf<CoachmarkSpotlight?>(null) }
  var triggerSpot    by remember { mutableStateOf<CoachmarkSpotlight?>(null) }
  var ejectSpot      by remember { mutableStateOf<CoachmarkSpotlight?>(null) }
  var settingsSpot   by remember { mutableStateOf<CoachmarkSpotlight?>(null) }

  // 4개 컴포저블 호출에 Box wrapper + onGloballyPositioned 또는 modifier 파라미터 추가
  // 현재 EjectButton/CallerChips/TriggerModeRow 는 modifier 파라미터 없음 → 함수 시그니처에 추가

  // 코치마크 호출부 spotlights 인자 변경
  spotlights = listOf(scenarioSpot, triggerSpot, ejectSpot, settingsSpot)
  ```
- 디자인 답변에 따라 추가 import / 컬러 / shape 도 적용

#### 영역 B — 수락/거절 버튼 회귀 fix
**수정 파일**: `app/src/main/java/com/ejectbutton/ui/call/FakeIncomingCallScreenV2.kt`

확정 변경 사항 (디자인 답변 무관):
- **line 233 의 `.offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }` 반드시 제거** (사용자 명시 요구)
- `Row.padding(horizontal = 56.dp)` (line 133) 와 outer Box 200dp 크기 (line 203) — 디자인 답변의 권고 dp 값으로 교체
- `PulsingCallButton` 함수 시그니처 변경 시 호출부 두 곳 (Accept/Decline) 동기화
- 사용자 명시 요구 외 항목은 디자인 답변의 옵션 (A/B/C/D) 따라 처리
- Tap 즉시 trigger 는 반드시 유지 (위험 상황 시나리오)
- Clarity 마스킹 (`callerName.clarityMask()`, `callerLabel.clarityMask()`) 유지

#### 영역 C — BottomBar 부드러운 전환
**수정 파일**: `app/src/main/java/com/ejectbutton/ui/main/MainScreen.kt` (line 1743~1787 의 `BottomBar` 함수)

디자인 답변의 옵션 (1/2/3/4) 권고에 따라 적용. 옵션 2 (sliding pill) 가 권장된 경우:
- `BoxWithConstraints` + `animateFloatAsState` + offset 계산
- Text color/weight 도 보간 (`animateColorAsState` + `FontWeight` 보간)
- 코너 `RoundedCornerShape(50)` 유지

### 3. 사용자 피드백 #3, #4, #5 코드 fix (디자인 답변 무관, 즉시 진행 가능)

#### #3 — 코치마크 진행 중 스와이프 차단
**파일**: `MainScreen.kt` (line ~476 의 outer Box `pointerInput(Unit)`)
```kotlin
.pointerInput(coachmarkVisible) {
    if (coachmarkVisible) return@pointerInput   // ← 추가
    var totalDrag = 0f
    detectHorizontalDragGestures( ... )
}
```

#### #4 — 시스템 테마 디폴트 = LIGHT
**파일**: `EjectPrefs.kt` (line ~496 `loadThemeMode`)
- `ThemeMode.SYSTEM.name` 두 군데 → `ThemeMode.LIGHT.name`
- `getOrDefault(ThemeMode.SYSTEM)` → `getOrDefault(ThemeMode.LIGHT)`
- 기존 사용자 (이미 SYSTEM 저장) 는 그대로 유지 — 새 첫 실행자만 LIGHT 시작

#### #5 — 영문 표현 다국어화 검증
1. `Select-String -Path "app\src\main\java\com\ejectbutton\**\*.kt" -Pattern '"EJECT|"Eject|"Command|"History|"Settings|"Systems"'` 으로 hard-coded 영문 리스트업
2. `MainScreen.kt` line 966, 1441, 1564 의 `"⏏ EJECT BUTTON"`, `"EJECT BUTTON"`, `"EJECT"` 등을 strings 추출
3. `AppStrings.kt` 에 신규 필드 추가 (예: `appBrandLabel`, `ejectButtonLabel`) + 7개 언어 번역
4. 호출부에서 `strings.*` 로 교체
5. 한국어 띄어쓰기 검증 ("비상 탈출" 자연 vs "비상 탈출 은" 어색)

### 4. 빌드 검증 (PowerShell, 사용자 paste-ready)
```powershell
Set-Location "E:\Eject Button"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug --warning-mode=summary 2>&1 | Tee-Object -FilePath build_v1.5.1.log
```
실패 시 마지막 30줄 paste 받아 fix.

### 5. 전체적 점검 (사용자 명시 요구) — 다음 항목 모두 검증
- [ ] 사용자 6가지 피드백 모두 처리됐는가? (체크리스트로 보고)
- [ ] 회귀 — v1.5.0 의 코치마크 자동 표시 / EjectPrefs.coachmarkSeen / 4-step 진행 정상 작동?
- [ ] 다국어 — 7개 언어 strings 모두 컴파일 성공? 한국어 띄어쓰기 자연?
- [ ] 시각 검증 — 디자인 답변의 SVG mockup 과 실제 스크린 비슷한지 (Layout Inspector 또는 사이드로드)
- [ ] 라이트/다크 모드 둘 다 자연스러운지
- [ ] minSdk 26 호환성 (사용한 API 버전 검증)
- [ ] Lint warning — `assembleDebug` 의 deprecation / unused import 정리
- [ ] Conventional Commits — `feat(onboarding/ui): v1.5.1 ...`
- [ ] 새 secrets 하드코딩 없음 (.env / secrets.properties 그대로)
- [ ] CHANGELOG / Release notes 작성

### 6. Commit + Tag + Push (사용자 PowerShell)
```powershell
git add app/src/main/java/com/ejectbutton/ui/onboarding/CoachmarkOverlay.kt
git add app/src/main/java/com/ejectbutton/ui/main/MainScreen.kt
git add app/src/main/java/com/ejectbutton/ui/call/FakeIncomingCallScreenV2.kt
git add app/src/main/java/com/ejectbutton/data/EjectPrefs.kt
git add app/src/main/java/com/ejectbutton/data/AppStrings.kt
git status

git commit -m "feat(ui): v1.5.1 design polish — coachmark spotlights, call buttons, bottom bar" -m "..."  # 상세는 별도 작성

git tag v1.5.1
git push origin feat/v1.4.2-ux-patches
git push origin v1.5.1
```

### 7. AAB 다운로드 + Release notes
```powershell
Set-Location "$env:USERPROFILE\Downloads"
gh release download v1.5.1 --repo Simon-YHKim/eject-button --pattern "*.aab" --clobber
gh release download v1.5.1 --repo Simon-YHKim/eject-button --pattern "*.apk" --clobber
gh release view v1.5.1 --web   # release notes 편집
```

---

## 절대 하지 말 것

- ❌ workspace bash 로 `git add/commit/push` 시도 (NTFS lock + OneDrive autocrlf 문제)
- ❌ EjectCoral / EjectSurface 외 새 브랜드 컬러 도입
- ❌ `Material NavigationBar` 컴포넌트 도입 (BottomBar 직접 작성 톤 유지)
- ❌ Tap 즉시 trigger (Accept/Decline) 제거
- ❌ Clarity 마스킹 깨기
- ❌ Force push / `git reset --hard` (Simon preferences 항목 10번)
- ❌ API 키 / 토큰 / 시크릿 하드코딩 (Simon preferences 항목 11번)
- ❌ HANDOFF 파일 자동 작성 (Simon 명시 요청 시에만)

---

## 보고 형식

작업 완료 후 다음 형식으로 보고:

```
## v1.5.1 작업 보고

### 적용 항목 (체크리스트)
- [x] #1 코치마크 디자인 — 영역 A 답변 적용
- [x] #2 수락/거절 버튼 — 영역 B 답변 적용
- [x] #3 스와이프 차단
- [x] #4 테마 디폴트 LIGHT
- [x] #5 영문 표현 다국어화 — N개 hard-coded → strings.*
- [x] #6 BottomBar 부드러운 전환 — 영역 C 답변 적용

### 변경 파일
| 파일 | 변경 |
|---|---|
| ... | ... |

### 빌드
BUILD SUCCESSFUL / FAILED + 핵심 warning

### 회귀 점검
- 코치마크 자동 표시: ✅ / ❌
- 4-step 진행: ✅
- ...

### 다음 단계
- v1.5.1 push: 완료 / 대기
- Play Console promote: 별도 진행 필요
```

---

**작성**: 2026-05-02 (v1.5.1 디자인 통합 프롬프트와 짝)
**짝 문서**: `PROMPT_v1.5.1_unified.md` (Claude Design 입력용)
