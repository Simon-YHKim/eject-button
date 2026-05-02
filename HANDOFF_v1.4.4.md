# Session Handoff — v1.4.4 → v1.5.0 (코치마크)

> **다음 세션 첫 메시지로 사용**: "HANDOFF_v1.4.4.md 읽고 1순위부터 이어가"

---

## 환경 / 컨텍스트 복구

### 메모리 파일 (반드시 첫 단계)
```
C:\Users\202502\AppData\Roaming\Claude\local-agent-mode-sessions\7df21563-246e-4ab0-a188-4667c1a1b715\c1cf5f48-3a1c-4658-bb57-c3e2fea21311\spaces\f806fa7d-1928-4fa8-8b86-99937470f3ab\memory\MEMORY.md
```
인덱스 + 4개 파일 (user_profile / project_eject_button / feedback_onedrive_git / reference_github_noreply).

### 프로젝트 사실
- 앱: Eject Button (회식 탈출 가짜 통화 앱), 한국어 명 "비상 탈출"
- Repo: github.com/Simon-YHKim/eject-button (private)
- Default 브랜치: `Eject_Button_app`
- 작업 브랜치: `feat/v1.4.2-ux-patches` (HEAD = 68e20a9, 마지막 tag v1.4.4)
- Gradle/Kotlin: AGP 8.6.1, Kotlin 2.0.0, compileSdk=35, minSdk=26
- Lottie 의존성 추가됨 (lottie-compose 6.4.0)

### 도구 환경
- workspace bash: 읽기 전용 git ops 만 (NTFS lock, mutate 불가)
- Windows-MCP PowerShell: 사용자 PC 직접 (gh CLI 인증됨, Android SDK + JDK 17)
- Chrome MCP: 활성 (Play Console / GA4 자동화 가능)

---

## 직전 세션 (v1.0.0 → v1.4.4) 완료 종합

| Tag | 핵심 |
|---|---|
| v1.0.1 | Clarity 마스킹 fix (4 파일 / 8군데) |
| v1.1.0 | ASO 7개 언어 reframe + decoy aliases manifest |
| v1.3.0 | Share button + Decoy switching UI |
| v1.3.1 | INAPP 광고 제거 일회성 (eject_remove_ads_lifetime ₩3,300) |
| v1.4.0 | GPS 지오펜스 infrastructure (UI 미통합) |
| v1.4.1 | 위장 아이콘 라벨 6개 언어 |
| v1.4.2 | 앱 이름 다국어 + Lottie 의존성 (BOM bug fix 포함) |
| v1.4.3 | FLAG_SECURE 임시 제거 (테스터 스크린샷 가능) |
| **v1.4.4** | **textAlign Center + Lottie drag_confirm UI 통합 + 'Eject'/'Eject Button' 6개 언어 다국어화** |

**Closed Testing 상태**: versionCode 1028 (v1.0.0-rebuild) 활성, 표시 이름은 "1012 (1.0.8)" (UI 표기 vs 실제 AAB 차이).

**v1.4.4 APK**: `release-build-41`, versionCode 1041, commit 68e20a9.

---

## 1순위 — 코치마크 5단계 튜토리얼 (v1.5.0)

### 사용자 의도
"사용자에게 사용방법을 전달할 a-ha 모먼트가 부족. 다른 앱처럼 사용 순서대로 버튼별 설명 이어가기. 단, 메인 화면만 설명, setting 의 사용 설명서 안내."

### 5단계 흐름
1. **Welcome**: "옆 사람 모르게 자연스럽게 빠져나오세요"
2. **사용 예시**: 회식 / 어색한 자리 / 위험 상황 (3 panel swipe)
3. **코치마크 투어** (메인 화면 4 buttons spotlight):
   - Step 1: 시나리오 선택 카드 → "어떤 가짜 통화가 올지 골라요"
   - Step 2: 모드 선택 토글 → "흔들기/측면버튼/즉시/지연"
   - Step 3: EJECT 큰 빨간 버튼 → "이걸로 발사!"
   - Step 4: ⚙ 설정 → "위장 아이콘/언어/설명서"
4. **준비됐나요?**: "지금 EJECT 시연해 볼까요?" (즉시 시연 / 나중에)
5. **튜토리얼 끝**: "준비 완료! 자세한 설명은 ⚙ 설정 → 사용 설명서"

### 작업 파일 (구현 plan)
1. **신규** `app/src/main/java/com/ejectbutton/ui/onboarding/CoachmarkOverlay.kt` (~150줄)
   - dimmed 배경 (rgba(0,0,0,0.85)) + 특정 영역 cutout (Path)
   - Tooltip Card + 화살표 cutout 향함
   - 4 step 순차 (`AnimatedContent`)
   - 진행도 (1/4, 2/4 ..)
   - "건너뛰기" / "다음 →"

2. **수정** `app/src/main/java/com/ejectbutton/ui/main/OnboardingScreen.kt`
   - 기존 4 페이지 (Welcome/Command/Trigger/Systems) → 5단계 새 흐름
   - 코치마크 투어는 별도 진입 (메인 화면 위에 overlay)

3. **수정** `app/src/main/java/com/ejectbutton/ui/main/MainScreen.kt`
   - 첫 진입 시 EjectPrefs.coachmarkSeen == false 면 CoachmarkOverlay 표시
   - 4개 버튼 위치 ref (Modifier.onGloballyPositioned) 로 spotlight 좌표 계산

4. **수정** `app/src/main/java/com/ejectbutton/data/EjectPrefs.kt`
   - `KEY_COACHMARK_SEEN` + saveCoachmarkSeen / loadCoachmarkSeen

5. **수정** `app/src/main/java/com/ejectbutton/data/AppStrings.kt`
   - 7개 언어 × 신규 텍스트 (welcome subtext, 3 use cases, 4 coachmark steps, ready/end)
   - ~30 신규 string 필드

### 구현 핵심 (디자인 자료 불필요)
순수 Compose 로 구현:
- Spotlight: `Canvas` 또는 `Box` + `Modifier.drawWithContent` + `BlendMode.Clear`
- Tooltip: `Card` + `offset { ... }` (버튼 좌표 기반)
- 화살표: `Polygon` shape

### 작업 시간 예상
- CoachmarkOverlay 컴포넌트: 3시간
- 5단계 onboarding 리팩토링: 2시간
- 7개 언어 텍스트: 1시간
- 빌드 검증 + 통합: 2시간
- **합계: ~8시간 = 1.5 주말**

---

## 2순위 — Lottie UI 검증 + 추가 개선

이전 세션 v1.4.4 의 Lottie 통합 검증 미완.

작업:
- 실기기 sideload (`eject-button-release-run41-68e20a9.apk`) → 가짜 통화 화면 → 드래그 → 동심원 ring 표시 검증
- HANDOFF.md (`docs/design/handoff-drag-confirm/HANDOFF.md`) 의 미구현 항목:
  - 누른 버튼 alpha 페이드 (`1f - dragFraction`)
  - 반대편 버튼 fade (`progress > 0.85` 시)
  - 햅틱 피드백 (`HapticFeedbackType.LongPress`) at trigger

### 추가 개선 후보
- Lottie 가 200dp 인데 모든 화면에서 잘 보이는지 검증
- 다크/라이트 테마 둘 다 회색 ring 자연스러운지

---

## 3순위 — Play Console v1.4.4 promote

현재 Closed Testing = v1.0.0 (1028).
v1.4.4 (1041) 를 promote 해야 testers 가 신기능 (다국어 / Lottie / Center align 등) 받음.

```powershell
# AAB 다운로드
Set-Location "$env:USERPROFILE\Downloads"
gh release download release-build-41 --repo Simon-YHKim/eject-button --pattern "*.aab" --clobber
```

Play Console UI:
1. 비공개 테스트 - Alpha → 새 버전 만들기
2. AAB 업로드 (1041) 또는 라이브러리에서 추가
3. 출시 노트 (v1.4.4 변경사항)
4. 검토 → 출시

---

## 4순위 — 외부 서비스 미완 (Simon 결제 정보 등록 후)

| 작업 | 시간 | 비고 |
|---|---|---|
| AdMob 결제 정보 입력 | 10분 | Simon 직접 |
| Play Console 결제/세금 정보 | 10분 | Simon 직접 |
| Play Console INAPP 상품 등록 | 5분 | `eject_remove_ads_lifetime` ₩3,300 |
| Clarity v1.0.1 마스킹 검증 | 24h 후 | Recordings 확인 |

---

## 5순위 — 큰 미시작 features

| 항목 | 작업량 |
|---|---|
| Wear OS Companion (Galaxy Watch 트리거) | 1주말 |
| Firebase 페어링 응급 연락처 (위치/시간/이름) | 3주말 |
| GPS 지오펜스 UI (infrastructure 만 v1.4.0 에 있음) | 2주말 |
| 위장 아이콘 PNG 4개 디자인 | Simon 본인 디자인 작업 |

---

## ⚠️ Critical 함정 (반드시 회피)

1. **PowerShell BOM 자동 추가**
   - `Set-Content -Encoding UTF8` → BOM 추가 → Gradle TOML 거부
   - 해결: `[System.IO.File]::WriteAllText(path, content, [System.Text.UTF8Encoding]::new($false))`
   - + BOM 검사 후 제거 패턴 (bytes[0..2] == EF BB BF 면 잘라냄)

2. **Play Console 출시 이름 vs 실제 AAB 차이**
   - 출시 이름 "1012 (1.0.8)" 인데 실제 AAB 1028 — UI 표기 자동 갱신 안 됨

3. **한국어 띄어쓰기**
   - "비상 탈출은" 자연스러움 (조사 직접 붙음). bulk 치환 시 "비상 탈출 은" 같은 어색 띄어쓰기 발생 가능
   - 다국어화 후 한국어 자연스러움 검증 필수

4. **채팅 markdown auto-render**
   - `.md`, `.json` 같은 확장자가 들어간 단어 → `[text](url)` literal 로 paste 됨
   - PowerShell paste 시 변수 사용으로 회피

5. **GitHub API rate limit**
   - gh CLI / REST API 자주 한도 도달 (특히 logs / asset download)
   - 다운로드는 Chrome 클릭이 안전

6. **workspace bash 의 stale cache**
   - 파일 Edit 후 bash 에서 mount path 로 읽으면 stale content 보일 수 있음
   - 검증은 Windows-MCP PowerShell 또는 Get-Content 로 해야 정확

---

## 다음 세션 권장 첫 액션

```
1. 메모리 파일 읽기 (MEMORY.md + 4개 인덱스 파일)
2. 이 HANDOFF_v1.4.4.md 읽기
3. TaskCreate 로 1순위 코치마크 작업 task list 생성
4. CoachmarkOverlay.kt 신규 작성부터 시작
5. v1.5.0 commit + tag + push + APK 다운로드 + Simon 전달
```

작업 중 막히는 부분 있으면 함정 섹션 먼저 확인.

---

**작성**: 2026-05-02 (v1.4.4 push 직후 토큰 절벽 도달)
**다음 세션 시작 시 이 문서 + 메모리 파일 읽고 컨텍스트 복구**
