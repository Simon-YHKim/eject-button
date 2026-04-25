# 로컬에서 Release AAB 빌드하기 (Windows 11 + Android Studio)

> Play Store 업로드용 production AAB 를 로컬에서 직접 빌드하기 위한 단계별 가이드.
> 작성일: 2026-04-26
>
> 본 가이드는 `SETUP_GUIDE.md` 의 보완본입니다 (Windows PowerShell 기준 + 현재 상태 반영).
>
> **현재 상황**: 저장소 루트의 `app-release.aab` 는 구버전(`com.ejectbutton.app` / v1.0)이라 사용 불가.
> 새로 빌드해야 합니다.

---

## ⚠️ 사전 점검 (SETUP_GUIDE.md 의 불일치 항목)

`SETUP_GUIDE.md` 에 일부 옛 정보가 남아 있어 그대로 따르면 잘못된 키 발급으로 이어질 수 있습니다.
**아래 값을 우선합니다** (현재 코드 기준):

| 항목 | SETUP_GUIDE 표기 (옛) | 실제 (현재 코드) |
|---|---|---|
| applicationId | `com.ejectbutton.app` | **`com.simonykim.ejectbutton`** |
| GitHub URL | `Learner-thepoorman/eject-button` | **`Simon-YHKim/eject-button`** |
| AdMob 변수명 | `ADMOB_BANNER_ID` | **`ADMOB_NATIVE_ID`** (build.gradle.kts 기준) |

→ AdMob/Firebase 에 앱 등록 시 **반드시 `com.simonykim.ejectbutton`** 패키지명을 입력해야 합니다.

---

## 1단계: 사전 준비

### 1-1. Android Studio 설치
- https://developer.android.com/studio 에서 최신 버전 설치
- SDK Manager 에서 **Android 15 (API 35)** + **Build Tools 35.0.0** 설치

### 1-2. JDK 17 확인
```powershell
java -version   # "openjdk version 17.x.x" 가 나오면 OK
```
Android Studio 내장 JBR 을 쓰면 자동 해결됩니다.

### 1-3. 저장소 위치 확인
```powershell
cd "C:\Users\202502\OneDrive\문서\Claude\Projects\Eject Button"
git status  # working tree clean 인지 확인
git log --oneline -5
```

---

## 2단계: keystore 준비

### Case A: 처음 빌드 (keystore 신규 생성)

⚠️ **주의**: 새로 만든 keystore 는 분실 시 앱 업데이트가 영원히 불가합니다.
Play App Signing 을 사용하면 Play 가 실제 서명 키를 보관하므로 안전합니다.

```powershell
cd "C:\Users\202502\OneDrive\문서\Claude\Projects\Eject Button\app"

# JDK 의 keytool 로 새 keystore 생성 (대화형)
keytool -genkeypair -v `
  -keystore eject-upload.jks `
  -alias eject-upload `
  -keyalg RSA -keysize 2048 -validity 10000

# 입력 항목:
# - keystore 비밀번호 → 길고 무작위로 (1Password 등 비번 매니저에 저장)
# - 이름: Simon Kim
# - 조직단위: (빈칸 가능)
# - 조직명: (빈칸 가능)
# - 도시: Seoul
# - 시/도: Seoul
# - 국가코드(2자리): KR
```

생성된 `eject-upload.jks` 는 **`app/` 폴더 안**에 둡니다 (`.gitignore` 로 제외됨).

> 🔐 **백업 필수**: `eject-upload.jks` 를 외장 드라이브 + 클라우드 비밀 저장소(예: 1Password, Bitwarden) 양쪽에 사본 보관.

### Case B: 이미 keystore 있음
- 가지고 있는 `.jks` 파일을 `app/eject-upload.jks` 로 복사 (또는 `keystore.properties` 의 `storeFile` 경로를 실제 경로로 지정)

---

## 3단계: keystore.properties 작성

프로젝트 **루트** (`build.gradle.kts` 가 있는 폴더) 에 다음 파일 생성:

```powershell
@"
storeFile=eject-upload.jks
storePassword=실제_keystore_비밀번호
keyAlias=eject-upload
keyPassword=실제_key_비밀번호
"@ | Out-File -Encoding utf8 "C:\Users\202502\OneDrive\문서\Claude\Projects\Eject Button\keystore.properties"
```

> `storeFile` 경로는 `app/` 모듈 디렉토리 기준으로 해석됩니다. `eject-upload.jks` 라고만 쓰면 `app/eject-upload.jks` 를 가리킵니다.

✅ **확인**: `git status` 했을 때 `keystore.properties` 가 untracked 가 아니라 **표시조차 안 되어야** 합니다 (`.gitignore` 가 작동하는지 검증).

```powershell
cd "C:\Users\202502\OneDrive\문서\Claude\Projects\Eject Button"
git check-ignore keystore.properties   # 결과: keystore.properties (= ignored 됨)
```

---

## 4단계: secrets.properties 작성

```powershell
@"
CLARITY_PROJECT_ID=실제_clarity_project_id
ADMOB_NATIVE_ID=ca-app-pub-3230287048532628/실제_네이티브_광고_unit_id
ADMOB_INTERSTITIAL_ID=ca-app-pub-3230287048532628/실제_전면_광고_unit_id
"@ | Out-File -Encoding utf8 "C:\Users\202502\OneDrive\문서\Claude\Projects\Eject Button\secrets.properties"
```

### 광고 unit ID 발급 방법
1. https://admob.google.com 접속
2. **앱 추가** → Android → 패키지명: **`com.simonykim.ejectbutton`** ⚠️
3. **광고 단위 만들기** 에서 두 종류 발급:
   - Native advanced → ID 복사 → `ADMOB_NATIVE_ID`
   - Interstitial → ID 복사 → `ADMOB_INTERSTITIAL_ID`
4. AdMob 의 **App ID** (`ca-app-pub-XXXX~YYYY` 형식) 는 `app/src/main/AndroidManifest.xml` 의 `com.google.android.gms.ads.APPLICATION_ID` 값과 일치해야 합니다.

### Clarity Project ID 발급
1. https://clarity.microsoft.com → New Project → "Eject Button"
2. Tracking 탭에서 Project ID 복사

✅ **확인**:
```powershell
git check-ignore secrets.properties   # 결과: secrets.properties (= ignored 됨)
```

---

## 5단계: 빌드 실행

### 5-1. 디버그 빌드 먼저 (검증용)
```powershell
cd "C:\Users\202502\OneDrive\문서\Claude\Projects\Eject Button"
.\gradlew assembleDebug
# 산출물: app\build\outputs\apk\debug\app-debug.apk
```

성공하면 `adb install -r app\build\outputs\apk\debug\app-debug.apk` 로 디바이스에 설치해 동작 확인.

### 5-2. Release AAB 빌드
```powershell
.\gradlew bundleRelease `
  -PversionCodeOverride=1009 `
  -PversionNameOverride=1.0.8
# 산출물: app\build\outputs\bundle\release\app-release.aab
```

> **versionCode 결정 규칙**: CI 가 1000 + GITHUB_RUN_NUMBER 로 자동 부여하므로,
> 로컬 빌드는 그보다 작은 값이 충돌하지 않게 1009 정도부터 시작 (또는 그냥 999 등 안전한 값).
> Play Console 에 한 번 올라간 versionCode 보다 큰 값이어야만 다음 업로드 가능.

### 5-3. Release APK 도 (사이드로드 테스트용)
```powershell
.\gradlew assembleRelease `
  -PversionCodeOverride=1009 `
  -PversionNameOverride=1.0.8
# 산출물: app\build\outputs\apk\release\app-release.apk
```

---

## 6단계: 산출물 검증

### 6-1. AAB 내부 패키지/버전 확인
```powershell
# bundletool 다운로드: https://github.com/google/bundletool/releases
java -jar bundletool.jar dump manifest --bundle="app\build\outputs\bundle\release\app-release.aab" `
  | Select-String -Pattern "package=|versionCode|versionName"
# 기대값: package="com.simonykim.ejectbutton" versionCode="1009" versionName="1.0.8"
```

또는 Android Studio → Build → Analyze APK → AAB 선택 → Manifest 트리에서 확인.

### 6-2. 서명 검증
```powershell
# build-tools 의 apksigner 사용
$BT = "$env:LOCALAPPDATA\Android\Sdk\build-tools\35.0.0"
& "$BT\apksigner.bat" verify --print-certs `
  "app\build\outputs\apk\release\app-release.apk"
# 인증서 정보가 출력되면 OK
```

### 6-3. ProGuard mapping 백업
```powershell
# 향후 크래시 디스 obfuscate 용
Copy-Item "app\build\outputs\mapping\release\mapping.txt" `
  "..\backups\mapping-1.0.8.txt"
```

---

## 7단계: Play Console 업로드

1. https://play.google.com/console 접속
2. 앱 만들기 → 이름: "Eject Button — Escape Call"
3. **Internal testing** track 선택 → "새 버전 만들기"
4. AAB 업로드: `app\build\outputs\bundle\release\app-release.aab`
5. Release notes 입력 (한국어 / 영어 / 일본어)
6. 저장 → 검토 → 출시

전체 등록 절차는 `docs/play-console-questionnaire.md` 참고.

---

## 트러블슈팅

| 증상 | 원인 | 해결 |
|---|---|---|
| `error: Required secret 'CLARITY_PROJECT_ID' is missing` | release 빌드인데 secrets.properties 가 없거나 값이 비어있음 | secrets.properties 에 실제 값 입력 |
| `Keystore was tampered with, or password was incorrect` | keystore 비밀번호 오타 | keystore.properties 의 storePassword/keyPassword 재확인 |
| `versionCode XXX has already been used` | Play Console 에 동일 versionCode 가 이미 업로드됨 | `-PversionCodeOverride=` 값을 더 큰 숫자로 |
| `Build failed: minSdkVersion (26) > deviceSdkVersion` | 디바이스가 너무 옛날 (Android 7 이하) | Android 8.0+ 디바이스/에뮬레이터 사용 |
| AAB 가 `com.ejectbutton.app` 으로 빌드됨 | 옛 캐시 또는 build.gradle.kts 수정 안 됨 | `.\gradlew clean` 후 재빌드 |

---

## 보안 체크리스트 (커밋 전)

```powershell
cd "C:\Users\202502\OneDrive\문서\Claude\Projects\Eject Button"

# 1. ignored 파일이 추적되지 않았는지
git check-ignore keystore.properties secrets.properties *.jks
# 모두 출력되어야 OK

# 2. staged 파일에 시크릿 패턴이 없는지
git diff --cached | Select-String -Pattern "ghp_|sk_live_|AIza|ca-app-pub-\d+/\d{6,}"
# (출력 없으면 OK. ca-app-pub-3940256099942544/... 는 Google 테스트 ID 라 무시 가능)

# 3. 빌드 산출물(APK/AAB) 은 커밋하지 말 것
git status   # *.apk *.aab 가 untracked 면 OK
```
