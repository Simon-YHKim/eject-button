# Play Console — Data Safety Form (Eject Button)

> Play Console → **App content → Data safety** 항목을 채우기 위한 답변 시트.
> 2022-07 부터 Play 의 모든 앱은 이 폼을 제출해야 하며, 실제 앱 동작과
> 불일치하면 정지 사유가 된다. 이 문서는 현재 레포 코드를 기준으로 정리한
> "사실 그대로" 의 선언이다.

앱 ID: `com.simonykim.ejectbutton`
SDK 동봉 목록:
- Firebase Analytics (`firebase-analytics-ktx`)
- Microsoft Clarity (`com.microsoft.clarity:clarity`)
- Google AdMob (`play-services-ads`)
- Google Play Billing
- Google Play Review API

---

## Q1: 앱이 required data 를 수집·공유합니까?

**답: 예 (Yes, this app collects or shares user data)**

이유: Firebase Analytics / Clarity / AdMob 세 SDK 모두 최소한
"앱 상호작용 (App interactions)", "기기 또는 기타 식별자 (Device or other IDs)",
"진단 데이터 (Crash logs, Performance)" 수준을 수집한다.

---

## Q2: 데이터 유형별 선언

각 항목에 대해 Play 는 아래를 묻는다:
1. 수집 여부 (Collected)
2. 공유 여부 (Shared with third parties)
3. 수집 목적 (Purposes)
4. 일시적 수집 여부 (Processed ephemerally)
5. 사용자가 수집을 거부할 수 있는가 (Optional for the user)

### 2-1. Personal info — **미수집 (None)**
이름·이메일·주소·전화번호·SSN·종교·정치적 견해·성 정체성·인종 등.
- 앱은 사용자 로그인 시스템이 없다.
- Firebase Analytics 가 자동 수집하는 "User pseudo ID" 는
  Personal info 가 아닌 **Device or other IDs** 에 속한다 (Google 공식 가이드).

### 2-2. Financial info — **미수집 (None)**
카드 번호·은행 계좌·결제 이력 등.
- 구독·일회성 결제는 Google Play Billing 에만 위임하며, 본 앱은 카드나
  계정 정보를 직접 다루지 않는다.

### 2-3. Health & fitness — **미수집 (None)**

### 2-4. Messages — **미수집 (None)**

### 2-5. Photos and videos — **미수집 (None)**
- Camera 권한은 플래시 LED 토글 전용. 촬영·저장·전송 없음.

### 2-6. Audio files — **미수집 (None)**
- 시스템 기본 벨소리만 재생 (오디오 녹음 안 함).

### 2-7. Files and docs — **미수집 (None)**

### 2-8. Calendar — **미수집 (None)**

### 2-9. Contacts — **수집하나 기기 외부로 전송하지 않음**
사용자가 가짜 발신자로 지정한 연락처의 이름·사진 URI 만 SharedPreferences
에 저장한다.
- Play 기준: "Processed ephemerally, on-device only" 체크 → **Not collected** 로 선언.
- 단, 심사관이 연락처 권한을 보고 재확인을 요청할 수 있으니
  `docs/play-policy-declarations.md` §4 의 근거를 함께 제시.

### 2-10. App activity — **수집 + 공유**

| 데이터 | 수집 | 공유 | 목적 | 사용자 선택 |
|---|---|---|---|---|
| App interactions (탭·화면 이동) | ✅ | ❌ | Analytics, App functionality | 수집 거부 불가 (개발자가 필요) |
| In-app search history | ❌ | ❌ | — | — |
| Installed apps | ❌ | ❌ | — | — |
| Other user-generated content | ❌ | ❌ | — | — |
| Other actions (광고 클릭) | ✅ | ✅ AdMob | Advertising | **거부 가능** (Premium 구매 시) |

### 2-11. Web browsing — **미수집 (None)**

### 2-12. App info and performance — **수집**

| 데이터 | 수집 | 공유 | 목적 |
|---|---|---|---|
| Crash logs | ✅ | ❌ | Analytics, App functionality (Firebase Crashlytics 는 아직 통합 안 됐으나 향후 추가 시 여기 체크) |
| Diagnostics | ✅ | ❌ | Analytics (Clarity 세션 로그) |
| Other app performance data | ✅ | ❌ | Performance |

### 2-13. Device or other IDs — **수집 + 공유**

| 데이터 | 수집 | 공유 | 목적 |
|---|---|---|---|
| Advertising ID (AAID) | ✅ | ✅ AdMob | Advertising |
| Firebase installation ID / pseudo ID | ✅ | ❌ | Analytics |
| Clarity session ID | ✅ | ❌ | Analytics |

---

## Q3: 데이터 전송 시 암호화되나요?

**답: 예 (All data is encrypted in transit using HTTPS/TLS)**

- Firebase / Clarity / AdMob 모두 HTTPS 로만 통신한다.
- 자체 서버 없음.

---

## Q4: 사용자가 데이터 삭제를 요청할 수 있나요?

**답: 예 — 앱 내 설정 또는 이메일 요청**

- 앱 삭제 시 로컬 SharedPreferences 자동 삭제.
- Firebase Analytics / AdMob 측 익명 식별자 삭제는 사용자가 직접
  기기 설정 → "광고 ID 재설정" 으로 수행 가능.
- 추가 삭제 요청은 `simonkim250405@gmail.com`.

---

## Q5: 독립 보안 검토를 거쳤나요?

**답: 아직 (Not yet — will update when third-party audit is completed)**

---

## Q6: Google Families Policy 준수 (아동 대상 앱)?

**답: 아니오 (Target age 13+)**

---

## 제출 전 실제 코드와 일치 확인

- [x] `READ_CONTACTS` → 연락처 **미수집 으로 선언** 가능 여부 검토 완료
- [x] AdMob → Advertising ID 수집·공유 선언
- [x] Firebase Analytics → pseudo ID 수집 선언
- [x] Clarity → 세션 로그 / 화면 녹화성 이벤트 수집 선언
- [ ] 향후 Crashlytics 추가 시 이 문서 §2-12 업데이트
- [ ] 향후 로그인 시스템 추가 시 §2-1 (Personal info) 재검토

---

## 참고

- [Play Console Help — Declare Data collection and security practices](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Firebase — Data disclosure for the Data safety section](https://firebase.google.com/support/guides/disclose-data-use)
- [AdMob — Data safety declaration for AdMob](https://support.google.com/admob/answer/11236162)
- [Microsoft Clarity — Data collected](https://learn.microsoft.com/en-us/clarity/faq/basic-faq#what-data-does-microsoft-clarity-collect)
