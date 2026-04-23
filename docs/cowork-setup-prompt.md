# Claude Cowork 세션 프롬프트 — v1.0.3+ 릴리스 사전 준비

아래 블록 전체를 복사해서 Claude Cowork (또는 별도 Claude Code 세션) 에
붙여넣으면, CI/CD 설정 + Play Console 등록 준비가 자동화됩니다.

대상 레포: `https://github.com/Simon-YHKim/eject-button`

---

## 📋 프롬프트 (복사해서 붙여넣기)

```
너는 지금 Eject Button Android 앱의 릴리스 준비 담당이야. v1.0.3 이상이
Play Store 에 정상 제출되려면 아래 3가지 범주의 작업을 순서대로 완료해야 해.
필요하면 나한테 값을 물어보고, 자동화 가능한 건 커맨드로 실행해.

작업은 섹션 A → B → C 순서. 각 섹션 안에서는 네가 알아서 판단해.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SECTION A — GitHub Actions Secrets 설정
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

현재 `.github/workflows/release-aab.yml` 은 release 빌드 전에
`secrets.properties` 를 아래 3개 값으로 작성하는 단계가 있어. 이 값이
없으면 v1.0.3+ 릴리스 CI 는 명시적 에러로 실패해 (이건 의도된 fail-fast 야).

필요한 repo secrets:
  1. ADMOB_NATIVE_ID       — 실제 AdMob 네이티브 광고 unit ID
                              (형식: ca-app-pub-3230287048532628/XXXXXXXXXX)
  2. ADMOB_INTERSTITIAL_ID — 실제 AdMob 전면 광고 unit ID
  3. CLARITY_PROJECT_ID    — Microsoft Clarity 프로젝트 ID

더불어 이미 설정되어 있어야 하는 키스토어 관련 시크릿:
  4. RELEASE_KEYSTORE_BASE64 — keystore (jks) base64 인코딩
  5. RELEASE_KEYSTORE_PASSWORD
  6. RELEASE_KEY_ALIAS
  7. RELEASE_KEY_PASSWORD

해야 할 일:

1) gh CLI 가 설치되어 있는지 확인 (`gh --version`).
   - 없으면: https://github.com/cli/cli#installation 에서 설치 가이드 제공하고
     OS 별 명령어 안내 (Windows: `winget install GitHub.cli`)
   - gh auth 되어 있는지 확인 (`gh auth status`). 미인증이면 `gh auth login`.

2) 현재 Simon-YHKim/eject-button 레포에 설정된 시크릿 목록 확인:
     gh secret list -R Simon-YHKim/eject-button

3) 없는 시크릿이 있다면 나한테 값을 물어본 뒤 아래처럼 설정:
     gh secret set ADMOB_NATIVE_ID       -R Simon-YHKim/eject-button --body "ca-app-pub-..."
     gh secret set ADMOB_INTERSTITIAL_ID -R Simon-YHKim/eject-button --body "ca-app-pub-..."
     gh secret set CLARITY_PROJECT_ID    -R Simon-YHKim/eject-button --body "XXXXXXXX"

4) AdMob 실제 unit ID 발급 방법을 모른다고 하면 이렇게 안내:
   - https://admob.google.com → 앱 `com.simonykim.ejectbutton` 선택
   - "광고 단위" 탭에서 네이티브 / 전면 광고 각각 "광고 단위 추가"
   - 생성되면 ID 를 바로 Secret 에 등록

5) Clarity 프로젝트 ID 발급:
   - https://clarity.microsoft.com → "New project" → Android SDK 선택
   - 생성 후 Settings → Overview → Project ID 복사 → Secret 에 등록

6) 모든 시크릿이 갖춰지면 확인 빌드 트리거:
     gh workflow run "Build Release AAB + APK" -R Simon-YHKim/eject-button
     gh run watch -R Simon-YHKim/eject-button

   빌드가 성공하면 https://github.com/Simon-YHKim/eject-button/releases 에서
   signed APK + AAB 확인 가능.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SECTION B — Play Console 등록 체크리스트
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Play Console 은 사람이 직접 웹에서 작업해야 해. 나한테 스크린샷 요청 없이,
아래 체크리스트를 순서대로 안내만 해주면 돼. 각 항목마다 "완료했어?" 로
확인받아 진행.

체크리스트 (https://play.google.com/console 에서):

□ B1. Internal testing 트랙에 AAB 업로드
     - App bundle: v1.0.3 AAB (GitHub Release 에서 다운로드)
     - Release name: 자동 (v1.0.3)
     - Release notes: fastlane/metadata/android/en-US/changelogs/default.txt 내용 복사

□ B2. App content → Data Safety 섹션
     다음 카테고리를 모두 "Collected" 로 선언:
     - Device or other IDs (AAID/GAID)  → AdMob, Firebase Analytics
     - App interactions                 → Firebase Analytics, Microsoft Clarity
     - Crash logs / diagnostics          → Firebase
     - Purchase history                 → Play Billing
     공유 받는 제3자: Google, Microsoft. 사용 목적: 광고 / 분석 / 앱 기능 / 구매 처리.

□ B3. App content → Ads → "Contains ads" 체크 (YES)

□ B4. App content → Target audience → Age 13+

□ B5. App content → Privacy policy URL 입력:
     https://eject-button.hwanydanh.workers.dev/privacy-policy

□ B6. App content → Sensitive permissions declarations
     각각 justification 작성:
     - SYSTEM_ALERT_WINDOW: "Display fake incoming-call screen over other apps — app's core function"
     - REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: "Scheduled fake-call delivery during Doze mode"
     - READ_PHONE_STATE: "Detect active real calls and suppress fake call overlay to avoid interference"
     - READ_CONTACTS: "Optional contact picker for custom caller name (local-only, never uploaded)"

□ B7. Advanced settings → Foreground services
     3가지 subtype justification:
     - fake_call_simulation:
       "Maintains fake-call UI + ringtone + vibration while screen is off. The
        call duration is bounded by user interaction — dismiss ends the service."
     - shake_gesture_pattern_detection:
       "Listens to accelerometer for a user-configured shake pattern to trigger
        a pre-scheduled fake call while app is backgrounded."
     - hardware_button_pattern_detection:
       "Listens to volume-button sequence for user-defined trigger pattern; uses
        MediaSession to route the key events while screen is off."

□ B8. Store listing
     - Title: Eject Button — Escape Call
     - Short description: (fastlane/metadata/android/en-US/short_description.txt)
     - Full description: fastlane/metadata/android/en-US/full_description.txt
     - Screenshots: 최소 2장, 권장 4-8장 (TODO: store-assets/ 확인)
     - Feature graphic: 1024×500 PNG
     - Icon: 512×512 PNG
     (ko/ja 각 언어별 Store Listing 동일 필드 업로드 — fastlane/metadata 참고)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
SECTION C — 프라이버시 정책 페이지 재배포
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

라이브 페이지 (https://eject-button.hwanydanh.workers.dev/privacy-policy) 는
현재 소스 파일 `docs/privacy-policy.html` 보다 오래된 버전이야. 라이브 페이지는
Firebase Analytics / Camera (플래시) / Battery Optimization 섹션이 없음.

소스 파일은 방금 업데이트돼서 3개 섹션이 다 들어있어. 라이브에 반영하려면:

1) Cloudflare Workers 프로젝트가 어디 있는지 확인 (사용자에게 물어봐).
   - 현재 레포에 wrangler.toml / workers/ 폴더 없음
   - 아마 별도 레포이거나 Cloudflare Dashboard 에서 직접 편집

2) 두 가지 선택:
   옵션 A) Worker 편집 권한이 있으면 최신 HTML 내용으로 덮어쓰기
           (docs/privacy-policy.html 전체 복사)
   옵션 B) Worker 접근 불가면, docs/ 를 GitHub Pages 로 배포 전환 후
           매니페스트/SettingsScreen/store listing 3곳 URL 교체:
             .github/workflows/pages.yml (현재 workflow_dispatch-only 로
             disabled 상태, `on: push: branches: [Eject_Button_app]` 로 활성화)

3) 옵션 A 채택 시, 라이브 반영 후 실제 URL 에서 확인:
     curl -s https://eject-button.hwanydanh.workers.dev/privacy-policy \
       | grep -E "Camera|Battery|Foreground Service|Firebase"
   4 키워드 모두 hit 해야 통과.

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
완료 기준
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
- Section A: `gh workflow run` 결과 빌드 성공, Release 에 AAB+APK 첨부됨
- Section B: Play Console 내 "App content" 섹션이 모두 녹색 체크
- Section C: 라이브 privacy policy 에 Camera/Battery/Foreground/Firebase 4개
  섹션 모두 존재

각 섹션을 마칠 때마다 한 줄 리포트해줘. 블로킹 이슈 있으면 그 시점에 나한테
에스컬레이션.
```

---

## 💡 사용 팁

- **가장 쉬운 경로**: Claude Code 새 세션을 열고 위 프롬프트 전체 복사 → 붙여넣기.
  이 세션과 별개로 돌기 때문에 동시 병렬 진행이 가능합니다.
- **gh CLI 인증**: Cowork 세션에서 처음 돌릴 때 `gh auth login` 실행 필요할 수
  있음. Device flow 사용 (브라우저 인증).
- **값이 민감**: AdMob unit ID, Clarity project ID 는 세션 기록에 남으니,
  세션 종료 후 채팅 로그가 외부로 나가지 않도록 주의.
- **Play Console 자동화 한계**: Play Console UI 는 Google 정책상 자동화 API 가
  없어 사람이 직접 클릭해야 함. Cowork 세션은 순서·지침만 제공하고 너는
  브라우저에서 수행.

## 🔗 관련 문서

- `SETUP_GUIDE.md` — 로컬 개발 환경 셋업
- `docs/play-policy-declarations.md` — Play Console declaration 원본 한국어 메모
- `docs/play-data-safety.md` — Data Safety form 메모
- `.github/workflows/release-aab.yml` — CI 릴리스 워크플로우
- `docs/privacy-policy.html` — 프라이버시 정책 소스 (EN + KO)
