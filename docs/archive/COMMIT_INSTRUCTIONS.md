# 이번 세션 변경 사항 커밋 가이드 (PowerShell 전용)

> 작성일: 2026-04-26
> 변경 파일: 6개 신규 (수정 0개 — 줄바꿈 가짜 modified 무시)

---

## 새로 추가된 파일

| 경로 | 설명 |
|---|---|
| `PRE_RELEASE_AUDIT.md` | 출시 전 점검 리포트 (이번 세션 핵심 산출물) |
| `docs/local-release-build-guide.md` | Windows 로컬 release AAB 빌드 단계별 가이드 |
| `docs/play-console-questionnaire.md` | Play Console 등록 설문 답변 시트 |
| `fastlane/metadata/android/zh-CN/` | 중국어 간체 4개 파일 (title/short/full/changelog) |
| `fastlane/metadata/android/es-ES/` | 스페인어 4개 파일 |
| `fastlane/metadata/android/hi-IN/` | 힌디어 4개 파일 |

---

## ⚠️ 가짜 "modified" 파일들 무시하는 이유

Windows에서 `git clone` 시 `core.autocrlf=true` 기본값이 적용되어 텍스트 파일이 LF → CRLF 로 자동 변환됩니다. WSL/Linux 도구로 보면 100+ 파일이 "modified" 로 표시되지만 **실제 콘텐츠는 동일**합니다. 그대로 add 하면 130개 파일을 변경 이력에 남기게 되므로 **반드시 새 파일만 명시적으로 add 해야 합니다**.

---

## PowerShell 실행 단계

```powershell
cd "C:\Users\202502\OneDrive\문서\Claude\Projects\Eject Button"

# 1) 현재 상태 확인 — Untracked files 6개만 의미 있음
git status

# 2) 새 파일만 명시적으로 add (modified 무시)
git add PRE_RELEASE_AUDIT.md
git add docs/local-release-build-guide.md
git add docs/play-console-questionnaire.md
git add fastlane/metadata/android/zh-CN/
git add fastlane/metadata/android/es-ES/
git add fastlane/metadata/android/hi-IN/

# 3) staged 내용 확인
git diff --cached --stat
# → 약 14개 파일 (점검 리포트 1, 가이드 2, 다국어 metadata 3*4=12) ※ 일부 폴더는 4개씩

# 4) 시크릿 누출 검사 (반드시 실행)
git diff --cached | Select-String -Pattern "ghp_|sk_live_|sk_test_|AIza[A-Za-z0-9_-]{20,}"
# → 출력이 없으면 안전. 있으면 절대 커밋하지 말고 alert.
# (ca-app-pub-3940256099942544/... 라는 Google 공식 테스트 ID 는 무시 가능)

# 5) Conventional Commits 형식으로 커밋
git commit -m "docs(play-store): add release readiness audit + multi-lang metadata + build guide" `
           -m "" `
           -m "- PRE_RELEASE_AUDIT.md: 출시 전 종합 점검 리포트 (B1: 구버전 AAB / B2: 스크린샷 누락 등)" `
           -m "- docs/local-release-build-guide.md: Windows 로컬 release AAB 빌드 단계별 가이드" `
           -m "- docs/play-console-questionnaire.md: Play Console 등록 설문 답변 시트 (광고/COPPA/콘텐츠 등급 등)" `
           -m "- fastlane/metadata/android/{zh-CN,es-ES,hi-IN}/: 다국어 store listing 추가 (title/short/full/changelog)" `
           -m "" `
           -m "Refs: PLAY_STORE_ASO.md (다국어 카피 출처)"

# 6) push (확인 후 진행)
git push origin Eject_Button_app
```

---

## 푸시 후 확인할 것

1. GitHub → 저장소 → Files 탭에서 새 폴더(zh-CN/es-ES/hi-IN) 확인
2. Cloudflare Workers (eject-button) — `docs/` 변경은 자동 배포되므로 https://eject-button.hwanydanh.workers.dev/ 접속해 새 docs 가 떠 있는지 확인
3. (선택) GitHub Releases 페이지에서 release-aab 워크플로우 수동 실행 → 새 production AAB 다운로드

---

## 만약 시크릿 검사에 무언가 걸렸다면 (긴급)

```powershell
# 1) staged 해제
git reset

# 2) 누출된 파일 정정 후 다시 add
# (CRITICAL: 이미 git history 에 들어간 시크릿은 BFG Repo-Cleaner 또는
#  git filter-repo 로 history 재작성 필요. 그리고 즉시 키 회전.)
```
