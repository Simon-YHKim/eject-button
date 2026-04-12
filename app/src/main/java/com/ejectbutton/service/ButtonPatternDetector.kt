package com.ejectbutton.service

import android.os.SystemClock
import com.ejectbutton.data.SideButtonCommand

/**
 * 볼륨 키 입력 시퀀스를 모아 사용자가 설정한 [SideButtonCommand] 와 일치하는지 판정.
 *
 * 사용 측이 [onVolumeUp] / [onVolumeDown] 을 호출하면 내부 큐에 타임스탬프가 추가되고,
 * 패턴이 일치하면 [onTrigger] 콜백이 호출된다.
 *
 * 시간 윈도우:
 * - 2회 패턴 → 700ms 이내
 * - 3회 패턴 → 1000ms 이내
 * 윈도우 밖의 오래된 입력은 자동으로 폐기된다.
 *
 * 매칭 성공 시 큐를 비워 즉시 재트리거되지 않도록 한다.
 */
class ButtonPatternDetector(
    @Volatile var command: SideButtonCommand,
    private val onTrigger: () -> Unit,
) {
    private val upTimestamps = ArrayDeque<Long>()
    private val downTimestamps = ArrayDeque<Long>()

    fun onVolumeUp() {
        if (!command.isEnabled || !command.isVolumeUp) return
        val now = SystemClock.elapsedRealtime()
        push(upTimestamps, now)
        if (matches(upTimestamps, command.tapCount)) {
            upTimestamps.clear()
            onTrigger()
        }
    }

    fun onVolumeDown() {
        if (!command.isEnabled || !command.isVolumeDown) return
        val now = SystemClock.elapsedRealtime()
        push(downTimestamps, now)
        if (matches(downTimestamps, command.tapCount)) {
            downTimestamps.clear()
            onTrigger()
        }
    }

    private fun push(queue: ArrayDeque<Long>, now: Long) {
        queue.addLast(now)
        // 오래된 항목 폐기 — 최대 윈도우(1초)보다 오래된 것은 의미 없음
        while (queue.isNotEmpty() && now - queue.first() > MAX_WINDOW_MS) {
            queue.removeFirst()
        }
    }

    private fun matches(queue: ArrayDeque<Long>, tapCount: Int): Boolean {
        if (tapCount < 2 || queue.size < tapCount) return false
        val window = if (tapCount == 2) DOUBLE_WINDOW_MS else TRIPLE_WINDOW_MS
        // 큐의 마지막 [tapCount] 개가 window 시간 안에 들어왔는지 확인
        val first = queue.elementAt(queue.size - tapCount)
        val last = queue.last()
        return (last - first) <= window
    }

    companion object {
        private const val DOUBLE_WINDOW_MS = 700L
        private const val TRIPLE_WINDOW_MS = 1000L
        private const val MAX_WINDOW_MS    = 1000L
    }
}
