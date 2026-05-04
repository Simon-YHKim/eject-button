# Changelog

모든 주요 변경사항은 이 파일에 기록됩니다.
형식: [Conventional Commits](https://www.conventionalcommits.org/) + [SemVer](https://semver.org/lang/ko/).

---

## [Unreleased]

## [1.5.2] — 2026-05-02

### Fixed
- **코치마크 spotlight cutout 회귀 fix** — `graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }` 누락으로 BlendMode.Clear 가 작동 안 해서 ring 안의 child content (시나리오 카드 / EJECT 버튼 / ⚙) 가 dim 으로 가려졌던 문제. cutout 정상 작동 + dim alpha 0.74 → 0.62 로 약화. ([#1])
- **코치마크 tooltip 카드 위치 안전 마진** — `placeTooltip` 가 spotlight 와 항상 안 겹치도록 위/아래 가용 공간 비교해서 큰 쪽에 배치. cardH 220dp → 180dp 컴팩트화 + 동적 좌표 계산. ([#1])
- **가짜 incoming-call 수락/거절 버튼이 손가락 따라가는 회귀 fix** — `Modifier.offset { IntOffset(offsetX, offsetY) }` 제거. 버튼 위치/크기는 그대로 유지하고 (사용자 명시) drag 인텐트는 Lottie ring progress 로만 시각화. ([#2])

### Changed
- **시스템 테마 디폴트 = LIGHT** — 신규 사용자 첫 실행 시 라이트 모드로 시작. 기존 사용자 (이미 SYSTEM/DARK/LIGHT 저장된) 는 그 값 유지. ([#4])
- **BottomBar 탭 라벨 + 앱 헤더 7개 언어 다국어화** — 기존 모든 언어가 영어 ("COMMAND/HISTORY/SETTINGS") 였던 회귀 fix. 신규 키 `appBrandLabel`, `ejectButtonLabel` + `tabCommand/History/Systems` 7개 언어 번역 추가. 한국어 = 명령/기록/설정, 일본어 = コマンド/履歴/設定, 중국어 간체 = 指令/记录/设置 등. ([#5])

### Documentation
- `CHANGELOG.md` 신규 작성 (v1.5.0/1/2 entries 추가).
- 사용자 preferences 항목 15번 (Auto-generate CHANGELOG-style summary) 적용 시작.

---

## [1.5.1] — 2026-05-02

### Added
- **코치마크 4-step 투어 디자인 적용** (Claude Design 답변 drop-in)
  - 신규 `app/src/main/java/com/ejectbutton/ui/coachmark/` 패키지
  - `CoachmarkState.kt` — `register(id, rect, shape)` + `index/isActive/start/next/dismiss`
  - `CoachmarkOverlay.kt` — 3-stop EjectRed halo (alpha 1.0/0.20/0.08), 4-dot progress, STEP N pill, ghost Skip, EjectRed primary pill. 라이트/다크 자동 분기 (theme tokens)
- **MainScreen 4 spotlight register**
  - `CallerChips` (scenario, RoundRect)
  - `TriggerModeRow` (trigger, RoundRect)
  - `EjectButton` (eject, Circle)
  - StitchTopBar 의 ⚙ `IconButton` (settings, RoundRect via callback)
- **코치마크 진행 중 메인 화면 좌우 스와이프 차단** — `pointerInput(coachmark.isActive)` + early return ([#3])

### Removed
- 기존 `app/src/main/java/com/ejectbutton/ui/onboarding/CoachmarkOverlay.kt` (v1.5.0 골격) 제거 — 새 패키지로 대체.

### Build
- `versionCode = 1043`, `versionName = "1.5.1"`
- Release Build #43 (commit `a682261`) — 14m 11s
- Tag `v1.5.1` + 백업 브랜치 `release/v1.5.0` (rollback 보장)

---

## [1.5.0] — 2026-05-02

### Added
- **코치마크 4-step 골격** (사용자 a-ha 모먼트 부족 → 메인 화면 4개 핵심 버튼 안내)
  - `EjectPrefs.coachmarkSeen` flag (save/load) — 첫 실행 1회 자동 표시
  - `MainScreen` 코치마크 overlay (COMMAND 탭 한정, fullscreen dim + tooltip)
  - `AppStrings.kt` 23 신규 필드 × 7 언어 = 161 string 추가 (코치마크 step 1~4 + 5단계 onboarding flow foundation)

### Notes
- v1.5.0 의 의도적 제약: spotlight 좌표는 `listOf(null × 4)` (실측 미구현) — v1.5.1 에서 정밀화 완료.
- 5단계 OnboardingScreen 리팩토링 (Welcome / Use Cases / Coachmark / Ready / End) 은 strings 만 미리 추가, UI 통합은 추후 진행.

### Build
- `versionCode = 1042`, `versionName = "1.5.0"`
- Release Build #42 (commit `fba0e99`) — 13m 21s
- Tag `v1.5.0`

---

## [1.4.0~1.4.4] — 이전 출시

상세는 GitHub Release 노트 참조:
- v1.4.4 — Lottie drag_confirm UI + textAlign Center + EJECT 다국어화
- v1.4.3 — FLAG_SECURE 임시 제거 (테스터 스크린샷 가능)
- v1.4.2 — 앱 이름 다국어 launcher
- v1.4.1 — 위장 아이콘 7개 언어 라벨
- v1.4.0 — GPS 지오펜스 infrastructure (UI 미통합)

---

[1.5.2]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.2
[1.5.1]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.1
[1.5.0]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.0
[#1]: ./HANDOFF_v1.5.0.md "사용자 피드백 #1 (코치마크 디자인)"
[#2]: ./HANDOFF_v1.5.0.md "사용자 피드백 #2 (수락/거절 버튼)"
[#3]: ./HANDOFF_v1.5.0.md "사용자 피드백 #3 (스와이프 차단)"
[#4]: ./HANDOFF_v1.5.0.md "사용자 피드백 #4 (테마 LIGHT)"
[#5]: ./HANDOFF_v1.5.0.md "사용자 피드백 #5 (영문 다국어화)"
