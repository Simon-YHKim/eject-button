# Play Console — 등록 설문 답변 시트 (Eject Button)

> Play Console 앱 등록 시 채워야 하는 모든 설문/선언/콘텐츠 항목에 대한 답변 시트.
> `docs/play-data-safety.md` 와 `docs/play-policy-declarations.md` 의 보완본.
>
> **앱 ID**: `com.simonykim.ejectbutton`
> **앱 이름**: Eject Button — Escape Call
> **카테고리**: Tools (1차) / Lifestyle (2차)
> **버전**: 1.0.8 (versionCode = CI에서 1000+RUN_NUMBER 로 자동 부여)
>
> 마지막 업데이트: 2026-04-25

---

## 1. App Content (앱 콘텐츠)

### 1-1. Privacy policy URL
| 항목 | 답변 |
|---|---|
| URL | `https://eject-button.hwanydanh.workers.dev/privacy-policy` |
| 비고 | Cloudflare Workers 호스팅 (`docs/privacy-policy.html`). 푸시 시 자동 배포. |

### 1-2. App access (테스터에게 로그인이 필요한가?)
| 답변 | All functionality is available without special access |
|---|---|
| 사유 | 본 앱은 회원가입·로그인 시스템이 없음. 모든 기능은 설치 즉시 사용 가능. |

### 1-3. Ads (광고 포함 여부)
| 항목 | 답변 |
|---|---|
| 광고 포함? | **Yes, my app contains ads** |
| 광고 형식 | Banner, Interstitial (Native ad는 미사용 — 차후 검토) |
| 광고 SDK | Google AdMob (`play-services-ads`) |
| 사용자에게 광고임을 명확히 표시? | Yes (AdMob의 표준 라벨링 사용) |
| 어린이용 광고 정책 (CYAd) 준수 필요? | No — 본 앱은 어린이용이 아님 (아래 Target audience 참조) |

### 1-4. Content rating (콘텐츠 등급)
IARC 설문 기반 예상 등급:

| 질문 | 답변 |
|---|---|
| 폭력 묘사 | **No** |
| 성적 콘텐츠 | **No** |
| 성인 언어/욕설 | **No** |
| 통제된 약물 (술/담배/마약) 묘사 | **No** |
| 도박 | **No** |
| 사용자 생성 콘텐츠 / 사용자 간 메시지 | **No** |
| 사용자 위치 공유 | **No** |
| 디지털 화폐/실물 화폐 매매 | **No** |
| 인앱 결제 (디지털 상품) | **Yes** — `eject_premium` ($2.99 일회성) |
| 사용자에게 광고 표시 | **Yes** — AdMob 배너+전면 |
| 무서운 / 끔찍한 콘텐츠 | **No** |
| 공포·잔인 묘사 | **No** |

**예상 등급**:
- ESRB (북미): **Everyone** 또는 **Everyone 10+**
- PEGI (유럽): **PEGI 3** 또는 **PEGI 7**
- IARC (KR/JP/기타): **All ages** ~ **Teen**
- 한국 게임물관리위원회: 게임 아님 → 자체등급 분류 면제

### 1-5. Target audience (대상 연령층)
| 항목 | 답변 |
|---|---|
| 대상 연령대 | **18세 이상** (Adult) |
| 사유 | 사회적 압박 상황(회식, 영업 미팅, 데이트)에서 사용하는 도구로, 성인 직장인·대학생 이상이 주 사용층. 어린이가 사용할 만한 콘텐츠가 없음. |
| 어린이 정책(Designed for Families) 적용 여부 | **No** |
| 어린이가 앱에 흥미를 가질 수 있는 시각적 요소가 있는가? | **No** — UI는 일반 통화 화면 스타일이고 캐릭터/게임 요소 없음 |

### 1-6. News app (뉴스 앱 여부)
| 답변 | **No** — 본 앱은 뉴스 콘텐츠를 게시하지 않음 |
|---|---|

### 1-7. COVID-19 contact tracing & status apps
| 답변 | **No** — 보건/감염 추적 기능 없음 |
|---|---|

### 1-8. Data safety (데이터 보안)
→ 별도 문서 `docs/play-data-safety.md` 참조. 요약:
- Personal info / Financial / Health / Messages / Photos / Audio / Files / Calendar — **수집 안 함**
- 수집: App activity (interactions), App info & performance (crash, diagnostics), Device or other IDs
- 공유: 동일 항목을 Firebase / AdMob / Clarity 와 공유
- 모든 데이터는 전송 시 암호화 (HTTPS)
- 사용자가 데이터 삭제 요청 가능 (Firebase Console DSAR + 이메일 문의)

### 1-9. Government apps
| 답변 | **No** — 정부/공공기관 앱 아님 |
|---|---|

### 1-10. Financial features
| 답변 | **No** — 금융 결제·송금·트레이딩 기능 없음. Google Play Billing 만 사용 (인앱 상품). |
|---|---|

### 1-11. Health apps
| 답변 | **No** — 의료/건강 데이터 처리 없음 |
|---|---|

### 1-12. Permission declarations
→ 별도 문서 `docs/play-policy-declarations.md` 참조. 권한별 사유서:
- `SYSTEM_ALERT_WINDOW` (overlay) — 핵심 기능
- `FOREGROUND_SERVICE_SPECIAL_USE` — 흔들기/볼륨 트리거 감지 (sensor monitoring)
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — MediaSession 볼륨 라우팅
- `READ_PHONE_STATE` — 실제 통화 시 가짜 화면 자동 숨김
- `READ_CONTACTS` — 선택 권한 (실제 연락처를 가짜 발신자로 선택 시)
- `POST_NOTIFICATIONS` — 백그라운드 서비스 상태 알림
- `CAMERA` (flash only) — 수신 시 LED 깜박임 (사진/영상 촬영 안 함)
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — Doze 모드에서 트리거 감지 유지

### 1-13. Sensitive permissions / API usage
| API | 사용 여부 | 비고 |
|---|---|---|
| AccessibilityService | No | |
| Notification Listener | No | |
| Default Phone/SMS handler | **No** — 단지 통화 상태 읽기만 함 |
| Manage External Storage | No | |
| All files access | No | |
| QUERY_ALL_PACKAGES | No | |
| Exact alarm | No | timer는 `Handler` + foreground service 로 구현 |
| Health Connect | No | |

---

## 2. Pricing & distribution (가격 & 배포)

### 2-1. App pricing
| 항목 | 답변 |
|---|---|
| 앱 자체 | **무료 (Free)** |
| 인앱 상품 | **있음** — `eject_premium` ($2.99) |

### 2-2. Countries / regions
**1차 출시 국가** (PLAY_STORE_ASO.md 권장):
- 🇰🇷 South Korea (Korean)
- 🇯🇵 Japan (Japanese)
- 🇺🇸 United States (English)
- 🇬🇧 United Kingdom
- 🇨🇦 Canada
- 🇦🇺 Australia
- 🇪🇸 Spain (Spanish)
- 🇲🇽 Mexico
- 🇮🇳 India (Hindi/English)
- 🇸🇬 Singapore (zh-CN, en)

**제외 국가**:
- 🇨🇳 China — Google Play 미지원
- 미국 제재 대상국 (북한, 이란 등) — Google Play 정책상 자동 제외

### 2-3. Devices
| 항목 | 답변 |
|---|---|
| 폰 (Phone) | **Yes** |
| 태블릿 (Tablet) | Yes — 단, UI가 폰 우선 (태블릿 최적화 검토 필요) |
| Wear OS | No |
| Android TV | No |
| Auto | No |
| Chrome OS | Maybe — 미테스트 |

---

## 3. Store presence (스토어 등록정보)

### 3-1. Store listing — 기본 정보
| 항목 | 값 | 출처 |
|---|---|---|
| 앱 이름 | Eject Button — Escape Call | `fastlane/metadata/.../title.txt` |
| 짧은 설명 | (언어별) | `fastlane/metadata/.../short_description.txt` |
| 자세한 설명 | (언어별) | `fastlane/metadata/.../full_description.txt` |
| 연락처 이메일 | simonkim250405@gmail.com | |
| 전화번호 | (개발자 본인 입력) | |
| 웹사이트 | https://eject-button.hwanydanh.workers.dev/ | |
| 개인정보처리방침 | https://eject-button.hwanydanh.workers.dev/privacy-policy | |

### 3-2. Graphic assets
| 자산 | 사양 | 상태 | 위치 |
|---|---|---|---|
| 앱 아이콘 | 512×512 PNG, 32-bit | ✅ 있음 | `store-assets/play-store-icon-512.png` |
| 피처 그래픽 | 1024×500 PNG/JPG | ✅ 있음 | `store-assets/play-store-feature-graphic-1024x500.png` |
| 폰 스크린샷 | 최소 2장 (권장 4-8장), 최소 320px | ❌ **누락** — Play 등록 차단 사유 | (촬영 필요) |
| 7"+ 태블릿 스크린샷 | 선택 | — | |
| 10"+ 태블릿 스크린샷 | 선택 | — | |
| 프로모션 비디오 (YouTube URL) | 선택 | — | |

### 3-3. Multilingual store listing (지원 언어)
| 언어 | title | short | full | changelog | 상태 |
|---|---|---|---|---|---|
| en-US | ✅ | ✅ | ✅ | ✅ | OK |
| ko-KR | ✅ | ✅ | ✅ | ✅ | OK |
| ja-JP | ✅ | ✅ | ✅ | ✅ | OK |
| zh-CN | ✅ | ✅ | ✅ | ✅ | **추가됨 (이번 라운드)** |
| es-ES | ✅ | ✅ | ✅ | ✅ | **추가됨 (이번 라운드)** |
| hi-IN | ✅ | ✅ | ✅ | ✅ | **추가됨 (이번 라운드)** |

---

## 4. Release management

### 4-1. Test track 진행 순서 (권장)
1. **Internal testing** — 본인 / 가까운 지인 5–10명. AAB 업로드 후 즉시 가용 (~수 분).
2. **Closed testing — Alpha** — 50–100명 모집. 최소 14일 운영 후 Production 신청 가능 (Play 정책 변경 2023-11~).
3. **Open testing — Beta** — 모집 페이지 공개. 선택사항.
4. **Production** — 단계적 출시 (10% → 25% → 50% → 100%).

### 4-2. App signing
| 항목 | 답변 |
|---|---|
| Play App Signing | **사용** (권장) — 첫 출시 시 자동 활성화 |
| Upload key | `app/release.jks` (CI에서 base64 시크릿 → 디코드) |
| 비고 | Upload key 가 분실되면 Play Console 에서 키 교체 신청 가능. 잘 보관할 것. |

### 4-3. Release types
- **Production**: 모든 사용자에게 공개
- **Open testing**: 모집 페이지로 누구나 참여
- **Closed testing**: 이메일 리스트 / Google 그룹
- **Internal testing**: 최대 100명, 즉시 배포

---

## 5. Monetization

### 5-1. Subscriptions
| 답변 | **No** — 구독 상품 없음. 일회성 인앱 상품만 사용. |
|---|---|

### 5-2. In-app products
| ID | 가격 | 유형 | 설명 |
|---|---|---|---|
| `eject_premium` | $2.99 | Managed (one-time) | 광고 제거 + 통화 중 스크립트 힌트 무제한 |

지역별 가격 (자동 변환 + 수동 보정 권장):
- 한국 ₩3,900
- 일본 ¥400
- 인도 ₹199
- 스페인/멕시코 €2.99 / MX$59
- 중국(미배포)

---

## 6. 출시 후 모니터링 체크리스트

| 도구 | 무엇을 본다 |
|---|---|
| Play Console → Vitals | ANR, 크래시율, 배터리 사용량, 와도/안드로이드 14+ 호환성 경고 |
| Firebase Analytics | DAU, WAU, Retention, 핵심 이벤트 (eject_triggered, premium_purchased) |
| Microsoft Clarity | 사용자 세션 녹화, 분노 탭, 죽은 클릭 |
| AdMob | eCPM, 노출, 채워짐 비율, 정책 위반 경고 |
| 리뷰 / 평점 | 알림 설정 (Play Console → 사용자 의견 → 알림) |

---

## 7. 향후 점검 트리거

다음 중 어떤 일이 생기면 이 시트를 재검토:
- 새 권한을 manifest 에 추가
- 새 SDK 추가 (특히 데이터 수집 SDK)
- 광고 형식 변경 (예: 보상형 광고 추가)
- 인앱 상품 가격/구조 변경
- 새 기능으로 대상 연령층 변동
- Google Play 정책 업데이트 (분기 1회 메일 확인)
