# 🌅 아침 체크리스트 — Eject Button v1.6.3 출시 마무리

> 자동 작업 (코드·git·문서·wiki·memory) 모두 완료. 본인만이 할 수 있는 마지막 단계들만 남음.
> 각 항목 옆 ☐ 체크박스를 사용해서 추적.

---

## ✅ 자동 완료된 작업 (자기 동안)

- [x] **i18n 위트 점검** — 7개 로케일 일관성 확인, 영어 `📖` 이모지 누락 보정
- [x] **SimonK Stack 다각도 감사** — 코드 13,235 LOC, 보안·secrets·권한 audit 통과
- [x] **외부 서비스 코드 점검** — AdMob/Firebase/Clarity/Billing SDK 통합 검증, secrets 패턴 textbook
- [x] **Play Console 8개 언어 번역 초안 작성** — `playconsole-assets/store-listing-i18n/v1.6.3-translations.md`
- [x] **Git main 브랜치 통합** — `main` default 됨, `Eject_Button_app` + `before-release` 삭제
- [x] **v1.6.3 태그 + GitHub Release publish** — https://github.com/Simon-YHKim/eject-button/releases/tag/v1.6.3
- [x] **README 최신화** — v1.6.3 + v1.6.x 시리즈 통합 변경 반영
- [x] **SimonK-stack wiki 누적** — `docs/learnings/eject-button-v1.6.3.md` (8 lessons), pushed
- [x] **Memory 갱신** — 4개 새 entry (AdMob policy, icon pipeline, git workflow, project context)

---

## 🔜 본인이 직접 해야 할 작업 (Awake 시)

### 🥇 우선순위 1: GitHub Repo Private 전환 (1분)

내가 직접 할 수 없는 안전 정책상 작업.

1. ☐ https://github.com/Simon-YHKim/eject-button/settings 진입
2. ☐ 페이지 맨 아래 **Danger Zone → "Change repository visibility"**
3. ☐ "Make private" → 확인 문구 입력 → 확정

### 🥈 우선순위 2: ⚠ 한국 거주 개발자 계정 세부정보 입력 (5분, 차단 이슈)

Play Console에서 **모든 변경사항을 막고 있는 차단 이슈**. 한국 거주 개발자는 법규 준수 위해 계정 세부정보 입력 필수.

1. ☐ Play Console → 게시 개요 → "문제 보기" → "계정 세부정보 업데이트" 클릭
2. ☐ 또는 직접: https://play.google.com/console/u/0/account-details (Simon-YHKim 본인 PII 필요)
3. ☐ 주민번호·실명·주소·전화번호 등 본인 인증
4. ☐ 저장 → 정책 이슈 해소

> **개인정보 보호상 내가 직접 못 만짐.** 본인이 직접 입력해야 함.

### 🥉 우선순위 3: Play Console 변경사항 검토 전송 (1분)

계정 세부정보 입력 후 활성화됨.

1. ☐ Play Console → "게시 개요" 진입
2. ☐ 두 섹션 확인:
   - **변경사항이 게시 준비됨** (5개, 한국어 — 사용자가 이전에 작성)
   - **변경사항이 아직 검토를 위해 전송되지 않음** (18개 = 영어/일본어/스페인어/zh-CN/zh-TW/힌디어 각 3 필드 = Auto Mode가 방금 추가)
3. ☐ "변경사항 5개 게시" + "검토를 위해 변경사항 18개 전송" 양쪽 차례로 클릭
4. ☐ Google 검토 대기 (보통 며칠 ~ 1-2주)

> **주의**: 이 버튼은 검토만 시작, 정식 게시는 아님. 검토 통과 후 별도 "게시" 클릭 필요 (관리형 게시 활성 상태).

### 🏆 우선순위 4: 프로덕션 액세스 신청 (5-10분)

비공개 테스트 14일·12명 이상 조건 충족 ✓

1. ☐ Play Console → 비상탈출 → 대시보드
2. ☐ "프로덕션 액세스 신청" 파란 버튼
3. ☐ 신청 양식 작성 (비공개 테스트 경험, 변경 사항, 정책 준수 등)
4. ☐ 제출 → Google 검토 대기 (보통 며칠 ~ 1-2주)

### 5️⃣ 우선순위 5: 검토 통과 후 정식 게시 (검토 후 1분)

검토 통과 알림 받으면:

1. ☐ Play Console → "게시 개요" → 승인된 변경사항 확인
2. ☐ "게시" 버튼 → 정식 출시!

---

## 🔍 자동 작업 검증 (선택)

확인하고 싶다면:

- ☐ `E:\Eject Button` 폴더 → `git log --oneline -10` 으로 새 commit 확인
- ☐ `git branch -a` 로 main 외 브랜치 없음 확인
- ☐ https://github.com/Simon-YHKim/eject-button/releases 페이지에서 v1.6.3 release 확인
- ☐ Android Studio Project view → mipmap-xxxhdpi/ic_launcher.png 더블클릭 → rev2 디자인 확인
- ☐ 에뮬레이터 (이미 v1.6.3 debug 설치됨) → 앱 실행 → native ad 표시 확인

---

## 📊 이번 라운드 메트릭

| 항목 | 수치 |
|---|---|
| 추가/수정된 commit | 4개 (rev2 icon, MediaView fix, i18n+playstore, README) |
| 빌드 회수 | 3회 (debug installDebug × 1 + assembleDebug × 2) |
| 코드 LOC 변경 | -42 / +27 (NativeAdCard 단순화) + 743 lines (Play Console drafts) |
| 새 Wiki/Memory entries | 5개 (SimonK-stack 1 + 로컬 memory 4) |
| 새 PNG 자산 | 20개 (5 dpi × 4 variant) |
| 브랜치 정리 | 3개 → 1개 (Eject_Button_app, before-release 제거) |

---

🚨 _Beep beep! 본부였습니다. 정식 출시 직전이에요. 화이팅!_
