# v1.0.10 자율 작업 인계 (사용자 자는 동안)

> 작성: 2026-04-26 새벽 / 자율 진행 결과 / Push만 사용자 1줄로 종료

---

## ✅ 자동 적용된 변경 3개 (4개 파일)

### 1. `MainScreen.kt:770` — Locale.US 명시
```kotlin
// before
String.format(strings.countdownFmt, countdown)
// after
String.format(java.util.Locale.US, strings.countdownFmt, countdown)
```
**이유**: 기본 로케일이 적용되면 일부 환경 (Arabic / Hindi / Persian numeral systems) 에서 카운트다운 숫자가 비-ASCII로 출력될 수 있음. 의도상 항상 0-9 ASCII.

### 2. `EjectPrefs.kt` — `savePermsRequested` / `loadPermsRequested` 함수 추가
새 키 `KEY_PERMS_REQUESTED = "perms_requested"` (기존 raw 키와 동일 → **마이그레이션 0**).

### 3. `MainActivity.kt:97-99` — raw SharedPreferences → EjectPrefs 호출
```kotlin
// before
val prefs = getSharedPreferences("eject_prefs", MODE_PRIVATE)
if (prefs.getBoolean("perms_requested", false)) return
prefs.edit().putBoolean("perms_requested", true).apply()
// after
if (EjectPrefs.loadPermsRequested(this)) return
EjectPrefs.savePermsRequested(this, true)
```
**이유**: 캡슐화 일관성. 다른 설정값은 모두 EjectPrefs를 거치는데 이것만 raw였음.

### 4. `ShakeDetectionService.kt:onDestroy` — NPE 가드
```kotlin
override fun onDestroy() {
    if (::sensorManager.isInitialized) {
        sensorManager.unregisterListener(this)
    }
    super.onDestroy()
}
```
**이유**: onCreate가 실패한 채로 onDestroy가 호출되는 edge case (저사양 기기 메모리 압박)에서 `UninitializedPropertyAccessException` 방지.

---

## 🚦 의도적으로 보류한 항목 3개

| 항목 | 사유 |
|---|---|
| **ButtonWatchService main-thread setStreamVolume** (Samsung ANR 가능성) | 재현 사례 없음 + worker thread로 옮기면 echo-suppression 로직이 깨질 위험. 추후 재현되면 별도 작업. |
| **BillingManager startConnection 무한 재시도 → backoff** | Handler/Coroutine scope 도입 필요 → patch 범위 초과. v1.1.0 후보. |
| **BillingManager initial isPremium read on main thread** | `Application.onCreate` 에서 prewarm 하는 게 깔끔 → v1.1.0 후보. |

---

## 🎯 사용자 — 이거 한 줄 (push + tag → CI 자동 빌드)

```powershell
cd "C:\Users\202502\OneDrive\문서\Claude\Projects\Eject Button"; Get-ChildItem ".git" -Filter "*.lock" -Recurse | Remove-Item -Force -ErrorAction SilentlyContinue; git add app/src/main/java/com/ejectbutton/ui/main/MainScreen.kt app/src/main/java/com/ejectbutton/data/EjectPrefs.kt app/src/main/java/com/ejectbutton/MainActivity.kt app/src/main/java/com/ejectbutton/service/ShakeDetectionService.kt; git commit -m "feat(v1.0.10): locale, perms encapsulation, NPE guard"; git push origin Eject_Button_app; git tag v1.0.10; git push origin v1.0.10
```

→ tag push 시 GitHub Actions `release-aab.yml` 자동 트리거 → 약 5–10분 후 새 AAB 발급.

---

## 📊 v1.0.10 fastlane 출시 노트 (Internal Testing 시 입력용)

**`<ko-KR>`**:
```
v1.0.10 — 작은 안정성 개선
- 카운트다운 숫자 표기 안정화 (모든 언어에서 ASCII 0-9 보장)
- 권한 요청 플래그 캡슐화 (내부 리팩터)
- 흔들기 서비스 종료 시 드물게 발생하던 크래시 방지
```

**`<en-US>`**:
```
v1.0.10 — Small stability improvements
- Countdown numerals now render as ASCII 0-9 in every locale
- Permission-request flag refactored into EjectPrefs (internal cleanup)
- Prevent rare crash in shake-detection service teardown
```

**`<ja-JP>`**:
```
v1.0.10 — 小さな安定性改善
- カウントダウン数字を全ロケールで ASCII 0-9 表記に統一
- 権限リクエストフラグを EjectPrefs に集約 (内部リファクタ)
- まれに発生していたシェイクサービス終了時のクラッシュを防止
```

(다른 언어는 v1.0.9 노트에서 핵심만 짧게 변환해 사용)

---

## 🔍 점검 결과 요약

**simon-stack 후속 deep dive 결과**:
- 🟢 v1.0.9의 6개 변경 모두 회귀 없이 정상 동작 확인
- 🟢 TODO/FIXME/HACK/XXX 주석 0건 (코드 위생 우수)
- 🟡 5개 권장 항목 중 3개 안전 → 자동 적용
- ⏸ 2개 권장 항목 + 신규 발견 1개 → 보류 (위 표 참조)

**v1.0.10 = patch 등급**. 사용자 체감 변화 거의 없음, 안정성·코드 일관성 개선.

---

## 다음 단계 (사용자 일어난 후)

1. 위 PowerShell 한 줄 실행 → CI 빌드 트리거
2. 5–10분 대기 → GitHub Releases에 새 AAB 발급
3. Play Console v1.0.9 검토 결과 확인:
   - ✅ 통과 → v1.0.10 AAB로 Internal Testing 출시 (v1.0.9는 스킵 가능)
   - ❌ 반려 → 메일 캡처해서 채팅에 올림
4. Internal Testing 실기기 설치 → 스모크 테스트 (특히 Firebase Analytics 이벤트 수신 확인 — mistakes-learned.md 패턴)
