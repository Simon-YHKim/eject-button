# LESSONS LEARNED — Eject Button Round 8+9 Push Session

> Session: 2026-05-19 | Branch: `claude/consolidate-chat-window-MustY`
> Context: 이전 세션에서 Round 9 기능 4개를 구현 완료 후, git push 503 에러로 코드를 원격에 올리지 못한 상태에서 이어받은 세션

---

## 1. 사용자 프로파일 분석

### 어떤 작업을 하려는 사람인가
- **Android 앱 개발자** (Jetpack Compose + Kotlin, 7개 언어 i18n, AdMob, IAP)
- **빠른 이터레이션** 선호: Round 1~9까지 연속적인 QA-fix 사이클을 돌림
- **결과물 중심**: 구현 → 빌드 → 테스트 → 배포의 흐름을 끊김 없이 진행하려 함
- **한국어 네이티브**: 지시는 한국어로, 코드/커밋은 영어로 — 이 경계를 존중할 것

### 어떤 경향의 사람인가
- **"계속 진행해"** 스타일: 세부 사항보다 전진을 우선시. 막히면 "나눠서 해"로 해법 제시
- **위임 후 검증**: AI에게 작업을 맡기고 결과만 확인하는 패턴
- **세션 연속성 중시**: 이전 세션의 맥락을 정확히 이어받기를 기대함
- **실용주의**: 완벽한 git history보다 "코드가 원격에 올라가는 것"이 우선

### 문제 인식 방법
- 에러 메시지를 직접 보고 판단하기보다, AI에게 원인 분석 + 해결을 일임
- "에러가 떴네? 작업량이 너무 많으면 나눠서 해" — 문제의 본질보다 우회로를 빠르게 찾는 성향
- 멀티태스킹: 여러 세션을 동시에 운영하며 "마지막으로 한 작업이 뭐야?"라고 물어봄

### 개선 방향
- 세션 시작 시 **핸드오프 문서**를 체계적으로 남기면 "마지막 작업이 뭐야?" 질문 자체가 불필요해짐
- `CLAUDE.md`에 **현재 브랜치/진행 상태** 섹션을 유지하면 세션 간 연속성 보장

---

## 2. 주로 놓치는 것들

### 2-1. 대형 바이너리의 git push 영향
- **현상**: `dist/eject-button-debug-round8.apk` (21MB)가 커밋에 포함되어 git push 전체를 503으로 만듦
- **놓친 이유**: 빌드 산출물을 `.gitignore`에 추가하기 전에 이미 커밋해버림
- **교훈**: APK/바이너리는 **절대 git에 커밋하지 말 것**. `.gitignore`를 먼저 설정하고, 이미 커밋된 바이너리는 `git rm --cached`로 즉시 제거

### 2-2. MCP push와 git push의 히스토리 분리
- **현상**: MCP API로 4개 파일을 `claude/sstack-qa-audit`에 푸시 → 로컬 git history와 완전히 분리됨
- **놓친 이유**: MCP push는 GitHub API를 직접 호출해서 커밋을 생성하므로, 로컬 git history와 무관한 별도 커밋 체인이 만들어짐
- **교훈**: MCP push 사용 시 로컬과 원격의 **히스토리가 영구적으로 분기**됨을 인지하고, 가능하면 한 가지 방법만 사용할 것

### 2-3. 제어 문자(U+001F, U+001E)의 MCP 전송 시 손상 위험
- **현상**: `EjectPrefs.kt`의 `F = ""`, `R = ""` 가 MCP push 과정에서 빈 문자열로 치환될 수 있음
- **교훈**: 제어 문자가 포함된 파일은 MCP push 후 반드시 원격 파일 내용을 검증할 것

### 2-4. 포크 레포의 MCP API 경로 혼동
- **현상**: `learner-thepoorman/eject-button`은 `Simon-YHKim/eject-button`의 포크. MCP API가 반환하는 URL에 부모 레포 경로가 나타나 혼동
- **교훈**: 포크 레포 작업 시 owner/repo를 항상 명시적으로 확인

---

## 3. 시행착오와 결론

### 시행착오 1: git push HTTP 503 해결 시도 (토큰 대량 소모)

| 시도 | 결과 | 토큰 낭비도 |
|------|------|------------|
| `git push` 기본 | 503 | 낮음 |
| `http.postBuffer=524288000` | 503 | 중간 |
| `http.version=HTTP/1.1` | 503 | 중간 |
| `--force-with-lease` | 503 | 중간 |
| 새 브랜치로 push | 503 | 중간 |
| soft reset + squash (APK 제거) | 503 | **높음** |
| soft reset + 소스만 커밋 (11 files) | 503 | **높음** |
| `GIT_CURL_VERBOSE=1` 진단 | `git-receive-pack POST`가 503 | 결론 도출 |

**결론**: proxy의 `git-receive-pack` 엔드포인트 자체가 503. GET은 정상, POST만 실패. git config로는 해결 불가.

**최적 경로 (회고)**: 
1. 첫 503 발생 시 `GIT_CURL_VERBOSE=1`로 즉시 진단 (GET vs POST 구분)
2. POST 503 확인 → **즉시 MCP API로 전환** (git push 재시도 중단)
3. 파일을 배치로 나눠 `push_files` 호출

→ 이 경로를 따랐다면 **시행착오 6회 × 30초 = 3분 + 수천 토큰 절약**

### 시행착오 2: 브랜치 혼동

| 문제 | 원인 | 해결 |
|------|------|------|
| `claude/sstack-qa-audit`에 MCP push 후 `claude/consolidate-chat-window-MustY`로 전환 요구 | 세션 지시서의 브랜치와 이전 세션의 작업 브랜치가 다름 | ff-merge로 통합 |
| 로컬 `sstack-qa-audit`과 원격 `sstack-qa-audit` 분기 | MCP push가 별도 커밋 체인 생성 | 포기하고 지정 브랜치로 이동 |

**결론**: 세션 시작 시 **지시서의 브랜치 이름을 최우선**으로 확인하고, 이전 세션 브랜치와 다르면 즉시 병합/체리픽 전략을 세울 것.

### 시행착오 3: 대형 파일 MCP push 전략

| 전략 | 결과 |
|------|------|
| 11개 파일 전체를 한 번에 push_files | 시도하지 않음 (페이로드 크기 우려) |
| 3-3-3-1-1 배치 분할 + 병렬 에이전트 | **성공** |

**결론**: 
- `push_files`는 작은 파일 3~5개씩 배치가 안전
- 대형 파일 (60KB+)은 `create_or_update_file`로 개별 푸시
- 배치 간 **순차 실행 불필요** — 각 배치가 branch tip을 자동으로 추적하므로 병렬 에이전트 활용 가능 (단, 동시에 같은 파일을 수정하면 충돌)

---

## 4. 다음 세션을 위한 최적화 체크리스트

### git push 503 발생 시 즉시 대응 프로토콜
```
1. GIT_CURL_VERBOSE=1 git push 2>&1 | grep "HTTP/1"
2. POST가 503이면 → git push 재시도 금지
3. MCP push_files로 전환:
   a. git diff --name-only origin/<branch>..HEAD 로 변경 파일 목록
   b. 파일 크기별 배치 분할 (소: 5개씩, 대: 1개씩)
   c. 병렬 에이전트로 배치 푸시
```

### 바이너리 관리
```
- APK/AAB는 절대 git add 하지 않음
- .gitignore에 /dist/ 가 있는지 첫 커밋 전에 확인
- 이미 커밋된 바이너리: git rm --cached → 새 커밋 → push
  (단, 이전 커밋에 바이너리가 남아있으면 pack이 여전히 큼 → 
   soft reset + 재커밋 또는 filter-repo 필요)
```

### 세션 핸드오프 템플릿
```markdown
## 현재 상태
- 브랜치: claude/xxx
- 원격 동기화: [완료/미완료]
- 미푸시 파일: [목록]

## 다음 할 일
1. ...

## 주의사항
- git push 503 발생 시 MCP 사용 (위 프로토콜 참조)
- EjectPrefs.kt의 / 제어 문자 보존 확인 필요
```

### 토큰 절약 패턴
| 상황 | 낭비 패턴 | 최적 패턴 |
|------|----------|----------|
| git push 실패 | 5+ 재시도 | 1회 실패 → verbose 진단 → MCP 전환 |
| 대형 파일 읽기 | Read로 4번 분할 읽기 | bash `cat` 또는 `wc -l`로 크기 확인 후 전략 결정 |
| 브랜치 혼동 | 잘못된 브랜치에서 작업 후 발견 | 세션 시작 시 지시서 브랜치 즉시 checkout |
| 세션 컨텍스트 복원 | 요약 재확인에 토큰 소모 | CLAUDE.md에 상태 섹션 유지 |

---

## 5. 이 세션의 최종 결과

| 항목 | 상태 |
|------|------|
| Round 8+9 소스 코드 11개 파일 | 원격 push 완료 |
| 브랜치 | `claude/consolidate-chat-window-MustY` |
| APK 빌드 | 로컬 `/dist/eject-button-debug-round8.apk` (원격 미포함) |
| .claude/ 벤더 파일 | 로컬 커밋 완료, 원격 미푸시 (503) |
| EjectPrefs.kt 제어 문자 | 검증 필요 (다음 세션에서 확인) |

---

*이 문서는 다음 세션에서 `/home/user/eject-button/LESSONS_LEARNED.md`로 참조 가능*
