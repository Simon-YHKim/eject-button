package com.ejectbutton.data

/**
 * 사이드 버튼(볼륨 키) 트리거 커맨드.
 *
 * 사용자가 설정에서 선택한 볼륨 키 패턴 — 이 패턴이 입력되면
 * 현재 EjectPrefs 에 저장된 발신자/트리거로 가짜 전화가 시작된다.
 *
 * - 동작 범위: 앱이 Foreground (Activity 가시) 또는 Background
 *   (ButtonWatchService 가 살아있는 상태) 일 때만.
 * - 화면 OFF / 앱 완전 종료 상태에서는 동작하지 않음.
 */
enum class SideButtonCommand {
    DISABLED,
    VOL_UP_DOUBLE,
    VOL_UP_TRIPLE,
    VOL_DOWN_DOUBLE,
    VOL_DOWN_TRIPLE,
    CUSTOM;          // 사용자 정의 시퀀스 (EjectPrefs 에서 동적 로드)

    val isEnabled: Boolean get() = this != DISABLED

    /** 프리셋이 볼륨 UP 만 사용하는지 여부. CUSTOM 은 동적 판정이므로 true. */
    val isVolumeUp: Boolean
        get() = this == VOL_UP_DOUBLE || this == VOL_UP_TRIPLE || this == CUSTOM

    /** 프리셋이 볼륨 DOWN 만 사용하는지 여부. CUSTOM 은 동적 판정이므로 true. */
    val isVolumeDown: Boolean
        get() = this == VOL_DOWN_DOUBLE || this == VOL_DOWN_TRIPLE || this == CUSTOM

    /** 2회/3회 등 고정 탭 수. CUSTOM 은 동적이므로 0. */
    val tapCount: Int
        get() = when (this) {
            VOL_UP_DOUBLE, VOL_DOWN_DOUBLE -> 2
            VOL_UP_TRIPLE, VOL_DOWN_TRIPLE -> 3
            else -> 0
        }

    companion object {
        fun fromName(name: String?): SideButtonCommand =
            entries.firstOrNull { it.name == name } ?: DISABLED
    }
}

/** 커스텀 시퀀스의 한 스텝 — 볼륨 UP 또는 DOWN. */
enum class SideButtonStep {
    UP, DOWN;

    companion object {
        /** "UP,DOWN,UP" 형식의 문자열을 파싱. 빈 문자열이면 빈 리스트. */
        fun parse(raw: String?): List<SideButtonStep> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(",").mapNotNull { token ->
                when (token.trim().uppercase()) {
                    "UP"   -> UP
                    "DOWN" -> DOWN
                    else   -> null
                }
            }
        }

        fun serialize(sequence: List<SideButtonStep>): String =
            sequence.joinToString(",") { it.name }
    }
}
