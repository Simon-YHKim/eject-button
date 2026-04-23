# Eject Button — 개발 환경 및 서비스 셋업 가이드

## 1. 로컬 빌드

### 사전 준비
1. **Android Studio** 설치 (https://developer.android.com/studio)
2. Android Studio 실행 → SDK Manager → **Android 14 (API 34)** 설치
3. 프로젝트 clone:
```bash
git clone https://github.com/Learner-thepoorman/eject-button.git
cd eject-button
```

### 디버그 빌드 (테스트용)
```bash
./gradlew assembleDebug
```
APK 위치: `app/build/outputs/apk/debug/app-debug.apk`

### 릴리즈 AAB 빌드 (Play Store 업로드용)
```bash
./gradlew bundleRelease
```
AAB 위치: `app/build/outputs/bundle/release/app-release.aab`

> **참고:** Android Studio에서 프로젝트를 열면 `local.properties`의 SDK 경로가 자동 설정됩니다.

---

## 2. 보안 설정

### keystore.properties (릴리즈 서명)
프로젝트 루트에 `keystore.properties` 생성. **이 파일은 `.gitignore` 로 제외되어
있으니 실제 값을 그대로 적어도 됩니다.** 예시 값은 placeholder 입니다 — 반드시
자신의 keystore 에서 발급한 비밀번호로 교체하세요:
```properties
storeFile=eject-upload.jks
storePassword=REPLACE_WITH_YOUR_STORE_PASSWORD
keyAlias=eject-upload
keyPassword=REPLACE_WITH_YOUR_KEY_PASSWORD
```

> ⚠️ 이전 가이드에 샘플 값이 실제 비밀번호처럼 보이는 문자열이었습니다.
> 만약 그 값을 그대로 쓰고 있다면 **지금 즉시 keystore 비밀번호를 회전** 하세요.
> CI 에서는 `RELEASE_KEYSTORE_PASSWORD` / `RELEASE_KEY_PASSWORD` 시크릿으로 주입됩니다.

### secrets.properties (API 키)
프로젝트 루트에 `secrets.properties` 생성. **release 빌드는 아래 3개 값이
비어있으면 빌드 실패합니다** (테스트 AdMob ID 로 출시되는 사고 방지용).
```properties
# MS Clarity (세션 기록)
CLARITY_PROJECT_ID=

# AdMob 실제 네이티브/인터스티셜 unit ID (필수)
ADMOB_NATIVE_ID=ca-app-pub-3230287048532628/XXXXXXXXXX
ADMOB_INTERSTITIAL_ID=ca-app-pub-3230287048532628/XXXXXXXXXX
```

debug 빌드는 미지정 시 Google test unit ID 로 fallback 해서 정상 동작합니다.

### AdMob 실제 키 발급 방법
1. https://admob.google.com 접속
2. **앱 추가** → Android → 패키지명: `com.ejectbutton.app`
3. **광고 단위 생성**:
   - 배너 광고 → ID 복사 → `ADMOB_BANNER_ID`에 입력
   - 전면 광고 → ID 복사 → `ADMOB_INTERSTITIAL_ID`에 입력
4. **AdMob App ID** 복사 → `AndroidManifest.xml`의 `com.google.android.gms.ads.APPLICATION_ID` 값 교체

> 두 파일 모두 `.gitignore`에 포함되어 GitHub에 업로드되지 않음.
> **중요:** `eject-upload.jks` 파일을 잃어버리면 앱 업데이트가 불가합니다. 안전한 곳에 백업하세요!

---

## 3. MS Clarity 설정 (사용자 행동 분석)

1. https://clarity.microsoft.com 접속
2. "New Project" 클릭
3. 앱 이름: "Eject Button", 카테고리: "Tools"
4. Project ID 복사
5. `secrets.properties`에 `CLARITY_PROJECT_ID=복사한ID` 입력
6. 앱 빌드 후 실행하면 자동으로 세션 수집 시작

### 주간 이메일 리포트 설정:
1. Clarity 대시보드 → Settings → Email Reports
2. "Weekly digest" 활성화
3. 수신 이메일: hwanydanh@gmail.com
4. 매주 월요일에 지난 주 세션/히트맵/인사이트 요약 수신

---

## 4. Google Analytics (Firebase) 설정

### 4-1. Firebase 프로젝트 생성
1. https://console.firebase.google.com 접속
2. "프로젝트 추가" → "Eject Button"
3. Google Analytics 활성화 (기본 설정 유지)

### 4-2. Android 앱 등록
1. 패키지 이름: `com.ejectbutton.app`
2. `google-services.json` 다운로드
3. `app/` 폴더에 복사

### 4-3. Gradle 설정 추가
`build.gradle.kts` (프로젝트 루트):
```kotlin
plugins {
    // 기존 플러그인...
    id("com.google.gms.google-services") version "4.4.2" apply false
}
```

`app/build.gradle.kts`:
```kotlin
plugins {
    // 기존 플러그인...
    id("com.google.gms.google-services")
}

dependencies {
    // 기존 의존성...
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
}
```

### 4-4. Crashlytics 이메일 알림
1. Firebase Console → Crashlytics → Settings
2. Email alerts 활성화 → hwanydanh@gmail.com
3. 크래시 발생 시 자동으로 이메일 수신

### 4-5. 주간 분석 리포트
1. Firebase Console → Analytics → Reports
2. "Schedule email" → Weekly → hwanydanh@gmail.com
3. 또는 https://analytics.google.com 에서 보고서 예약

---

## 5. 크래시 리포팅 (현재 구현)

현재 앱에 내장된 크래시 리포터:
- 크래시 발생 → `crash_reports/` 폴더에 로그 자동 저장
- 다음 앱 실행 시 → 이메일 전송 화면 자동 오픈 (hwanydanh@gmail.com)
- 포함 정보: 디바이스, Android 버전, 스택 트레이스, cause chain

Firebase Crashlytics 설정 후에는 두 시스템이 병행됨:
- 내장 리포터: 상세 로그 이메일 (개발자 직접 분석용)
- Crashlytics: 대시보드 + 통계 + 자동 알림 (트렌드 분석용)

---

## 6. GitHub에서 작업 이어가기

리포지토리: https://github.com/Learner-thepoorman/eject-button (private)

```bash
# 다른 PC에서 클론
git clone https://github.com/Learner-thepoorman/eject-button.git
cd eject-button

# keystore.properties, secrets.properties는 수동 생성 필요
```

Claude 웹에서 작업 시:
1. https://claude.ai/code 접속
2. GitHub 연동 후 `eject-button` 리포지토리 선택
3. 코드 수정 → git commit → git push

---

## 7. 인앱 결제 (Google Play Billing) 설정

### Play Console에서 인앱 상품 등록
1. Google Play Console → 앱 선택 → **수익 창출** → **인앱 상품**
2. **상품 만들기** 클릭
3. 상품 ID: `eject_premium`  (코드에 하드코딩되어 있으므로 정확히 입력)
4. 이름: "Eject Premium" / 설명: "광고 제거 + 무제한 커스텀 발신자"
5. 가격: $2.99 (한국 ₩3,900 / 일본 ¥400)
6. **활성화** 클릭

### 라이선스 테스터 설정 (결제 테스트)
1. Play Console → **설정** → **라이선스 테스트**
2. 테스트용 Gmail 주소 추가
3. 라이선스 응답: "RESPOND_NORMALLY"
4. 이 Gmail로 로그인한 기기에서 결제하면 실제 과금 없이 테스트 가능

---

## 8. Play Store 출시 체크리스트

### 필수 준비물
- [ ] Google Play Console 가입 ($25)
- [ ] `PLAY_STORE_ASO.md` 내용으로 스토어 리스팅 작성
- [ ] 스크린샷 5장 (메인, 수신화면, 통화화면, 설정, 발신자 설정)
- [ ] 512x512 앱 아이콘 업로드
- [ ] 1024x500 기능 그래픽 이미지
- [ ] 개인정보 처리방침 URL
- [ ] 콘텐츠 등급 질문지 작성
- [ ] 타겟 국가: 한국 → 일본 → 영어권

### 수익화 설정
- [ ] AdMob 계정 생성 → 앱 등록 → 광고 단위 생성
- [ ] `secrets.properties`에 실제 광고 ID 입력
- [ ] `AndroidManifest.xml`에 실제 AdMob App ID 입력
- [ ] Play Console에서 인앱 상품 `eject_premium` 등록 ($2.99)
- [ ] 라이선스 테스터로 결제 흐름 테스트

### 빌드 & 업로드
- [ ] `./gradlew bundleRelease`로 AAB 빌드
- [ ] Play Console → 내부 테스트 → AAB 업로드
- [ ] 테스트 완료 후 프로덕션으로 승격
- [ ] 단계적 출시 (10% → 50% → 100%)
