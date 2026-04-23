package com.ejectbutton.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * `randomKoreanMobileLabel` 에 대한 최소 단위 테스트.
 *
 * Round 30 — 엄마/아빠 프리셋의 `callerLabel` 을 매 통화마다 새로 만들기 위해 도입된
 * 유틸이라 (1) 포맷 (2) 분산성 두 가지만 확인한다.
 */
class PhoneNumberUtilTest {

    private val pattern = Regex("""^폰 010-\d{4}-\d{4}$""")

    @Test
    fun `formatMatchesPhonePattern100Iterations`() {
        repeat(100) {
            val label = randomKoreanMobileLabel()
            assertTrue(
                "expected '폰 010-XXXX-XXXX' pattern, got: $label",
                pattern.matches(label),
            )
        }
    }

    @Test
    fun `midAndTailSegmentsAvoidLeadingZeroBlock`() {
        // 범위가 1000..9999 이므로 4자리 앞자리가 "0000" 이 되는 경우는 없다.
        // (실제 통신사 번호처럼 보이게 하려는 의도적 제한)
        repeat(200) {
            val label = randomKoreanMobileLabel()
            val (_, _, mid, tail) = Regex("""^(폰) (010)-(\d{4})-(\d{4})$""")
                .find(label)!!
                .groupValues
                .let { listOf(it[1], it[2], it[3], it[4]) }
            assertTrue("mid=$mid tail=$tail 둘 다 1000 이상이어야 함",
                mid.toInt() in 1000..9999 && tail.toInt() in 1000..9999)
        }
    }

    @Test
    fun `fiftyCallsProduceReasonableDistribution`() {
        val labels = (1..50).map { randomKoreanMobileLabel() }.toSet()
        // 9000 * 9000 = 81M 가능 조합 중 50개 뽑아 중복은 극히 낮아야 함
        assertTrue(
            "50회 호출에 10개 이상 유일해야 함 — got: ${labels.size}",
            labels.size >= 10,
        )
    }

    @Test
    fun `seededRandomIsDeterministic`() {
        val a = randomKoreanMobileLabel(Random(42))
        val b = randomKoreanMobileLabel(Random(42))
        assertEquals("same seed → same output", a, b)
    }
}
