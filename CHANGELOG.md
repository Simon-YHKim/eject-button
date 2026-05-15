# Changelog

筌뤴뫀諭?雅뚯눘??癰궰野껋럩沅??? ?????뵬??疫꿸퀡以??몃빍??
?類ㅻ뻼: [Conventional Commits](https://www.conventionalcommits.org/) + [SemVer](https://semver.org/lang/ko/).

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