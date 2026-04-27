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

/**
 * v1.2 — 다국어 프리셋 시나리오. 현재 앱 언어에 맞춰 mom/dad 프리셋의 callerName/name 을
 * 매핑한다. defaultScenarios 의 mom/dad 프리셋은 한국어 "엄마"/"아빠" 가 하드코딩되어
 * 영어/일본어/스페인어 사용자에게 어색함. UI 레이어 (MainScreen, SideButtonTrigger)
 * 가 이 함수를 호출해 현재 strings 의 callerMom/callerDad 를 적용.
 *
 * 기존 inline 매핑 (`when (id) { "mom" -> ...; "dad" -> ... }`) 을 헬퍼로 추출 →
 * 같은 로직이 두세 곳에 흩어지던 중복 제거 + ScenarioRuntimeTest 로 단위 테스트 가능.
 */
fun localizedDefaultScenarios(strings: AppStrings): List<Scenario> {
    return defaultScenarios.map { scenario ->
        when (scenario.id) {
            "mom" -> scenario.copy(
                callerName = strings.callerMom,
                name       = strings.callerMom,
            )
            "dad" -> scenario.copy(
                callerName = strings.callerDad,
                name       = strings.callerDad,
            )
            else -> scenario
        }
    }
}

/**
 * v1.2 — 가짜 통화 시작 시점에 callerLabel 을 동적으로 결정.
 *
 * isRandomPhone=true (mom/dad 프리셋) 이면 매번 새 한국식 모바일 라벨을 생성해서
 * 동일 발신자라도 호출마다 다른 번호로 떠 가짜 통화 리얼리티를 유지.
 * 그 외 (사용자 정의 시나리오) 는 입력한 callerLabel 그대로 유지.
 */
fun Scenario.withRuntimeCallerLabel(): Scenario {
    return if (isRandomPhone) {
        copy(callerLabel = randomKoreanMobileLabel())
    } else {
        this
    }
}
