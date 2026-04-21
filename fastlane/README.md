# Fastlane Supply — Play Store Metadata

Play Console 리스팅 카피·스크린샷·changelog 를 레포로 버전 관리하기 위한
[Fastlane Supply](https://docs.fastlane.tools/actions/supply/) 디렉터리 구조.

## 디렉터리 구조

```
fastlane/metadata/android/
├── ko-KR/                 # 한국 (1차 타겟)
│   ├── title.txt                       (≤30자)
│   ├── short_description.txt           (≤80자)
│   ├── full_description.txt            (≤4000자)
│   ├── changelogs/default.txt          (≤500자)
│   └── images/
│       ├── icon.png                    (512×512)
│       ├── featureGraphic.png          (1024×500)
│       └── phoneScreenshots/           (1080×1920 권장, 2–8장)
├── en-US/                 # 영어권
└── ja-JP/                 # 일본
```

## 수동 업로드 (현재 방식)

1. Play Console 열기 → Store listing.
2. `fastlane/metadata/android/<locale>/*.txt` 내용을 해당 필드에 복붙.
3. `store-assets/play-store-icon-512.png`, `store-assets/play-store-feature-graphic-1024x500.png` 업로드.
4. 스크린샷은 `images/phoneScreenshots/` 에 저장 후 수동 업로드.

## 자동 업로드 (향후 전환)

Fastlane 으로 CI 에서 자동 업로드하려면:

```bash
gem install fastlane -NV
# fastlane/Appfile, fastlane/Fastfile 생성
# Play Console → Setup → API access → Service account JSON 키 발급
# GitHub secret PLAY_SERVICE_ACCOUNT_JSON 에 저장
fastlane supply --aab app/build/outputs/bundle/release/app-release.aab \
                --track internal \
                --json_key_data "$PLAY_SERVICE_ACCOUNT_JSON"
```

## 리스팅 카피 출처

복붙용 마스터 카피는 `PLAY_STORE_ASO.md` 참조. 이 디렉터리의 txt 는
Fastlane Supply 스펙에 맞춰 파일로 분리한 사본이다. 카피 수정은 양쪽
모두에 반영해야 한다.

## 로케일 추가 계획

현재: `ko-KR`, `en-US`, `ja-JP` — 우선 3개국.

다음 단계:
- `zh-CN` (Google Play 미지원이지만 사이드로드용 번역 보관)
- `es-ES`, `es-MX`
- `hi-IN`
