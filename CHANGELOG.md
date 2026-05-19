# Changelog

筌뤴뫀諭?雅뚯눘??癰궰野껋럩沅??? ?????뵬??疫꿸퀡以??몃빍??
?類ㅻ뻼: [Conventional Commits](https://www.conventionalcommits.org/) + [SemVer](https://semver.org/lang/ko/).

---

## [Unreleased — v1.7.1 후보] — 2026-05-19

> **v1.7.0 출시 직후 발견된 결제 흐름 3건 핫픽스.** 사용자 보고:
> - 설정 → "비상탈출 Mayday 업그레이드" 버튼 → 팝업이 "프리미엄 업그레이드" 로 표시
> - 가격이 ₩3,000 이 아님 (옛 ₩1,900 노출)
> - "구독시작" 눌러도 Play Store 결제 화면 안 뜨고 팝업만 닫힘

### Fixed
- **PremiumUpgradeDialog 제목 브랜드 통일** — 7개 로케일 `prTitle` 을 "Mayday 업그레이드"
  (en: "Upgrade to Mayday", ko: "Mayday 업그레이드", ja: "Mayday へアップグレード",
  zh-CN: "升级到 Mayday", zh-TW: "升級至 Mayday", es: "Actualizar a Mayday",
  hi: "Mayday में अपग्रेड") 로 변경. 기존 `premiumTitle` ("비상탈출 Mayday")
  의 브랜드 voice 와 일관.
- **Fallback 가격 PPP 매핑 동기화** — 7개 로케일 `prMonthlyPrice` / `prAnnualPrice` /
  `prAnnualAvg` 를 v1.6.2 PPP 재책정 결과 (한국 ₩3,000 = $2.49 = €2.29 = ¥350 등) 로
  일괄 갱신. **`SettingsScreen.kt` 의 `localizedFallbackPrice()` 도 `MainScreen.kt`
  의 v1.6.2 매핑과 100% 동기화** — 이전엔 Settings 진입 시 옛 ₩1,900, Main 진입 시
  ₩3,000 로 어긋나 사용자가 같은 dialog 에서 다른 금액 보는 inconsistency.
- **결제 silent fail 안내** — `BillingManager.launchPurchase` / `launchPurchaseRemoveAds`
  가 `subsDetails` / `inappDetails` null 일 때 silent return 하던 것을 **Toast 안내
  ("Play 스토어와 연결 중이에요. 잠시 후 다시 눌러주세요") + `queryProducts()` 재호출**
  로 변경. 사용자가 "팝업만 닫히고 결제 화면 안 뜸" 으로 보던 증상 해결. 가능한 원인은
  Play Console 미등록 / inactive product, license tester 미등록, BillingClient
  connection 지연, queryProductDetailsAsync 네트워크 실패 — Toast 출력 후 다음 시도에서
  재시도 가능.

### Changed
- `AppStrings.kt`: 7-locale `billingNotReadyMsg` 신규 추가.

### Notes
- **실제 결제 가격은 Play Console 의 product 가격을 따라간다.** 본 PR 의 fallback 변경은
  Play Console 이 응답 못 했을 때 노출되는 임시 가격일 뿐. Play Console 에 등록된 가격이
  ₩3,000 인지 확인 필요 (Simon 의 후속 작업). 등록된 가격과 fallback 이 다르면 사용자가
  서로 다른 금액을 보고 결제 시점에 놀랄 수 있음.
- 결제가 여전히 안 뜨면 (Toast 가 뜨고 다시 눌러도 같은 증상): Play Console 에 product
  미등록 가능성이 가장 높음. logcat 의 "BillingManager" tag 메시지 확인.

---

## [Unreleased — v1.7.0 후보] — 2026-05-19

> **v1.6.10 직후 minor 출시.** 두 가지 큰 흐름:
> 1. **In-App Update Flexible flow 통합** — 사용자의 수동 갱신 의존 제거. 다음 출시부터
>    앱이 직접 새 버전을 받아 "재시작" 만 요청.
> 2. **Manager 패턴 통일 + 안전 가드 강화** — UpdateManager 추출로 architectural
>    consistency 회복 (BillingManager / AdManager / ConsentManager 와 동일 시그너처),
>    동시에 active eject 중 process kill 금지 등 safety-critical 가드 다중화.

### Refactored — UpdateManager 추출 (v1.7.0 architectural)
- 신규 `com.ejectbutton.update.UpdateManager` — Activity 에서 In-App Update lifecycle
  전체 분리. `BillingManager` 와 동일하게 `StateFlow<UpdateState>` 노출
  (`Idle` / `Downloading` / `Downloaded` / `Failed`). MainActivity 는 매니저 instantiate
  + state collect + Snackbar 노출만 담당, In-App Update 도메인 로직 0 라인.
- `MainActivity.kt`: -120 lines (In-App Update 관련 모두 UpdateManager 로 이관).
- 테스트 이동: `MainActivityUpdateGuardTest` → `UpdateManagerGuardTest`.

### Added — Failure status handling (Adversarial #8 → adopted)
- `UpdateManager.installStateListener` 가 `InstallStatus.FAILED` / `CANCELED` 도 처리
  → `_state.value = UpdateState.Failed`. UI 는 짧은 (Short duration) Snackbar 로
  "다운로드 실패 — 다음에 다시 시도할게요" 안내. 다음 cold launch / Wi-Fi 자동 재시도.
  silent failure 로 사용자가 "왜 재시작 안 뜨지" 라는 상황 방지.
- `AppStrings.kt`: 7-locale `updateFailedMsg` 신규 추가.



### Added — In-App Update (Flexible)
- `MainActivity` — `AppUpdateManager` + `InstallStateUpdatedListener` 등록. 앱 진입 시
  `appUpdateInfo` 로 새 버전 확인 → 가능하면 **백그라운드 다운로드** 시작. 다운로드 완료
  시 `updateDownloaded` state 가 true 가 되고 Compose 트리가 화면 하단에 **Material3
  Snackbar (Indefinite duration)** 로 "업데이트 다운로드 완료 — 재시작" 안내. 사용자가
  Restart 액션을 누르면 `appUpdateManager.completeUpdate()` 호출 → 자동 재시작/적용.
- `onResume` — 사용자가 잠시 다른 앱으로 이탈한 사이 다운로드가 완료된 경우에도 다시
  Snackbar 가 뜨도록 install status 재평가.
- `onDestroy` — listener 누수 방지.
- `gradle/libs.versions.toml` + `app/build.gradle.kts` — `com.google.android.play:app-update-ktx:2.1.0` 의존성 추가.
- `AppStrings.kt` — 7개 로케일 (en/ko/zh-CN/zh-TW/ja/es/hi) 에 `updateDownloadedMsg` /
  `updateRestartBtn` 2개 신규 string 추가.

### Added — Upgrade funnel attribution (Phase 9 deferred → adopted)
- `EjectPrefs.loadLastSeenVersionCode` / `saveLastSeenVersionCode` — 매 cold start
  마다 마지막으로 본 `BuildConfig.VERSION_CODE` 추적.
- `EjectAnalytics.logAppUpdated(prev, new)` — `Application.onCreate` 에서 versionCode
  변화 감지 시 발사 (최초 설치 시점은 0 → 발사 안 함, `first_open` 으로 대체됨).
- `FirebaseCrashlytics.setCustomKey("last_seen_version_code", ...)` — 매 cold start
  custom key. v1.6.12 첫 launch 가 크래시할 경우 `update_in_progress` (In-App Update
  경로일 때만 set) + `last_seen_version_code` 결합으로 어떤 경로의 어떤 버전 회귀인지
  정확 식별. Play Store 자동 업데이트 / In-App Update / 사이드로드 모두 attribution.

### UX + Analytics polish (post-Phase 5/9 specialist audit)
SimonK-stack Phase 5 (UX walkthrough) + Phase 9 (analytics/Crashlytics 연속성) audit 결과 5건 반영:

- **Metered network 가드** — `checkForFlexibleUpdate()` 시작에서 `ConnectivityManager.NET_CAPABILITY_NOT_METERED`
  확인. Wi-Fi / 이더넷 / 무제한 5G 가 아니면 silent skip. 모바일 데이터에서 50~150MB
  자동 다운로드 방지 (KR/IN/BR 데이터 절약 시장 중요). `ACCESS_NETWORK_STATE` 는 implicit.
- **Post-update relaunch 시 2초 splash 건너뜀** — `completeUpdate()` 직전에
  `EjectPrefs.markPostUpdateRelaunch(this)` (.commit() 동기 저장) → 다음 `onCreate` 에서
  `consumePostUpdateRelaunch()` (atomic read-and-reset) 가 true 면 `splashDone = true` 로
  시작. 사용자가 능동 재시작한 상황에서 panic-open 의 friction 제거.
- **In-App Update funnel 이벤트** — `EjectAnalytics.logUpdateDownloaded()` (listener 의
  DOWNLOADED 시점) + `logUpdateRestartClicked()` (Snackbar action 직후, cleanup 전).
  두 이벤트 비율이 자발 갱신율 KPI. Firebase SDK worker thread disk persist → kill 직전
  안전.
- **Crashlytics `update_in_progress` custom key** — completeUpdate() 직전 set. v1.6.12 첫
  launch 가 크래시할 경우 이 tag 가 함께 박혀 "In-App Update 직후 크래시" 인지 식별
  가능 (post-update regression 진단용).
- **Pre-kill analytics 순서 보장** — analytics / Crashlytics / prefs flag 호출이
  `billingManager.destroy()` / `AdManager.destroy()` / `completeUpdate()` 보다 먼저 실행되어
  disk persistence 시간 확보.

Deferred to v1.7 (별도 ticket):
- Day-0 first-launch update prompt 게이팅 (`installAgeDays >= 1` 조건)
- `last_seen_version` pref + `app_updated` 이벤트 (upgrade funnel attribution)
- Clarity custom tag `entered_via=in_app_update`
- Crashlytics breadcrumb 기반 forensic clarity

### Safety — Emergency-aware guards (post-/review specialist audit)
SimonK-stack 6명 specialist (security / lifecycle / i18n / code-health / build / adversarial)
종합 점검 결과 발견된 ship-blocker 5건 즉시 반영:
- **`completeUpdate()` 가 active eject 중 절대 발화 금지** — `FakeCallOverlayService.isRunning` /
  `ShakeDetectionService.isRunning` 가드. 가짜 통화 / 흔들기 감지 진행 중에 process kill 되면
  fake call 환상이 즉시 붕괴 → 정확히 이 앱이 막아야 할 위협 모델 (가해자 옆에 있는 상황).
  두 Service 의 companion object 에 `@Volatile var isRunning` flag 신설, onCreate/onDestroy 에서
  set. MainActivity 의 Snackbar `LaunchedEffect` 가 2초 polling 으로 안전 상태까지 대기 후 노출.
- **Rotation/config-change 시 consent prompt 재출현 차단** — `onCreate` 의 `checkForFlexibleUpdate()`
  호출을 `savedInstanceState == null && !emergencyActive` 로 가드. 회전마다 update sheet 재발화
  방지.
- **Rotation 시 Snackbar 재발화 차단** — `snackbarHandled` 를 `remember` → `rememberSaveable` 로
  격상. dismiss 상태가 config change 를 넘어 보존됨.
- **`completeUpdate()` 더블 콜 가드** — `@Volatile var updateCompleting` flag 로 Snackbar 액션 빠른
  두 번 탭 시 두 번째 호출 묵음.
- **`completeUpdate()` 전 명시적 cleanup** — Process.killProcess + relaunch 는 `onDestroy` 보장 X
  → `billingManager.destroy()` + `AdManager.destroy()` 명시 호출 후 restart 트리거. pending purchase
  acknowledge 는 다음 launch 에서 BillingClient 재진입 시 Play Store entitlement 재조회로 복구.
- **이미 진행 중인 update 재트리거 차단** — `checkForFlexibleUpdate()` 가 `installStatus` 가
  DOWNLOADING / DOWNLOADED / INSTALLED 면 즉시 return.
- **ProGuard 방어** — `proguard-rules.pro` 에 `com.google.android.play.core.**` keep + dontwarn 추가.
  AAR consumer rules 가 자동 적용되지만, v1.0.9 의 Firebase Analytics 패턴 ("이전에 keep 누락으로
  release 빌드에서 silent failure 위험") 과 동일한 defensive policy. R8 full-mode + 미래 AGP
  업그레이드 시 `InstallStateUpdatedListener` SAM 이 reflection-invoked 라 strip 되면 listener
  silent-fail 가능.
- **Release 빌드 log 노이즈 최소화** — `Log.w` / `Log.d` 호출을 `BuildConfig.DEBUG` 로 gate.
- **Snackbar nav-bar inset 적용** — `windowInsetsPadding(WindowInsets.navigationBars)` 추가 →
  edge-to-edge 환경에서 system nav bar 뒤에 깔리지 않음.

### Notes
- **첫 trigger 시점**: v1.6.11 이 앱에 ship 된 이후 출시되는 다음 버전 (v1.6.12+) 부터.
  v1.6.10 → v1.6.11 업데이트는 여전히 Play Store 의 일반 자동 업데이트로 도착.
- **권한**: In-App Update 는 추가 manifest permission 불필요.
- **디버그/사이드로드 빌드**: `startUpdateFlow` 가 즉시 실패 → Log.d 로만 표시되고 사용자
  UX 에는 영향 없음.
- **Flexible vs Immediate**: 본 출시는 비강제 Flexible 만 채택. 보안/결제 치명 이슈가 생기면
  Immediate flow 로 한 줄 변경하여 강제 업데이트 가능.

---

## [Unreleased — v1.6.10 후보] — 2026-05-19

> 정식 출시 직후 발견된 두 가지 사용자 보고 이슈에 대한 핫픽스.

### Fixed
- **흰 런처 아이콘 (Themed icon)** — Android 13+ 단말에서 "테마 아이콘" 토글이 ON 인 경우
  `<monochrome>` 실루엣이 wallpaper 색조로 tint 되어 거의 흰색 아이콘으로 노출되던 현상 수정.
  `mipmap-anydpi-v26/ic_launcher.xml` 및 `ic_launcher_round.xml` 의 `<monochrome>` 선언 제거 →
  Material You themed-icon 미지원으로 전환하고 항상 브랜드 RED adaptive icon 으로 통일.
  비상안전 도구 특성상 브랜드 식별성이 OS 테마 통합성보다 우선. monochrome PNG 자산은 보존 (향후
  더 강한 실루엣 재디자인 후 재활성화 가능).
- **권한 거부 시 EJECT silent fail** — 이전엔 `requestInitialPermissionsIfNeeded` 가 온보딩 직후
  한 번만 권한 요청 + `perms_requested` flag 로 영구 차단. 사용자가 배터리 최적화 제외/오버레이/
  알림 권한을 거부하면 그 이후 EJECT 누름이 silent fail (특히 화면 OFF 상태에서 SHAKE/SIDE_BUTTON
  무반응). 사용자 보고 케이스: "백그라운드 작동 허용 안 해서 화면 끄니 반응 없음".

### Added — Pre-arm permission gate
- `data/PermissionGate.kt` — Trigger 모드별 필수 권한 매트릭스 + missing 계산 (pure utility).
    - `OVERLAY`: 모든 모드 필수 (가짜 통화 화면 그리기).
    - `POST_NOTIFICATIONS`: API 33+ 항상 (FGS 알림 가시성).
    - `BATTERY_OPT`: SHAKE / SIDE_BUTTON / 10초 이상 delay (화면 OFF 생존 필요).
- `MainActivity.PermissionGateDialog` — EJECT 시점에 미충족 권한 있으면 다이얼로그로 안내 + 시스템
  intent 순차 launch. ON_RESUME 마다 missing 재평가 → 모두 grant 되면 자동으로 arm/fire 진행.
  거부 시 폐기되어 **다음 EJECT 누름에 다시 prompt** (사용자 요구 spec: "권한이 없는 작업에 대해
  처음 설치했을때 처럼 사용자에게 다시 요청").
- `MainScreen` — 모든 trigger 분기 (SIDE_BUTTON / SHAKE / IMMEDIATE / DELAYED / CUSTOM) 가
  `ensurePermissions(mode, delayMs)` 콜백을 거치도록 wrap. 기존의 분기별 `canDrawOverlays` 단편
  체크는 통합 게이트로 대체되어 제거.

### Changed
- `AppStrings.kt` — 7개 로케일 (en/ko/zh-CN/zh-TW/ja/es/hi) 에 `permGateTitle` / `permGateBody` /
  `permGateBtnGrant` / `permGateBtnCancel` 4개 신규 string 추가. 톤 가이드 (본부 무전사 voice)
  일관 유지.

---

## [Unreleased — v1.6.9 후보] — 2026-05-18

### Added
- **`BannerAdCard.kt`**: AdView 기반 메인 상시 광고 Composable (v1.6.7 NativeAdCard 대체).
- AdView **Activity lifecycle 연동** (`LifecycleEventObserver` → ON_RESUME/ON_PAUSE/ON_DESTROY) — AdMob "Google 최적화" 자동 새로고침이 정상 동작 → 노출수 ↑ → 광고 수익 1.5~3× 기대 (v1.6.9).
- `PhoneNumberUtil.kt` **`randomMobileLabel(locale)`** — 로케일별 모바일 라벨 (KR 폰/JP 携帯/CN 手机/TW 行動/HK 流動/ES Móvil/IN/US/CA Mobile/기타). 한국어 prefix 하드코딩 i18n bug 수정.

### Changed
- **AdMob 콘솔**: Native + Rewarded 광고단위 영구 삭제. 활성 광고단위는 Banner + Interstitial 2개만.
- **GitHub repo secrets**: `ADMOB_NATIVE_ID` / `ADMOB_REWARDED_ID` 삭제, `ADMOB_BANNER_ID` 신규 추가 (이전에 누락되어 있던 critical issue).
- **BannerAdCard edge-to-edge 레이아웃**: padding(horizontal = 24.dp) + RoundedCornerShape(12) 제거. CTA 우측 잘림 ("OPEN" 버튼 truncated) 정책 위반 위험 해결 (v1.6.8).
- **secrets.properties / .github/workflows/release-aab.yml**: 사용 안 하는 ID 참조 제거 + 코멘트 정리.
- **ShakeDetectionService.kt**: callerName/callerLabel fallback 의 한국어 하드코딩 (`"엄마"`/`"휴대전화"`) → 빈 문자열. intent extra 누락 시 영어 사용자도 한국어 노출 안 됨.

### Removed (Dead code cleanup, 총 약 270줄)
- **`NativeAdCard` 함수 (122줄)** + 관련 imports 12개 — `MainScreen.kt` 에서 v1.6.7 BannerAdCard 교체 후 dead code 였음.
- **`geofence/GeofenceTransitionReceiver.kt`** (91줄) + manifest receiver 등록 + `EjectPrefs.kt` 의 6개 geofence 함수 + 2개 상수. v1.5.8 GPS 권한 제거 + v1.5.10 location dependency 제거 이후 dead code.

### Security
- **`google-services.json`** `.gitignore` 추가 — Android Firebase API key 는 public-safe 이지만 principle 차원에서 untrack.

### Documentation
- `docs/local-release-build-guide.md` / `docs/cowork-setup-prompt.md` — `ADMOB_NATIVE_ID` → `ADMOB_BANNER_ID` 안내 갱신. 비활성 광고단위 cleanup 가이드 추가.

### Phase 1~9 종합 감사 결과 (SimonK Stack 다각도)
- **보안 (Phase 2 /cso)**: CRITICAL/HIGH 0건. MEDIUM 1건 — AdMob SDK 23.2.0 → 23.6+ 업그레이드 권장 (별도 PR).
- **CEO 전략 (Phase 4)**: SELECTIVE EXPANSION 권장. 10-star gap = AI 음성 + WearOS 트리거 + 프로필 팩 마이크로 결제 (별도 backlog).
- **콘솔 통합 (Phase 6)**: Clarity/Firebase/Crashlytics/Play Console assets 모두 OK. PII 마스킹 OK, user_id leak 없음.

---

## [Unreleased]

### Changed — Theme Refactor (Navy + Cream)
- **브랜드 톤 재정렬: 빨강 → 딥 네이비 + 웜 크림.** 앱 아이콘(Deep Navy + Cream)과 앱 내부 UI 톤 단절 해소. "조용히 작동하는 사회 안전 도구" 포지셔닝 강화.
- **Theme.kt — TacticalRed* 상수 값을 네이비로 재정의** (변수명은 호환 유지). `TacticalRedInv` `#BA1A20` → `#1B2D4A` (brand navy), `TacticalRedDeep` `#6A0008` → `#0A1525` (deep navy), 외 3개.
- **Light Palette — Cool gray → Warm cream + Navy ink.** `bg` `#F4F5F7` → `#F2EDE0`, `surface` `#FFFFFF` → `#FBF7EC`, `onSurface` `#1A1C1E` → `#1B2D4A`, `primaryContainer` `#1A1C1E` → `#0F1929` (MAYDAY 카드 deep navy), 등.
- **Dark Palette — Cockpit Dark → Navy Dark + Cream CTA 인버전.** `bg` `#121416` → `#0A1525`, `surface` `#1E2022` → `#13223D`, `red(=CTA)` `#BA1A20` → `#F2EDE0` (cream, "달처럼 빛나는" 효과 의도).
- **Material3 ColorScheme — Light/Dark 동기화.** 모든 surface/background/outline 토큰 새 톤으로.
- **드로어블 빨강 → 네이비**: `ic_eject_mark_red.xml`, `ic_disguise_off.xml`, `ic_disguise_on.xml` 의 fillColor `#A82430` → `#1B2D4A`. 위장 4종 아이콘(계산기/메모/날씨/시계) 고유색 보존.
- **카운트다운/사이드버튼/흔들기 대기 배너 alpha** `0.10f` → `0.14f` — 어두운 네이비 강조색에 맞춰 가시성 미세 보강.
- **앱 런처 아이콘 재디자인** — `ic_launcher_background.xml` 빨강 `#B6191A` → 네이비 `#1B2D4A`. `ic_launcher_foreground.xml` ⏏ 글리프 → 도망치는 사람 + 휴대폰 + 신호 arcs + 말풍선/문 픽토그램. minSdk 26 이므로 adaptive icon 만으로 충분 — raster PNG 없음.
- **EJECT 버튼 글리프 교체** — `EjectButton` composable 의 ⏏ Text(108sp) → 새 vector `ic_hangup_eject.xml` (흰색 수화기 down, 96dp Icon). 메타포가 "eject from situation" → "hang up the call" 로 일관됨. "탈출" 텍스트 + pulse 애니메이션 보존.

### Preserved
- **error 시맨틱은 빨강 보존** — Material3 `error/onError/errorContainer/onErrorContainer` 슬롯은 4곳 모두 hex 명시 고정 (`#BA1A20`, `#FFB4A8` 등). TacticalRed* 상수 참조 제거.
- **TacticalCyan `#00DAF3` (SHAKE 시그니처) 보존, TacticalYellow `#F8BD2A` (SIDE_BUTTON 시그니처) 보존.**
- **`FakeIncomingCallScreenV2`, `InCallScreenV2` 통화 화면은 한 줄도 미변경** — 의도된 격리.
- **TacticalTypography, TacticalShapes 그대로** — 시스템 SansSerif + 0dp 정사각 모서리 유지.

### Known Issues
- ⚠ 라이트 모드에서 가짜 통화 화면이 여전히 다크 그라데이션 → 메인 UI(크림) 와 톤 단절. 별도 PR `refactor(call-screen): theme-aware gradient` 로 후속 처리 예정.
- ⚠ 다크 모드 `outlineVariant` `#3A4A66` 가 deep navy bg 에 대해 ~2.1:1 — 디바이더 용도면 허용, 클릭 윤곽선으로 쓰는 경우 발견 시 `#4A5F82` 상향 검토.

### Design Source
- `docs/design/DESIGN_KNOWLEDGE_TRANSFER.md` — 전체 디자인 시스템 컨텍스트.
- `docs/design/NAVY_CREAM_THEME_REFACTOR_PROMPT.md` — 본 변경의 의도/매핑/검증 명세.

---

## [1.6.0] - 2026-05-17

### 종합 정리·최종 출시 패치

v1.5.x 시리즈의 누적 수정·점검·정리·출시 패치. **앱 핵심 기능 + 신뢰성 + 협업성 동시 정상화**.

### Added — Lock-screen bypass infrastructure (v1.5.23 ~ v1.5.26 누적)

화면 끄고 잠금 후 trigger 발동 시 가짜 통화가 즉시 표시되도록 4단계 진화:

- **v1.5.23**: AndroidManifest `USE_FULL_SCREEN_INTENT` 권한 + `IncomingCallActivity` (transparent trampoline, `setShowWhenLocked + setTurnScreenOn`) + HIGH-importance 알림 채널 `eject_call_v2` + `Notification.Builder.setFullScreenIntent`.
- **v1.5.24**: full-screen intent 권한 fallback 우회 — Service 의 trigger 발동 시점에 `IncomingCallActivity` 를 직접 `startActivity()` (SYSTEM_ALERT_WINDOW + foreground service notification 보유 → background activity start 정책 통과).
- **v1.5.25**: secure keyguard (PIN/패턴) 환경에서 `requestDismissKeyguard` 가 인증 prompt 강제 trigger → 제거. Activity 의 `finish()` 도 제거하여 lifecycle 동안 show-when-locked 권한 유지. Service `dismiss()` 가 `ACTION_FAKE_CALL_ENDED` broadcast 발사 → Activity 자동 finish.
- **v1.5.26**: standing FGS notification 의 HIGH 채널 + `setFullScreenIntent + CATEGORY_CALL + PRIORITY_HIGH` 가 sibling heads-up 으로 노출되는 부작용 → LOW 채널 + 단순 ongoing notification 으로 환원.

결과: 패턴 잠금 상태에서 trigger 발동 → 잠금화면이 그대로 유지된 채 가짜 통화 UI 가 위에 표시, 인증 prompt 없음, sibling 알림 없음.

### Changed — i18n military tone removed across 6 languages

`de91b49` (v1.5.15) 의 ko-only (106 keys) 친근화 작업을 6국으로 확장:

| 영역 | en | zh-CN | zh-TW | ja | es | hi |
|---|---|---|---|---|---|---|
| `tabCommand` | DEPLOY→DIAL | 出动 | 出動 | 出動 | DESPLEGAR→LLAMAR | कमांड→बुलाओ |
| `historySubtitle` | Your escapes | 任务记录→脱身记录 | 任務記錄→脫身記錄 | 脱出履歴 | Registro de misiones→escapes | मिशन लॉग→निकास लॉग |
| `systemsSubtitle` | HQ settings | 战术设置→总部设置 | 戰術設定→總部設定 | 本部設定 | Ajustes tácticos→del cuartel | टैक्टिकल→मुख्यालय |
| `settingsManual` | Field manual→User guide | 任务手册→使用说明 | 任務手冊→使用說明 | 使い方ガイド | Manual de campo→Guía de uso | मिशन मैनुअल→उपयोग गाइड |
| `premiumSubtitle` | Full escape kit | 战术装备→脱身工具包 | 戰術裝備→脫身工具包 | フル脱出装備 | Equipo táctico→Kit de escape | टैक्टिकल लोडआउट→निकास किट |
| `premiumActiveSubtitle` | "deployed"→"ready" | 已部署→已就绪 | 已部署→已就緒 | 戦術装備配備完了→脱出装備準備完了 | desplegado→activado | तैनात→तैयार |
| `premiumActiveBadge` | ACTIVE | (n/a) | (n/a) | (n/a) | OPERATIVO→ACTIVO | (n/a) |
| `unmaskConfirmBody` | "mission profile"→"Emergency Exit icon" | (ko-equivalent ok) | (ok) | (ok) | (ok) | (ok) |
| `actionDisguiseOn` | Engage→Turn on disguise | (ok) | (ok) | (ok) | (ok) | (ok) |
| `coachmarkStepDisguise` (v1.5.22) | loadout→mode | 装备→模式 | 裝備→模式 | 装備→モード | Equipo→Modo | गियर→मोड |
| onboardingCommandBody | DEPLOY tab→DIAL tab | (ok) | (ok) | (ok) | Desplegar→Llamar | (ok) |
| sideModeArmedMsg | command/deploys→volume keys/rings | (ok) | (ok) | (ok) | (ok) | (ok) |

각 언어 고유 의성어 보존 (Beep beep / 嘀嘀 / ピーポーピーポー / Bip bip / बीप बीप).

### Changed — Settings 위장 row icon (v1.5.22)

`MainScreen.kt` 의 `SystemsRow` 에 `iconRes: Int` overload 추가. 위장 row (`settingsDecoy`) 가 placeholder 🎭 emoji 대신 현재 활성화된 decoy 색깔 픽토그램 (계산기 초록 / 메모 노랑 / 날씨 파랑 / 시계 보라) 또는 DEFAULT 시 `ic_disguise_off` 표시. picker dialog · top-bar IconButton 과 시각 시그니처 통일.

### Changed — Decoy picker dialog 아이콘 (v1.5.20)

`MainScreen.kt` L1191-1202 (StitchTopBar picker) + L1821-1832 (Settings picker) 의 `Text("🎭", 18sp)` placeholder 제거 → `Image(painter = painterResource(R.mipmap.ic_decoy_*_foreground), 36dp)` 로 교체. v1.5.16 user-provided raster (1024px) 가 picker 안에 실제 노출.

### Changed — Onboarding step 2 emoji (v1.5.20)

`OnboardingScreen.kt` L100: `🏃` → `👤` — caller chip 기본 avatar (`MainScreen.kt` L2432) 와 unicode 통일.

### Repository hygiene (v1.5.21)

- **Root 잡파일 45MB** — `app-debug.apk` / `app-release.aab` (Apr 25 stale) / `splash_t0.png` / `splash_verify.png` 삭제.
- **8개 미사용 drawables** — `ic_decoy_*_fg.xml`, `ic_decoy_master.png`, `ic_eject_mark{,_red}.xml`, `ic_hangup_eject.xml`. `drawable-nodpi/` 빈 폴더 제거.
- **`store-assets/ic_play_store_512.png`** — `play-store-icon-512.png` 와 md5 동일 dup. 정상 명칭 보존.
- **`.gitignore` 강화** — `release-builds/` (260MB+ 로컬 빌드), `.kotlin/`, `ui*.xml` 추가.
- **`.gitattributes` 신규** — 텍스트 EOL LF 강제 / `*.bat`·`*.cmd`·`*.ps1` 만 CRLF / 바이너리 `-text`. FakeCallOverlayService.kt 의 CRLF phantom-diff (1,125 lines) 재발 방지.

### Fixed (v1.5.18 ~ v1.5.19)

- **v1.5.18**: v1.5.17 의 `ic_disguise_off.xml`/`ic_disguise_on.xml` BitmapDrawable wrapper 가 `painterResource` 호환 안 됨 → vector drawable 직접 참조 롤백.
- **v1.5.19**: Onboarding page 4 `Icons.Filled.Settings` 누락 import 추가. AdMob native ad validator 32×32dp 정책 → `iconView` 48dp + `ScaleType.CENTER_INSIDE` + `minimumWidth/Height` + `adjustViewBounds=false`.

### Verification

- `:app:assembleDebug` BUILD SUCCESSFUL ~31s, 0 errors.
- `:app:lintDebug` 88 warnings, 0 errors. UnusedResources 0.
- Live emulator + 실제 Android 14 phone (Galaxy / LG U+ pattern lock) field test 통과: 잠금화면 + 화면 꺼짐 → trigger → 인증 prompt 없이 fake call UI 즉시 노출 + heads-up sibling 알림 없음.

### Deferred to v1.6.1 / v1.7.0

- `MainScreen.kt` 2,612줄 → 책임별 split (`StitchTopBar`, `DeployTab`, `HistoryTab`, `SettingsTab`, `DecoyDialogs`, `NativeAdCard`).
- `AppStrings.kt` 2,006줄 → 언어별 파일 분리.
- 7국 톤의 잔여 군용 어휘 (en `command`/`HQ panel`, hi `कमांड`/`टैक्टिकल`/`मिशन मैनुअल` 등 secondary keys) 최종 정리.
- Post-call 광고 사이클 (Interstitial every call, RewardedDialog every 3rd call, Premium excluded).
- 구독료 ₩1,900 → ₩3,000 인상 + 14국 PPP fallback (Play Console 가격 정책 변경 + 코드 `localizedFallbackPrice()` 갱신 병행).
- Android 14+ `canUseFullScreenIntent()` Settings deep-link 안내 카드 (rare OEM 대응).

---

## [1.5.21] - 2026-05-17

### Repository hygiene & cleanup

코드/리포 정리 패치. **앱 기능 변경 없음** — 협업 친화성 + 빌드 무결성 + 리포 사이즈 정상화.

### Removed
- **Root 잡파일 45MB** — `app-debug.apk` (21.3MB, 4월 25일 옛 빌드), `app-release.aab` (24MB, 4월 25일), `splash_t0.png`/`splash_verify.png` (v1.5.15 디버그 캡처 425KB). 모두 git 미트래킹 + 코드 참조 0건.
- **`store-assets/ic_play_store_512.png`** — `play-store-icon-512.png` 의 정확한 바이트 중복본 (md5 `03DDE964` 동일). 정상 명칭(`play-store-icon-512.png`) 쪽 보존.
- **8개 미사용 drawables** (lint `UnusedResources` 신뢰 후 grep 재검증):
  - `drawable/ic_decoy_calculator_fg.xml`, `ic_decoy_clock_fg.xml`, `ic_decoy_memo_fg.xml`, `ic_decoy_weather_fg.xml` — v1.5.20 의 mipmap `ic_decoy_*_foreground` 채택 후 잔재된 `_fg.xml` 구버전.
  - `drawable-nodpi/ic_decoy_master.png` — v1.5.16 raster 일괄 적용 후 미참조 마스터.
  - `drawable/ic_eject_mark.xml`, `ic_eject_mark_red.xml` — v1.5.13 rebrand 이전 글리프 잔재.
  - `drawable/ic_hangup_eject.xml` — 롤백된 Navy+Cream theme 잔재 (`EjectButton` composable 이 `Text("⏏")` 로 복귀했음에도 vector 만 남아 있었음).
- **`drawable-nodpi/` 빈 폴더** 제거 (마지막 파일 `ic_decoy_master.png` 삭제 후).

### Changed
- **`.gitignore` 강화** — `release-builds/` (260MB+ 로컬 빌드 산출물 7개 버전), `.kotlin/` (Kotlin daemon 세션 캐시), `ui*.xml` (uiautomator QA 덤프) 추가.
- **`.gitattributes` 신규** — 텍스트 EOL LF 강제 (`*.kt`, `*.kts`, `*.xml`, `*.md`, `.gitignore` 등), Windows-only `*.bat`/`*.cmd`/`*.ps1`/`gradlew.bat` 만 CRLF, 바이너리 (`*.png`/`*.apk`/`*.aab`/`*.jks` 등) `-text`. **FakeCallOverlayService.kt 의 CRLF 부작용으로 발생했던 1,125줄 phantom diff 재발 방지**.
- **`store-assets/play-store-feature-graphic-ko-1024x500 (2).png`** → `play-store-feature-graphic-ko-1024x500.png` 로 리네임 (Windows duplicate-copy `(2)` suffix 제거).

### Fixed
- 이전 세션 크래시로 미드-라인 truncate 된 6개 파일 (`MainScreen.kt` -121줄, `OnboardingScreen.kt` -62, `AppStrings.kt` -35, `MainActivity.kt` -11, `CHANGELOG.md` -172, `.gitignore` -5) 를 HEAD(v1.5.20) 상태로 `git checkout` 복구. 작업 트리 clean 확보 후 본 패치 적용.

### Verification
- `:app:assembleDebug` **BUILD SUCCESSFUL in 32s**, 0 errors, 기존 4건의 `Deprecated in Java` 경고 (PhoneStateListener / setMediaButtonReceiver / callState) 만 유지.
- `:app:lintDebug` 88 warnings 0 errors. `UnusedResources` 8건 → 본 패치에서 모두 처리.
- Emulator 설치 + 콜드스타트 splash 렌더 정상 → 8개 dead drawable 삭제 무관성 검증.
- v1.5.20 decoy picker 4종 아이콘 (계산기 초록 / 메모 노랑 / 날씨 파랑 / 시계 보라) + onboarding step 2 `👤` 시각 검증 통과 (live emulator screencap).

### Deferred (v1.6.x)
- `MainScreen.kt` 2,612줄 god-object → 책임별 split (`StitchTopBar`, `DeployTab`, `HistoryTab`, `SettingsTab`, `DecoyDialogs`, `NativeAdCard` 등) 권장.
- `AppStrings.kt` 2,006줄 → 언어별 파일 분리 (`strings_ko.kt`, `strings_en.kt` …) 검토.
- 정체된 feature 브랜치 6개 (`feat/v1.4.2-ux-patches` ~ `feat/v1.5.16-asset-refresh`) → main 머지 검증 후 일괄 삭제.
- 스테일 `[Unreleased]` 섹션 (롤백된 Navy+Cream theme) — 의도된 historical context vs noise 판단 후 정리.

---

## [1.5.20] - 2026-05-17

### Fixed — Decoy picker 시각 매핑 + Onboarding emoji 통일

- **Decoy icon picker 다이얼로그 (2곳)** 가 placeholder 이모지 `🎭` 대신 v1.5.16 사용자 디자인 아이콘을 노출.
  - `MainScreen.kt` L1191-1202 — StitchTopBar IconButton 의 picker (4 옵션): `Text("🎭", 18sp)` 제거, `Image(painter = painterResource(R.mipmap.ic_decoy_{calculator,memo,weather,clock}_foreground), 36dp)` 로 교체.
  - L1821-1832 — Settings AlertDialog picker (5 옵션 + DEFAULT): RadioButton 와 label 사이 동일 Image 삽입. DEFAULT 옵션은 `R.drawable.ic_eject_button` (메인 앱 아이콘) fallback.
- **Onboarding page 2 emoji** `🏃` → `👤` — caller chip 기본 avatar (`MainScreen.kt` L2432, `emoji = "👤"`) 와 unicode 통일. "DEPLOY 탭에서 누가 호출할지 정한다" 메타포가 사용자가 caller 추가 후 실제 보게 되는 chip emoji 와 시각적으로 일치.

### Notes
- `painterResource(R.mipmap.ic_decoy_*_foreground)` 는 plain raster PNG (5 density) 라 안전 — v1.5.17 의 adaptive icon XML 시도가 BitmapDrawable wrapper 문제로 실패했던 케이스와 다름.
- 라이브러리/build graph 변경 없음.

---

## [1.5.19] - 2026-05-17

### Fixed
- **Onboarding page 4 settings icon** — `Icons.Filled.Settings` 누락된 imports 추가 (v1.5.19 hotfix).
- **AdMob native ad validator 32×32dp 정책 강화** → `iconView` 48dp + `ScaleType.CENTER_INSIDE` + `minimumWidth/Height` + `adjustViewBounds=false` 다중 안전장치 (v1.5.17 의 22→32dp 만으론 validator 가 여전히 violation 표시했음).

---

## [1.5.18] - 2026-05-17

### Fixed
- **`painterResource cannot load BitmapDrawable XML`** 크래시 — v1.5.17 의 `ic_disguise_off.xml`/`ic_disguise_on.xml` 를 BitmapDrawable wrapper 로 만든 시도가 `painterResource` 와 호환되지 않아 런타임 크래시 발생. vector drawable 직접 참조로 롤백.

---

## [1.5.17] - 2026-05-17

### Fixed — AdMob native ad validator, onboarding icons, disguise toggle, splash crop

사용자 4종 후속 피드백 반영.

#### [A] AdMob NativeAdCard 32×32dp 위반 해소

- AdMob native ad validator 가 "Resolution less than 32x32dp or points" 경고로 ad 서빙을 차단할 위험.
- `MainScreen.kt` `NativeAdCard` 의 `iconView` 사이즈 **22dp → 32dp** (정책 최소치). marginEnd 8dp → 10dp 로 미세 조정.
- 한 줄 배너 형태 유지 (row 자체 높이는 setPadding 으로 흡수, 시각적 변동 최소).

#### [B] OnboardingScreen 5개 use-case 아이콘 매핑 갱신

이전 → 이후 (사용자 피드백 매핑):

| Page | 이전 | 이후 |
|---|---|---|
| 1 (Welcome) | `⏏` | `🚨` 사이렌 (브랜드 컨셉 = 비상) |
| 2 (Command) | `🎯` | `🏃` 달리는 사람 ("탈출" 메타포) |
| 3 (Trigger) | `🚨` | `📞` 전화기 ("가짜 통화 발사" 메타포) |
| 4 (Systems) | `⚙` | `⚙` (유지 — 앱 내 설정 아이콘과 일관) |
| 5 (Final Mayday) | `🚨` | `Image(R.drawable.ic_eject_button)` — 사용자 v1.5.16 자산 적용 (앱 아이덴티티 직접 노출) |

#### [C] 위장 토글 아이콘 (ic_disguise_off/on) 신규 디자인 교체

- 사용자가 제공한 `Dedcoy.png` (1024×1024 RGBA, v1.5.16 commit 의 `App_assets/`) 을 5 density raster 로 변환.
  - `drawable-{m,h,xh,xxh,xxxh}dpi/ic_disguise.png` 신규 (24/36/48/72/96 px).
- `drawable/ic_disguise_off.xml`, `ic_disguise_on.xml` 두 vector 를 **BitmapDrawable wrapper** 로 교체 — 같은 ic_disguise raster 참조.
  - 기존: 가면 + ⏏ glyph vector (v1.5.12 design).
  - 신규: 사용자 디자인 (Dedcoy.png) 통일.
  - ON/OFF state 시각 차이는 동일 디자인. 사용자 요청 ("디코이 아이콘 변경") 그대로 적용 — state indicator 추가는 follow-up.

#### [D] 시스템 splash icon 잘림 fix (inset drawable)

- v1.5.14 부터 `windowSplashScreenAnimatedIcon = @mipmap/ic_launcher` (adaptive icon XML at full size) 사용 → adaptive foreground 108dp 캔버스 가득 차는 v1.5.16 사용자 디자인이 splash 192dp mask 가장자리에 잘림 발생 (사용자 보고: "로딩화면은 아이콘이 좀 잘려서 적용이 된듯").
- **`drawable/ic_splash_logo.xml` 신규 추가** — `@mipmap/ic_launcher_foreground` 를 **25% inset** 으로 감싸 splash 안전영역 보장.
  ```xml
  <inset android:drawable="@mipmap/ic_launcher_foreground" android:inset="25%" />
  ```
- `values-v31/themes.xml` 의 `windowSplashScreenAnimatedIcon` 참조를 `@mipmap/ic_launcher` → `@drawable/ic_splash_logo` 로 갱신.
- 결과: splash 192dp 캔버스 안에 글리프 ~96dp 영역 배치, 가장자리 잘림 0.

### Build / Release

- 태그 `v1.5.17` push → `release-aab.yml` 트리거. versionName=1.5.17, versionCode=1000+RUN_NUMBER.
- 라이브러리 의존성 변경 없음.

### Verification 계획

1. **AdMob**: APK 설치 후 메인 화면 native ad 영역에 "AdMob native ad validator" 패널이 더 이상 빨강 ⚠ 경고 표시하지 않는지.
2. **Onboarding**: 콜드스타트 → 5개 화면 각각의 아이콘 = 🚨 / 🏃 / 📞 / ⚙ / 앱 아이콘 순.
3. **위장 토글**: TopBar 우상단 ⏏ 위치에 신규 Dedcoy 디자인 노출.
4. **시스템 splash**: 콜드스타트 직후 1~2초간 EmergencyRed 화면 + 중앙 글리프 가장자리 잘림 없음.

### Known follow-up

- ic_disguise ON/OFF state 시각 차이 (selected 표시 / 색 tint / dot indicator) 추가 — UX 보강 차후 v1.5.18.
- step 5 의 Image 와 step 1~4 의 emoji 가 시각 톤 차이 — 일관성을 위해 모든 step 을 vector drawable 또는 PNG 로 통일은 v1.5.18+ 검토.

---

## [1.5.16] - 2026-05-17

### Changed — User-provided final brand assets applied

사용자 디자인 자산 (`App_assets/`) 을 5 density 로 일괄 적용. 모두 1024~1254px 정사각 RGBA 원본 (`App_assets/*.ai` Illustrator 원본 + 같은 이름 `.png` 익스포트).

#### 1. 앱 런처 아이콘 (`App Icon.png`, 1254×1254)

- **`mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_foreground.png`** × 5 density (108/162/216/324/432 px). adaptive icon foreground.
- **`mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_monochrome.png`** × 5 density (동일 raster — Android 13+ themed icon 가 자동 grayscale).
- **`mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher.png`** × 5 density (48/72/96/144/192 px). legacy raster fallback.
- **`mipmap-{m,h,xh,xxh,xxxh}dpi/ic_launcher_round.png`** × 5 density (48/72/96/144/192 px). round-launcher raster.
- **`store-assets/ic_play_store_512.png`**, **`store-assets/play-store-icon-512.png`** (512×512).

#### 2. 인앱 EJECT 메인 버튼 (`Eject Button.png`, 1254×1254)

- **`drawable-{m,h,xh,xxh,xxxh}dpi/ic_eject_button.png`** × 5 density (192/288/384/576/768 px).

#### 3. 4개 위장 launcher 아이콘 (`Decoy_{Calculator,Clock,Note,Weather}.png`, 1024~1042×같은)

- **`mipmap-{m,h,xh,xxh,xxxh}dpi/ic_decoy_{calculator,clock,memo,weather}_foreground.png`** × 4종 × 5 density = 20 raster.
- **`mipmap-anydpi-v26/ic_decoy_{calculator,clock,memo,weather}.xml`** 4개 의 `<foreground>` 참조를 vector drawable (`@drawable/ic_decoy_*_fg`) → raster mipmap (`@mipmap/ic_decoy_*_foreground`) 로 갱신. `monochrome` 도 동일 raster 사용.
- Naming: `Decoy_Note.png` → `ic_decoy_memo_foreground.png` (코드/manifest의 `memo` 명명 유지).
- `background` 는 기존 vector drawable (`ic_decoy_*_bg.xml`, 단색) 그대로 — 위장 컨셉별 brand color 보존.

#### 4. Decoy 마스터 (`Dedcoy.png`, 1024×1024) — 보존만, UI 미적용

- **`drawable-nodpi/ic_decoy_master.png`** (432×432). 위장 picker dialog 헤더 또는 toggle 아이콘 후보로 보존. 어디에 적용할지 사용자 확정 후 별도 commit.

#### 5. 자산 원본 보존

- **`App_assets/`** 폴더 add — Illustrator 원본 (`.ai`) + export PNG. 향후 재익스포트 / 재리사이즈 시 truth source.

### Resize 기술 세부

- PowerShell + `System.Drawing.Graphics` (HighQualityBicubic + HighQuality SmoothingMode + HighQuality PixelOffsetMode + HighQuality CompositingQuality) 로 일괄 리사이즈.
- 원본 RGBA 알파 채널 보존. 모든 출력 PNG 8-bit alpha.
- 총 48 PNG 신규/재생성 + 4 XML 갱신.

### Build / Release

- 태그 `v1.5.16` push → `release-aab.yml` 트리거. versionName=1.5.16, versionCode=1000+RUN_NUMBER.
- 라이브러리 의존성 변경 없음. AGP/Gradle 그래프 unchanged.

### Verification (Android Studio 가상 디바이스 교차 검증)

본 PR 머지 또는 sideload 검증 단계에서 확인할 시각 포인트:
1. **런처 아이콘**: 홈/앱 서랍에서 v1.5.13 픽토그램 → 사용자 신규 디자인 (App Icon.png) 으로 교체된 모습.
2. **시스템 splash screen** (Android 12+): launcher 아이콘과 같은 디자인으로 자동 노출 (v1.5.14 splash fix 와 결합).
3. **인앱 EJECT 메인 버튼**: 메인 화면 중앙 빨강 버튼 = 사용자 신규 Eject Button.png 디자인.
4. **위장 picker → 4개 옵션 선택 → 홈 화면 확인**: launcher 의 4개 위장 아이콘이 사용자 신규 디자인 (Calculator/Clock/Note=Memo/Weather) 으로 노출.
5. **TopAppBar 좌측 leading icon**: v1.5.15 부터 ic_eject_button drawable 사용 → 새 디자인 자동 반영.

---

## [1.5.15] - 2026-05-16

### Changed — Coachmark UX, Brand Icon, In-App i18n

- **[#1] 코치마크 step 4 (EJECT 버튼) spotlight Circle → RoundRect.** v1.5.13에서 EJECT 메인 버튼이 빨간 원형(⏏ 텍스트 글리프)에서 둥근 사각형 픽토그램(`ic_eject_button` drawable)으로 바뀌었음. spotlight 모양도 동기화 — `MainScreen.kt`의 `coachmark.register("eject", ..., SpotShape.Circle)` → `SpotShape.RoundRect`.
- **[#2] 코치마크 step 5 ↔ step 6 순서 교체.** 이전: settings(본부 정비) → disguise(위장 모드). 변경: disguise → settings. 시각 동선 좌→우(⏏ → ⚙) 자연스러워짐, 마지막 step에 모든 설정 진입점(settings) 두어 투어 마무리. primary 라벨도 함께 이동: disguise=`coachmarkNext`, settings=`onboardingFinalDismiss`.
- **[#3] TopAppBar 좌측 ⏏ 글리프 → v1.5.13 EmergencyRed 픽토그램.** `StitchTopBar`의 `Text("⏏ ${strings.appBrandLabel}")` → `Row { Image(R.drawable.ic_eject_button, 28dp) + Spacer + Text(brandLabel) }`. 메인 EJECT 버튼과 같은 픽토그램 = 브랜드 정체성 일관성.
- **[#4] '출동' 화면 "언제 올까?" → "얼마나 기다려?" (7개 언어).**
  - en: `When?` → `How long?`
  - ko: `언제 올까?` → `얼마나 기다려?` (sectionDelay + triggerTimer 둘 다)
  - zh-CN: `什么时候？` → `等多久？`
  - zh-TW: `什麼時候？` → `等多久？`
  - ja: `いつ来る？` → `あとどれくらい？`
  - es: `¿Cuándo?` → `¿Cuánto esperamos?`
  - hi: `कब?` → `कितना इंतज़ार?`
- **[#5] 위장 picker 영문 잔존 해소 (system locale → in-app 언어 동기화).** Root cause: 위장 옵션 라벨이 `R.string.decoy_label_*` (Android resource = system locale 기반)로 가져와짐 → 인앱 언어를 한국어로 바꿔도 system locale이 영어면 영문 라벨 노출.
  - `AppStrings.kt`에 4개 신규 필드: `decoyLabelCalculator/Memo/Weather/Clock` × 7개 언어
  - `MainScreen.kt` 두 군데(StitchTopBar + Settings AlertDialog)의 8개 `stringResource(R.string.decoy_label_*)` → `strings.decoyLabel*` 교체
  - `res/values*/strings.xml`의 `decoy_label_*`는 그대로 — manifest activity-alias `android:label`(런처 표시명)은 system locale 기반이 맞음

### Changed — i18n 카피라이팅 reframe (working tree 통합)

- **zh-CN, zh-TW, es의 onboarding 카피 군용 톤 → 친근한 톤** reframe. 한국어 베이스("비상 탈출")와 일관성.
- **`MainActivity.kt`의 in-app SplashScreen ⏏ Text → `R.mipmap.ic_launcher_foreground` Image** (28dp rounded EmergencyRed `#B71720`). v1.5.14 시스템 splash + v1.5.13 launcher 아이콘과 시각 일관성.
- **fastlane/metadata 7개 언어 Play Store 설명·title 갱신** — "비상탈출" 브랜드 reframe.

### Removed (cleanup)

- 옛 핸드오프/계획 문서 8종 정리 (일부 `docs/archive/`·`docs/handoffs/` 이동, 일부 영구 삭제).
- 옛 통화 화면 V1 (`FakeInCallScreen.kt`, `FakeIncomingCallScreen.kt`) — V2 마이그레이션 완료.
- `.gitignore`에 `splash_*.png`, `*.tmp` 추가.

### Build / Release

- 태그 `v1.5.15` push로 `release-aab.yml` 트리거. versionName=1.5.15, versionCode=1000+RUN_NUMBER.
- 라이브러리 의존성 변경 없음.

### Known follow-up

- v1.5.15 main commit `8091f41`에 임시 파일 `.commit_msg_v1515.tmp`가 의도치 않게 포함됐음 (다음 commit `38f2c28`에서 정리). v1.5.15 태그가 8091f41을 가리키므로 빌드 산출물에는 영향 없음 (`.tmp`는 컴파일에 영향 없음).

---

## [1.5.14] - 2026-05-16

### Fixed — Splash Screen Icon

- **시스템 스플래시 화면에 구형 아이콘이 노출되던 회귀 수정.** v1.5.13에서 adaptive launcher icon foreground/background를 신규 EmergencyRed 픽토그램으로 교체했으나, `themes.xml`에 `windowSplashScreen*` 속성이 명시되지 않아 Android 12+ 가 디바이스 캐시·OEM 런처 폴백 경로에 따라 이전 리비전 아이콘을 끌어다 쓰는 케이스 발생. ("앱 실행 시 로딩 화면에 구형 아이콘이 아직 들어가있어" v1.5.13 dogfood 보고).
- **`res/values-v31/themes.xml` 신규 추가** — `Theme.EjectButton` 을 v31 (Android 12, API 31) 한정 오버라이드로 재선언하고 다음 splash 속성 명시:
  - `windowSplashScreenAnimatedIcon = @mipmap/ic_launcher` (adaptive icon XML 자체 지정 → OS 가 foreground + background 합성 후 system splash mask 적용)
  - `windowSplashScreenIconBackgroundColor = #B71720` (EmergencyRed)
  - `windowSplashScreenBackground = #B71720` (브랜드 동일색 → 화면 전체 EmergencyRed + 중앙 글리프).
- **minSdk 26 호환 보장** — Android 8.0~11 (API 26~30) 은 system splash screen API 가 없으므로 `values/themes.xml` 의 기존 정의가 그대로 적용. v31 분기는 system splash 가 실제로 적용되는 디바이스만 영향.
- **라이브러리 의존성 변경 없음** — `androidx.core:core-splashscreen` 도입 없이 OS 네이티브 속성으로만 처리. AGP/Gradle 빌드 그래프·ProGuard rule 모두 영향 없음. 롤백은 `res/values-v31/themes.xml` 단일 파일 삭제로 즉시 가능.

### Migration Notes

- 디바이스에 구버전 APK 가 캐시된 경우 새 splash icon 이 적용되려면 1회 uninstall + reinstall 필요 (Android 12+ system splash icon 은 install 시점에 캐시된다). Closed Testing tester 에게는 Play Store 자동 업데이트로 over-install 강제되므로 별도 안내 불필요.
- adaptive icon foreground PNG (`mipmap-{density}/ic_launcher_foreground.png`) 는 v1.5.13 에서 이미 신규 EmergencyRed 픽토그램으로 교체 완료. 본 패치는 OS 레벨 splash icon 라우팅 명시화만 담당.

### Build / Release

- 태그 `v1.5.14` push 로 `.github/workflows/release-aab.yml` 트리거 → `versionCode = 1000 + GITHUB_RUN_NUMBER`, `versionName = 1.5.14` 로 AAB + APK 빌드 후 GitHub Release 게시.
- Play Console upload: 본 빌드의 AAB 를 Closed Testing 트랙에 promote → 12명 테스터에게 자동 배포.

---

## [1.5.13] - 2026-05-12

### Changed — Brand Icon Refresh (EmergencyRed Pictogram)

- **앱 런처 아이콘 신규 픽토그램 도입** — 흰 비상문(말풍선/문 결합 형상) + 빨간 사람(휴대폰을 귀에 대고 신호선 3 arc + 달리는 자세) 디자인. ISO 7010 비상구 표지의 시각 언어를 유지하면서 "통화 중 탈출" 메타포를 직접적으로 표현. SVG 원본 `docs/design/icon-v1.5.13.svg` (1254×1254 viewBox, 47 paths, EmergencyRed `#B71720` 단색 + 흰색).
- **Adaptive icon 자산 일괄 교체** — `ic_launcher_background.xml` 단색 EmergencyRed `#B71720` 로 갱신 (`#1B2D4A` 네이비 → 빨강 환원). 전경은 비트맵으로 전환: 5개 density mipmap PNG (mdpi 48 / hdpi 72 / xhdpi 96 / xxhdpi 144 / xxxhdpi 192). `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` 의 foreground 참조 `@drawable/ic_launcher_foreground` → `@mipmap/ic_launcher_foreground` 로 변경. monochrome 슬롯도 동일 비트맵 사용 (Android 13+ 테마 아이콘 시스템이 자동 마스킹/틴트).
- **Legacy mipmap PNG 자산 추가** — `mipmap-mdpi`~`mipmap-xxxhdpi` 각 폴더에 `ic_launcher.png` + `ic_launcher_round.png` 추가 (Android 7 이하 단말이 adaptive icon 대신 사용).
- **메인 화면 EJECT 버튼 비주얼 통일** — `EjectButton` composable 의 빨간 원 + 흰 hangup 글리프 → 라운드 스퀘어 비상구 픽토그램 이미지 (`ic_eject_button` drawable, 5 density)로 교체. 앱 아이콘과 즉시 시각 연관되도록 동일 디자인 적용. pulse 애니메이션 + halo + shadow는 보존. CANCEL 모드(빨간 원 + ✕)는 미변경 — "정지" 단순 메타포 유지.
- **Play Store 아이콘 갱신** — `store-assets/ic_play_store_512.png` 신 디자인 512×512 PNG 로 교체.

### Added
- **`res/drawable-{density}/ic_eject_button.png`** — 5 density 인앱 EJECT 버튼 비트맵 (192~768 px).
- **`res/mipmap-{density}/ic_launcher_foreground.png`** — 5 density adaptive foreground 비트맵.

### Removed
- `res/drawable/ic_launcher_foreground.xml` — 벡터 전경 (도망 사람 vector) 사용 중단. 비트맵 전경으로 대체.

### Migration Notes
- v1.6.0 Navy + Cream theme refactor (Unreleased 섹션)는 별도 브랜치(`feat/v1.6.0-navy-cream`)로 분리 검토 — 본 v1.5.13 은 EmergencyRed 브랜드 정체성 유지.
- monochrome 슬롯 호환성: Android 13+ 의 themed icon 기능은 비트맵을 grayscale 마스크로 자동 변환. 시각적 일관성을 위해 향후 vector monochrome 자산 별도 제작 권장.
- versionCode: CI `versionCodeOverride` 가 빌드 시 결정 (수동 변경 불필요).

### Design Source
- `docs/design/icon-v1.5.13.svg` — 디자이너 원본 SVG (1254×1254, EmergencyRed 단색 + 흰색).
- `docs/design/ICON_REDESIGN_v1.5.13.md` — 디자인 근거 + 색상 사양 + density 매트릭스.

---

## [1.5.12] - 2026-05-05

### Changed
- **위장 아이콘을 토글 방식으로 변경** — TopAppBar 의 위장 IconButton 이 항상 노출되며 두 상태로 토글:
  - 일반 모드: `ic_disguise_off` (가면 위 + ⏏ 아래) → 클릭 시 4개 위장 옵션 picker (계산기/메모/날씨/시계).
  - 위장 모드: `ic_disguise_on` (⏏ 위 + 가면 떨어짐, 기울어짐) → 클릭 시 복구 확인 다이얼로그.
- 디자인 출처: Claude Design (api.anthropic.com/v1/design/h/t6dOk8pOvqHBzXGxgN00IQ) → handoff-unmask/preview-final.html.
- 이전 v1.5.11 의 단방향 (위장 모드일 때만 노출) 동작 폐기.

### Added
- **프리미엄 "구독 중" 카드** — 결제 후 사용자에게 노출되던 단순 ⭐ + "MAYDAY" 한 줄 배지를 풍부한 구독 중 카드로 교체.  활성 배지 (✓ + "구독 중") + 제목 ("탈출 Mayday — 활성") + features 활성 표시 (✓ × 3) + "구독 관리" 버튼 (Play Store Subscriptions deep link). 4개 신규 strings 키 (actionDisguiseOn 외 — premiumActiveBadge / premiumActiveTitle / premiumActiveSubtitle / premiumManageBtn) × 7개 언어.
- **`res/drawable/ic_disguise_off.xml`** — STATE 1 vector drawable (가면 + ⏏, 미위장 상태).
- **`res/drawable/ic_disguise_on.xml`** — STATE 2 vector drawable (⏏ + 떨어진 가면 -8° rotation, 위장 상태).
- **`res/drawable/ic_eject_mark.xml`** + **`ic_eject_mark_red.xml`** — master ⏏ glyph (tintable + 빨강 hard-coded).
- **코치마크 Step 6 (disguise)** — TopAppBar 위장 토글 spotlight 추가. settings step 다음에 배치되어 시각적 흐름 자연스러움. 이전 마지막 step (settings) primary label 을 "다음" 으로 변경, 새 disguise step 의 primary label 이 "옛썰!" (마지막).
- **설정 → 사용 설명서 메뉴** — 코치마크 step 4 desc ("사용 설명서") 가 안내하는 메뉴를 실제로 추가. 클릭 시 EjectPrefs.saveCoachmarkSeen(false) → COMMAND 탭 자동 전환 → coachmark.start() 으로 6-step 투어 재실행.
- **`actionDisguiseOn`** strings 키 (7개 언어) — 일반 모드 IconButton contentDescription ("앱 위장").
- **`coachmarkStepDisguiseTitle/Desc`** strings 키 (7개 언어) — Step 6 코치마크 카피.
- **`settingsManual`** strings 키 (7개 언어) — "사용 설명서" 메뉴 라벨.

### Fixed
- 🐛 **버전 표시 버그** — 모든 언어에서 `settingsVersion = "v1.0 · ..."` 하드코딩 (실제 v1.5.x 와 mismatch). `BuildConfig.VERSION_NAME` 으로 동적 표시 (`"v" + BuildConfig.VERSION_NAME + "  ·  " + strings.catchphrase`). CI가 `-PversionNameOverride` 로 주입한 실제 버전이 자동 반영.

### Notes
- preview-final.html 은 phantom half-mask 디자인 제안이지만 4개 vector XML 은 venetian symmetric mask 로 production-ready 상태. HANDOFF.md 가 canonical 이라 그대로 ship.
- 검증: compileDebugKotlin SUCCESSFUL.
## [1.5.11] - 2026-05-05

### Added
- **위장 복구 (Unmask) TopAppBar IconButton** — 위장 모드 (계산기/메모/날씨/시계) 일 때만 ⚙ Settings 왼쪽에 노출되는 액션 아이콘.  탭 → 확인 다이얼로그 → `DecoyManager.setActive(ctx, Decoy.DEFAULT)` 호출로 launcher 가 즉시 원래 "Eject Button" 아이콘+이름으로 복구. `EjectPrefs.loadDecoy(ctx)` 동기 read 로 재추적, 상태 변경 시 즉시 아이콘 사라짐.
- **`res/drawable/ic_unmask.xml`** — 24×24 vector drawable. Design: handoff-unmask/preview-v2.html V1 (PICK).  ⏏ 글리프 (ink `#1A1A1A` solid + 1.8 stroke 가로 바, 메인 EJECT 버튼과 동일 비율) + 베네치아 가면 (deep red `#A82430`, -10° 회전 + (0.4, 0.4) translate, 가로 띠 + V 컷 코 + 양 끝 살짝 솟음 + 아몬드 슬릿 눈).
- **7개 언어 strings 4종 추가** — `actionUnmask` (icon contentDescription), `unmaskConfirmTitle`, `unmaskConfirmBody`, `unmaskConfirmCta`. ko: "위장 복구" / "원래 아이콘으로 복구할까요?" / "런처 아이콘과 앱 이름이 원래대로 돌아갑니다…" / "복구". en/ja/zh-CN/zh-TW/es/hi 모두 동일 의미로 번역. `dialogCancel` 은 기존 키 재사용 (중복 선언 안 함).

### Notes
- 위장 안 한 일반 사용자에게는 아이콘 자체가 렌더되지 않아 **TopAppBar 시각 무게 변화 0** — 위장 사용자만 눈에 띄게 표시되어 가장 빠르게 복구 가능.
- `tint = Color.Unspecified` 로 vector drawable 의 hard-coded fillColor (deep red + ink) 를 그대로 노출. 메인 EJECT 버튼과 동일 톤이라 위장 모드에서 "이 색이 진짜다" 라는 시그니처 역할.
- 디자인 출처: Claude Design (api.anthropic.com/v1/design/h/gOL7DQKFwGE_CsQvoqKmaQ) → handoff-unmask/preview-v2.html V1 (PICK).
## [1.5.10] - 2026-05-05

### Fixed
- **lint release: 1 ERROR → 0 ERROR** — `UnrememberedMutableInteractionSource` (CoachmarkOverlay.kt). 매 recomposition마다 새 `MutableInteractionSource()` 생성하던 것을 `remember { }` 로 감싸 안정화.
- **lint release: SetTextI18n** — `NativeAdCard` 의 `text = "AD"` 광고 disclosure 라벨 (Google AdMob 정책상 영어 유지). `@SuppressLint("SetTextI18n")` + `import android.annotation.SuppressLint` 추가, 정책 근거 인라인 주석 명시.
- **lint release: ApplySharedPref** — `EjectPrefs.savePremium` / `saveAdsRemoved` 의 `.commit()` 의도된 동기 호출 (BillingManager 결제 콜백 직후 크래시 시 `.apply()` 의 비동기 disk write 유실 → 결제했는데 `is_premium=false` 사고 가능). `@Suppress("ApplySharedPref")` 추가, 의도 인라인 주석 명시.

### Removed
- **dead dependency: `play-services-location`** — v1.5.8 패치에서 ACCESS_FINE_LOCATION + ACCESS_BACKGROUND_LOCATION + GeofenceManager 모두 제거됐는데, `app/build.gradle.kts` 에 `implementation(libs.google.location)` 만 남아있던 잔재. `gradle/libs.versions.toml` 의 `playServicesLocation` 버전 + `google-location` 라이브러리 정의도 정리. 코드에 사용처 0건 확인 후 제거.

### Notes
- **검증**: SimonK Stack v85 풀스택 검증 → `compileDebugKotlin` SUCCESSFUL, `lintRelease` 0 errors / 48 warnings (모두 Information level 또는 BoM 안정성 우선의 의도된 outdated dep, 즉 ship-blocker 아님). 시크릿 스캔 0건.

## [1.5.9] - 2026-05-04

### Added
- **위장 아이콘 디자인 4종**: 위장(decoy) 기능이 이전엔 앱 *이름*만 변경하고 아이콘은 그대로 ic_launcher를 썼던 한계 해결. 각 alias에 자체 vector drawable 적용:
  - 계산기 (다크 그레이 + display + 4×3 버튼 grid + orange 연산자)
  - 메모장 (소프트 옐로 + paper sheet with dog-ear + 텍스트 라인)
  - 날씨 (하늘색 + sun with rays + cloud)
  - 시계 (다크 그린 + analog face + 12/3/6/9 markers + hour/minute hands + center dot)
- 각 위장에 adaptive-icon (background + foreground + monochrome for Android 13+ Material You) + manifest 4개 alias의 `android:icon`/`android:roundIcon` 변경.

### Changed
- **설정 - 업그레이드 카드 혜택 표시**: 카드에 어떤 혜택이 있는지 사용자에게 명확히 알림. `premiumFeature1`/`Feature2`/`Feature3` (이미 7개 언어 정의됨)을 ✓ 아이콘 + 텍스트로 list 표시 (광고 제거 / 무제한 호출 대상 / 통화 중 대사 힌트).
- **하단 "광고 제거" 결제 버튼을 업그레이드 카드와 통합**: SystemsRow의 별도 광고 제거 결제 옵션 제거. 광고 제거는 이제 프리미엄 업그레이드 혜택 중 하나로만 노출 — 두 결제 path 사이 사용자 혼란 해소. 이미 광고 제거 결제한 기존 사용자는 "✅ 광고 제거됨" 행이 그대로 유지되어 결제 인식이 영구 보존됨 (BillingManager 결제 흐름 자체는 모두 보존).

[1.5.9]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.9

## [1.5.8] - 2026-05-04

### Removed
- **dead code**: `com.ejectbutton.geofence.GeofenceManager.kt` (149 lines, 호출하는 곳 0건). v1.4.0에 추가됐으나 실제 wiring이 안 된 채 잔존.
- **manifest 위치 권한**: `ACCESS_FINE_LOCATION`, `ACCESS_BACKGROUND_LOCATION`. dead code였던 GeofenceManager 외에 사용처 없었음. Play Console 비공개 테스트 검토에서 "선언되지 않은 민감한 권한" 오류로 출시 차단됨 → 권한 제거로 해결. 향후 지오펜스 기능 재도입 시 권한 + 정당화 양식 다시 추가 필요.

### Note
v1.5.7 의 모든 변경은 그대로 포함. 이번 patch는 Play Console 검토 통과를 위한 권한 cleanup만.

[1.5.8]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.8

## [1.5.7] - 2026-05-04

### Fixed
- **코치마크 spotlight 위치 (v1.5.6 known issue 해결)**: STEP 1·2·3 ring이 의도한 register와 130px 어긋나 다음 register 영역에 그려지던 off-by-one shift. Root cause: register는 `boundsInRoot()` (window root 좌표, status bar 포함) 사용, overlay Box는 `statusBarsPadding()` 적용된 outer Box 안에 위치 → 좌표계 mismatch ≈ statusBarHeight. CoachmarkHost를 outer Box 밖 root composition level로 이동해 fix.
- **가짜 통화 수신 화면 Lottie ring fade-out 버그**: max swipe (progress=1.0) 시 Lottie spec의 frame 100에서 두 ring 모두 opacity 0으로 fade out → ring 사라짐. `dragFraction.coerceIn(0f, 0.95f)`로 cap해 frame 95에서 멈춰 max size + visible opacity 유지.

### Changed
- **가짜 통화 수신 버튼 layout 정확도 (frame 2 reference 매칭)**: outer Box 200dp → 120dp (대칭 확보, SpaceBetween overlap 해결), Row horizontal padding 56dp → 16dp (좌우 가장자리 18%/82% 위치).
- **가짜 통화 수신 swipe 애니메이션 속도**: `dragThresholdPx` 120f → 300f. swipe 거리 2.5배 증가로 ring expansion이 자연스럽게 천천히 진행.
- **Lottie ring max 크기**: `Modifier.requiredSize(460.dp)`로 parent constraint 우회 (이전 `Modifier.size`는 outer Box 120dp constraint에 의해 clip됨). max ring 우측 끝이 화면 width 60%에 도달 (사용자 spec).
- **CallerChips**: `LazyRow` → `Row + horizontalScroll`로 변경 (eager measure, horizontal scroll 유지).

### Removed
- 사용하지 않는 `androidx.compose.foundation.lazy.LazyRow` import 제거.

[1.5.7]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.7

## [1.5.6] - 2026-05-02

### Added
- 肄붿튂留덊겕 5-step ?뺤옣 (timing ?좉퇋) + AppStrings.coachmarkStepTiming* 횞 7 ?몄뼱

### Changed
- 肄붿튂留덊겕 ?띿뒪??援곕? ??(?쒓뎅???곸뼱 ?곗꽑): 肄쒖궗???몃━嫄?留됱궗/移댁슫?몃떎????- EJECT 鍮④컙 梨꾩슦湲?+ ?????덉텧 + 50% ?ъ씠利???(72??08sp, 13??0sp)
- placeTooltip ?붾㈃ 鍮꾩쑉 湲곕컲 (cardH=screenH*0.22, gap=*0.02 ??
- BottomBar ?몃쾭/?대┃ ?뚯쁺 ?듭씪 (clickable area = sliding pill area)

### Fixed
- Tooltip dim 媛?ㅼ쭚: drawWithContent ?쒖꽌 (dim ??cutout ??drawContent)

### Known issue (v1.5.7)
- Step 1/2/timing spotlight ?꾩튂: verticalScroll + LazyRow boundsInRoot timing ?댁뒋

[1.5.6]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.6

## [1.5.3] ??2026-05-02

### Changed
- **BottomBar ?紐껊탵?냈??꾧숲 sliding pill ?袁れ넎** ??v1.5.0~v1.5.2 ??step function on/off ??? fix.
  `animateFloatAsState` 嚥?active pill ?袁⑺뒄 ?????諭?+ text color/fontWeight ??쑬? 癰귣떯而?
  iOS UISegmentedControl / Material3 NavigationBar ?? ?꾨뗀瑗?shadow/border ?遺우쁽????뽯뮞??域밸챶?嚥??醫?.

### User feedback addressed
- #6 BottomBar ?紐껊탵?냈??꾧숲 ?봔??뺤쑎???袁れ넎 - applied (?遺우쁽????? ????筌욊낯???닌뗭겱, 亦낅슣??????2)

[1.5.3]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.3

## [1.5.2] ??2026-05-02

### Fixed
- **?꾨뗄?귨쭕?딄쾿 spotlight cutout ??? fix** ??`graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }` ?袁⑥뵭??곗쨮 BlendMode.Clear 揶쎛 ?臾먮짗 ????곴퐣 ring ??됱벥 child content (??뺢돌?귐딆궎 燁삳?諭?/ EJECT 甕곌쑵??/ ?? 揶쎛 dim ??곗쨮 揶쎛??쇱１???얜챷?? cutout ?類ㅺ맒 ?臾먮짗 + dim alpha 0.74 ??0.62 嚥????? ([#1])
- **?꾨뗄?귨쭕?딄쾿 tooltip 燁삳?諭??袁⑺뒄 ??됱읈 筌띾뜆彛?* ??`placeTooltip` 揶쎛 spotlight ?? ??湲???野껊???袁⑥쨯 ???袁⑥삋 揶쎛???⑤벀而???쑨???곴퐣 ??筌잛럩肉?獄쏄퀣?? cardH 220dp ??180dp ?뚮똾??紐낆넅 + ??덉읅 ?ル슦紐??④쑴沅? ([#1])
- **揶쎛筌?incoming-call ??롮뵭/椰꾧퀣??甕곌쑵????癒????怨뺤뵬揶쎛????? fix** ??`Modifier.offset { IntOffset(offsetX, offsetY) }` ??볤탢. 甕곌쑵???袁⑺뒄/??由??域밸챶?嚥??醫???랁?(?????筌뤿굞?? drag ?紐낅?紐껊뮉 Lottie ring progress 嚥≪뮆彛???볦퍟?? ([#2])

### Changed
- **??뽯뮞?????춳 ?酉琉??= LIGHT** ???醫됲뇣 ?????筌???쎈뻬 ????깆뵠??筌뤴뫀諭뜻에???뽰삂. 疫꿸퀣???????(??? SYSTEM/DARK/LIGHT ???貫留? ??域?揶??醫?. ([#4])
- **BottomBar ????곌볼 + ????삳쐭 7揶??紐꾨선 ??븍럢??꾩넅** ??疫꿸퀣??筌뤴뫀諭??紐꾨선揶쎛 ?怨몃선 ("COMMAND/HISTORY/SETTINGS") ??????? fix. ?醫됲뇣 ??`appBrandLabel`, `ejectButtonLabel` + `tabCommand/History/Systems` 7揶??紐꾨선 甕곕뜆肉??곕떽?. ??볥럢??= 筌뤿굝議?疫꿸퀡以???쇱젟, ??곕궚??= ??덇묻??덇퉳/畑두?/庸?痢? 餓λ쵌???揶쏄쑴猿?= ??뤿묽/繹リ꼈??繹レ뼇???? ([#5])

### Documentation
- `CHANGELOG.md` ?醫됲뇣 ?臾믨쉐 (v1.5.0/1/2 entries ?곕떽?).
- ?????preferences ????15甕?(Auto-generate CHANGELOG-style summary) ?怨몄뒠 ??뽰삂.

---

## [1.5.1] ??2026-05-02

### Added
- **?꾨뗄?귨쭕?딄쾿 4-step ??堉??遺우쁽???怨몄뒠** (Claude Design ??? drop-in)
  - ?醫됲뇣 `app/src/main/java/com/ejectbutton/ui/coachmark/` ???텕筌왖
  - `CoachmarkState.kt` ??`register(id, rect, shape)` + `index/isActive/start/next/dismiss`
  - `CoachmarkOverlay.kt` ??3-stop EjectRed halo (alpha 1.0/0.20/0.08), 4-dot progress, STEP N pill, ghost Skip, EjectRed primary pill. ??깆뵠????쎄쾿 ?癒?짗 ?브쑨由?(theme tokens)
- **MainScreen 4 spotlight register**
  - `CallerChips` (scenario, RoundRect)
  - `TriggerModeRow` (trigger, RoundRect)
  - `EjectButton` (eject, Circle)
  - StitchTopBar ????`IconButton` (settings, RoundRect via callback)
- **?꾨뗄?귨쭕?딄쾿 筌욊쑵六?餓?筌롫뗄???遺얇늺 ?ル슣???????꾨늄 筌△뫀??* ??`pointerInput(coachmark.isActive)` + early return ([#3])

### Removed
- 疫꿸퀣??`app/src/main/java/com/ejectbutton/ui/onboarding/CoachmarkOverlay.kt` (v1.5.0 ?ⓥ몿爰? ??볤탢 ???????텕筌왖嚥???筌?

### Build
- `versionCode = 1043`, `versionName = "1.5.1"`
- Release Build #43 (commit `a682261`) ??14m 11s
- Tag `v1.5.1` + 獄쏄퉮毓??됰슢?뽫㎉?`release/v1.5.0` (rollback 癰귣똻??

---

## [1.5.0] ??2026-05-02

### Added
- **?꾨뗄?귨쭕?딄쾿 4-step ?ⓥ몿爰?* (?????a-ha 筌뤴뫀????봔鈺???筌롫뗄???遺얇늺 4揶????뼎 甕곌쑵????덇땀)
  - `EjectPrefs.coachmarkSeen` flag (save/load) ??筌???쎈뻬 1???癒?짗 ??뽯뻻
  - `MainScreen` ?꾨뗄?귨쭕?딄쾿 overlay (COMMAND ????뽰젟, fullscreen dim + tooltip)
  - `AppStrings.kt` 23 ?醫됲뇣 ?袁⑤굡 ??7 ?紐꾨선 = 161 string ?곕떽? (?꾨뗄?귨쭕?딄쾿 step 1~4 + 5??ｍ?onboarding flow foundation)

### Notes
- v1.5.0 ????롫즲????뽯튋: spotlight ?ル슦紐??`listOf(null ??4)` (??쇰? 沃섎㈇??? ??v1.5.1 ?癒?퐣 ?類????袁⑥┷.
- 5??ｍ?OnboardingScreen ?귐뗫솯?醫딆춦 (Welcome / Use Cases / Coachmark / Ready / End) ?? strings 筌?沃섎챶???곕떽?, UI ?????? ?곕???筌욊쑵六?

### Build
- `versionCode = 1042`, `versionName = "1.5.0"`
- Release Build #42 (commit `fba0e99`) ??13m 21s
- Tag `v1.5.0`

---

## [1.4.0~1.4.4] ????곸읈 ?곗뮇??
?怨멸쉭??GitHub Release ?紐낅뱜 筌〓챷??
- v1.4.4 ??Lottie drag_confirm UI + textAlign Center + EJECT ??븍럢??꾩넅
- v1.4.3 ??FLAG_SECURE ?袁⑸뻻 ??볤탢 (???뮞????쎄쾿?깃퀣爰?揶쎛??
- v1.4.2 ??????已???븍럢??launcher
- v1.4.1 ???袁⑹삢 ?袁⑹뵠??7揶??紐꾨선 ??곌볼
- v1.4.0 ??GPS 筌왖??쎈쟼??infrastructure (UI 沃섎챸???

---

[1.5.2]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.2
[1.5.1]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.1
[1.5.0]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.0
[#1]: ./HANDOFF_v1.5.0.md "???????곕굡獄?#1 (?꾨뗄?귨쭕?딄쾿 ?遺우쁽??"
[#2]: ./HANDOFF_v1.5.0.md "???????곕굡獄?#2 (??롮뵭/椰꾧퀣??甕곌쑵??"
[#3]: ./HANDOFF_v1.5.0.md "???????곕굡獄?#3 (?????꾨늄 筌△뫀??"
[#4]: ./HANDOFF_v1.5.0.md "???????곕굡獄?#4 (???춳 LIGHT)"
[#5]: ./HANDOFF_v1.5.0.md "???????곕굡獄?#5 (?怨론???븍럢??꾩넅)"