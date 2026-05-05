# Changelog

筌뤴뫀諭?雅뚯눘??癰궰野껋럩沅??? ?????뵬??疫꿸퀡以??몃빍??
?類ㅻ뻼: [Conventional Commits](https://www.conventionalcommits.org/) + [SemVer](https://semver.org/lang/ko/).

---

## [Unreleased]

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
