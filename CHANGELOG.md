# Changelog

紐⑤뱺 二쇱슂 蹂寃쎌궗??? ???뚯씪??湲곕줉?⑸땲??
?뺤떇: [Conventional Commits](https://www.conventionalcommits.org/) + [SemVer](https://semver.org/lang/ko/).

---

## [Unreleased]

## [1.5.3] ??2026-05-02

### Changed
- **BottomBar ?몃뵒耳?댄꽣 sliding pill ?꾪솚** ??v1.5.0~v1.5.2 ??step function on/off ?뚭? fix.
  `animateFloatAsState` 濡?active pill ?꾩튂 ?щ씪?대뱶 + text color/fontWeight 鍮꾨? 蹂닿컙.
  iOS UISegmentedControl / Material3 NavigationBar ?? 肄붾꼫/shadow/border ?붿옄???쒖뒪??洹몃?濡??좎?.

### User feedback addressed
- #6 BottomBar ?몃뵒耳?댄꽣 遺?쒕윭???꾪솚 - applied (?붿옄???듬? ???吏곸젒 援ы쁽, 沅뚯옣 ?듭뀡 2)

[1.5.3]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.3

## [1.5.2] ??2026-05-02

### Fixed
- **肄붿튂留덊겕 spotlight cutout ?뚭? fix** ??`graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }` ?꾨씫?쇰줈 BlendMode.Clear 媛 ?묐룞 ???댁꽌 ring ?덉쓽 child content (?쒕굹由ъ삤 移대뱶 / EJECT 踰꾪듉 / ?? 媛 dim ?쇰줈 媛?ㅼ죱??臾몄젣. cutout ?뺤긽 ?묐룞 + dim alpha 0.74 ??0.62 濡??쏀솕. ([#1])
- **肄붿튂留덊겕 tooltip 移대뱶 ?꾩튂 ?덉쟾 留덉쭊** ??`placeTooltip` 媛 spotlight ? ??긽 ??寃뱀튂?꾨줉 ???꾨옒 媛??怨듦컙 鍮꾧탳?댁꽌 ??履쎌뿉 諛곗튂. cardH 220dp ??180dp 而댄뙥?명솕 + ?숈쟻 醫뚰몴 怨꾩궛. ([#1])
- **媛吏?incoming-call ?섎씫/嫄곗젅 踰꾪듉???먭????곕씪媛???뚭? fix** ??`Modifier.offset { IntOffset(offsetX, offsetY) }` ?쒓굅. 踰꾪듉 ?꾩튂/?ш린??洹몃?濡??좎??섍퀬 (?ъ슜??紐낆떆) drag ?명뀗?몃뒗 Lottie ring progress 濡쒕쭔 ?쒓컖?? ([#2])

### Changed
- **?쒖뒪???뚮쭏 ?뷀뤃??= LIGHT** ???좉퇋 ?ъ슜??泥??ㅽ뻾 ???쇱씠??紐⑤뱶濡??쒖옉. 湲곗〈 ?ъ슜??(?대? SYSTEM/DARK/LIGHT ??λ맂) ??洹?媛??좎?. ([#4])
- **BottomBar ???쇰꺼 + ???ㅻ뜑 7媛??몄뼱 ?ㅺ뎅?댄솕** ??湲곗〈 紐⑤뱺 ?몄뼱媛 ?곸뼱 ("COMMAND/HISTORY/SETTINGS") ????뚭? fix. ?좉퇋 ??`appBrandLabel`, `ejectButtonLabel` + `tabCommand/History/Systems` 7媛??몄뼱 踰덉뿭 異붽?. ?쒓뎅??= 紐낅졊/湲곕줉/?ㅼ젙, ?쇰낯??= ?녈깯?녈깋/掠ζ?/鼇?츣, 以묎뎅??媛꾩껜 = ?뉏빱/溫겼퐬/溫양쉰 ?? ([#5])

### Documentation
- `CHANGELOG.md` ?좉퇋 ?묒꽦 (v1.5.0/1/2 entries 異붽?).
- ?ъ슜??preferences ??ぉ 15踰?(Auto-generate CHANGELOG-style summary) ?곸슜 ?쒖옉.

---

## [1.5.1] ??2026-05-02

### Added
- **肄붿튂留덊겕 4-step ?ъ뼱 ?붿옄???곸슜** (Claude Design ?듬? drop-in)
  - ?좉퇋 `app/src/main/java/com/ejectbutton/ui/coachmark/` ?⑦궎吏
  - `CoachmarkState.kt` ??`register(id, rect, shape)` + `index/isActive/start/next/dismiss`
  - `CoachmarkOverlay.kt` ??3-stop EjectRed halo (alpha 1.0/0.20/0.08), 4-dot progress, STEP N pill, ghost Skip, EjectRed primary pill. ?쇱씠???ㅽ겕 ?먮룞 遺꾧린 (theme tokens)
- **MainScreen 4 spotlight register**
  - `CallerChips` (scenario, RoundRect)
  - `TriggerModeRow` (trigger, RoundRect)
  - `EjectButton` (eject, Circle)
  - StitchTopBar ????`IconButton` (settings, RoundRect via callback)
- **肄붿튂留덊겕 吏꾪뻾 以?硫붿씤 ?붾㈃ 醫뚯슦 ?ㅼ??댄봽 李⑤떒** ??`pointerInput(coachmark.isActive)` + early return ([#3])

### Removed
- 湲곗〈 `app/src/main/java/com/ejectbutton/ui/onboarding/CoachmarkOverlay.kt` (v1.5.0 怨④꺽) ?쒓굅 ?????⑦궎吏濡??泥?

### Build
- `versionCode = 1043`, `versionName = "1.5.1"`
- Release Build #43 (commit `a682261`) ??14m 11s
- Tag `v1.5.1` + 諛깆뾽 釉뚮옖移?`release/v1.5.0` (rollback 蹂댁옣)

---

## [1.5.0] ??2026-05-02

### Added
- **肄붿튂留덊겕 4-step 怨④꺽** (?ъ슜??a-ha 紐⑤㉫??遺議???硫붿씤 ?붾㈃ 4媛??듭떖 踰꾪듉 ?덈궡)
  - `EjectPrefs.coachmarkSeen` flag (save/load) ??泥??ㅽ뻾 1???먮룞 ?쒖떆
  - `MainScreen` 肄붿튂留덊겕 overlay (COMMAND ???쒖젙, fullscreen dim + tooltip)
  - `AppStrings.kt` 23 ?좉퇋 ?꾨뱶 횞 7 ?몄뼱 = 161 string 異붽? (肄붿튂留덊겕 step 1~4 + 5?④퀎 onboarding flow foundation)

### Notes
- v1.5.0 ???섎룄???쒖빟: spotlight 醫뚰몴??`listOf(null 횞 4)` (?ㅼ륫 誘멸뎄?? ??v1.5.1 ?먯꽌 ?뺣????꾨즺.
- 5?④퀎 OnboardingScreen 由ы뙥?좊쭅 (Welcome / Use Cases / Coachmark / Ready / End) ? strings 留?誘몃━ 異붽?, UI ?듯빀? 異뷀썑 吏꾪뻾.

### Build
- `versionCode = 1042`, `versionName = "1.5.0"`
- Release Build #42 (commit `fba0e99`) ??13m 21s
- Tag `v1.5.0`

---

## [1.4.0~1.4.4] ???댁쟾 異쒖떆

?곸꽭??GitHub Release ?명듃 李몄“:
- v1.4.4 ??Lottie drag_confirm UI + textAlign Center + EJECT ?ㅺ뎅?댄솕
- v1.4.3 ??FLAG_SECURE ?꾩떆 ?쒓굅 (?뚯뒪???ㅽ겕由곗꺑 媛??
- v1.4.2 ?????대쫫 ?ㅺ뎅??launcher
- v1.4.1 ???꾩옣 ?꾩씠肄?7媛??몄뼱 ?쇰꺼
- v1.4.0 ??GPS 吏?ㅽ렂??infrastructure (UI 誘명넻??

---

[1.5.2]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.2
[1.5.1]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.1
[1.5.0]: https://github.com/Simon-YHKim/eject-button/releases/tag/v1.5.0
[#1]: ./HANDOFF_v1.5.0.md "?ъ슜???쇰뱶諛?#1 (肄붿튂留덊겕 ?붿옄??"
[#2]: ./HANDOFF_v1.5.0.md "?ъ슜???쇰뱶諛?#2 (?섎씫/嫄곗젅 踰꾪듉)"
[#3]: ./HANDOFF_v1.5.0.md "?ъ슜???쇰뱶諛?#3 (?ㅼ??댄봽 李⑤떒)"
[#4]: ./HANDOFF_v1.5.0.md "?ъ슜???쇰뱶諛?#4 (?뚮쭏 LIGHT)"
[#5]: ./HANDOFF_v1.5.0.md "?ъ슜???쇰뱶諛?#5 (?곷Ц ?ㅺ뎅?댄솕)"
