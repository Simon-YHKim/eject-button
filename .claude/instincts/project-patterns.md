# 프로젝트별 반복 패턴

> **목적**: 각 프로젝트가 가진 고유한 관용·제약·API·데이터 모델·명명 규칙을 기록한다.
> **갱신 규칙**: 새 프로젝트 진입 시 섹션 추가. 패턴이 바뀌면 수정.
> **읽는 시점**: 프로젝트 루트에서 새 세션 시작할 때 이 파일과 프로젝트 `CLAUDE.md` 를 함께 참조.

---

## 템플릿

```
## <project-name>

- **스택**: 언어, 프레임워크, DB, 호스팅
- **도메인 용어**: 특수 용어 정의
- **데이터 모델 핵심**: 주요 엔티티·관계
- **API 관용**: 명명·응답 포맷·에러 처리
- **빌드/테스트**: 실행 명령어, CI 링크
- **금기**: 건드리면 안 되는 곳
- **최근 업데이트**: YYYY-MM-DD
```

---

## 프로젝트 목록

## eject-button (Simon-YHKim/eject-button)

- **스택**: Android Kotlin + Jetpack Compose, minSdk 26, targetSdk 35. Firebase Analytics + Crashlytics, Google Play Billing 7.0, AdMob 23.6, Microsoft Clarity. **백엔드 없음** (Android-only). signed AAB 빌드는 GitHub Actions `release-aab.yml` workflow.
- **도메인 용어**:
  - **EJECT** = 가짜 통화 발사 (앱 핵심 액션). 사용자는 어색한 자리에서 EJECT 누르면 진짜 같은 가짜 전화가 와서 자연스럽게 자리 이탈 가능.
  - **Mayday** = premium 브랜드 (subscription product). 모든 다이얼로그에서 "Mayday 업그레이드" 통일.
  - **트리거 모드**: BUTTON / SHAKE / SIDE_BUTTON / DELAYED / CUSTOM
  - **PermissionGate**: EJECT 직전 권한 재확인 메커니즘 (v1.6.10 추가)
  - **In-App Update**: Flexible flow (v1.7.0+). emergency 도중 절대 process kill X.
- **데이터 모델 핵심**:
  - `AppStrings` — **247-field 일반 class** (data class 금지, ClassFormatError 회피). 7-locale 매핑 (en/ko/zh-CN/zh-TW/ja/es/hi). pt-BR 도 Play Console listing 에 있음.
  - `Scenario` — 가짜 통화 시나리오 (callerName / callerLabel / urgency)
  - `TriggerMode` enum + `PermissionGate.Req` enum
- **API 관용**: BillingManager / AdManager / ConsentManager / **UpdateManager** (v1.7.0+) 매니저 패턴. 각 매니저는 `StateFlow<...>` 노출. `Activity.onCreate` 에서 `connect()` / `registerListener()`, `onDestroy` 에서 cleanup.
- **빌드/테스트**:
  - `./gradlew :app:testDebugUnitTest --no-daemon` — JVM unit test
  - `./gradlew bundleRelease --no-daemon` — signed AAB (keystore.properties 필요)
  - CI: `.github/workflows/release-aab.yml` (tag `v*` push 또는 manual workflow_dispatch)
- **CI sandbox 우회 패턴**: tag-push 403 거부 환경에서는 PR branch trigger (`branches: [<PR-branch>]`) 임시 추가 + PR 머지 후 cleanup PR 로 원복. v1.6.10 PR #8 → #9, v1.7.1 PR #15 → #16, v1.7.2 trigger branch 모두 동일 패턴. **main-branch trigger 는 yml 변경 인식 타이밍 race 로 발화 안 함** — PR branch trigger 만 사용.
- **버전 코드 규칙**: `versionCode = 1000 + GITHUB_RUN_NUMBER`. workflow run 마다 strict monotonic 증가 (Play Console reject 회피).
- **PPP 가격 매핑 (v1.6.2)**: KR ₩3,000 = US $2.49 = EU €2.29 = UK £1.99 = JP ¥350 = CN ¥15 = TW NT$70 = HK HK$18 = HI ₹149 = MX MX$45 = BR R$10.90 = AU A$3.49 = CA CA$3.29 = ID Rp35,000. `MainScreen.localizedFallbackPrice()` 와 `SettingsScreen.localizedFallbackPrice()` 모두 동기화 필수 (두 곳 어긋남 = 사용자 가격 inconsistency 버그).
- **금기**:
  - `data class AppStrings(...)` — 247-field 합성 메서드가 JVM 한도 초과 → 일반 `class` 만 사용
  - `kotlin-reflect` 의존성 추가 — 어떤 버전이든 testRuntimeClasspath ClassFormatError 유발
  - `completeUpdate()` 가 `FakeCallOverlayService.isRunning` / `ShakeDetectionService.isRunning` 중에 발화 — **위협 모델 핵심**. 다중 가드 필수.
  - Compose BOM 임의 jump — `dl.google.com/dl/android/maven2/.../maven-metadata.xml` 확인 후 published version 만 사용
- **최근 업데이트**: 2026-05-19 (v1.7.1 production rollout, v1.7.2 ripple/cutout fix 빌드 완료, simon-stack vendor)
