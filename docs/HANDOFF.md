# HANDOFF — 비상탈출 (Eject Button)

> **2층 구조.** 이 위쪽 "고정 블록"은 **덮어쓴다**(항상 현재 상태 1개).
> 아래 "세션 로그"는 **맨 위에 덧붙인다**(prepend, 지우지 않는다).
> 세션을 시작하면 먼저 읽고, 끝낼 때 갱신·커밋한다 (`docs(handoff): …`).
>
> 역할 구분 — 이 파일은 **기록**(커밋되는 역사)이고,
> `E:/2ndB/_sync/`(gitignore, 로컬 전용)는 **전달**(읽으면 비우는 우편함)이다.
> 세션 간 즉시 메시지는 `_sync/TO-CLI.md` · `_sync/TO-GUI.md` 를 쓴다.

---

## 고정 블록

### 목적
Android 앱 "비상탈출". 곤란한 자리에서 빠져나올 수 있게 **가짜 전화**를 걸어주는 앱.
Play 프로덕션 운영 중(패키지 `com.simonykim.ejectbutton`, 177개국).

### 최종 갱신
**2026-09-04 03:25 KST · Claude Code (eject-button 세션)**

### 현재 상태

**지금까지**
- **1091 (1.7.4)** — 프로덕션 **라이브**. targetSdk 36 상향분. 설치 10회.
- **1092 (1.7.5)** — 09-04 01:09 KST **프로덕션 검토 제출**, 검토 중.
  `restoreGeneration` 분리 + `isBillingReady` 복구. 관리형 게시 꺼짐 → 통과 시 **자동 게시**.
  산출물: GitHub Release `release-build-92` (SHA-256 `a01929a5…c82a9f`).
- **난독화 23% 건 (REQ-260904-01)** — 코드 측 **완료**. PR #23 → main `258ecde`.
  `-keep` blanket 3줄 제거로 **20.22% → 73.76%**, release APK **−48.8%**.

**다음 1개**
👉 **1092 검토 결과 확인.** 게시되면 → 인앱 상품 2종 재활성화(Simon 확인 후)
→ `release-aab.yml` **1093 / 1.7.6** dispatch → Simon 이 AAB 업로드
→ Play 에서 **난독화 카드 소멸 실측** (= REQ-260904-01 최종 완료조건).

**막힌 것**
- 1092 Google 검토 대기 — 우리가 당길 수 없음.
- 그동안 **`release-aab.yml` dispatch 금지** (Simon 지시). 1092 를 갈아엎지 않기 위함.

### TODO
- [ ] 1092 게시 확인 → 상품 2종 재활성화
- [ ] 1093 / 1.7.6 빌드 → 업로드 → 난독화 카드 소멸 확인
- [ ] Play "권장 조치" 3건 — **콘솔 상세 문구 미확보**. 아래 REQ-260904-03 참조.

### 미해결 질문
- **Q-260904-01** — Play 의 "난독화 비율" 산식. 콘솔 표기 **23%** vs 우리 측정 **20.23%**
  (클래스 기준, 프로덕션 1091 AAB 의 `proguard.map` 실측). 2.8%p 차이의 원인 불명이고
  Play 는 산식을 공개하지 않는다. **막히는 것**: 73.76% 가 기준을 넘는지 **업로드 전에는
  확정 불가**. 다만 기준 25% 의 약 3배라 산식이 달라도 넘을 여유는 크다.
  → 1093 업로드 후 카드 소멸 여부로 자동 해소된다. 별도 조사 불필요.

### 요청

#### REQ-260904-03 → GUI(Play Console) 세션 · Play 권장 조치 3건 상세 문구 확보

- **목적** — 권장 조치 3건을 코드로 대조했지만 **셋 다 근거를 못 찾았다.**
  제목 한 줄만으로는 무엇을 고쳐야 하는지 특정이 안 된다. 상세 문구가 있어야 착수한다.
  안 하면: 추측으로 코드를 고치게 되고, 16KB 오판(REQ-260902-09)을 반복한다.
- **화면** — Play Console → 비상탈출 → **출시 개요 / 앱 최적화** (난독화 카드와 같은 패널).
  ⚠ 미확인 — 이 세션은 콘솔 접근 권한이 없어 캡쳐를 뜨지 못했다.
- **행동** — 아래 3개 카드를 **각각 펼쳐** 상세 문구와 "영향받는 항목"을 그대로 복사한다.
  1. 일부 사용자에게는 더 넓은 화면이 표시되지 않을 수 있습니다 (사용자 환경)
  2. 앱에서 더 넓은 화면용으로 지원 중단된 API 또는 파라미터를 사용합니다 (사용자 환경)
  3. 비트맵 다운샘플링으로 앱 성능을 개선하세요 (메모리 사용량)
- **원클릭** — 붙여넣을 자리: `E:/2ndB/_sync/TO-CLI.md` 에 append.
- **성공 신호** — 각 카드마다 **본문 2줄 이상 + 지목된 API·리소스 이름**이 확보되면 성공.
  펼쳐도 제목만 나오고 본문이 없으면 "본문 없음"이라고 그대로 적어 보낼 것.

**CLI 가 이미 확인한 것 (중복 조사 방지)** — 아래는 전부 실측이고 **문제 없음**:

| 항목 | 실측 결과 |
|---|---|
| 화면 지원 | `aapt2 badging` → `supports-screens: small normal large xlarge` **전부** |
| 기기 제외 | 암시 기능은 `android.hardware.faketouch` 하나뿐. **telephony 제외 없음** |
| 방향 고정 | `screenOrientation` · `setRequestedOrientation` · `resizeableActivity` · `maxAspectRatio` **전부 0건** |
| 지원 중단 디스플레이 API | `getDefaultDisplay` · `getSize` · `getRealMetrics` · `DisplayMetrics` **전부 0건** |
| 오버레이 창 | `FakeCallOverlayService.kt:289` — 가로/세로 **`MATCH_PARENT`**, 고정 픽셀 아님 |
| 비트맵 | 앱 코드에 `BitmapFactory`/`decodeResource` **0건**. 최대 리소스 48 KB, 밀도 버킷 정상 |
| lint | `:app:lintRelease` **0 errors / 72 warnings**, 대화면 관련 지적 **0건** |

---

## 알아둘 것 (반복 질문)

- **versionCode 는 지정할 수 없다.** `release-aab.yml:125` 이 `1000 + GITHUB_RUN_NUMBER` 로
  계산한다. dispatch 입력은 `version_name` **하나뿐**이다. "1091 뽑아줘" 같은 발주는
  성립하지 않는다 — 다음 실행이 몇 번인지로 결정된다.
- **`release-aab.yml` 은 main push 로 발화하지 않는다.** `v*` 태그 push 와 수동 dispatch 만.
  main 에 머지해도 릴리스 빌드가 자동으로 돌지 않는다.
- **`build-debug.yml` 은 `claude/**` · `main` · `Eject_Button_app` push 에 발화**한다.
  PR 트리거는 없다. 그래서 CI 신호를 받으려면 브랜치 이름을 `claude/...` 로 판다.
  단, 디버그 빌드는 **R8 을 돌리지 않으므로** 난독화·shrink 관련 검증은 로컬 release 빌드로만 가능하다.
- **로컬 `google-services.json` 은 placeholder(711 B)** 다. 로컬 release 빌드를 기기에서 돌리면
  Firebase Installations / Crashlytics settings 오류가 뜨는데 **정상**이다. 프로덕션 APK 를
  같은 기기에 깔아도 같은 로그가 나온다(09-04 대조 확인).
- **난독화 비율을 재는 법** — 새로 빌드할 필요 없다. AAB 안에
  `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map` 이 들어 있다.
  클래스 줄(`orig -> new:`)에서 `new != orig` 비율을 세면 된다.

---

## 세션 로그

### 2026-09-04 · Claude Code · 난독화 23% 해결 (REQ-260904-01)
- 진단: 프로덕션 1091 AAB 의 `proguard.map` 전수 분석(SHA-256 릴리스 API digest 대조).
  kept 클래스 22,973개를 `-keep` 규칙별 귀속 → **`androidx.compose.**` 단독 18,308개(63.57%)**.
- 조치: blanket keep 3줄 제거(compose / kotlin / coroutines). 라이브러리가 consumer 규칙을
  자체 동봉해 중복이었음. 앱 코드·Clarity·Billing·AdMob·Firebase·Play Core keep 은 **미변경**.
- 결과(로컬 A/B, 동일 툴체인): 난독화 **20.22% → 73.76%** · 클래스 28,807 → 11,027 ·
  **APK 13,408,564 B → 6,861,253 B (−48.8%)**.
- 검증: 유닛테스트·lintVital·CI 통과. 에뮬레이터(API 36) 실행하여 온보딩 렌더 →
  오버레이 권한 인텐트까지 정상, R8 계열 예외 **0건**. 프로덕션 1091 APK 대조군으로
  잔여 로그가 환경 문제임을 확인.
- 산출: PR #23 → `258ecde`.
- 부수: 이 세션 시작 시 받은 발주(patch `git am` → PR → 머지 → 1.7.4 dispatch)는
  **08-31 에 이미 전량 집행된 건**이라 재실행하지 않았다. 재실행했다면 1091 이 아니라
  1093 이 나오고 1.7.4 버전명이 중복됐을 것이다.

### 2026-08-31 · v1.7.5 (1092)
`restoreGeneration` 분리 + `isBillingReady` 복구. PR #20 → release-aab #92 성공.

### 2026-08-31 · v1.7.4 (1091)
targetSdk 36 상향(Play 정책 경고 해소). PR #19 → release-aab #91 성공 → 프로덕션 게시.

### 2026-08-29 · v1.7.3
Play Billing 9.1.0 이전. PR #18.

> 그 이전 이력은 `CHANGELOG.md` 와 `docs/handoffs/HANDOFF_v*.md`(v1.5.0 까지) 참조.
