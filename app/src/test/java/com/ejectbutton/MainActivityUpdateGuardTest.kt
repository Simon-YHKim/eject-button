package com.ejectbutton

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.6.11 — [MainActivity.shouldCompleteUpdate] 가드 회귀 방지.
 *
 * 이 predicate 가 잘못 되면 process kill (= Play Core completeUpdate) 이
 * active fake call / shake 중에 발생할 수 있고, 이는 이 앱의 핵심 위협 모델
 * (가해자가 옆에서 보는 상황에서 가짜 통화 환상 붕괴) 그 자체. 한 줄짜리
 * predicate 지만 회귀 비용이 매우 크므로 반드시 테스트.
 */
class MainActivityUpdateGuardTest {

    @Test
    fun `safe state allows completion`() {
        assertTrue(
            MainActivity.shouldCompleteUpdate(
                emergencyActive = false,
                alreadyCompleting = false,
            )
        )
    }

    @Test
    fun `emergency active blocks completion`() {
        // 가짜 통화 또는 shake 감지 진행 중에는 절대 process kill 금지.
        assertFalse(
            MainActivity.shouldCompleteUpdate(
                emergencyActive = true,
                alreadyCompleting = false,
            )
        )
    }

    @Test
    fun `already completing blocks completion`() {
        // 더블 탭 시 두 번째 호출 차단.
        assertFalse(
            MainActivity.shouldCompleteUpdate(
                emergencyActive = false,
                alreadyCompleting = true,
            )
        )
    }

    @Test
    fun `both flags block completion`() {
        assertFalse(
            MainActivity.shouldCompleteUpdate(
                emergencyActive = true,
                alreadyCompleting = true,
            )
        )
    }
}
