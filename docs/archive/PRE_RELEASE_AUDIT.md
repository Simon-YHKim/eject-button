# Eject Button — Play Store 출시 전 점검 리포트

**점검일**: 2026-04-25
**점검 대상**: `Simon-YHKim/eject-button` (브랜치 `Eject_Button_app`, 최신 커밋 `bda4afa` v1.0.8)
**점검자**: Claude (Cowork mode)

---

## 종합 등급: 🟡 **출시 준비 85% 완료** — 실행 차단 이슈 2건 + 마감 작업 3건

핵심 코드, 빌드 파이프라인, 정책 문서는 이미 매우 잘 만들어져 있습니다.
다만 **Play Console에 실제로 업로드하기 전에 반드시 처리해야 할 항목**이 있어 아래에 우선순위로 정리했습니다.

---

## 🔴 출시 차단 (Blockers) — 반드시 해결

### B1. 저장소 루트의 `app-release.aab`는 구버전이며 업로드 불가

| 항목 | 현재 코드 (build.gradle.kts) | `app-release.aab` 내부 (실제) |
|---|---|---|
| applicationId | `com.simonykim.ejectbutton` | `com.ejectbutton.app` ❌ |
| versionCode | CI에서 1000+RUN_NUMBER | `1` ❌ |
| versionName | CI에서 주입 (현재 1.0.8) | `1.0` ❌ |
| compileSdk | `35` | `34` ❌ |

**원인**: 루트의 AAB는 초기 라운드에서 만든 스냅샷으로 보임. 현재 v1.0.8 코드와 패키지명·버전·SDK가 모두 다름.

**조치**:
- Play Console에 이 파일을 업로드하면 패키지명 불일치로 거절됩니다.
- 반드시 GitHub Actions `release-aab.yml` 워크플로우를 새로 트리거해서 **현재 코드 기반의 production AAB**를 만들어야 합니다.
- 옵션 A (권장): `git tag v1.0.8 && git push origin v1.0.8`
- 옵션 B: GitHub → Actions → "Build Release AAB + APK" → Run workflow (수동 트리거)

### B2. Play Store 스크린샷 없음

- Play Console은 **휴대폰 스크린샷 최소 2장 필수** (권장 4–8장, 1080×1920 이상).
- 현재 `store-assets/`에는 앱 아이콘(512)과 피처 그래픽(1024×500)만 있음.
- `fastlane/metadata/android/*/images/phoneScreenshots/`에 png 0개.

**조치**:
1. 디버그 APK(`app-debug.apk`)를 실제 기기/에뮬레이터에 설치.
2. 핵심 화면 캡처 (권장 컷):
   - 메인 화면 (EJECT 버튼)
   - 가짜 수신 전화 화면 (One UI 스타일)
   - 통화 중 스크립트 힌트 화면
   - 타이머 / 흔들기 / 볼륨 트리거 설정
   - 다국어 지원 (한/영 등)
   - 히스토리 화면
3. `fastlane/metadata/android/{en-US,ko-KR,ja-JP}/images/phoneScreenshots/` 폴더에 1.png, 2.png … 형태로 배치.

---

## 🟡 마감 작업 (Should-fix before launch)

### S1. fastlane metadata 언어 누락

- 풀 디스크립션은 7개 언어 지원 광고 (한/영/일/중/스/힌)
- `fastlane/metadata/android/`에는 3개만 있음 (en-US, ja-JP, ko-KR)
- 누락: **zh-CN, es-ES, hi-IN** (그리고 ASO 문서 기준 **de-DE 등**도 검토)

**조치**: 누락된 언어의 metadata 폴더 생성 + title/short_description/full_description/changelogs 채우기. (PLAY_STORE_ASO.md 에 이미 번역 텍스트 일부 존재)

### S2. 사용자가 "main/master" 브랜치를 답변했으나 실제 기본 브랜치는 `Eject_Button_app`

- 원격 기본 브랜치: `Eject_Button_app`
- main/master 브랜치 자체가 없음
- Cloudflare 자동 배포(wrangler)는 이미 `Eject_Button_app` 기준으로 설정됨

**조치 (선택)**:
- 그대로 둠 (현재 작동 중) → 권장
- 또는 `main`으로 리네임 (CI 워크플로우 trigger branch 변경, Cloudflare GitHub 연동 재설정 필요)

### S3. 기본 정책 문서 외 추가 점검 필요

**Play Console에서 묻는 항목 중 아직 코드 기반으로 정확히 답할 수 있는지 미확인**:
- COPPA / GDPR-K 대응 (어린이 콘텐츠 분류) → **No** 명시 필요
- 광고 ID 사용 — Firebase Analytics + AdMob 사용하므로 **Yes** + 사유 작성 필요
- 정부 앱 / 의료 앱 분류 — 모두 **No**

`docs/play-data-safety.md`에 일부 답변 초안 있음 (양호).

---

## 🟢 양호 (잘 되어 있는 것 — 칭찬)

| 항목 | 상태 |
|---|---|
| `.gitignore`로 keystore/secrets 제외 | ✅ |
| 코드 내 하드코딩된 시크릿 없음 (grep 검증) | ✅ |
| Release 빌드 시 AdMob 테스트 ID **fail-fast** 처리 | ✅ |
| GitHub Actions로 production AAB + APK 자동 빌드 | ✅ |
| versionCode 자동 증가 (1000 + GITHUB_RUN_NUMBER) | ✅ |
| keystore base64 → 디코드 → 빌드 → 즉시 삭제 | ✅ |
| ProGuard/R8 활성화 (`isMinifyEnabled = true`) | ✅ |
| compileSdk/targetSdk = 35 (Android 15, Play 최신 요구) | ✅ |
| Play Policy Declarations 문서화 (한/영, 권한별 사유) | ✅ |
| Data Safety Form 답변 시트 작성 | ✅ |
| Privacy Policy HTML + Cloudflare Workers 호스팅 설정 | ✅ |
| 단위 테스트 3건 (PhoneNumberUtil 등) | ✅ |
| Conventional Commits 준수 (feat:, fix:, chore:) | ✅ |
| 7개 언어 i18n (UI 문자열) | ✅ |

---

## 📋 출시까지 추천 순서

1. **(B1)** GitHub Actions에서 release-aab 워크플로우 수동 실행 → 새 AAB 다운로드
2. **(B2)** 에뮬레이터에 디버그 APK 설치 → 스크린샷 4–8장 촬영 → fastlane 폴더에 배치
3. **(S1)** zh-CN, es-ES, hi-IN fastlane metadata 채우기 (PLAY_STORE_ASO.md 참고)
4. **(S2)** 브랜치 정책 결정 (현재 유지 권장)
5. **Play Console 작업**:
   - 앱 만들기 → 패키지명 `com.simonykim.ejectbutton` 등록
   - Internal Testing track에 새 AAB 업로드
   - Data Safety Form 작성 (`docs/play-data-safety.md` 그대로 입력)
   - Policy Declarations 입력 (`docs/play-policy-declarations.md`에서 복사)
   - 스크린샷·아이콘·피처 그래픽 업로드
   - 콘텐츠 등급 설문 → 13+ 또는 16+ 예상
   - Production track 출시 또는 Closed Testing 먼저
6. **출시 후 모니터링**: Microsoft Clarity 대시보드, Firebase Analytics, Play Console Vitals

---

## 🛡️ 보안 점검 결과 (CSO 모드 요약)

| 항목 | 상태 | 비고 |
|---|---|---|
| 시크릿 관리 | ✅ | secrets.properties + GitHub Secrets, 둘 다 git 제외 |
| keystore | ✅ | base64로 GitHub Secrets에만 보관, CI 종료 시 삭제 |
| 의존성 | ✅ | Firebase BoM, AndroidX BoM 사용 (버전 일관성) |
| 권한 사용 | ⚠️ | 9개의 sensitive permission — 모두 `play-policy-declarations.md`에 사유 문서화됨 |
| 데이터 수집 공시 | ✅ | data-safety.md 작성됨 |
| ProGuard | ✅ | release에 minify + 기본 규칙 |
| 광고 ID 정책 | ✅ | targetSdk 35라 자동 advertising ID 권한 처리 필요 — manifest 확인 필요 (확인 항목으로 추가) |

---

## 📌 다음 단계 — 사용자 결정 필요

위 차단 이슈 (B1, B2)를 **저(Claude)에게 어떻게 진행시킬지** 알려주시면 바로 이어서 작업하겠습니다.
