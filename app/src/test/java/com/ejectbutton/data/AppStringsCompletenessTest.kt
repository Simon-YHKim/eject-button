package com.ejectbutton.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.full.memberProperties

/**
 * v1.6.11 — 7개 로케일 [AppStrings] entries 가 모두 비어있지 않은지 reflection
 * 으로 검증. translation drift 방지 — 새 string field 가 추가되었을 때 일부
 * 로케일 entry 가 누락되거나 빈 문자열로 들어가면 사용자에게 빈 칸이 노출됨.
 *
 * data class 의 모든 String property 가 .isNotBlank() 인지 확인. enum 값
 * 추가/제거 시 자동으로 새 로케일도 검사 대상에 포함됨.
 */
class AppStringsCompletenessTest {

    private val stringProps = AppStrings::class.memberProperties
        .filter { it.returnType.classifier == String::class }

    @Test
    fun `every locale has every string field non-blank`() {
        val failures = mutableListOf<String>()
        for (lang in AppLanguage.values()) {
            val strings = lang.strings()
            for (prop in stringProps) {
                val value = prop.get(strings) as String
                if (value.isBlank()) {
                    failures += "${lang.name}.${prop.name} is blank"
                }
            }
        }
        assertTrue(
            "Blank string entries found:\n${failures.joinToString("\n")}",
            failures.isEmpty(),
        )
    }

    @Test
    fun `all seven locales present in stringsMap`() {
        for (lang in AppLanguage.values()) {
            assertTrue(
                "AppLanguage.${lang.name} missing from stringsMap",
                stringsMap.containsKey(lang),
            )
        }
    }

    @Test
    fun `v1_6_11 update strings present in every locale`() {
        // 명시적으로 v1.6.11 신규 필드를 명명해 회귀 시 친절한 에러 메시지.
        for (lang in AppLanguage.values()) {
            val strings = lang.strings()
            assertFalse(
                "${lang.name}.updateDownloadedMsg is blank",
                strings.updateDownloadedMsg.isBlank(),
            )
            assertFalse(
                "${lang.name}.updateRestartBtn is blank",
                strings.updateRestartBtn.isBlank(),
            )
        }
    }
}
