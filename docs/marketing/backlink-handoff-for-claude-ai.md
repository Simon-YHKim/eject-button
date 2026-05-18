# Handoff: Eject Button 백링크 작업 — claude.ai 에서 이어서 진행

이 문서는 cowork 모드에서 작성된 백링크 launch kit (`backlink-launch-kit.md`) 을 claude.ai 채팅창에서 이어 작업하기 위한 핸드오프 자료입니다. 첫 메시지로 이 문서를 통째로 붙여넣고, `backlink-launch-kit.md` 파일을 첨부하면 컨텍스트 한 번에 전달됩니다.

---

## 1. 작업 의도 (한 문장)

내가 만든 안드로이드 앱 **Eject Button (비상탈출 — 가짜 전화 알리바이)** 의 Play Store 페이지로 **자연스러운 외부 백링크** 를 유도하기 위해 6개 채널 (Product Hunt / Reddit r/introvert / Reddit r/socialanxiety / 디시인사이드 직장인갤 / 클리앙 / Indie Hackers + Hacker News) 에 게시할 글을 다듬고, 실제 게시 후 댓글/DM 응답 + 효과 측정까지 진행하고 싶다.

## 2. 앱 컨텍스트 (claude.ai 가 알아야 할 최소 정보)

- **이름**: 비상탈출 / Emergency Exit (Fake Call Alibi)
- **카테고리**: 안드로이드 앱 (Google Play Store only, iOS 없음)
- **패키지명**: `com.simonykim.ejectbutton`
- **Play Store URL**: `https://play.google.com/store/apps/details?id=com.simonykim.ejectbutton`
- **현재 버전**: v1.6.9 (2026-05-18 release)
- **개발자**: 솔로 개발자, 내향형 직장인 본인용으로 만들기 시작 → 같은 고민 있는 사람용으로 확장
- **핵심 기능**: 한 번 누르면 진짜 같은 가짜 통화 수신 화면이 뜨고, 어색한 자리에서 자연스럽게 빠져나올 수 있게 도와줌
- **트리거 3가지**: 화면 큰 빨간 버튼 / 폰 흔들기 / 측면 볼륨 버튼 (주머니 속에서도 발동)
- **언어**: 8개 (한/영/일/중-간/중-번/스/힌/포-BR)
- **수익모델**: 무료 + AdMob 배너 (메인) + Interstitial (통화 후) + 일회성 광고 제거 결제 (₩3,300) + 월 구독 (₩3,000)
- **명시적 비대상**: 사기/괴롭힘/속임수 — 앱 설명에 "엔터테인먼트/시뮬레이션 용도" 명확히 표기

## 3. 페르소나 (왜 백링크가 필요한가)

- 회식이 자꾸 길어지는 직장인
- 소개팅·미팅에서 빠져나갈 명분 필요한 사람
- 거절을 잘 못하는 마음 약한 사람
- 내향형(I) 성격, 사회생활이 버거운 사람
- "괜찮아요" 한마디가 입에서 안 나오는 사람
- 사회불안 (social anxiety) 가 있는 사람

이 페르소나가 모여있는 커뮤니티 = Reddit r/introvert, r/socialanxiety / 디시 직장인갤 / 클리앙 직장인 토픽 / Indie Hackers (개발자 빌더 스토리 angle).

## 4. 첨부 파일

**`backlink-launch-kit.md`** — 이미 작성된 6개 채널 게시글 초안 (Claude 가 작성한 1차 초안). 각 글은 본인 톤으로 한 번씩 손봐야 함. 이 파일을 claude.ai 채팅창에 함께 첨부할 것.

## 5. 발행 우선순위 + 일정

(launch-kit 8번 섹션과 동일, 여기서는 한 줄 요약)

| 주차 | 채널 | 진행 |
|---|---|---|
| 1주차 화요일 KST 1AM | **Product Hunt** | Maker 계정 + 앱 등록 + first comment, 24시간 댓글 응답 |
| 1주차 주말 | **r/introvert** | Karma 100+ 계정 필요. 응답 12시간 |
| 2주차 평일 | **디시 직장인갤** | 익명. 짤방 1장 첨부하면 노출 ↑ |
| 2주차 주말 | **클리앙 모임·새소식** | 클리앙 계정 + 기존 활동 약간 필요 |
| 3주차 | **r/socialanxiety** | r/introvert 반응 보고 톤 조정 |
| 3~4주차 | **Indie Hackers** | 게시 24시간 후 같은 글 Hacker News 에 "Show HN:" 으로 submit |

같은 글이 동시에 여러 채널 = spam 분류. **1~2주 간격 분산** 필수.

## 6. 채널별 deep dive (claude.ai 가 모를 수 있는 부분)

### Reddit r/introvert (200만)
- self-promo rule: 90/10 (자기 글 10% 이내). **본문에서 직접 링크 X**, 댓글에서 DM 또는 별도 답변으로 링크.
- 게시 best time: 미국 동부 오후 7~9시 (KST 화요일~목요일 오전 9~11시)
- 톤: "I made a thing for our people" — 솔직한 1인 개발자 톤, 광고 X
- ban 회피: 첫 글은 본인 경험 위주, 도구는 부차적

### Reddit r/socialanxiety (800K)
- **자기홍보가 가장 엄격한 sub**. self-promo flair 또는 mod approval 필요할 수 있음
- 톤: 공감 우선. "I have a coping mechanism" 처럼 자기 경험으로 시작
- 안전한 방법: 본문에 링크 0개 → 댓글 요청자에게만 DM 으로 전달
- 도구 추천 톤보다 "혹시 이런 경험 있으세요?" 톤 우선

### 디시인사이드 직장인 갤러리
- 디시체: "ㅋㅋ" / "ㅇㅇ" / "ㄱㄱ" / "함" / "~음" 어미. 존댓말 금지 (광고 인식 즉시)
- 익명 가능. 닉네임 없어도 게시 가능
- 짤방 1장 (앱 스크린샷 또는 회식 짤) 첨부하면 클릭률 ↑
- 광고 의심 댓글 자주 달림 → "1인 개발이고 광고 아님 ㅇㅇ" 솔직 답변
- 베스트 갤러리 진입하면 노출 ↑

### 클리앙 모임·새소식 게시판
- 디시와 정반대 톤: 존댓말 + 정중. IT 친화적, 1인 개발 응원 분위기
- 클리앙 회원 가입 + 기존 활동 (댓글 몇 개) 있어야 게시 권한 OK
- 광고 의심 댓글 거의 없음, 따뜻한 응원 많음
- 추천 받으면 메인 페이지 노출

### Product Hunt
- Maker 계정 필요 (Twitter/X 또는 LinkedIn 연동 권장)
- 화요일 KST 1AM = PST 일요일 5PM = 가장 트래픽 많은 슬롯
- 등록 후 24시간 안에 댓글 활발히 응답 → 랭킹 알고리즘이 응답 빈도 중시
- "Top 5 Product of the Day" 진입 시 Tech 매체가 픽업할 가능성 ↑ (BetaList, AppAdvice 등)

### Indie Hackers
- "Show IH:" 또는 "Building in public" 톤
- AdMob 정책 분쟁 같은 실제 빌딩 이슈 → 빌더 공감 + 자연스러운 백링크
- 게시 24시간 후 같은 글 Hacker News 에 "Show HN: I built a fake-call alibi for introverts (Android)" 으로 submit. **Title 에 "Show HN" 필수.**
- HN front page 진입하면 일시적 트래픽 폭증 (수만~수십만 visitor) → AdMob 수익 day spike

## 7. claude.ai 에게 부탁할 작업 패턴

다음 작업들을 새 채팅에서 claude.ai 에게 부탁할 수 있다:

### A. 게시 전 글 다듬기
- "첨부한 launch-kit 의 섹션 X 글을 내 톤으로 다듬어줘. 나는 [본인 특징 설명] 이야"
- "이 글에서 광고 같은 느낌 나는 부분 찾아서 자연스럽게 바꿔줘"
- "[특정 sub] 의 rule 5번 위반 가능성 있나 점검해줘"

### B. 댓글/DM 응답
- "Reddit 글에 이런 댓글 달렸어: [원문]. 어떻게 답할까?"
- "디시에서 '광고 아님? ㅋㅋ' 같은 댓글에 자연스럽게 답하는 톤 알려줘"
- "DM 으로 'iOS 는 안 되나요?' 물어왔어. 솔직하게 답하면서 다른 채널 가입 유도하는 방법?"

### C. A/B 변형
- "Product Hunt tagline 이거 3개 변형 만들어줘. 더 짧고 더 위트 있게"
- "Indie Hackers 제목 후보 5개 더 만들어줘. 각각 다른 angle"

### D. 효과 측정 + 다음 액션
- "Play Console 획득 보고서에서 'Reddit referrer' 가 50, 'Product Hunt referrer' 가 200 이야. 다음 어디에 더 투자할까?"
- "Indie Hackers 글 댓글 12개 / Reddit 글 댓글 3개. 어디가 더 좋은 신호인가?"
- "1주일 지났는데 다운로드 spike 없음. 원인 분석 + 다음 액션"

### E. 실패 케이스 대응
- "r/socialanxiety 글이 mod 가 삭제했어. 어떻게 했어야 했나?"
- "디시 글에 광고 의심 댓글 도배. 대응 방법?"
- "Product Hunt 댓글 0개. 죽은 launch 됐는데 살릴 방법?"

## 8. claude.ai 가 할 수 없는 것 (out of scope)

- Reddit / 디시 / 클리앙 / Product Hunt / Indie Hackers / HN 계정 가입·게시 (Simon 이 직접)
- 실시간 알람 (Slack/Discord 연동 필요)
- Play Console 직접 접근 (별도 도구 필요)
- AdMob/Firebase Analytics 데이터 자동 수집 (Simon 이 데이터 가져와서 붙여넣기)
- 게시글 자동 모니터링 (수동으로 댓글 확인 후 claude.ai 에게 물어봄)

## 9. 응답/감정 관리 (claude.ai 가 빠뜨리기 쉬운 부분)

백링크 launch 는 **24~72시간이 진짜 작업**. 게시 후 응답이 가장 중요:

- **첫 댓글 3시간 안에 응답** → 알고리즘이 글 노출 ↑
- **부정 댓글에 방어적 대응 X** → "good point, here's what I thought" 톤
- **DM 요청에는 30분~1시간 안에 응답** → 진짜 유저 conversion 확률 ↑
- **광고 의심 받으면 솔직히 인정 + 1인 개발 컨텍스트 제공** → 의외로 옹호자 생김

claude.ai 에게 "내가 지친다 / 답하기 싫다 / 부정 댓글 무섭다" 같은 감정도 솔직히 말해도 OK. 운영 페이스 조정에 도움.

## 10. 효과 측정 — 1주일 뒤 부터 확인할 지표

```
Play Console → 통계 → 획득
└─ 신규 사용자 source (organic / search / referral)
└─ "referral" 의 어느 도메인에서 왔는지

AdMob → 보고서 → 일별
└─ 노출 수 / eCPM 갑자기 오른 날 = 백링크 효과 일 (correlation)

Firebase Analytics (있으면) → 신규 사용자 → first_open referrer
└─ install referrer 헤더 (Play Store 가 자동 captures)

MS Clarity → 세션 → referrer 분포
└─ Play Store 에서 들어온 사용자가 어디서 referral 됐는지 (Play Store referrer 헤더로 추적)
```

수치를 claude.ai 에 가져와서 "다음 어디에 투자할지" 물어보면 다음 라운드 우선순위 조정 가능.

---

## 11. 마지막 — claude.ai 에게 처음 던질 메시지 템플릿

새 claude.ai 채팅 시작할 때 이렇게 시작:

> 안녕. 내가 만든 안드로이드 앱 "비상탈출" 의 백링크 작업을 cowork 모드에서 시작했는데, 여기서 이어서 진행하려고 해. 첨부한 핸드오프 문서 (`backlink-handoff-for-claude-ai.md`) 와 launch kit (`backlink-launch-kit.md`) 읽어줘. 다 읽으면 어떤 순서로 진행하면 좋을지 한 문단으로 요약해줘. 그 다음 [Product Hunt / Reddit / 디시 / 클리앙 / Indie Hackers] 중 어느 채널부터 다듬을지 내가 선택할게.

이 메시지 + 두 파일 첨부 = claude.ai 가 전체 컨텍스트 흡수 → 바로 작업 가능.

---

## 12. 메타

- 작성일: 2026-05-18 KST
- 작성자: cowork 모드 (Claude)
- 마지막 git commit: v1.6.9 (예정)
- 첨부 동반 파일: `backlink-launch-kit.md`
- 후속 turn 에서 추가할 수 있는 자료: 게시 후 실제 응답 로그, A/B 변형, 효과 측정 raw data
