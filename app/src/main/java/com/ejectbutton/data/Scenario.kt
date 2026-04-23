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
    /**
     * true 이면 표시 시점에 callerLabel 을 무시하고 매번 무작위 전화번호
     * ("폰 010-XXXX-XXXX") 를 생성해서 보여준다. 기본 프리셋(엄마·아빠) 에
     * 고정 번호가 박혀 있으면 매번 같은 번호로 떠서 가짜 전화 감수 리얼리티가
     * 떨어지기 때문에 Round 30 부터 프리셋은 랜덤화한다.
     *
     * 사용자가 직접 추가한 커스텀 호출 대상은 입력한 번호를 존중하므로 false 가 기본.
     */
    val isRandomPhone: Boolean = false,
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
        callerLabel = "",   // isRandomPhone=true 이므로 표시 시점에 동적 생성
        preSmsText = "",
        prompterHint = "",
        urgency = Urgency.NORMAL,
        isRandomPhone = true,
    ),
    Scenario(
        id = "dad",
        emoji = "👨",
        name = "아빠",
        callerName = "아빠",
        callerLabel = "",   // isRandomPhone=true
        preSmsText = "",
        prompterHint = "",
        urgency = Urgency.NORMAL,
        isRandomPhone = true,
    ),
)
