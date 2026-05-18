# Eject Button — 개발 환경 및 서비스 셋업 가이드

## 1. 로컬 빌드

```bash
# 디버그 빌드
./gradlew assembleDebug

# 릴리즈 빌드 (keystore.properties 필요)
./gradlew assembleRelease
```

릴리즈 APK 위치: `app/build/outputs/apk/release/app-release.apk`

---

## 2. 보안 설정

### keystore.properties (릴리즈 서명)
프로젝트 루트에 `keystore.properties` 생성:
```properties
storeFile=../eject-button-release.jks
storePassword=YOUR_PASSWORD
keyAlias=eject-button
keyPassword=YOUR_PASSWORD
```

### secrets.properties (API 키)
프로젝트 루트에 `secrets.properties` 생성:
```properties
CLARITY_PROJECT_ID=your_clarity_project_id
```

> 두 파일 모두 `.gitignore`에 포함되어 GitHub에 업로드되지 않음.

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

## 7. Play Store 출시 체크리스트

- [ ] Google Play Console 가입 ($25)
- [ ] `PLAY_STORE_ASO.md` 내용으로 스토어 리스팅 작성
- [ ] 스크린샷 5장 (메인, 수신화면, 통화화면, 설정, 발신자 설정)
- [ ] 512x512 아이콘 업로드
- [ ] 1024x500 그래픽 이미지 제작
- [ ] 개인정보 처리방침 URL 필요
- [ ] 콘텐츠 등급 질문지 작성
- [ ] 타겟 국가: 한국, 일본, 영어권 우선
