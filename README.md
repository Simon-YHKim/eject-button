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
- **3가지 출동 방식** — 탭 한 번 / 폰 흔들기 / 옆면 버튼 꾹
- **3가지 타이밍** — 지금 당장 / 10초 뒤 / 내 맘대로
- **위장 아이콘** — 계산기·메모·날씨·시계로 위장 가능
- **7개 언어 지원** — ko · en · ja · zh-CN · zh-TW · es · hi
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

- **현재 트랙**: 비공개 테스트 (Internal Testing)
- **다음 단계**: 프로덕션 신청 (12명 이상 비공개 테스트 14일 완료 후)
- **출시 listing**: 7개 언어 완료 ([`playconsole-assets/listings/`](playconsole-assets/listings/))

## 🤝 개발자

**Simon-YHKim** (hwanydanh@gmail.com) — 개발자 계정 ID `4795577270086966748`

---

🚨 비상탈출 본부였습니다. _Beep beep!_
