# HANDOFF — 비상탈출 (Eject Button)

> **Android 앱 "비상탈출".** 곤란한 자리에서 빠져나올 수 있게 **가짜 전화**를 걸어주는 앱.
> Play 프로덕션 운영 중 (패키지 `com.simonykim.ejectbutton`, 177개국).
>
> **읽는 법** — 맨 위 `## Latest` 하나만 읽으면 복귀된다. 그 아래 `## 알아둘 것` 은
> 반복해서 다시 알아내게 되는 사실 모음이고, `## 세션 로그` 는 누적 이력이다.
> 세션을 끝낼 때 새 `## Latest` 를 맨 위에 덧붙이고 **직전 블록의 `Latest — ` 접두사를 뗀다**
> (`## Latest` 마커는 항상 정확히 하나).
>
> **역할 구분** — 이 파일은 **기록**(커밋되는 역사)이다.
> `E:/2ndB/_sync/`(gitignore, 로컬 전용)는 **전달**(읽으면 비우는 우편함)이고,
> 세션 간 즉시 메시지는 `_sync/TO-CLI.md` · `_sync/TO-GUI.md` 를 쓴다.

---

## Latest — 2026-09-04 / R8 난독화 23% → 73.76% 해결 + 측정 도구·HANDOFF 신설

### 어디까지 왔나

- **main HEAD**: `18f4c2a` (2026-09-04 03:40 KST)
- **이번 세션 머지된 PR**
  - **#23** `perf(r8): drop blanket -keep rules so R8 can obfuscate and shrink` → `258ecde`
  - **#24** `docs(handoff): add the living HANDOFF record the workflow assumes` → `9af92f7`
  - **#25** `chore(scripts): add MeasureObfuscation so the R8 ratio is reproducible` → `18f4c2a`
- **테스트 상태**: `build-debug` CI 3건 전부 SUCCESS. 로컬 `:app:testReleaseUnitTest` 통과,
  `lintRelease` 0 errors / 72 warnings.
- **working tree**: clean (0 files), origin/main 과 동기.

**릴리스 현황**

| 버전 | 상태 |
|---|---|
| **1091 (1.7.4)** | 프로덕션 **라이브** · 177개국 · 설치 10회 · targetSdk 36 |
| **1092 (1.7.5)** | 09-04 01:09 KST **검토 제출**, 검토 중. 관리형 게시 꺼짐 → 통과 시 **자동 게시**.<br>산출물 `release-build-92`, SHA-256 `a01929a5…c82a9f` |
| **1093 (1.7.6)** | **아직 빌드 안 함.** 난독화 수정분이 여기 실린다 |

**난독화 건 (REQ-260904-01) — 코드 측 완료, 검증 절반 남음**

`proguard-rules.pro` 의 blanket `-keep` 3줄이 원인이었다. 프로덕션 1091 AAB 의
`proguard.map` 을 전수 분석해 kept 클래스 22,973개를 규칙별 귀속한 결과
**`androidx.compose.**` 단독 18,308개 = 전체 클래스의 63.57%**.

| 지표 | before | after |
|---|---:|---:|
| 난독화율(클래스) | 20.22% | **73.76%** |
| 클래스 수 | 28,807 | 11,027 (−61.7%) |
| release APK | 13,408,564 B | **6,861,253 B (−48.8%)** |

셋 다 라이브러리가 consumer 규칙을 자체 동봉해 **중복**이었다. 앱 코드 keep
(`com.ejectbutton.*`)과 Clarity·Billing·AdMob·Firebase·Play Core keep 은 **미변경** —
앱 코드는 전체의 0.20%라 효과 대비 위험만 산다.

### 활성 인프라

- **GitHub**: `Simon-YHKim/eject-button`. 워크플로 `release-aab.yml` · `build-debug.yml` · `pages.yml`.
- **서명**: `eject-button-release.jks` + `keystore.properties` (둘 다 gitignore). CI 는 secret 주입.
- **Firebase**: 프로젝트 `eject-button`. `app/google-services.json` 은 **로컬만 placeholder(711 B)**.
- **Play Console**: 프로덕션 트랙 활성. 인앱 상품 2종 **비활성 상태**(1092 게시 후 재활성화 예정).
- **세션 우편함**: `E:/2ndB/_sync/` — `TO-CLI.md`(콘솔→CLI) · `TO-GUI.md`(CLI→콘솔) · `history/`.
  gitignore 라 이 저장소에 안 들어온다. **세션 시작 시 `TO-CLI.md` 부터 읽고 비운다.**

### 다음 작업 큐

| # | 작업 | 크기 | 권장 |
|---|---|---|---|
| **A** | **1092 검토 결과 확인** → 게시되면 인앱 상품 2종 재활성화 (Simon 확인 후) | small | ⭐ 나머지 전부가 여기에 걸려 있다. 우리가 당길 수 없고 Google 이 메일로 알린다 |
| **B** | **1093 / 1.7.6** dispatch → Simon 이 AAB 업로드 → **난독화 카드 소멸 실측** | medium | A 직후. 이게 REQ-260904-01 의 **진짜 완료조건**. 업로드 전 `MeasureObfuscation` 으로 25% 충족 선판정할 것 |
| **C** | **REQ-260904-03** — Play 권장 조치 3건 콘솔 상세 문구 확보 (GUI 세션 몫) | small | 문구 오면 착수. 지금은 근거가 없어 손대면 안 된다 |
| **D** | REQ-260904-02 — ASC App Privacy 11종 코드 대조 | medium | **이 저장소 아님.** 2nd-B 세션 몫 |

**A 가 오기 전까지 이 저장소에서 할 일은 없다.**

#### C 상세 — 권장 조치 3건은 왜 착수하지 않았나

콘솔 카드 제목만 있고 상세 문구가 없어 코드로 전수 대조했는데 **셋 다 근거가 없었다.**
제목 한 줄로 추측 수정하면 16KB 오판(REQ-260902-09, GUI 가 스스로 철회)을 반복한다.

| 항목 | 실측 결과 |
|---|---|
| 화면 지원 | `aapt2 badging` → `supports-screens: small normal large xlarge` **전부** |
| 기기 제외 | 암시 기능은 `android.hardware.faketouch` 하나뿐. **telephony 제외 없음** |
| 방향 고정 | `screenOrientation`·`setRequestedOrientation`·`resizeableActivity`·`maxAspectRatio` **0건** |
| 지원 중단 디스플레이 API | `getDefaultDisplay`·`getSize`·`getRealMetrics`·`DisplayMetrics` **0건** |
| 오버레이 창 | `FakeCallOverlayService.kt:289` — 가로/세로 **`MATCH_PARENT`**, 고정 픽셀 아님 |
| 비트맵 | 앱 코드에 `BitmapFactory`/`decodeResource` **0건**. 최대 리소스 48 KB, 밀도 버킷 정상 |
| lint | `:app:lintRelease` **0 errors / 72 warnings**, 대화면 관련 지적 **0건** |

⚠ **틀린 가설 1개 기록** — `READ_PHONE_STATE` 가 `android.hardware.telephony` 필수를 암시해
태블릿을 배포에서 뺀 것이라 의심했으나 `aapt2` 로 확인하니 **아니었다**. 현대 aapt2 는
그 권한으로 telephony 를 암시하지 않는다. 같은 길로 다시 가지 말 것.

### 미해결 질문

- **Q-260904-01** — Play 의 "난독화 비율" 산식. 콘솔 표기 **23%** vs 우리 측정 **20.23%**
  (같은 1091 번들, 클래스 기준). Play 는 산식을 공개하지 않아 2.8%p 차이를 재현하지 못했다.
  **막히는 것**: 73.76% 가 Play 기준을 넘는지 **업로드 전에는 확정 불가**.
  다만 기준 25% 의 약 3배라 산식이 달라도 넘을 여유는 크다.
  → **B 작업(1093 업로드) 후 카드 소멸 여부로 자동 해소된다. 별도 조사 불필요.**

### 적용 중인 정책 (영구)

1. **`release-aab.yml` dispatch 는 Simon 지시가 있을 때만.** 1092 검토 중에는 금지 —
   검증된 산출물을 갈아엎지 않기 위함. (2026-09-04 Simon 명시)
2. **main 직접 push 금지. 반드시 PR 경유.**
3. **AAB 업로드·Play 게시는 Simon 손.** CLI 는 빌드까지만.
4. **추측으로 고치지 않는다.** 콘솔 카드 제목만 있고 상세가 없으면 상세를 먼저 요청한다.
5. **후속 빌드 번호는 1093 / 1.7.6** 으로 사전 합의됨.

### 핵심 파일 위치

```
app/proguard-rules.pro                  R8 keep 규칙. blanket keep 금지 — 난독화율 직결
app/build.gradle.kts                    minify/서명/SDK. isMinifyEnabled = true
.github/workflows/release-aab.yml:125   versionCode = 1000 + GITHUB_RUN_NUMBER
scripts/MeasureObfuscation.java         난독화율 측정 + 게이트 (AAB/mapping.txt 둘 다)
scripts/CheckAndroidBillingVersion.java 바이너리에서 billing/version 검증 (CI 에서 호출)
CHANGELOG.md                            v1.7.6 후보 항목이 맨 위
E:/2ndB/_sync/TO-CLI.md                 세션 시작 시 먼저 읽고 비울 것 (repo 밖)
```

### 검증

```bash
# CI 가 실제로 돌리는 것
./gradlew :app:testDebugUnitTest --no-daemon

# 난독화 게이트 — 1093 AAB 를 올리기 전에 반드시
java scripts/MeasureObfuscation.java --self-test
java scripts/MeasureObfuscation.java --minimum-class-ratio 25 <릴리스>.aab
echo "exit=$?"   # 0 이어야 통과. 파이프로 보면 안 된다 (뒤 명령의 종료코드가 잡힌다)
```

### 다음 세션 시작하는 법

```bash
git fetch origin main && git pull origin main
cat docs/HANDOFF.md
cat E:/2ndB/_sync/TO-CLI.md    # 우편함부터. 읽었으면 history/ 로 옮기고 비운다
# A 작업(1092 검토 결과)이 왔는지부터 확인
```

---

## 알아둘 것 (반복 질문)

- **versionCode 는 지정할 수 없다.** `release-aab.yml:125` 이 `1000 + GITHUB_RUN_NUMBER` 로
  계산한다. dispatch 입력은 `version_name` **하나뿐**이다. "1091 뽑아줘" 같은 발주는
  성립하지 않는다 — 다음 실행이 몇 번인지로 결정된다.
- **`release-aab.yml` 은 main push 로 발화하지 않는다.** `v*` 태그 push 와 수동 dispatch 만.
  main 에 머지해도 릴리스 빌드가 자동으로 돌지 않는다. (이번 세션 머지 3건으로 실측 확인)
- **`build-debug.yml` 은 `claude/**` · `main` · `Eject_Button_app` push 에 발화**한다.
  PR 트리거는 없다. 그래서 CI 신호를 받으려면 브랜치 이름을 `claude/...` 로 판다.
  단, 디버그 빌드는 **R8 을 돌리지 않으므로** 난독화·shrink 검증은 로컬 release 빌드로만 가능하다.
- **로컬 `google-services.json` 은 placeholder(711 B)** 다. 로컬 release 빌드를 기기에서 돌리면
  Firebase Installations / Crashlytics settings 오류가 뜨는데 **정상**이다. 프로덕션 APK 를
  같은 기기에 깔아도 같은 로그가 나온다 (2026-09-04 대조 확인).
- **난독화 비율을 재는 법** — 새로 빌드할 필요 없다. AAB 안에
  `BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map` 이 들어 있어
  **이미 게시된 번들도 사후 감사**할 수 있다. 도구는 `scripts/MeasureObfuscation.java`.
- **Windows Git Bash 에서 `TZ=Asia/Seoul date` 는 조용히 UTC 를 뱉는다.** zoneinfo DB 가 없어서다.
  `TZ=KST-9 date` 를 쓴다. (`_sync/README.md` 규칙 ④ 보충에도 같은 내용)

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
  오버레이 권한 인텐트까지 정상, R8 계열 예외 **0건**. 프로덕션 1091 APK 를 같은
  에뮬레이터에 깔아 대조군으로 삼아 잔여 로그가 환경 문제임을 확인.
- 산출: PR #23 → `258ecde` · PR #24(HANDOFF 신설) → `9af92f7` ·
  PR #25(`MeasureObfuscation.java`) → `18f4c2a`.
  Java 구현과 세션 중 쓴 Python 구현이 **같은 수치**를 냈다(20.23% / 73.76%, 패키지 귀속까지 일치).
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
