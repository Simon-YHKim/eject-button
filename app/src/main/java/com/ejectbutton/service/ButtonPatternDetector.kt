package com.ejectbutton.service

import android.os.SystemClock
import com.ejectbutton.data.SideButtonCommand
import com.ejectbutton.data.SideButtonStep

/**
 * 볼륨 키 입력 시퀀스를 모아 사용자가 설정한 [SideButtonCommand] 와 일치하는지 판정.
 *
 * 사용 측이 [onVolumeUp] / [onVolumeDown] 을 호출하면 내부 큐에 타임스탬프가 추가되고,
 * 패턴이 일치하면 [onTrigger] 콜백이 호출된다.
 *
 * 시간 윈도우:
 * - 2회 패턴 → 700ms 이내
 * - 3회 패턴 → 1000ms 이내
 * - CUSTOM  → sequenceLength * 600ms (최소 1200ms, 최대 3000ms)
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

    /** CUSTOM 모드용 — (step, timestamp) 타임스탬프 큐. */
    private val customEvents = ArrayDeque<Pair<SideButtonStep, Long>>()

    /** CUSTOM 모드용 — 매칭 대상 시퀀스 (동적 로드). */
    @Volatile var customSequence: List<SideButtonStep> = emptyList()

    fun onVolumeUp() {
        if (!command.isEnabled) return
        if (command == SideButtonCommand.CUSTOM) {
            pushCustom(SideButtonStep.UP)
            return
        }
        if (!command.isVolumeUp) return
        val now = SystemClock.elapsedRealtime()
        push(upTimestamps, now)
        if (matches(upTimestamps, command.tapCount)) {
            upTimestamps.clear()
            onTrigger()
        }
    }

    fun onVolumeDown() {
        if (!command.isEnabled) return
        if (command == SideButtonCommand.CUSTOM) {
            pushCustom(SideButtonStep.DOWN)
            return
        }
        if (!command.isVolumeDown) return
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

    // ── CUSTOM mode ──────────────────────────────────────────────────────────

    private fun pushCustom(step: SideButtonStep) {
        val seq = customSequence
        if (seq.isEmpty()) return
        val now = SystemClock.elapsedRealtime()
        val window = customWindowMs(seq.size)

        customEvents.addLast(step to now)
        // 윈도우 밖은 폐기
        while (customEvents.isNotEmpty() && now - customEvents.first().second > window) {
            customEvents.removeFirst()
        }
        if (customMatches(seq, window)) {
            customEvents.clear()
            onTrigger()
        }
    }

    private fun customMatches(seq: List<SideButtonStep>, window: Long): Boolean {
        if (customEvents.size < seq.size) return false
        val startIdx = customEvents.size - seq.size
        for (i in seq.indices) {
            if (customEvents.elementAt(startIdx + i).first != seq[i]) return false
        }
        val first = customEvents.elementAt(startIdx).second
        val last = customEvents.last().second
        return (last - first) <= window
    }

    companion object {
        // 배경 모드에서는 ContentObserver 전달 지연 때문에 키 입력 간격이
        // 실제보다 크게 계측될 수 있어 원래 700ms/1000ms 는 너무 빡빡했다.
        // 1200ms / 1600ms 로 완화. 사람의 '빠른 더블탭' 은 여전히 이 범위 안.
        private const val DOUBLE_WINDOW_MS = 1200L
        private const val TRIPLE_WINDOW_MS = 1600L
        private const val MAX_WINDOW_MS    = 1600L

        private const val CUSTOM_STEP_MS     = 600L
        private const val CUSTOM_WINDOW_MIN  = 1200L
        private const val CUSTOM_WINDOW_MAX  = 3000L

        fun customWindowMs(sequenceLength: Int): Long =
            (sequenceLength * CUSTOM_STEP_MS)
                .coerceIn(CUSTOM_WINDOW_MIN, CUSTOM_WINDOW_MAX)
    }
}
