# 비상탈출 (Emergency Exit)

> 어색한 자리에서 자연스럽게 빠져나오는 가짜 전화 앱
> One-tap fake call to slip out of awkward moments — gracefully.

[![Build](https://img.shields.io/badge/build-Android%20Gradle-success)](#)
[![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-blue)](#)
[![License](https://img.shields.io/badge/license-Proprietary-lightgrey)](#)

---

## 🎯 한 줄 요약

큰 빨강 버튼 한 번. 진짜 같은 가짜 전화가 와요. 자연스럽게 받고, 자연스럽게 빠져나오세요.

## ✨ 주요 기능

- **진짜 같은 가짜 전화** — 실제 수신 화면 그대로
- **잠금화면 우회 (v1.5.23+)** — 화면 끈 상태·패턴/PIN 잠금 위에서도 즉시 통화 화면 노출 (인증 prompt 없이)
- **3가지 출동 방식** — 탭 한 번 / 폰 흔들기 / 옆면 버튼 꾹
- **3가지 타이밍** — 지금 당장 / 10초 뒤 / 내 맘대로
- **위장 아이콘 (v1.5.20+)** — 계산기·메모·날씨·시계 4종 색깔 픽토그램 picker, 메인 + 설정 양쪽 시각 시그니처 일관
- **7개 언어 지원** — ko · en · ja · zh-CN · zh-TW · es · hi (모두 친근한 본부 무전사 voice 톤)
- **다크 모드** — 어두운 식당·영화관·카페에서도 자연스럽게

## 🏗️ 기술 스택

- **언어**: Kotlin 1.9+
- **UI**: Jetpack Compose (Material 3)
- **빌드**: Android Gradle Plugin 8.x
- **최소 SDK**: API 26 (Android 8.0)
- **타겟 SDK**: API 34 (Android 14)
- **AdMob**: 광고 + 인앱 결제 (구독·1회 결제)

## 📁 폴더 구조

```
.
├── app/                          # Android 앱 모듈
│   └── src/main/
│       ├── java/com/ejectbutton/ # Kotlin 소스
│       │   ├── data/              # AppStrings.kt (7-lang i18n), EjectPrefs
│       │   ├── service/           # FakeCallOverlayService, ShakeDetection
│       │   ├── ui/                # Compose screens, theme, components
│       │   └── ads/               # AdManager (AdMob)
│       └── res/                  # drawable, mipmap, strings.xml × 7
├── docs/                         # 설계·정책 문서
│   ├── design/                    # 디자인 핸드오프, 아이콘 시안
│   ├── handoffs/                  # 과거 버전 핸드오프 아카이브
│   ├── archive/                   # 운영 문서 아카이브
│   ├── play-*.md                  # Play Console 정책·데이터 보안
│   └── privacy-policy.html       # 개인정보 처리방침 (Cloudflare Pages 호스팅)
├── fastlane/metadata/android/   # Play Console 자동화 메타데이터 (5 locales)
├── playconsole-assets/           # listing-{locale}.md, ASO 키워드 풀세트
│   └── listings/                 # 7개 언어 listing markdown
├── store-assets/                 # Play Store 아이콘·피처 그래픽
├── gradle/                       # gradle wrapper
├── CHANGELOG.md                  # 버전별 변경사항
└── wrangler.jsonc                # Cloudflare Pages (정책 페이지) 배포 설정
```

## 🚀 로컬 빌드

```bash
# 디버그 빌드 + 에뮬레이터 설치
./gradlew installDebug

# 릴리즈 AAB 생성 (서명 필요)
./gradlew bundleRelease
```

상세 빌드 가이드는 [`docs/local-release-build-guide.md`](docs/local-release-build-guide.md) 참조.

## 🌐 다국어

`app/src/main/java/com/ejectbutton/data/AppStrings.kt` 한 파일에 7개 언어 (en/ko/zh-CN/zh-TW/ja/es/hi) 가 모두 정의되어 있습니다. 시스템 언어에 따라 자동 적용되며, 앱 내 설정에서도 수동 선택 가능.

톤 가이드: **비상탈출 본부 무전사** 캐릭터 — 의성어 호들갑 (`삐뽀삐뽀`, `Beep beep!`, `ピーポーピーポー`), 짧은 문장, 친근한 의문문.

## 🎨 디자인 시스템

- **브랜드 RED**: `#BA1A20` (primary), `#B71720` (launcher 아이콘), `#6A0008` (EJECT 음영)
- **0dp 모서리 전역 강제** — 둥근 모서리 금지 (cockpit 느낌)
- **타이포그래피**: SansSerif Black/ExtraBold + Monospace bodySmall

자세한 디자인 토큰은 [`app/src/main/java/com/ejectbutton/ui/theme/Theme.kt`](app/src/main/java/com/ejectbutton/ui/theme/Theme.kt) 참조.

## 🔒 정책

- Google Play 콘텐츠 정책 준수 — 보이스피싱·스파이·감시 키워드 금지
- 개인정보 처리방침: <https://ejectbutton.pages.dev/privacy-policy>
- 데이터 보안 선언: [`docs/play-data-safety.md`](docs/play-data-safety.md)

## 📦 출시

- **현재 버전**: v1.6.3 (2026-05-17)
- **현재 트랙**: 프로덕션 액세스 신청 자격 충족 (closed testing 14일 + 12명 이상 ✓)
- **출시 listing**: 7개 언어 완료 — `playconsole-assets/listings/` (v1.5) + [`playconsole-assets/store-listing-i18n/v1.6.3-translations.md`](playconsole-assets/store-listing-i18n/v1.6.3-translations.md) (v1.6.3 신규 톤)
- **단일 main 브랜치** — 레거시 브랜치 (`Eject_Button_app`, `before-release`) 정리됨
- **APK 사이드로드**: [GitHub Releases](https://github.com/Simon-YHKim/eject-button/releases) 의 최신 태그에서 `app-debug-v*.apk` 다운로드

### v1.6.x 시리즈 주요 변경 (vs v1.5.x 종합)

| 영역 | 변경 |
|---|---|
| AdMob compliance (v1.6.3) | native ad `iconView (ImageView, 48dp)` → `mediaView (MediaView, 120dp)`. validator "MediaView not used" + "too small for video" 두 단계 통과. image/video 광고 풀 quality 노출 |
| 앱 아이콘 (v1.6.3) | `1. App Icon_rev2.png` (1068×1090 EmergencyRed) 적용. 5 dpi × 4 variant = 20개 PNG 재생성. Pixel adaptive mask 정상, Android 13+ themed icon 호환 |
| i18n 일관성 (v1.6.3) | 7개 로케일 시그너처 onomatopoeia (`삐뽀삐뽀`/`Beep beep!`/`嘀嘀！`/`ピーポー`/`¡Bip bip!`/`बीप बीप`) + 군사용어 비유 (`본부`/`HQ`/`总部`/`cuartel`/`मुख्यालय`) + 이모지 prefix 정렬 |
| 와이프 feedback (v1.6.2) | 7 polish items — premium feature3 (false promise) 제거, 가격 ₩1,900 → ₩3,000 + 14국 PPP 재책정 |
| 단일 화면 (v1.6.1) | Command 탭 단일 스크린화 + tutorial polish + brand rev2 자산 |
| 잠금화면 우회 (v1.6.0) | full-screen intent + `IncomingCallActivity` trampoline + secure keyguard passive overlay 전략 (인증 prompt 없이 통화 화면 노출) |
| Decoy picker (v1.6.0) | placeholder 🎭 emoji → v1.5.16 사용자 디자인 4종 launcher 아이콘 (계산기·메모·날씨·시계) 채택 (메인 + 설정 + picker dialog 일관) |
| 7국 i18n 톤 (v1.6.0) | de91b49 의 ko-only 친근화를 6국 (en/zhCN/zhTW/ja/es/hi) 으로 확장. 군용 어휘 (loadout/装备/Equipo táctico/गियर 등) → 친근한 본부 무전사 voice 매칭 |
| 리포 hygiene (v1.5.21) | dead code 8개 drawable 제거, root 45MB 잡파일 정리, EOL 정책 (`.gitattributes`), `release-builds/` 트래킹 차단 |

## 🤝 개발자

**Simon-YHKim** (hwanydanh@gmail.com) — 개발자 계정 ID `4795577270086966748`

---

🚨 비상탈출 본부였습니다. _Beep beep!_

## 📊 버전 히스토리 요약

| 버전 | 핵심 변경 |
|---|---|
| **v1.6.3** | 최종 audit — AdMob native ad MediaView 120dp + rev2 아이콘 + i18n consistency + main 단일 브랜치 |
| v1.6.2 | 와이프 feedback 7 polish (premium feature3 제거, 가격 ₩1,900→₩3,000 + 14국 PPP) |
| v1.6.1 | Command 탭 단일 스크린 + tutorial polish + brand rev2 자산 |
| v1.6.0 | 종합 정리·출시 — 7국 톤 + 잠금화면 우회 + decoy picker + 리포 hygiene |
| v1.5.26 | foreground service notification heads-up 제거 (LOW 채널 환원) |
| v1.5.25 | secure keyguard 인증 prompt 회피 (passive overlay) |
| v1.5.24 | trigger 발동 시 IncomingCallActivity 직접 startActivity (background activity start allowlist) |
| v1.5.23 | USE_FULL_SCREEN_INTENT + IncomingCallActivity 트램폴린 + HIGH 채널 + setFullScreenIntent 도입 |
| v1.5.22 | i18n 톤 (coachmarkStepDisguise 6국) + Settings 위장 row icon (실제 decoy 픽토그램) |
| v1.5.21 | dead code + .gitattributes + release-builds/ 차단 + EOL 정규화 |
| v1.5.20 | decoy picker 4종 아이콘 매핑 + onboarding step 2 emoji 통일 (`👤`) |
| v1.5.13 | Emergency Red rebrand (Navy+Cream rollback) |

전체 변경 이력은 [`CHANGELOG.md`](CHANGELOG.md) 참조.
