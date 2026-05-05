package com.ejectbutton.ui.coachmark

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.5.2 — CoachmarkState 의 핵심 상태 전이 검증 (TDD).
 * SimonK-stack dev-orchestrator 흐름 중 "TDD (simon-tdd Guard)" 단계 충족.
 *
 * androidx.compose.runtime.mutableStateOf 가 unit test 환경에서 plain delegate 처럼
 * 동작하므로 (Snapshot 없이도 read/write OK) — Robolectric 없이 순수 JVM 테스트.
 * 만약 mutableStateOf 가 androidTest 환경 필요해서 빌드 실패하면 이 파일을
 * `app/src/androidTest/` 로 이동.
 */
class CoachmarkStateTest {

    @Test
    fun `start sets isActive=true and resets index to 0`() {
        val s = CoachmarkState()
        assertFalse("초기엔 비활성", s.isActive)
        assertEquals("초기 index = 0", 0, s.index)

        s.start()

        assertTrue("start 후 활성", s.isActive)
        assertEquals("start 후 index = 0", 0, s.index)
    }

    @Test
    fun `start after next resets index to 0`() {
        val s = CoachmarkState()
        s.start()
        s.next()
        s.next()
        assertEquals(2, s.index)

        s.start()
        assertEquals("재시작 시 index 0 으로 reset", 0, s.index)
    }

    @Test
    fun `next increments index`() {
        val s = CoachmarkState()
        s.start()
        s.next()
        assertEquals(1, s.index)
        s.next()
        s.next()
        assertEquals(3, s.index)
    }

    @Test
    fun `dismiss clears isActive`() {
        val s = CoachmarkState()
        s.start()
        s.dismiss()
        assertFalse(s.isActive)
    }

    @Test
    fun `register stores spot and spotFor returns it`() {
        val s = CoachmarkState()
        val rect = Rect(left = 10f, top = 20f, right = 110f, bottom = 80f)
        s.register("eject", rect, SpotShape.Circle)

        val spot = s.spotFor("eject")
        assertEquals(rect, spot?.rect)
        assertEquals(SpotShape.Circle, spot?.shape)
    }

    @Test
    fun `spotFor unknown id returns null`() {
        val s = CoachmarkState()
        assertNull(s.spotFor("nonexistent"))
    }

    @Test
    fun `register overwrites previous rect for same id`() {
        val s = CoachmarkState()
        val r1 = Rect(0f, 0f, 50f, 50f)
        val r2 = Rect(100f, 100f, 200f, 200f)

        s.register("scenario", r1, SpotShape.RoundRect)
        s.register("scenario", r2, SpotShape.RoundRect)

        assertEquals("최신 register 가 이전 좌표 덮어씀", r2, s.spotFor("scenario")?.rect)
    }

    @Test
    fun `multiple ids stored independently`() {
        val s = CoachmarkState()
        val rE = Rect(0f, 0f, 196f, 196f)
        val rS = Rect(50f, 0f, 90f, 40f)

        s.register("eject", rE, SpotShape.Circle)
        s.register("settings", rS, SpotShape.RoundRect)

        assertEquals(rE, s.spotFor("eject")?.rect)
        assertEquals(SpotShape.Circle, s.spotFor("eject")?.shape)
        assertEquals(rS, s.spotFor("settings")?.rect)
        assertEquals(SpotShape.RoundRect, s.spotFor("settings")?.shape)
    }
}
