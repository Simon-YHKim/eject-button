# Claude Code 자동 통합 프롬프트 — 통화 중 화면 (In-Call Screen V2)

아래 블록을 복사해서 Claude Code 에 붙여넣으세요. `InCallScreenV2.kt` 파일과 이 마크다운도 함께 레포 루트에 제공해 주세요.

---

## 📋 Claude Code 에 붙여넣을 프롬프트

```
이 레포의 통화 중(활성 통화) 화면을 One UI 8.5 스타일로 교체해줘.
`InCallScreenV2.kt` 와 `INTEGRATION_INCALL.md` 파일은 별도로 제공했어.

1. `InCallScreenV2.kt` 를
   `app/src/main/java/com/ejectbutton/ui/call/` 경로에 넣어줘.

2. `app/src/main/java/com/ejectbutton/data/AppStrings.kt` 의 AppStrings
   data class 에 아래 필드를 추가해줘. 이미 존재하는 키는 건너뛰어.

      val callAssist: String,       // 이미 있으면 skip (수신 화면 통합 때 추가됨)
      val mute: String,
      val speaker: String,
      val keypad: String,
      val bluetooth: String,
      val more: String,
      val record: String,
      val endCall: String,
      val transcribing: String,
      val callAssistActive: String,

3. 7개 언어 블록 각각에 아래 값을 추가해줘:

   en:
     mute = "Mute"
     speaker = "Speaker"
     keypad = "Keypad"
     bluetooth = "Bluetooth"
     more = "More"
     record = "Record"
     endCall = "End call"
     transcribing = "Transcribing…"
     callAssistActive = "Call Assist active"

   ko:
     mute = "내 소리 차단"
     speaker = "스피커"
     keypad = "키패드"
     bluetooth = "블루투스"
     more = "더 보기"
     record = "녹음"
     endCall = "통화 종료"
     transcribing = "텍스트로 변환 중…"
     callAssistActive = "통화 어시스트 사용 중"

   zhCN:
     mute = "静音"
     speaker = "扬声器"
     keypad = "拨号键盘"
     bluetooth = "蓝牙"
     more = "更多"
     record = "录音"
     endCall = "结束通话"
     transcribing = "正在转换为文本…"
     callAssistActive = "通话助手已启用"

   zhTW:
     mute = "靜音"
     speaker = "擴音"
     keypad = "撥號鍵盤"
     bluetooth = "藍牙"
     more = "更多"
     record = "錄音"
     endCall = "結束通話"
     transcribing = "正在轉換為文字…"
     callAssistActive = "通話助理使用中"

   ja:
     mute = "ミュート"
     speaker = "スピーカー"
     keypad = "キーパッド"
     bluetooth = "Bluetooth"
     more = "その他"
     record = "録音"
     endCall = "通話終了"
     transcribing = "テキスト変換中…"
     callAssistActive = "通話アシスタント使用中"

   es:
     mute = "Silenciar"
     speaker = "Altavoz"
     keypad = "Teclado"
     bluetooth = "Bluetooth"
     more = "Más"
     record = "Grabar"
     endCall = "Finalizar llamada"
     transcribing = "Transcribiendo…"
     callAssistActive = "Asistente de llamada activo"

   hi:
     mute = "म्यूट"
     speaker = "स्पीकर"
     keypad = "कीपैड"
     bluetooth = "ब्लूटूथ"
     more = "और"
     record = "रिकॉर्ड"
     endCall = "कॉल समाप्त करें"
     transcribing = "टेक्स्ट में बदल रहा है…"
     callAssistActive = "कॉल असिस्ट सक्रिय"

4. 기존 통화 중 화면 composable 을 찾아 InCallScreenV2 로 교체해줘.
   시그니처:

      InCallScreenV2(
          callerName = "...",
          callerLabel = "...",           // "폰 010-..." 또는 관계/번호 형식
          elapsedSeconds = ...,          // 통화 경과 시간 (초). 없으면 아래 helper 사용
          isRecording = true,
          statusSubtext = LocalAppStrings.current.transcribing,  // or .callAssistActive / null
          bluetoothDeviceName = "Galaxy Watch3\n(36A1)",          // or null
          onMute = { ... },
          onRecordingToggle = { ... },
          onSpeaker = { ... },
          onKeypad = { ... },
          onBluetooth = { ... },
          onMore = { ... },
          onAssist = { ... },
          onEndCall = { ... },
      )

   elapsedSeconds 가 필요한데 기존 ViewModel 에 없다면, 파일에 포함된
   `rememberCallTimer()` helper 를 써도 돼:

      val secs by rememberCallTimer(startSeconds = 2)
      InCallScreenV2(..., elapsedSeconds = secs, ...)

5. 아이콘 사용 — `material-icons-extended` 의존성이 이미 있어야 해.
   `app/build.gradle(.kts)` dependencies 에 없으면 추가해줘:

      implementation("androidx.compose.material:material-icons-extended")

6. 빌드 에러가 나면 직접 고쳐줘. 기존 통화 중 화면 파일은 아직 삭제하지 마.
```

---

## 📦 함께 전달할 파일
- `InCallScreenV2.kt`
- `INTEGRATION_INCALL.md`

## 🎯 HTML 프로토타입 대비 반영 사항
- 상태바 / 내비게이션 바 **비표시 (시스템 UI 가 그 자리에 표시되도록 공간만 확보)**
- 상단 중앙 📞 + 타이머 + 서브텍스트 (i18n)
- 우상단 🎥 비디오 (상시) + 🎤 녹음 중 녹색 뱃지
- 발신자 이름 32sp / Bold + 서브라벨 14sp
- AI 어시스트 플로팅 (우측 정렬, 컨트롤 상단)
- 3×2 컨트롤 — 녹음 버튼만 다크 스쿼클 + 녹색 아이콘, 나머지는 원형 반투명
- 녹음 중 라벨 = 경과시간, 꺼지면 "녹음" 문구
- 블루투스 라벨은 연결 기기명이 있으면 작게 2줄로 출력
- 60dp 빨간 원형 통화 종료 버튼
- 전체 복사본은 `LocalAppStrings` 에서 가져오므로 7개 언어 자동 반영

## 🧪 커스터마이징 포인트
- `RecordingTile` 내부 `Icons.Filled.Mic` 를 프로젝트의 `rec-icon-green.png` 로 교체하려면
  `painterResource(R.drawable.rec_icon_green)` + `Image(..)` 로 바꿔줘
- 그라데이션 컬러는 파일 상단 `InCallGradient` 에서 조정
- 시스템 UI 높이 예약은 `Column` 의 `padding(top = 48.dp, bottom = 36.dp)` 에서 조정
