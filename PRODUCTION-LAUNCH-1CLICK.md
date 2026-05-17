# 🚀 Production 정식 출시 — 1-Click Ready

> Production access 승인 알림 받으면 → 이 문서 따라 30초 ~ 5분 내 출시 가능
> 현재 상태: Production access 검토 중 (신청 2026-05-17 17:51, ETA ~5/24)

---

## ⚡ 가장 빠른 길 (10초 안에 trigger)

Production access 승인 이메일 받은 후, Claude에게 한 마디:

> **"production 출시 진행해줘"**

또는 영문:

> **"do production launch"**

내가 Chrome MCP로 자동 실행:
1. Play Console → 프로덕션 → 새 출시 만들기
2. App Bundle 라이브러리에서 **1072 (1.6.4)** 추가
3. Release notes 7개 언어 inject
4. 다음 → 저장 및 출시 → 다이얼로그 확인 → 최종 publish

→ 너는 결과 확인만 (5분 이내)

---

## 📦 미리 준비된 자료

### 1. AAB — 업로드 완료 ✅
- Play Console Library에 있음 (Internal Test에 이미 publish됨)
- **버전**: `1072 (1.6.4)`
- 서명: GitHub Actions production key (검증됨)
- 포함 변경:
  - v1.6.3: AdMob MediaView (광고 정책 통과), rev2 아이콘, 7개 언어 위트 정렬
  - v1.6.4: 사이드버튼 모드 스크롤 fix

### 2. Release Notes 7개 언어 ✅
- 위치: `playconsole-assets/release-notes/v1.6.3.md` (v1.6.4 fix 내용도 동일하게 사용 가능)
- 한국어 / English / 日本語 / 中文(简体) / 中文(繁體) / Español / हिन्दी
- 각 ≤500자 (Play Console 한도 내)

### 3. Description (스토어 등록정보) — Google 검토 중 ⏳
- 한국어: 이미 라이브 (사용자 작성분)
- 영어 / 일본어 / 스페인어 / 중국어 간체+번체 / 힌디어: 검토 중 (18개 변경사항)
- ETA: 며칠 내 통과 → 자동 게시 (관리형 게시 OFF 상태)

### 4. App content declarations ✅
- 전체 화면 인텐트: "통화 주고받기" + 사전 부여 "아니요" — 완료

---

## 🧭 수동 가이드 (Claude 없이 직접 할 경우 5분)

### Step 1: Play Console 진입
- URL: https://play.google.com/console/u/0/developers/4795577270086966748/app/4974211650203902764/tracks/production
- 또는: Play Console → 비상탈출 → 테스트 및 출시 → 프로덕션

### Step 2: 새 출시 만들기
- 우측 상단 **"새 출시 만들기"** 클릭 (잠금 해제됨 — production access 승인 후)

### Step 3: AAB 추가
- App Bundle 섹션 → **"라이브러리에서 추가"** 클릭
- 목록에서 **`1072 1.6.4`** 체크박스 ✓
- "버전에 추가" 클릭

### Step 4: 출시명 + 노트
- 출시명: 자동으로 `1072 (1.6.4)` 채워짐
- 출시 노트 텍스트 영역에 아래 복붙:

```xml
<ko-KR>
🚨 v1.6.4 정식 출시
• 광고 정책 준수 강화 — 광고 노출이 더 부드러워졌어요
• 앱 아이콘 디자인 개선 (rev2)
• 7개 언어 위트 문구 점검 — '본부' 톤 일관성
• 사이드 버튼 모드 스크롤 수정

삐뽀삐뽀! 본부였습니다.
</ko-KR>
<en-US>
🚨 v1.6.4 production release
• Tighter AdMob policy compliance — smoother ad rendering
• Refreshed app icon (rev2 design)
• Witty copy review across 7 locales
• Side button mode scroll fix

Beep beep! Emergency Exit HQ, over and out.
</en-US>
<ja-JP>
🚨 v1.6.4 正式リリース
• 広告ポリシー準拠を強化
• アプリアイコンをリフレッシュ (rev2)
• 7か国語のコピーを点検
• サイドボタンモードのスクロール修正

ピーポーピーポー！緊急脱出本部より。
</ja-JP>
<zh-CN>
🚨 v1.6.4 正式发布
• 广告政策合规强化
• 应用图标焕新 (rev2 设计)
• 7 国语言文案精修
• 侧键模式滚动修复

嘀嘀！紧急脱身总部完毕。
</zh-CN>
<zh-TW>
🚨 v1.6.4 正式發布
• 廣告政策合規強化
• 應用程式圖示煥新 (rev2 設計)
• 7 國語言文案精修
• 側鍵模式捲動修復

嘀嘀！緊急脫身總部完畢。
</zh-TW>
<es-ES>
🚨 Lanzamiento v1.6.4
• Cumplimiento de política AdMob reforzado
• Icono de la app renovado (diseño rev2)
• Revisión de copia ingeniosa en 7 idiomas
• Corrección del scroll en modo botón lateral

¡Bip bip! Cuartel Salida de Emergencia, corto.
</es-ES>
<hi-IN>
🚨 v1.6.4 आधिकारिक रिलीज़
• AdMob नीति अनुपालन सख्त
• ऐप आइकन रिफ्रेश (rev2 डिज़ाइन)
• 7 भाषाओं की कॉपी समीक्षा
• साइड बटन मोड स्क्रॉल फिक्स

बीप बीप! इजेक्ट बटन हेडक्वार्टर, ओवर एंड आउट।
</hi-IN>
```

### Step 5: 다음 → 저장 및 출시
- 페이지 우측 하단 **"다음"** 클릭 → "미리보기 및 확인" 페이지
- 확인 후 **"저장 및 출시"** (또는 "검토 보내기") 클릭
- 다이얼로그 확인 → 최종 publish 클릭

### Step 6: Google 검토 대기
- 검토 시간: 며칠 ~ 1-2주 (production은 internal/closed보다 엄격)
- 검토 통과 시 — 관리형 게시 OFF 상태라 **자동으로 전 세계 라이브** 🌍

---

## 🛑 만약 문제 발생 시

| 문제 | 해결 |
|---|---|
| Production 트랙이 여전히 잠김 | Access 검토 미완료 — 이메일 알림 다시 확인 |
| AAB 라이브러리에 1072 (1.6.4) 안 보임 | Internal/Closed test 트랙에서 promote — 아래 ⬇️ |
| "버전 코드 변경 필요" 오류 | versionCode 1072 이상 새로 빌드 (GitHub Actions tag push) |
| 검토 거부됨 | 거부 이메일 보고 정책 위반 항목 fix 후 재제출 |

### AAB Library에 안 보일 때 — Promote 경로
1. Play Console → 테스트 및 출시 → 내부 테스트 (현재 1072 1.6.4 라이브)
2. **"버전 승급"** 드롭다운 → 프로덕션 선택
3. Promote dialog → 확인 → production track으로 같은 AAB 자동 복사

---

## 📌 알아둘 것 (1-Click 후)

- **Closed test 트랙의 v1.6.3 검토 중** — production 출시되면 closed test 무관 (둘 다 그대로 작동)
- **6개 언어 등록정보 검토 중** — 통과 시 자동 게시 (관리형 게시 OFF). 이 검토와 production 검토는 별개
- **앱 아이콘 변경**은 store 페이지에 표시되는 데 24시간 정도 캐시 시간 걸릴 수 있음

---

🚨 _Beep beep! 본부였습니다. 1-click 대기 중._
