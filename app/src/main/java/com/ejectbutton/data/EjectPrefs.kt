package com.ejectbutton.data

import android.content.Context

object EjectPrefs {
    private const val PREF = "eject_prefs"
    private const val KEY_SCENARIOS   = "custom_scenarios"
    private const val KEY_HISTORY     = "eject_history"
    private const val KEY_LANGUAGE    = "app_language"
    private const val KEY_RINGTONE    = "setting_ringtone"
    private const val KEY_VIBRATION   = "setting_vibration"
    private const val KEY_HAPTIC      = "setting_haptic"
    private const val KEY_FLASH       = "setting_flash"
    private const val KEY_EJECT_COUNT = "eject_count"
    private const val KEY_PREMIUM     = "is_premium"
    private const val F = "\u001F"
    private const val R = "\u001E"

    // ── Scenarios ─────────────────────────────────────────────────────────────

    fun saveScenarios(ctx: Context, scenarios: List<Scenario>) {
        val encoded = scenarios.joinToString(R) { s ->
            listOf(s.id, s.emoji, s.name, s.callerName, s.callerLabel,
                   s.preSmsText, s.prompterHint, s.urgency.name).joinToString(F)
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_SCENARIOS, encoded).apply()
    }

    fun loadScenarios(ctx: Context): List<Scenario> {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_SCENARIOS, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(R).mapNotNull { record ->
            val f = record.split(F)
            if (f.size < 8) return@mapNotNull null
            runCatching {
                Scenario(
                    id           = f[0],
                    emoji        = f[1],
                    name         = f[2],
                    callerName   = f[3],
                    callerLabel  = f[4],
                    preSmsText   = f[5],
                    prompterHint = f[6],
                    urgency      = Urgency.valueOf(f[7]),
                )
            }.getOrNull()
        }
    }

    // ── History ───────────────────────────────────────────────────────────────

    fun addHistory(ctx: Context, entry: String) {
        val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_HISTORY, "") ?: ""
        val updated = (listOf(entry) + existing.split(R).filter { it.isNotBlank() })
            .take(50)
            .joinToString(R)
        prefs.edit().putString(KEY_HISTORY, updated).apply()
    }

    fun loadHistory(ctx: Context): List<String> {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_HISTORY, "") ?: return emptyList()
        return raw.split(R).filter { it.isNotBlank() }
    }

    fun clearHistory(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY_HISTORY).apply()
    }

    // ── Language ──────────────────────────────────────────────────────────────

    fun saveLanguage(ctx: Context, code: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, code).apply()
    }

    fun loadLanguage(ctx: Context): AppLanguage =
        AppLanguage.fromCode(
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
        )

    // ── Feature Toggles ───────────────────────────────────────────────────────

    fun saveRingtone(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_RINGTONE, enabled).apply()
    }

    fun loadRingtone(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_RINGTONE, true)

    fun saveVibration(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_VIBRATION, enabled).apply()
    }

    fun loadVibration(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_VIBRATION, true)

    fun saveHaptic(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HAPTIC, enabled).apply()
    }

    fun loadHaptic(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAPTIC, true)

    fun saveFlash(ctx: Context, enabled: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FLASH, enabled).apply()
    }

    fun loadFlash(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_FLASH, false)

    // ── Eject count (인앱 리뷰 트리거용) ─────────────────────────────────────

    fun incrementEjectCount(ctx: Context): Int {
        val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_EJECT_COUNT, 0) + 1
        prefs.edit().putInt(KEY_EJECT_COUNT, next).apply()
        return next
    }

    fun getEjectCount(ctx: Context): Int =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt(KEY_EJECT_COUNT, 0)

    // ── Premium ──────────────────────────────────────────────────────────────

    fun savePremium(ctx: Context, premium: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PREMIUM, premium).apply()
    }

    fun loadPremium(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREMIUM, false)
}
