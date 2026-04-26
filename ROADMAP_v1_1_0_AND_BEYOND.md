# Eject Button — v1.1.0+ 로드맵 (사용자 결정 필요 항목 정리)

> 작성: 2026-04-26 / 8개 트리거 평가 + Play Console / UX / 수익 / 호환성 재점검
>
> v1.0.10까지 자동 적용한 후, 다음 결정 사항들을 사용자가 직접 우선순위 매기고 진행할 항목입니다.

---

## 🎯 즉시 효과 큰 것 — Play Console UI 작업만 (코드 변경 X, 1시간 내)

### A. Subscription Free Trial 7일 추가
**왜**: 산업 평균, free trial 있는 구독 앱은 전환율 2-3배 상승.
**어디**: Play Console → Monetize → Subscriptions → `eject_premium_monthly` → Edit → Add offer → **Introductory price** 또는 **Free trial 7 days**
**영향**: 다운로드 후 결제 conversion 개선

### B. Annual Plan 추가
**왜**: 월 $1.99 × 12개월 = $23.88 vs Annual $19.99 → 17% 할인 → 장기 lock-in + 일시 결제 LTV ↑.
**어디**: 같은 페이지 → Add base plan → ID `annual` → 1 year billing → $19.99
**영향**: 평균 LTV +40% (업계 통계)

### C. Pre-registration 캠페인
**왜**: 카페 모집 중인 베타 외에 일반 사용자에게도 "출시 알림" 받을 수 있게. Play Store 검색 결과 가산점.
**어디**: Play Console → Grow users → **Pre-registration**
**영향**: 출시일 첫 다운로드 1.5–3배

---

## 🤔 코드 변경 동반 — 사용자 결정 후 v1.1.0 진행

### 1. Firebase Crashlytics 추가
**비용**: app/build.gradle.kts 의존성 + plugin 추가 + EjectApplication.onCreate 1줄 + ProGuard keep.
**효과**:
- 자동 클라우드 크래시 수집 (현재 자체 CrashReportManager는 사용자가 메일 보내야 함 → 보고율 1-5%)
- Crashlytics는 100% 자동 수집 → 미발견 크래시 즉시 인지
- 일별 크래시 트렌드 / 영향 사용자 수 자동 집계

**진행 시 변경**:
```kotlin
// app/build.gradle.kts
plugins {
    id("com.google.firebase.crashlytics")
}
dependencies {
    implementation("com.google.firebase:firebase-crashlytics")
}

// EjectApplication.onCreate()
FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
```

**ProGuard**: 이미 firebase-analytics keep 있어서 추가 keep 불필요.

**권장도**: ⭐⭐⭐⭐⭐ (출시 직전에 거의 필수)

---

### 2. Edge-to-Edge 적용 (Android 15 권장, 미적용 시 lint 경고)
**비용**: Theme.xml 수정 + Activity windowCompat 설정 + 일부 Composable 패딩 조정.
**효과**:
- Android 15 (API 35) 에서 시스템 바가 항상 투명해야 함 (강제는 아니지만 권장 → 안 하면 deprecated 경고)
- 화면을 풀로 활용 → 모던한 UI 인상

**진행 시 변경**:
```xml
<!-- res/values/themes.xml -->
<style name="Theme.EjectButton" parent="...">
    <item name="android:windowOptOutEdgeToEdgeEnforcement">false</item>
    <item name="android:statusBarColor">@android:color/transparent</item>
    <item name="android:navigationBarColor">@android:color/transparent</item>
</style>
```
```kotlin
// MainActivity.onCreate
WindowCompat.setDecorFitsSystemWindows(window, false)
```

**위험**: 일부 Composable 의 statusBarsPadding / navigationBarsPadding 누락으로 콘텐츠가 시스템 바에 겹칠 수 있음 → 디자인 회귀 테스트 필요.

**권장도**: ⭐⭐⭐⭐ (출시 후 1개월 내)

---

### 3. Predictive Back Gesture 지원 (Android 14+ 권장)
**비용**: OnBackPressedDispatcher 패턴 적용 (이미 Compose 사용이라 BackHandler { } 만 추가).
**효과**:
- Android 14+ 사용자가 뒤로가기 제스처할 때 다음 화면 미리보기 (animated)
- 미적용 시 일반 instant back

**위치**: 가짜 통화 화면, 통화 중 화면, 발신자 추가 다이얼로그 등 Compose 화면 곳곳.

**권장도**: ⭐⭐⭐ (UX polish, 출시 후 진행)

---

### 4. Rewarded Ad 추가 (스크립트 힌트 1회 무료)
**비용**: AdManager + UI flow + business logic 100+ 줄.
**효과**:
- 비-결제 사용자가 광고 1개 시청으로 통화 중 스크립트 힌트 1회 무료 사용
- 1) ARPU 상승 (Rewarded eCPM은 Interstitial 대비 1.5–2배), 2) 결제 conversion funnel ("아 이거 좋네 → 무제한은 결제")

**비즈니스 결정**:
- 너무 자주 무료 제공하면 결제 유인 감소
- 너무 적게 제공하면 효과 미미
- 적정선: "통화당 1회" 또는 "일 3회"

**권장도**: ⭐⭐⭐ (출시 후 1-2개월, 데이터 보고 진행)

---

### 5. (보류 P1 6건 — COMPREHENSIVE_QA_REPORT.md 참조)
- AdManager isPremium race
- 카운터 키 commit
- CrashReportManager 사용자 알림
- PhoneNumberUtil 자릿수 검증
- EjectAnalytics scenario_id PII (특히 중요)

---

## 🌍 호환성 — 구형 / 신형 / 미래

### 현재 정상

| 영역 | 상태 |
|---|---|
| Android 8.0 (API 26) ~ Android 15 (API 35) | ✅ minSdk=26, targetSdk=35 |
| MediaSession (API 21+) | ✅ |
| TelephonyCallback (API 31+) + PhoneStateListener fallback | ✅ |
| CameraManager.setTorchMode (API 23+, 권한 불요) | ✅ |
| FGS subtype 선언 (Android 14+) | ✅ |

### 미래 대응

| 시점 | 대응 |
|---|---|
| Android 16 (API 36) — 2026년 8월 예정 | targetSdk=36 업그레이드 (대략 2026년 9-10월) |
| Compose Material 4 — TBD | BOM 따라 자동 |
| Foldable 폼 팩터 | 가짜 통화 overlay가 접힘 상태에서 어떻게 보일지 별도 테스트 필요 |
| Tablet 최적화 | 현재 phone-first, 출시 후 사용자 데이터 보고 결정 |

---

## 💰 수익 창출 다음 단계 (출시 후 90일)

| 시점 | 액션 |
|---|---|
| 출시 D-day | Pre-registration 푸시 |
| D+7 | 카페·블라인드 등 한국 직장인 커뮤니티 본격 홍보 |
| D+14 | 첫 ARPU/Conversion 데이터 확인. Free trial 7d → 14d 실험? |
| D+30 | Annual plan ARPU 비중 확인. 가격 A/B 테스트 검토 |
| D+60 | Rewarded ad 도입 여부 결정 (2번 항목) |
| D+90 | Crashlytics 데이터 기반 v1.2.0 안정성 패치 plan |

---

## ⛔ 변경 불필요 (그대로 유지)

이전 권장에서 빼야 할 것들:
- minSdk 변경 (26 적정)
- Compose 메이저 (Material 4 출시 전엔 BOM 자동)
- targetSdk 변경 (Android 16 출시 전엔 35 유지)
- 새 권한 추가 (새 기능 없음)
- 새 언어 추가 (트래픽 보고 결정)
- FGS 변경 (이미 OK)
- Billing 모델 변경 (구독 잘 작동)

---

## 📋 사용자 결정 시트

진행하실 우선순위 정해주세요 (✅/❌/추후):

```
[ ] A. Subscription free trial 7일 추가 (Play Console UI, 5분)
[ ] B. Annual plan 추가 (Play Console UI, 5분)
[ ] C. Pre-registration 캠페인 켜기 (Play Console UI, 5분)
[ ] 1. Firebase Crashlytics 추가 (코드, 30분 + 새 빌드)
[ ] 2. Edge-to-Edge 적용 (코드 + 디자인 회귀, 1-2시간)
[ ] 3. Predictive Back Gesture (코드, 30분)
[ ] 4. Rewarded Ad 추가 (코드 + 비즈니스 디자인, 1일+)
[ ] P1-7 EjectAnalytics PII 점검 (Scenario.kt id 로직 확인 필요)
```

→ 결정한 것만 알려주시면 그것부터 자동 진행하겠습니다.
