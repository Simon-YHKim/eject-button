# Eject Button — Backlink Launch Kit (v1.6.9)

백링크 유도용 게시 글 초안 모음. 각 섹션은 그대로 복붙 가능하지만, 등록 직전에 한 번씩 Simon 의 목소리로 손보는 걸 권장.

플레이스토어 링크 (공통):
- `https://play.google.com/store/apps/details?id=com.simonykim.ejectbutton`

준비물 (공통):
- 스크린샷 5장 (한국어 + 영어 mix)
- 데모 영상 30초 (선택)
- 앱 아이콘 (rev3)

---

## 1. Product Hunt — 글로벌 런칭

### 1-1. Tagline (60자 한계, 영어)

**1순위:**
> Beep beep! One tap → realistic fake call. Slip out of awkward dinners.

**예비:**
> Your one-tap exit from awkward dinners, dates, and meetings.

### 1-2. Description (260자 한계)

> Emergency Exit is a one-tap fake-call alibi for introverts and "can't-say-no" types. A realistic incoming-call screen rings in — answer naturally, stand up naturally, walk out. No real calls placed. Built for entertainment / simulation only. 8 languages, dark mode, offline.

### 1-3. First comment (Maker introduction, 영어, ~500자)

> Hey Product Hunt 👋
>
> I'm Simon, a solo dev. I built Emergency Exit because I kept getting stuck in 2-hour "30-minute" meetings and dinners that wouldn't end. I'm an introvert and "no thanks" never quite makes it out of my mouth.
>
> So I made the app I wished I had: one tap, a fake call rings in, you walk out. The whole interface is a single big red button. Works from inside your pocket. No real calls, no signup, no internet needed.
>
> v1.6.9 ships with 8 languages, AdMob banner ads (no rewarded ads — we used a share-to-unlock model instead), and a clear "this is entertainment / simulation only, please don't deceive anyone" notice.
>
> Open to feedback — especially on the dark "social anxiety" tone vs. light "office humor" tone. Which lands better with you?

### 1-4. Reply templates (FAQ 빠른 응답)

**Q: "Why not just call a friend?"**
> Friends aren't always free, and asking someone to call you trains them to expect the favor back. This is zero social debt.

**Q: "Is this deceptive?"**
> The disclaimer is clear in the listing: it simulates a call on your own device only. Same as a fake-call gag app from 2010, but better designed and i18n'd. We explicitly forbid scam/harassment use.

**Q: "Will the call screen look weird on my OEM?"**
> Tested on Samsung One UI, Pixel stock, and Xiaomi MIUI. Side-button mode uses volume keys so it survives lock-screen + Do-Not-Disturb. Bug reports welcome.

### 1-5. 런칭 타이밍

- **화요일 KST 1AM (PST 일요일 5PM)** = 가장 트래픽 많은 슬롯
- 등록 24시간 동안 댓글 활발히 응답 (Product Hunt 랭킹 알고리즘이 댓글 수 중시)

---

## 2. Reddit r/introvert (영어, ~200만명)

### 톤
"I made a thing for our people" — 솔직한 1인 개발자 톤. self-promo rule (대부분 sub 90/10) 준수 위해 본문 위주 + 댓글에서 링크.

### 제목
> I built a fake-call alibi app for introverts who can't say "no" out loud

### 본문 (~800자)

> Hey r/introvert 👋
>
> Solo dev here. I'm the kind of person who gets dragged into a "30-minute meeting" and ends up there for two hours because "I'm okay, thanks" never comes out of my mouth fast enough.
>
> So I built **Emergency Exit** — a one-tap fake-call alibi. You pick who's calling (Mom, your boss, a delivery driver — anyone), pick the timing, and one tap later your phone rings with a realistic incoming-call screen. You answer it like a normal call, do the "oh sorry I have to take this" thing, and walk out.
>
> Three triggers: tap the big red button, shake the phone, or use a discreet side-button combo (works from inside your pocket).
>
> Things I wanted to get right:
> - Looks identical to a real incoming call on Samsung/Pixel/Xiaomi
> - No internet needed (offline = safer, no tracking)
> - No real calls placed — this is a screen simulation only
> - 8 languages, dark mode, lightweight (~10MB)
>
> Things I deliberately did NOT do:
> - No video/rewarded ads (instead: share the app once = permanent +1 caller slot)
> - No fake-call recording (the deception risk is too high)
> - No "I'm in trouble, call me!" partner-app (that's a different product I won't build)
>
> If you want to test it: comment / DM me, I'll drop the Play Store link. Genuinely curious whether the "dark introvert" vibe lands or feels off.
>
> Edit: thanks for the responses, the link is: [Play Store URL]

### 댓글 응답 톤
- "It's not deception, it's social self-defense" 으로 윤리 우려 대응
- "Battery drain?" → "Foreground service runs only during the timer countdown (max 1 minute), then stops"
- "iOS?" → "Android only for now, sorry. Apple's call-screen restrictions make this much harder on iOS."

---

## 3. Reddit r/socialanxiety (영어, ~800K)

### 톤
공감 우선. r/introvert 보다 더 감정적, 더 부드럽게. **자기홍보 느낌이 강하면 ban.** 본문은 도구가 아니라 경험에 초점.

### 제목
> The "I can't take this call right now, sorry" lie I tell to escape — anyone else?

### 본문 (~600자)

> I have a weird coping mechanism. When a work dinner / family thing / meeting starts feeling like too much, I pretend my phone is ringing and walk out to "take the call."
>
> Sometimes it's just me staring at the screen in the bathroom for 5 minutes.
>
> The problem is half the time my phone doesn't actually ring on cue. So a few months ago I built a tiny app for myself that makes a fake-call screen appear on demand — one tap, side button, or phone shake. Looks identical to a real incoming call.
>
> It's been... weirdly helpful. The "I have to take this" excuse is universal and zero-judgment in a way that "I need to leave, sorry" isn't.
>
> Does anyone else do this? Or is it just me? I'm half-curious whether to keep it as a personal weirdness or share it more widely.
>
> (Not trying to plug — happy to DM the app if anyone wants, no big deal either way.)

### 응답 가이드
- DM 요청 시 Play Store 링크 + "no pressure" 톤
- "사용자가 자기 자신을 위해 만든 도구" 포지셔닝 유지

---

## 4. 디시인사이드 직장인 갤러리 (한국, 익명)

### 톤
짤방 + 디시체. "ㅋㅋ" / "ㅇㅇ" / "ㄱㄱ". 솔직, 자조적. 광고 티 나면 즉시 비추 → 노출 ↓.

### 제목 (선택지)
- **A:** 회식 빠져나오는 어플 만들었다 ㅋㅋ
- **B:** "전화왔다 미안" 이거 어플로 만든 사람 있냐?

### 본문 (~400자, A 제목 기준)

> 안녕 ㅋㅋ 1인 개발자임
>
> 회식 자꾸 길어지는데 "저 먼저 가볼게요" 한마디가 안 나옴
> 그래서 가짜 전화 어플 만듬
>
> - 한 번 누르면 진짜 같은 가짜 전화 옴 (삼성 원UI 그대로)
> - 옆사람 봐도 못 알아챔
> - 흔들기 / 음량 버튼으로도 발동 (주머니 속에서도 됨)
> - 인터넷 필요 없음
> - 무료 (구독 안 사도 됨)
>
> 솔직히 내가 쓰려고 만든거고
> 비슷한 사람 있으면 써보셈
>
> 안드로이드만 됨 ㅇㅇ
>
> [Play Store 링크]
>
> 광고 아님. 내가 만든거 자랑하는거임. 후기 받으면 좋고 ㅋ

### 응답
- "광고 아님?" → "1인 개발 사이드 프로젝트라 ㅇㅇ 광고비 1원도 안 씀"
- "iPhone 은?" → "애플이 통화 화면 막아놔서 안드만 가능"
- "수익은?" → "AdMob 배너만. 솔직히 용돈벌이"

---

## 5. 클리앙 모임·새소식 (한국, IT 친화)

### 톤
존댓말 + 정중. 클리앙은 디시보다 톤 점잖음. 1인 개발 응원 분위기.

### 제목
> [1인 개발] 회식·소개팅에서 자연스럽게 빠져나오는 가짜 통화 앱 만들었습니다

### 본문 (~700자)

> 안녕하세요. 사이드 프로젝트로 안드로이드 앱을 만들고 있는 1인 개발자입니다.
>
> 회식이 자꾸 늘어지고 "먼저 일어나겠습니다" 한마디가 안 나오는 분들 계시지 않나요. 저는 그런 사람이라 직접 해결책을 만들었습니다.
>
> **비상탈출 (Emergency Exit) — 가짜 전화 알리바이 앱**
>
> 한 번 누르면 진짜 같은 가짜 통화 수신 화면이 뜹니다. 자연스럽게 받고, 자연스럽게 일어나세요. 그게 다입니다.
>
> 핵심 기능:
> - 호출 대상 자유 설정 (엄마/거래처/배달 등)
> - 발신 타이밍 3가지 (즉시 / 10초 / 커스텀)
> - 호출 방식 3가지 (탭 / 흔들기 / 측면버튼)
> - 인터넷 없어도 작동
> - 다크 모드 자동
> - 7개 언어 (한/영/일/중-간/중-번/스/힌)
>
> 의도적으로 안 만든 것:
> - 보상형 동영상 광고 (대신 앱 공유 1회 = 영구 unlock)
> - 가짜 통화 녹음 기능 (사기 악용 위험)
> - 위치 기반 자동 발동 (개인정보 부담)
>
> 엔터테인먼트/시뮬레이션 용도로만 만들었고 앱 설명에도 명시되어 있습니다. 누군가를 속이는 용도가 아니라, 거절을 잘 못하는 본인의 사회생활 방어 도구로 봐주시면 좋겠습니다.
>
> 후기/버그 제보 환영합니다. 안드로이드만 지원합니다.
>
> [Play Store 링크]
>
> 읽어주셔서 감사합니다.

---

## 6. Indie Hackers — 빌딩 스토리 (영어, ~1500자)

### 제목 후보

- **A (technical):** What I learned shipping a fake-call alibi app to 8 locales as a solo dev
- **B (story):** I built a fake-call app for introverts. Here's what AdMob did to my v1.0 plan.
- **C (counter-narrative):** Why I deleted my app's rewarded video ads (and how a single share now unlocks +1 caller)

### 본문 (B 제목 기준, ~1500자)

> I'm a solo dev. I built **Emergency Exit** — a one-tap fake-call alibi for introverts and people who can't say "no." It's a Play Store-only Android app, in 8 languages, with ~270 lines of dead code that I just deleted last week. Here's what shipped, and what AdMob made me change.
>
> ## The original plan
>
> v1.0 was the simplest possible thing: one screen, one red button, one fake call. AdMob banner + rewarded video for the "free user wants a 2nd caller slot" upgrade prompt. Standard freemium loop.
>
> ## What AdMob made me do
>
> Three things broke that plan:
>
> 1. **Native ad MediaView size** (v1.5.x). I designed a compact 80dp native ad card to match the main UI. AdMob's validator screamed "MediaView too small for video, must be 120dp+." I bumped it. Users complained the ad card was huge. I shrunk it back. Validator screamed again. I added a code-side video filter. Validator still warned because it only looks at the MediaView size, not the runtime content.
>
> 2. **Switching to Banner ad format** (v1.6.7). After the third round of "fix one thing, break another," I just deleted the native ad and switched to a Banner ad. Banner is structurally image-only — the validator can't complain about video that can't be served. UX is identical to the user. Account warning risk: 0.
>
> 3. **Rewarded video had no reward worth giving** (v1.6.6). I removed rewarded video entirely. The reward for watching a 30-second video was "unlock 1 more caller slot." But the value of one extra caller slot to the user was tiny, and the AdMob revenue per impression was also tiny. I replaced it with **share-to-unlock**: share the app once, unlock the slot permanently. The user gets viral PR, I get a permanent backlink-ish loop, and AdMob policy stays clean.
>
> ## What surprised me
>
> - **8-locale i18n was harder than 8x harder.** Each locale needs its own *cultural* hook, not just translation. "회식" (Korean work dinner) has no English equivalent. "合コン" (Japanese group blind date) has no Chinese equivalent. The full description in each language is a fresh write, not a translation.
>
> - **AdMob's "Google Optimization" auto-refresh requires AdView lifecycle hooks.** Without `adView.pause()` / `adView.resume()` on Activity callbacks, the refresh timer breaks across app foreground/background transitions. I missed this for two months. Revenue was probably 30% lower than it should have been.
>
> - **GitHub repo secrets are silent landmines.** I had `ADMOB_NATIVE_ID` and `ADMOB_REWARDED_ID` in repo secrets long after I removed them from code. When I switched to Banner ad, I forgot to add `ADMOB_BANNER_ID`. The release CI would have failed at the next `bundleRelease` task with "ADMOB_BANNER_ID missing." I caught it during a routine audit. Lesson: audit secrets every minor version.
>
> ## What I'd do differently
>
> Pick the simplest ad format from day 1. Native is tempting because it visually integrates, but Banner is a no-policy-drama default. Custom integration = custom policy violations.
>
> ---
>
> Eject Button is at v1.6.9. If you're an Android dev and curious about the dead-code cleanup pass (I deleted a 122-line `NativeAdCard` Composable and a 91-line `GeofenceTransitionReceiver` from a v1.4.0 feature I ripped out a year ago), the repo's private but happy to share the diff via DM.

### Hacker News 조교

Indie Hackers 글 게시 24시간 후 같은 글을 Hacker News 에 "Show HN: I built a fake-call alibi for introverts (Android)" 으로 submit. **Title 에 "Show HN" 필수.**

---

## 7. 등록 후 체크리스트

- [ ] Product Hunt 런칭 24시간 동안 모든 댓글 응답
- [ ] Reddit/디시/클리앙 게시 12시간 응답 모니터링 + DM 회신
- [ ] Indie Hackers 게시 후 Hacker News submit
- [ ] Google Search Console 에서 백링크 트래킹 (`/search/test-mobile-friendly` 이전에 site 등록 필요)
- [ ] 1주일 뒤 Play Console "획득 보고서" 에서 referral 트래픽 확인

## 8. 무엇이 효과 있었는지 측정

- **Play Console → 통계 → 획득** : 신규 트래픽 source 어디서 왔는지 확인
- **AdMob → 보고서** : eCPM/노출 갑자기 오른 날 = 백링크 효과
- **Firebase Analytics** : 신규 사용자 first_open referrer 추적
- **MS Clarity** : 신규 세션 출처 (Play Store referrer 헤더)
