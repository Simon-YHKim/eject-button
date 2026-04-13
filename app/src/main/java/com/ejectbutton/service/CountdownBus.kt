package com.ejectbutton.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * 가짜 전화 카운트다운 종료 시각(ms) 공유 버스.
 *
 * EJECT 탭 / SHAKE 감지 / SIDE_BUTTON 트리거 등 어떤 경로로 카운트다운이
 * 시작되든 MainScreen 의 UI 가 동일한 배너로 남은 시간을 표시하도록 한다.
 */
object CountdownBus {
    private val _endMs = MutableStateFlow(0L)
    val endMs: StateFlow<Long> = _endMs

    fun start(delayMs: Long) {
        _endMs.value = if (delayMs > 0L) System.currentTimeMillis() + delayMs else 0L
    }

    fun clear() {
        _endMs.value = 0L
    }
}
