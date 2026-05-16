# Icon Redesign — v1.5.13 (EmergencyRed Pictogram)

작성일: 2026-05-12
브랜치: `feat/v1.5.13-icon-redesign`
원본 SVG: `docs/design/icon-v1.5.13.svg` (1254×1254 viewBox, 47 paths)

---

## 디자인 의도

ISO 7010 비상구(emergency exit) 표지의 시각 언어를 차용하면서 "통화 중 탈출"이라는
앱 본연의 메타포를 직접적으로 표현. 흰 비상문(말풍선 + 문 결합 형상) 내부에 빨간
사람이 휴대폰을 귀에 대고 달려나가는 자세를 배치.

기존 ⏏ 픽토그램은 추상적인 "eject" 글리프였으나, 신 디자인은 다음을 모두 시각화:

1. **휴대폰(전화 중)** — 사람이 든 직사각형 + 신호선 3 arc
2. **달리는 자세** — 다리 벌림 + 팔 흔들기로 동적 표현
3. **비상문(탈출 경로)** — 흰 사각형 (살짝 사다리꼴 → 원근감)
4. **EmergencyRed 단색** — 시인성·일관성·국제 안전 표지 매핑

## 색상 사양

| 토큰 | Hex | 용도 |
|------|-----|------|
| EmergencyRed | `#B71720` | 배경 + 사람 figure 단색 |
| White | `#FFFFFF` | 비상문 + 신호 arcs |

> SVG 원본은 anti-alias 효과로 47개의 색조 변이를 포함하지만, 의미상 단색 2종 디자인이다.

## Android 자산 매트릭스

### Adaptive Icon (Android 8.0+, API 26+)
| 자산 | 경로 | 사양 |
|------|------|------|
| 배경 | `res/drawable/ic_launcher_background.xml` | `<shape>` solid `#B71720` |
| 전경 | `res/mipmap-{density}/ic_launcher_foreground.png` | 비트맵, density별 48/72/96/144/192 px |
| 정의 | `res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml` | `<adaptive-icon>` 참조 |
| 테마 | monochrome 슬롯도 동일 비트맵 (시스템이 grayscale 마스킹) |

### Legacy Icon (Android 7 이하)
| Density | px | 경로 |
|---------|----|------|
| mdpi    | 48 | `res/mipmap-mdpi/ic_launcher.png` + `ic_launcher_round.png` |
| hdpi    | 72 | `res/mipmap-hdpi/...` |
| xhdpi   | 96 | `res/mipmap-xhdpi/...` |
| xxhdpi  | 144 | `res/mipmap-xxhdpi/...` |
| xxxhdpi | 192 | `res/mipmap-xxxhdpi/...` |

### In-app EJECT 버튼
| Density | px | 경로 |
|---------|----|------|
| mdpi    | 192 | `res/drawable-mdpi/ic_eject_button.png` |
| hdpi    | 288 | `res/drawable-hdpi/...` |
| xhdpi   | 384 | `res/drawable-xhdpi/...` |
| xxhdpi  | 576 | `res/drawable-xxhdpi/...` |
| xxxhdpi | 768 | `res/drawable-xxxhdpi/...` |

### Play Store
| 자산 | 경로 | 사양 |
|------|------|------|
| Hi-res 아이콘 | `store-assets/ic_play_store_512.png` | 512×512 PNG, 32-bit |

## 사용처

- 앱 런처 아이콘 (홈 화면 / 앱 서랍)
- 통계 알림 패널 아이콘
- Play Store 등록 페이지
- `MainScreen` EJECT 버튼 (라운드 스퀘어, 232dp + pulse 애니메이션)

## 제외

- CANCEL 모드 (취소 버튼) 은 빨간 원 + ✕ UI 유지 — 정지/취소 단순 메타포 보존
- 위장 아이콘 4종 (계산기/메모/날씨/시계) 은 v1.5.12 유지
- 알림 small icon (status bar) 은 별도 monochrome vector 권장 (후속 PR)

## 빌드 검증 체크리스트

- [ ] `./gradlew :app:assembleDebug` 빌드 성공
- [ ] `./gradlew :app:lintRelease` 0 errors
- [ ] 에뮬레이터 — 홈 화면 아이콘 시각 확인 (원/사각/squircle 마스크 모두)
- [ ] 메인 화면 EJECT 버튼 시각 + tap 동작 확인
- [ ] Android 13+ 테마 아이콘 미리보기
- [ ] Play Store Internal Testing 빌드 업로드 후 상점 페이지 미리보기
