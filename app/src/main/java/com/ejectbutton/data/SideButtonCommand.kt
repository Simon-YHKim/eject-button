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
    VOL_DOWN_TRIPLE;

    val isEnabled: Boolean get() = this != DISABLED
    val isVolumeUp: Boolean get() = this == VOL_UP_DOUBLE || this == VOL_UP_TRIPLE
    val isVolumeDown: Boolean get() = this == VOL_DOWN_DOUBLE || this == VOL_DOWN_TRIPLE
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
