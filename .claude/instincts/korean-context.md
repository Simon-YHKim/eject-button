# 한국어·한국 서비스 컨텍스트

> **목적**: 한국 시장·한국어 처리·국내 API의 특이사항을 누적한다. 영어권 디폴트가 깨지는 지점들.
> **갱신 규칙**: 새 관용어·API 함정 발견 시 append.

---

## 언어·용어

- **존댓말 모드**: 사용자 요청 톤에 맞춰 반말/존댓말 일관성 유지. 혼용 금지.
- **부동산 용어**: 전세·월세·반전세·보증금·권리금·전용면적·공급면적 — 영어 직역 금지. (예: 전세 ≠ "jeonse lease", 그냥 "jeonse" + 각주)
- **결제 용어**: 무통장입금, 가상계좌, 실시간계좌이체, 간편결제(카카오·네이버·토스페이)
- **주소 체계**: 도로명주소 / 지번주소 2중. API에 따라 둘 다 요구될 수 있음

---

## 국내 API 특이사항

### 토스페이먼츠 / Toss Payments
- 승인 API: `POST /v1/payments/confirm` — idempotency-key 필수
- 웹훅: `tosspayments-signature` 헤더로 HMAC-SHA256 검증
- 금액은 `Long` (원 단위, 소수점 없음)

### 네이버 / Naver
- 지도 API: client ID + secret 2중. CORS 우회 위해 서버 프록시 필요
- 검색 API: 일일 25,000회 제한 기본

### 카카오 / Kakao
- REST API key / JavaScript key / Admin key 3종 분리
- Kakao Map: `autoload=false` + 수동 `kakao.maps.load()` 패턴 권장

### 네이버 부동산 / 직방 / 다방
- 공개 API 없음 → 스크래퍼는 robots.txt, Rate 준수, User-Agent 명시 필수
- JSON endpoint 바뀔 수 있음 → 회귀 테스트 필수

---

## 한국 특유 UX 패턴

- **휴대폰 번호 인증**: 가입·결제 기본 플로우. 나이스·KG이니시스·SMS 기반
- **본인인증**: PASS 앱 / 공동인증서 / 카카오 인증
- **실명 정책**: 게임·금융 서비스는 실명 필수
- **주민등록번호**: **수집 금지** (법적 제한). CI/DI 값만 사용

---

## LLM·번역 함정

- 한국어 토크나이징: GPT·Claude 모두 한국어 1자당 2-3 토큰 → 비용 계산 주의
- 날짜: "4월 12일" vs "04/12" vs "12/04" — 서버 로케일 고정 필요
- 숫자: "억"·"만" 단위를 영어로 번역하면 의미 손실

---

## Google Play Console (한국 개발자 관점)

### 출시 노트 한도
- **언어당 500자** (UTF-8 codepoint 기준). 스페인어 / 포르투갈어 / 힌디어는 한국어보다 ~1.5배 길어지므로 짧게 다듬어야 함.
- 한국어가 가장 압축적이라 마진 가장 큼. 한국어로 먼저 작성 후 영어/일본어로 짧게 변환, 그 다음 스페인어/힌디어는 핵심만 추려서.

### 임시 버전 충돌
- 이전에 만들다 만 "제목 없는 버전 (임시)" 가 있으면 "새 버전 만들기" 버튼 비활성. **"버전 수정"** 클릭해서 그 임시 버전 안에 새 AAB 업로드하거나, **"임시 출시 삭제"** 후 새로 시작.

### Production 단계적 출시 (Staged rollout)
- 단계적 출시 첫 사이클은 **20% 권장**. 24~48h Vitals (크래시율 / ANR) 확인 후 50% → 100%. 회귀 발견 시 즉시 "출시 중단" 가능 — 영향 사용자 비율만큼만.

### Library 에서 추가 (이전 AAB 재사용)
- Production / Internal Test 에서 **"라이브러리에서 추가"** = 이전에 업로드한 AAB versionCode 그대로 재사용. CI 다시 안 돌리고 promote.

### 디버그 기호 (Native debug symbols) 경고
- "App Bundle 이 네이티브 코드를 포함하며 디버그 기호 미업로드" 경고는 AdMob / Firebase / Clarity 등 transitive .so 파일 때문. **앱 동작 영향 없음** — Crashlytics native trace 의 symbol resolution 만 안 됨. v1.6.9 production 도 동일 경고로 정상 출시 중.

### PPP 가격 매핑 (구독 / IAP)
한국 ₩3,000 기준 14국 PPP 변환 (eject-button v1.6.2 채택):

| 국가 | 가격 |
|---|---|
| KR | ₩3,000 |
| US | $2.49 |
| EU (ES/DE/FR/IT/NL) | 2,29 € |
| UK | £1.99 |
| JP | ¥350 |
| CN | ¥15 |
| TW | NT$70 |
| HK | HK$18 |
| IN | ₹149 |
| MX | MX$45 |
| BR | R$10.90 |
| AU | A$3.49 |
| CA | CA$3.29 |
| ID | Rp35,000 |

**fallback 가격 (Play Console 미응답 시 노출)** 도 항상 Play Console 실제 등록 가격과 일치시킬 것. 어긋나면 사용자가 다이얼로그 가격 ≠ 결제 가격 경험.
