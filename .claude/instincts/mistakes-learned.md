# Claude 실수 학습 로그

> **목적**: Claude가 세션 중 저지른 실수를 누적 기록한다. 새 세션은 이 파일을 먼저 읽고 같은 실수를 반복하지 않도록 한다.
> **갱신 규칙**: 사용자 지적 → 즉시 append. 날짜 / 증상 / 원인 / 예방책 4필드 필수.
> **관리**: `/retro` 에서 주 1회 리뷰. 유효하지 않은 항목은 보존하되 `~~취소선~~` 처리 후 이유 기록.

---

## 템플릿

```
### YYYY-MM-DD — <한 줄 제목>
- **증상**: 무엇이 잘못됐나
- **원인**: 왜 그렇게 됐나 (근본 원인, 피상적 원인 금지)
- **예방책**: 다음에 같은 상황에서 해야 할 것
- **출처**: <세션 ID / 프로젝트명 / 파일>
```

---

## 로그

### 2026-04-12 — Skill 설치 시 SKILL.md 만 복사하고 runtime 누락
- **증상**: Gstack skill 36개 SKILL.md 를 복사했는데, 각 skill 이 참조하는 `bin/gstack-*` 헬퍼 스크립트가 없음 → 스킬 발동은 되지만 텔레메트리·learnings·config 기능 silent failure
- **원인**: 스킬 = 마크다운이라고만 생각해서 런타임 의존을 놓침. Gstack 은 `bin/`, `scripts/`, `package.json` 을 포함한 풀 트리가 런타임
- **예방책**: skill 설치 시 항상 (1) SKILL.md 내부에서 외부 경로 grep → `~/.claude/skills/<name>/bin`, `scripts/`, `lib/` 참조 여부 확인 (2) `package.json` 있으면 의존성 설치 (3) 설치 후 스모크 테스트로 실제 bin 호출
- **출처**: simon-stack v1 설치 세션

### 2026-04-12 — 검증 grep 패턴에 실제 시크릿 substring 포함
- **증상**: Plan 파일에 "키 누출 검사" 의 grep 명령을 예시로 적어뒀는데, 패턴에 실제 키 prefix 문자열이 들어가 파일 자체가 검증에 걸림
- **원인**: 검증 로직 = 데이터 가 헷갈림. 스크립트가 아닌 설명 문서에 패턴을 그대로 적으면 해당 문서가 "유출 문서" 가 됨
- **예방책**: 검증 패턴은 정규식만 (`AQ\.[A-Za-z0-9_-]{20,}`) 혹은 shell 변수로만 표현. 절대 리터럴 prefix 기록 금지
- **출처**: simon-stack v1 plan 파일

### 2026-05-19 — 빌드 실패에 추측 fix 5번 반복 (root cause 회피)
- **증상**: eject-button v1.7.x release-aab.yml 의 `testDebugUnitTest` 가 `java.lang.ClassFormatError` 로 fail. 5번 연속 fix 시도 (kotlin-reflect 2.0.21 → 2.0.0 → 제거 → test 재작성 → `by lazy` wrap) 모두 동일 에러로 실패. 사용자가 "근본 원인 확인해야 하는 거 아냐?" 지적 후에야 logs cascade 단서 (무관 `ScenarioRuntimeTest` 도 fail = `AppStrings` 클래스 본체 로드 실패) 분석 → **247-field `data class`** 가 자동 합성하는 `componentN/copy/equals/hashCode/toString` 메서드들이 JVM **method size 64KB / constant pool 65535 entry** 한도 초과 → ClassFormatError. `data` 키워드 제거 한 줄로 해결.
- **원인**: hypothesis chain ("kotlin-reflect 가 문제일 거다") 에 빠져 logs 의 결정적 단서 (무관 test cascade fail, `ClassLoader.java:-2`) 를 무시. 첫 추측 실패하면 같은 가설 변형으로 또 시도. 사용자가 명시적 지적 전까지 5번.
- **예방책**: 빌드 실패 logs 에서 cascade 패턴 발견 시 **즉시 추측 멈추고 의존성 그래프 거꾸로 추적**. (a) 무관한 test 까지 fail → 클래스 본체 / classpath 문제. (b) `ClassLoader.java:-2` → JVM 클래스 로딩 단 fail. (c) `ClassFormatError` detail 메시지 잘림 시 → 컴파일 산출물 자체 검사 (filed count, generated synthetic methods). 동일 hypothesis 변형 fix 2번 연속 실패 시 self-stop.
- **출처**: eject-button v1.7.2, PR #15 작업 사이클

### 2026-05-19 — 라이브러리 버전 추측 (Maven metadata 미확인)
- **증상**: Compose BOM 업그레이드 시 `2024.12.00` 으로 지정 (월별 발행 가정). 실제로 Maven 에 없는 phantom version → `Could not find androidx.compose:compose-bom:2024.12.00` 빌드 실패. `2024.12.01` 이 실제 published patch.
- **원인**: BOM 출시 패턴이 "매월 .00" 일 거라는 추측. Maven 실제 published 버전 확인 안 함.
- **예방책**: 외부 라이브러리 버전 변경 시 **`dl.google.com/dl/android/maven2/<group>/<artifact>/maven-metadata.xml`** 또는 maven central API 직접 조회. WebFetch 한 번이면 실제 verison list 확인 가능. 추측한 버전은 무조건 검증 후 사용.
- **출처**: eject-button v1.7.2 Compose BOM bump
