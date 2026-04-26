package com.ejectbutton.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenarioRuntimeTest {

    @Test
    fun localizedDefaultsUseCurrentLanguageForPresetCallerNames() {
        val defaults = localizedDefaultScenarios(AppLanguage.ENGLISH.strings())

        assertEquals("Mom", defaults.first { it.id == "mom" }.callerName)
        assertEquals("Dad", defaults.first { it.id == "dad" }.callerName)
    }

    @Test
    fun randomPhonePresetGetsRuntimeCallerLabel() {
        val scenario = defaultScenarios.first { it.id == "mom" }.withRuntimeCallerLabel()

        assertTrue(scenario.callerLabel.contains("010-"))
        assertFalse(scenario.callerLabel.isBlank())
    }

    @Test
    fun customScenarioKeepsUserSuppliedCallerLabel() {
        val custom = Scenario(
            id = "custom",
            emoji = "*",
            name = "Safe Caller",
            callerName = "Safe Caller",
            callerLabel = "Mobile",
            preSmsText = "",
            prompterHint = "",
            urgency = Urgency.NORMAL,
            isRandomPhone = false,
        )

        assertEquals("Mobile", custom.withRuntimeCallerLabel().callerLabel)
    }
}
