package com.ejectbutton.service

import android.content.Context
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.Scenario
import com.ejectbutton.data.TriggerMode
import com.ejectbutton.data.defaultScenarios

/**
 * 사이드 버튼(또는 향후 다른 외부 트리거)이 발동했을 때
 * 현재 EjectPrefs 에 저장된 발신자/트리거 조합으로 가짜 전화를 시작.
 *
 * MainScreen 의 selectedScenario / selectedTrigger / customDelaySec 가
 * 변경될 때마다 EjectPrefs 에 동기화되어 있어야 한다.
 */
object SideButtonTrigger {

    fun fire(ctx: Context) {
        val scenario = resolveScenario(ctx)
        val delayMs = resolveDelayMs(ctx, scenario).coerceAtLeast(0L)

        CountdownBus.start(delayMs)
        FakeCallOverlayService.start(
            ctx,
            scenario.callerName,
            scenario.callerLabel,
            scenario.prompterHint,
            delayMs,
        )
    }

    private fun resolveScenario(ctx: Context): Scenario {
        val savedId = EjectPrefs.loadSelectedScenarioId(ctx)
        val all: List<Scenario> = defaultScenarios + EjectPrefs.loadScenarios(ctx)
        return savedId
            ?.let { id -> all.firstOrNull { it.id == id } }
            ?: all.first()
    }

    private fun resolveDelayMs(ctx: Context, scenario: Scenario): Long {
        // 시간 선택은 모드와 독립적으로 저장된다 (IMMEDIATE / AFTER_10S / CUSTOM).
        // 기존 사용자 폴백: KEY_SELECTED_TIME_CHOICE 가 비어 있으면 legacy trigger 를 해석한다.
        val timeChoice = EjectPrefs.loadSelectedTimeChoice(ctx)
        if (timeChoice != null) {
            return when (timeChoice) {
                "AFTER_10S" -> 10_000L
                "CUSTOM"    -> EjectPrefs.loadCustomDelaySec(ctx) * 1000L
                else        -> 0L // IMMEDIATE 또는 알 수 없는 값
            }
        }
        val triggerName = EjectPrefs.loadSelectedTrigger(ctx) ?: TriggerMode.IMMEDIATE.name
        val mode = runCatching { TriggerMode.valueOf(triggerName) }.getOrDefault(TriggerMode.IMMEDIATE)
        return when (mode) {
            TriggerMode.IMMEDIATE -> 0L
            TriggerMode.AFTER_10S -> 10_000L
            TriggerMode.AFTER_30S -> 30_000L
            TriggerMode.AFTER_1MIN -> 60_000L
            TriggerMode.SHAKE     -> 0L
            TriggerMode.SIDE_BUTTON -> 0L
            TriggerMode.CUSTOM    -> EjectPrefs.loadCustomDelaySec(ctx) * 1000L
        }
    }
}
