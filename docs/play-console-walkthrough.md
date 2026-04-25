# Play Console 등록 단계별 워크스루 (Eject Button)

> 작성일: 2026-04-26
> Play Console에 로그인된 상태에서 시작. 모든 입력값을 복붙용으로 미리 준비함.
> 단계마다 ✅ 체크하면서 진행.

---

## 🟢 0단계 — Create app 클릭

좌측 메뉴 → **All apps** → 우상단 **Create app** 버튼

| 필드 | 입력값 |
|---|---|
| App name | `Eject Button — Escape Call` |
| Default language | `Korean — ko-KR` |
| App or game | **App** |
| Free or paid | **Free** |
| Declarations: Developer Program Policies 준수 | ✅ 체크 |
| Declarations: US export laws 준수 | ✅ 체크 |

→ **Create app** 버튼 클릭. 좌측에 새 앱이 나타남.

---

## 🟢 1단계 — 좌측 사이드바 "Set up your app" 섹션 (필수 9개)

각 항목 옆에 빨간 점이 있고, 모두 초록 체크로 바뀌어야 Production 출시 가능.

### 1-1. App access (앱 액세스)

→ "All functionality is available without any special access" 선택

### 1-2. Ads (광고)

→ "Yes, my app contains ads" 선택

### 1-3. Content rating (콘텐츠 등급)

→ Start questionnaire 클릭
- Email: `simonkim250405@gmail.com`
- App category: **Reference, News, or Educational** (또는 Utility — 둘 다 OK)
- 모든 질문에 **No** 선택 (자세한 답변은 `docs/play-console-questionnaire.md` § 1-4 참고)
- Submit → IARC 등급 자동 발급 (예상 Everyone / PEGI 3)

### 1-4. Target audience (타겟 연령)

| 화면 | 답변 |
|---|---|
| Target age | **18 and over** 만 체크 |
| Appeals to children | **No** |
| Targeted to children but appeals to others | **No** |

### 1-5. News app (뉴스 앱)

→ "No" 선택

### 1-6. COVID-19 contact tracing & status apps

→ "My app is not a publicly available COVID-19 contact tracing or status app" 선택

### 1-7. Data safety

→ Start 클릭. 답변은 `docs/play-data-safety.md` 그대로 입력.
요약:
- Data collection: **Yes** (Firebase, AdMob, Clarity)
- Personal info, Financial, Health, Messages, Photos, Calendar, Contacts, Location, Web history → **모두 No**
- App activity → **Yes** (App interactions)
- App info & performance → **Yes** (Crash logs, Diagnostics)
- Device or other IDs → **Yes**
- Encryption in transit: **Yes**
- User can request deletion: **Yes**

### 1-8. Government apps

→ "No" 선택

### 1-9. Financial features

→ "My app doesn't provide any financial features" 선택

→ 9개 항목 모두 ✅ 표시되면 1단계 완료.

---

## 🟢 2단계 — Store presence (스토어 등록정보)

좌측 사이드바 → **Grow users** → **Store presence** → **Main store listing**

### 2-1. App details

| 필드 | 값 (복붙) |
|---|---|
| App name | `Eject Button — Escape Call` |
| Short description | `회식·미팅·데이트에서 탈출할 가짜 전화. 흔들기·타이머·즉시 발신.` |
| Full description | `fastlane/metadata/android/ko-KR/full_description.txt` 파일 내용 그대로 |

### 2-2. Graphic assets

| 자산 | 파일 위치 |
|---|---|
| App icon (512×512) | `store-assets/play-store-icon-512.png` |
| Feature graphic (1024×500) | `store-assets/play-store-feature-graphic-1024x500.png` |
| Phone screenshots | **아래 6장 순서대로 업로드** |

**스크린샷 업로드 순서** (왼쪽부터):
1. 메인 화면 (EJECT 빨간 버튼)
2. 수신 전화 (엄마 + 번호 + 받기/거절)
3. 통화 중 (00:02 + 컨트롤 버튼)
4. 흔들기 모드 활성화 (빨간 X 취소 버튼)
5. 언어 선택 (7개 언어 리스트)
6. HISTORY 탭

> ⚠️ "발신자 추가" 다이얼로그 스크린샷은 빈 박스 때문에 미사용.

### 2-3. Categorization

| 필드 | 값 |
|---|---|
| App category | **Tools** |
| Tags (선택) | Productivity, Utility |
| Contact details — Email | `simonkim250405@gmail.com` |
| Contact details — Phone | (선택, 본인 번호) |
| Contact details — Website | `https://eject-button.hwanydanh.workers.dev/` |
| Privacy Policy | `https://eject-button.hwanydanh.workers.dev/privacy-policy` |

→ **Save** 클릭.

---

## 🟢 3단계 — 다국어 store listing 추가 (선택, 출시 임팩트 ↑)

**Main store listing 페이지 → 우상단 "Manage translations" → Add your own translations**

추가할 언어 (이미 fastlane에 작성됨):
- English (United States) — `en-US`
- Japanese — `ja-JP`
- Chinese (Simplified) — `zh-CN`
- Spanish (Spain) — `es-ES`
- Hindi — `hi-IN`

각 언어마다 title / short / full description 입력 (`fastlane/metadata/android/{lang}/` 폴더 파일 내용 그대로 복붙).

스크린샷은 **모든 언어가 한국어 스크린샷을 자동 폴백 사용**하므로 별도 업로드 불필요. (영어 스크린샷을 별도로 찍은 후 추가하면 더 좋음 — 출시 후 업데이트로 진행 가능)

---

## 🟢 4단계 — Subscription 등록 (월 구독, ₩1,900 / $1.99)

⚠️ **중요**: Round 12에 일회성 → **월 구독**으로 전환됨. 반드시 Subscriptions 메뉴로 등록.

좌측 → **Monetize** → **Products** → **Subscriptions** → **Create subscription**

| 필드 | 값 |
|---|---|
| Product ID | `eject_premium_monthly` |
| Name | `Eject Premium (Monthly)` |
| Description | `광고 제거 + 통화 중 스크립트 힌트 무제한 — 월 구독` |
| Benefits | `광고 없음`, `통화 스크립트 힌트 무제한` |

**Base plan 추가**:
| 필드 | 값 |
|---|---|
| Base plan ID | `monthly` |
| Billing period | **1 month** (Auto-renewing) |
| Price | `$1.99` (자동으로 한국 ₩1,900 / 일본 ¥250 등 변환) |
| Free trial (선택) | 7일 권장 — Play Console에서 "Add offer" → Free trial |

→ **Save** → **Activate** (subscription + base plan 모두 활성화 필수)

> 🚨 코드는 SUBS ProductType으로만 조회합니다. In-app products로 등록하면 `queryProductDetailsAsync` 결과가 빈 배열 → 결제 버튼 무반응.

---

## 🟢 5단계 — Internal testing 트랙에 AAB 업로드

좌측 → **Test and release** → **Testing** → **Internal testing** → **Create new release**

### 5-1. App signing
첫 release 업로드 시 자동으로 **Play App Signing**이 활성화됩니다. 그대로 진행.

### 5-2. AAB 업로드

**다운로드 위치**: https://github.com/Simon-YHKim/eject-button/releases (가장 최신 release)
파일명: `eject-button-release-runXX-XXXXXXX.aab` (run 번호가 가장 큰 것)

→ **Upload** 버튼 → AAB 파일 선택 → 약 2분 처리

### 5-3. Release details

| 필드 | 값 |
|---|---|
| Release name | `1.0.8 (run XX)` (자동으로 입력됨) |
| Release notes — `<ko-KR>` | `fastlane/metadata/android/ko-KR/changelogs/default.txt` 내용 |
| Release notes — `<en-US>` | `fastlane/metadata/android/en-US/changelogs/default.txt` 내용 |
| Release notes — `<ja-JP>` | `fastlane/metadata/android/ja-JP/changelogs/default.txt` 내용 |

→ **Save** → **Review release** → **Start rollout to Internal testing**

### 5-4. Internal tester 추가

Internal testing → **Testers** 탭 → **Create email list** → 본인 Gmail 주소 추가
→ Opt-in URL이 생성됨. 그 URL을 모바일에서 열어 테스터로 등록.

→ 약 5–10분 후 Play Store에서 앱이 보임 (테스터 한정).

---

## 🟢 6단계 — Production 출시 (테스트 후)

Internal testing에서 며칠 동안 dogfooding한 뒤:

좌측 → **Test and release** → **Production** → **Create new release**

→ **Use existing release from another track** 옵션으로 Internal testing의 AAB 그대로 promote 가능.

### 단계적 출시 권장
- 첫날: 10%
- 3일 후 Vitals 확인 (크래시율 < 1%, ANR < 0.5%) → 50%
- 7일 후 → 100%

### 출시 국가
PLAY_STORE_ASO.md 권장:
- 1차: 🇰🇷 KR, 🇯🇵 JP
- 2차: 🇺🇸 US, 🇬🇧 UK, 🇨🇦 CA, 🇦🇺 AU
- 3차: 🇪🇸 ES, 🇲🇽 MX, 🇮🇳 IN, 🇸🇬 SG

---

## ⏰ 예상 소요 시간

| 단계 | 소요 |
|---|---|
| 0-1단계 (앱 만들기 + 9개 설문) | 30–45분 |
| 2단계 (한국어 store listing + 그래픽) | 20분 |
| 3단계 (다국어 5개 추가) | 30분 |
| 4단계 (인앱 상품) | 5분 |
| 5단계 (Internal testing AAB 업로드) | 10분 + 처리 대기 30분 |
| Google 자동 검토 (Internal track) | 즉시 ~ 수시간 |
| 6단계 Production 신청 → 첫 검토 | 1–7일 (보통 24시간 이내) |

**합계**: 본인 작업 약 2시간 + Google 검토 대기.

---

## 🚨 거절 자주 받는 항목 미리 대비

1. **Privacy policy URL이 404** → 푸시 후 Cloudflare 자동배포 정상 작동 확인 (https://eject-button.hwanydanh.workers.dev/privacy-policy 접속해서 내용 보이는지)
2. **Data safety form 불일치** → 광고 ID 사용을 "No"로 표시했는데 AdMob 사용 → "Yes"로 정정
3. **권한 사유 누락** → SYSTEM_ALERT_WINDOW은 manifest에 있는 것만으로 자동 review queue. `docs/play-policy-declarations.md`의 사유를 그대로 입력하면 통과율 ↑
4. **콘텐츠 등급과 실제 콘텐츠 불일치** → 솔직하게 답변하면 됨. AdMob 광고가 13+ 콘텐츠 송출 가능성 있다고 표시.
5. **테스트 가짜 광고 ID 노출** → CI 빌드는 secrets.properties로 실제 ID 주입되므로 문제 없음. (build.gradle.kts가 release에 fail-fast)

---

## 📞 막혔을 때

각 단계마다 화면 캡처해서 채팅에 올려주시면 즉시 진단해드립니다.
