package com.ejectbutton.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * v1.6.11 / v1.7.0 / v1.7.1 — 새 string fields 가 7-locale 전부 채워졌는지 검증.
 *
 * 이전 (v1.6.11) 버전은 kotlin-reflect 로 모든 String 필드를 자동 iterate 했으나,
 * project Kotlin compiler (2.0.0) 와 kotlin-reflect 의 binary mismatch 가 모든
 * test class 의 ClassFormatError 를 유발 → release-aab 빌드 차단. v1.7.1 에서
 * reflection 의존성 제거 + 새 fields explicit 검증으로 재작성. 새 string 추가 시
 * 이 파일에 한 줄 assertion 추가 (자동 검출 안 됨, 가독성 trade-off).
 *
 * 새 string 누락이 무엇인지 명확히 보이는 이점도 있음 — reflection 보다 fail
 * message 가 더 actionable.
 */
class AppStringsCompletenessTest {

    private val allLanguages = AppLanguage.values()

    @Test
    fun `all seven locales present in stringsMap`() {
        for (lang in allLanguages) {
            assertTrue(
                "AppLanguage.${lang.name} missing from stringsMap",
                stringsMap.containsKey(lang),
            )
        }
    }

    @Test
    fun `v1_6_11 update strings present in every locale`() {
        for (lang in allLanguages) {
            val s = lang.strings()
            assertFalse(
                "${lang.name}.updateDownloadedMsg is blank",
                s.updateDownloadedMsg.isBlank(),
            )
            assertFalse(
                "${lang.name}.updateRestartBtn is blank",
                s.updateRestartBtn.isBlank(),
            )
        }
    }

    @Test
    fun `v1_7_0 update strings present in every locale`() {
        for (lang in allLanguages) {
            val s = lang.strings()
            assertFalse(
                "${lang.name}.updateFailedMsg is blank",
                s.updateFailedMsg.isBlank(),
            )
        }
    }

    @Test
    fun `v1_7_1 billing strings present in every locale`() {
        for (lang in allLanguages) {
            val s = lang.strings()
            assertFalse(
                "${lang.name}.billingNotReadyMsg is blank",
                s.billingNotReadyMsg.isBlank(),
            )
            assertFalse(
                "${lang.name}.prTitle is blank",
                s.prTitle.isBlank(),
            )
            assertFalse(
                "${lang.name}.prMonthlyPrice is blank",
                s.prMonthlyPrice.isBlank(),
            )
        }
    }
}
