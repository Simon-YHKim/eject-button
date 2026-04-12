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

// 기본 발신자 프리셋
val defaultScenarios = listOf(
    Scenario(
        id = "mom",
        emoji = "👩",
        name = "엄마",
        callerName = "엄마",
        callerLabel = "휴대전화",
        preSmsText = "",
        prompterHint = "",
        urgency = Urgency.NORMAL,
    ),
    Scenario(
        id = "dad",
        emoji = "👨",
        name = "아빠",
        callerName = "아빠",
        callerLabel = "휴대전화",
        preSmsText = "",
        prompterHint = "",
        urgency = Urgency.NORMAL,
    ),
)
