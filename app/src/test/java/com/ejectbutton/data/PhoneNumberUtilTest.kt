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

    // ── formatPhoneWithHyphens (Round 32) ────────────────────────────────────

    @Test
    fun `korean 11 digit gets 3-4-4 hyphens`() {
        assertEquals("010-2484-1120", formatPhoneWithHyphens("01024841120", "KR"))
    }

    @Test
    fun `korean with country code plus noise normalized`() {
        assertEquals("010-2484-1120", formatPhoneWithHyphens("+82 10-2484 1120", "KR"))
    }

    @Test
    fun `korean 10 digit falls back to 3-3-4`() {
        // 10 자리는 현실적으론 "02-XXXX-XXXX" 같은 패턴이 흔하지만 현재 구현은
        // 단순 3-3-4 로 fallback 한다. 사용 빈도가 낮은 legacy path 라 이렇게 유지.
        assertEquals("021-234-5678", formatPhoneWithHyphens("0212345678", "KR"))
    }

    @Test
    fun `us 10 digit gets 3-3-4`() {
        assertEquals("415-555-0123", formatPhoneWithHyphens("4155550123", "US"))
    }

    @Test
    fun `empty returns empty`() {
        assertEquals("", formatPhoneWithHyphens("", "KR"))
    }

    @Test
    fun `non-digit only returns trimmed`() {
        // 숫자 하나도 없으면 원본을 trim 해서 반환 — 망가뜨리지 않음 계약.
        assertEquals("---", formatPhoneWithHyphens(" --- ", "KR"))
    }

    @Test
    fun `unknown country falls back to 3-4-4 for 11 digits`() {
        assertEquals("010-2484-1120", formatPhoneWithHyphens("01024841120", "XX"))
    }
}
