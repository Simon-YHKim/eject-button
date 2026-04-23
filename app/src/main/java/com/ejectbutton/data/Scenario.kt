package com.ejectbutton.data

data class Scenario(
    val id: String,
    val emoji: String,
    val name: String,           // 화면에 표시되는 시나리오 이름 (예: "회식")
    val callerName: String,     // 가짜 발신자 이름 (예: "엄마")
    val callerLabel: String,    // 발신자 부제목 (예: "휴대전화")
    val preSmsText: String,     // 전화 전 문자 내용
    val prompterHint: String,   // 통화 중 내가 할 말
    val urgency: Urgency,
)

enum class Urgency { NORMAL, URGENT, PANIC }

enum class TriggerMode {
    IMMEDIATE,   // 즉시
    AFTER_10S,   // 10초 후
    AFTER_30S,   // 30초 후
    AFTER_1MIN,  // 1분 후
    SHAKE,       // 흔들기
    SIDE_BUTTON, // 사이드 버튼 (볼륨 키)
    CUSTOM,      // 사용자 지정 지연
}

// 기본 발신자 프리셋 — Round 22
// callerLabel 은 삼성 One UI 8.5 실제 통화 화면의 "폰 010-XXXX-XXXX" 포맷으로
// 통일해서 in-call 레이아웃이 디자인 레퍼런스와 동일하게 보이도록 한다.
// 예전에는 "휴대전화" (기기 타입 라벨) 이었는데 실제 갤럭시 레퍼런스 샷은
// 저장된 연락처라도 번호를 함께 보여주는 케이스가 대부분이라 그 포맷으로 바꿨다.
val defaultScenarios = listOf(
    Scenario(
        id = "mom",
        emoji = "👩",
        name = "엄마",
        callerName = "엄마",
        callerLabel = "폰 010-2484-1120",
        preSmsText = "",
        prompterHint = "",
        urgency = Urgency.NORMAL,
    ),
    Scenario(
        id = "dad",
        emoji = "👨",
        name = "아빠",
        callerName = "아빠",
        callerLabel = "폰 010-1234-5678",
        preSmsText = "",
        prompterHint = "",
        urgency = Urgency.NORMAL,
    ),
)
