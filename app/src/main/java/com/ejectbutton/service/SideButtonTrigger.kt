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
        val delayMs = resolveDelayMs(ctx, scenario)

        if (delayMs == -1L) {
            // SHAKE 트리거는 사이드 버튼 흐름과 의미상 충돌 — 즉시 발신으로 폴백
            FakeCallOverlayService.start(
                ctx,
                scenario.callerName,
                scenario.callerLabel,
                scenario.prompterHint,
                0L,
            )
            return
        }

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
        val triggerName = EjectPrefs.loadSelectedTrigger(ctx) ?: TriggerMode.IMMEDIATE.name
        val mode = runCatching { TriggerMode.valueOf(triggerName) }.getOrDefault(TriggerMode.IMMEDIATE)
        return when (mode) {
            TriggerMode.IMMEDIATE -> 0L
            TriggerMode.AFTER_10S -> 10_000L
            TriggerMode.AFTER_30S -> 30_000L
            TriggerMode.AFTER_1MIN -> 60_000L
            TriggerMode.SHAKE     -> -1L
            TriggerMode.SIDE_BUTTON -> 0L
            TriggerMode.CUSTOM    -> EjectPrefs.loadCustomDelaySec(ctx) * 1000L
        }
    }
}
