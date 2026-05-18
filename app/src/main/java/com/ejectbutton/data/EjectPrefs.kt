package com.ejectbutton.data

import android.content.Context
import java.util.Locale

enum class ThemeMode { LIGHT, DARK, SYSTEM }

object EjectPrefs {
    private const val PREF = "eject_prefs"
    private const val KEY_THEME_MODE  = "theme_mode"
    private const val KEY_SCENARIOS   = "custom_scenarios"
    private const val KEY_HISTORY     = "eject_history"
    private const val KEY_LANGUAGE    = "app_language"
    private const val KEY_RINGTONE    = "setting_ringtone"
    private const val KEY_VIBRATION   = "setting_vibration"
    private const val KEY_HAPTIC      = "setting_haptic"
    private const val KEY_SIDE_BUTTON = "setting_side_button_command"
    private const val KEY_SIDE_BUTTON_CUSTOM_SEQUENCE = "setting_side_button_custom"
    private const val KEY_SELECTED_SCENARIO = "selected_scenario_id"
    private const val KEY_SELECTED_TRIGGER  = "selected_trigger_mode"
    private const val KEY_SELECTED_TIME_CHOICE = "selected_time_choice"
    private const val KEY_SIDE_BUTTON_ARMED = "side_button_armed"
    private const val KEY_CUSTOM_DELAY_SEC  = "custom_delay_sec"
    private const val KEY_EJECT_COUNT = "eject_count"
    private const val KEY_PREMIUM     = "is_premium"
    // v1.3.0 — 광고 제거 일회성 unlock (eject_remove_ads_lifetime).
    // KEY_PREMIUM 과 별도 — 사용자가 premium 구독 안 해도 ₩3,300 일회성 으로 광고만 제거 가능.
    // AdManager 가 (isPremium || isAdsRemoved) 둘 중 하나라도 true 면 광고 로드 안 함.
    private const val KEY_ADS_REMOVED = "ads_removed"
    private const val KEY_SHOW_ONBOARDING = "show_onboarding"
    // v1.5.0 — 메인 화면 4-step 코치마크 투어를 본 적이 있는지 여부.
    // 첫 실행에 OnboardingScreen 종료 후 MainScreen 진입 시 false 면 overlay 표시.
    // 한 번 다 보거나 "건너뛰기" 누르면 true 로 영구 저장 (재진입에 다시 안 뜸).
    // Settings → "사용 설명서" 에서 다시 보기 가능 (saveCoachmarkSeen(false) 로 리셋).
    private const val KEY_COACHMARK_SEEN = "coachmark_seen"
    private const val KEY_BATTERY_OPT_ASKED = "battery_opt_asked"
    // Round 30 — 사용자가 삭제한 프리셋 ID 집합 (mom/dad). 사용자가 원하면 프리셋도 숨길 수 있게.
    private const val KEY_DELETED_PRESETS = "deleted_preset_ids"
    // Round 31 — Play Store in-app review 팝업을 이미 요청했는지 여부.
    // 한 번 프롬프트가 뜨고 나면 (사용자가 별을 남겼든 그냥 닫았든) 다시 띄우지 않는다.
    private const val KEY_REVIEW_REQUESTED  = "review_requested"
    // v1.6.6 — 사용자가 앱을 한 번이라도 공유했는지 여부.
    //   Share-to-unlock 모델 (v1.6.6): 무료 사용자가 caller 1명 초과로 추가하려 하면
    //   ShareToUnlockDialog 가 뜨고, "공유하기" 버튼 클릭 시 ACTION_SEND intent 발사
    //   + 본 플래그 true. 한 번 true 가 되면 영구 unlock (이전 rewarded 광고 모델은
    //   1회 시청 = 1회 unlock 이었으나 v1.6.6 부터 공유 1회 = 영구 unlock 으로 단순화).
    private const val KEY_HAS_SHARED        = "has_shared"
    // v1.0.9 — 전면 광고 일별 cap 추적용 (AdManager 가 사용).
    private const val KEY_INTER_DAY_BUCKET  = "ad_inter_day_bucket"
    private const val KEY_INTER_DAY_COUNT   = "ad_inter_day_count"
    // v1.0.10 — 권한 최초 요청 여부 (이전엔 MainActivity 가 raw SharedPreferences 호출).
    private const val KEY_PERMS_REQUESTED   = "perms_requested"
    // v1.2 — Conversion event 1회 발사용 dedupe 플래그.
    private const val KEY_FIRST_EJECT_LOGGED = "first_eject_logged"
    private const val KEY_FIRST_SCENARIO_LOGGED = "first_scenario_logged"
    // v1.2.0 — 위장 launcher 아이콘 (Decoy) 선택값.
    // 값은 DecoyManager.Decoy enum 의 name (e.g. "DEFAULT", "CALCULATOR")
    private const val KEY_DECOY = "decoy_alias"
    // v1.4.0~v1.5.7: 지오펜스 관련 KEY (REMOVED v1.6.9). GPS 권한/dependency
    // 제거 후 dead code 였던 GeofenceTransitionReceiver + 관련 함수들 정리.
    private const val F = "\u001F"
    private const val R = "\u001E"

    // ── Scenarios ─────────────────────────────────────────────────────────────

    fun saveScenarios(ctx: Context, scenarios: List<Scenario>) {
        val encoded = scenarios.joinToString(R) { s ->
            listOf(s.id, s.emoji, s.name, s.callerName, s.callerLabel,
                   s.preSmsText, s.prompterHint, s.urgency.name,
                   s.isRandomPhone.toString()).joinToString(F)
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
                    id             = f[0],
                    emoji          = f[1],
                    name           = f[2],
                    callerName     = f[3],
                    callerLabel    = f[4],
                    preSmsText     = f[5],
                    prompterHint   = f[6],
                    urgency        = Urgency.valueOf(f[7]),
                    // Round 30 — 9번째 필드가 없으면 legacy 저장본이므로 false 로 간주.
                    isRandomPhone  = f.getOrNull(8)?.toBooleanStrictOrNull() ?: false,
                )
            }.getOrNull()
        }
    }

    // ── Deleted preset ids (Round 30) ─────────────────────────────────────────

    /**
     * mom/dad 프리셋을 사용자가 "삭제" 했을 때 그 id 를 저장한다.
     * 리스트 구성 시 `defaultScenarios` 를 이 set 으로 필터링해서 숨긴다.
     * Settings 의 "프리셋 복원" 으로 언제든 되돌릴 수 있기 때문에 soft-delete.
     */
    fun saveDeletedPresetIds(ctx: Context, ids: Set<String>) {
        val encoded = ids.joinToString(F)
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_DELETED_PRESETS, encoded).apply()
    }

    fun loadDeletedPresetIds(ctx: Context): Set<String> {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_DELETED_PRESETS, "") ?: return emptySet()
        if (raw.isBlank()) return emptySet()
        return raw.split(F).filter { it.isNotBlank() }.toSet()
    }

    fun clearDeletedPresetIds(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().remove(KEY_DELETED_PRESETS).apply()
    }

    // ── Review prompt (Round 31) ──────────────────────────────────────────────

    /**
     * Play In-App Review 프롬프트를 이미 사용자에게 띄웠는지 여부.
     *
     * ※ Google Play 의 API 는 자체 쿼터로 동일 사용자에게 너무 자주 안 띄우므로
     *   이 플래그가 true 여도 실제 UI 가 뜨는 것은 보장되지 않는다 (그게 API 의 의도).
     *   우리는 그저 "앱 내부 조건 (예: eject 3회 이상) 은 만족했으니 한 번은 호출했다"
     *   를 표시하는 용도로만 쓴다. 계정 초기화(= 앱 데이터 삭제) 시 리셋된다.
     */
    fun saveReviewRequested(ctx: Context, requested: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_REVIEW_REQUESTED, requested).apply()
    }

    fun loadReviewRequested(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_REVIEW_REQUESTED, false)

    // ── Share-to-unlock (v1.6.6) ─────────────────────────────────────────────

    /**
     * 앱 공유 ACTION_SEND intent 발사 시 호출 → 영구 true. ShareToUnlockDialog
     * 의 "공유하기" 버튼이 이 함수를 호출하고, 이후 caller 추가 게이팅에서 통과시킴.
     *
     * 주의: ACTION_SEND 의 결과 (사용자가 실제로 공유했는지)는 system 이 알려주지
     * 않으므로 intent 발사 자체를 success 로 간주한다. 사용자가 chooser 에서 cancel
     * 해도 unlock 됨 — friendly default.
     */
    fun saveHasShared(ctx: Context, shared: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_HAS_SHARED, shared).apply()
    }

    fun loadHasShared(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_HAS_SHARED, false)

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

    fun loadLanguage(ctx: Context): AppLanguage {
        val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_LANGUAGE, null)
        if (saved != null) return AppLanguage.fromCode(saved)
        // Round 18 — 첫 실행에는 저장된 값이 없으므로 기기의 시스템 로케일을
        // 읽어 앱이 지원하는 가장 가까운 언어로 자동 매핑한다.
        return detectDefaultLanguage()
    }

    /**
     * 기기 언어를 앱이 지원하는 7개 언어 중 하나로 매핑.
     * 매칭이 없으면 ENGLISH fallback.
     * (Round 18 - 한국 시장 우선이라 기본 우선순위도 자연스럽게 한국어가 먼저)
     */
    private fun detectDefaultLanguage(): AppLanguage {
        val locale = Locale.getDefault()
        val lang = locale.language.lowercase()
        val country = locale.country.uppercase()
        return when {
            lang == "ko" -> AppLanguage.KOREAN
            lang == "zh" && (country == "TW" || country == "HK" || country == "MO") ->
                AppLanguage.CHINESE_TRADITIONAL
            lang == "zh" -> AppLanguage.CHINESE_SIMPLIFIED
            lang == "ja" -> AppLanguage.JAPANESE
            lang == "es" -> AppLanguage.SPANISH
            lang == "hi" -> AppLanguage.HINDI
            else         -> AppLanguage.ENGLISH
        }
    }

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

    // ── Side button trigger ──────────────────────────────────────────────────

    fun saveSideButtonCommand(ctx: Context, command: SideButtonCommand) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_SIDE_BUTTON, command.name).apply()
    }

    fun loadSideButtonCommand(ctx: Context): SideButtonCommand =
        SideButtonCommand.fromName(
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_SIDE_BUTTON, SideButtonCommand.DISABLED.name)
        )

    fun saveSideButtonCustomSequence(ctx: Context, sequence: List<SideButtonStep>) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_SIDE_BUTTON_CUSTOM_SEQUENCE, SideButtonStep.serialize(sequence)).apply()
    }

    fun loadSideButtonCustomSequence(ctx: Context): List<SideButtonStep> =
        SideButtonStep.parse(
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_SIDE_BUTTON_CUSTOM_SEQUENCE, "")
        )

    // ── Selected scenario / trigger (사이드 버튼 트리거 발동 시 사용) ──────────

    fun saveSelectedScenarioId(ctx: Context, id: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_SELECTED_SCENARIO, id).apply()
    }

    fun loadSelectedScenarioId(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_SCENARIO, null)

    fun saveSelectedTrigger(ctx: Context, name: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_SELECTED_TRIGGER, name).apply()
    }

    fun loadSelectedTrigger(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_TRIGGER, null)

    fun saveSelectedTimeChoice(ctx: Context, name: String) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_SELECTED_TIME_CHOICE, name).apply()
    }

    fun loadSelectedTimeChoice(ctx: Context): String? =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_TIME_CHOICE, null)

    fun saveSideButtonArmed(ctx: Context, armed: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SIDE_BUTTON_ARMED, armed).apply()
    }

    fun loadSideButtonArmed(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_SIDE_BUTTON_ARMED, false)

    fun saveCustomDelaySec(ctx: Context, sec: Int) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putInt(KEY_CUSTOM_DELAY_SEC, sec).apply()
    }

    fun loadCustomDelaySec(ctx: Context): Int =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getInt(KEY_CUSTOM_DELAY_SEC, 60)

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

    // ── Interstitial day cap (v1.0.9) ────────────────────────────────────────
    //
    // AdMob 정책 + UX 보호를 위해 전면 광고를 하루에 일정 횟수 이하로만 보여준다.
    // bucket 은 UTC epoch day (System.currentTimeMillis() / 86_400_000).
    // 새 날이 되면 자동으로 카운터가 0 으로 리셋되는 효과.

    private fun currentEpochDay(): Long =
        System.currentTimeMillis() / (24L * 60L * 60L * 1000L)

    /** 오늘 (UTC) 전면 광고를 maxPerDay 회 미만으로 보여줬는지 여부. */
    fun canShowInterstitialToday(ctx: Context, maxPerDay: Int): Boolean {
        val today = currentEpochDay()
        val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val bucket = prefs.getLong(KEY_INTER_DAY_BUCKET, -1L)
        val count = if (bucket == today) prefs.getInt(KEY_INTER_DAY_COUNT, 0) else 0
        return count < maxPerDay
    }

    /** 전면 광고 노출 시 호출. bucket 갱신 + count +1. */
    fun recordInterstitialShown(ctx: Context) {
        val today = currentEpochDay()
        val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val bucket = prefs.getLong(KEY_INTER_DAY_BUCKET, -1L)
        val newCount = if (bucket == today) prefs.getInt(KEY_INTER_DAY_COUNT, 0) + 1 else 1
        prefs.edit()
            .putLong(KEY_INTER_DAY_BUCKET, today)
            .putInt(KEY_INTER_DAY_COUNT, newCount)
            .apply()
    }

    // ── Premium ──────────────────────────────────────────────────────────────

    @Suppress("ApplySharedPref") // 의도된 동기 commit (BillingManager 결제 콜백 크래시 대비)
    fun savePremium(ctx: Context, premium: Boolean) {
        // v1.0.10 — apply() 가 아닌 commit() 사용.
        // BillingManager.onPurchasesUpdated 콜백 직후에 앱이 크래시 되면
        // apply() 의 비동기 disk write 가 유실되어 사용자가 결제했는데도
        // is_premium=false 로 남는 사고가 가능. 단일 키 commit() 은 main
        // thread 에서 보통 1-5ms 라 ANR 임계 (5초) 와 비교하면 안전.
        // 다른 키들은 critical 하지 않아 apply() 유지.
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PREMIUM, premium).commit()
    }

    fun loadPremium(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_PREMIUM, false)

    // ── Ads Removed (v1.3.0 INAPP one-time unlock) ───────────────────────────
    //
    // 사용자가 eject_remove_ads_lifetime (₩3,300 일회성) 구매 시 true.
    // INAPP 결제는 한 번 acknowledged 되면 영구 — 디바이스 재설치 시 restorePurchases() 가
    // 자동 복원. KEY_PREMIUM 과 분리되어 premium 구독 없어도 광고만 제거 가능.

    @Suppress("ApplySharedPref") // 의도된 동기 commit (BillingManager 결제 콜백 크래시 대비)
    fun saveAdsRemoved(ctx: Context, removed: Boolean) {
        // v1.0.10 패턴 — apply() 가 아닌 commit() 사용. BillingManager.onPurchasesUpdated
        // 콜백 직후 앱 크래시 대비. 단일 키 commit() 은 1-5ms 라 ANR 임계 (5초) 와
        // 비교하면 안전.
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ADS_REMOVED, removed).commit()
    }

    fun loadAdsRemoved(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_ADS_REMOVED, false)

    // ── Initial permissions request flag (v1.0.10) ───────────────────────────
    //
    // 첫 실행에서 런타임 퍼미션 + 오버레이 퍼미션을 요청한 적이 있는지 여부.
    // true 면 다시 묻지 않는다. 키 이름은 v1.0.9 이전 raw 호출과 동일해서
    // 기존 사용자 마이그레이션 부담 없음.

    fun savePermsRequested(ctx: Context, requested: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_PERMS_REQUESTED, requested).apply()
    }

    fun loadPermsRequested(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_PERMS_REQUESTED, false)

    // ── Onboarding ───────────────────────────────────────────────────────────

    fun saveShowOnboarding(ctx: Context, show: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_ONBOARDING, show).apply()
    }

    fun loadShowOnboarding(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_SHOW_ONBOARDING, true)

    // ── Coachmark tour (v1.5.0) ───────────────────────────────────────────────
    //
    // 메인 화면 4-step 코치마크 투어 (시나리오 카드 / 모드 토글 / EJECT 버튼 / ⚙ 설정).
    // 신규 사용자에게 a-ha 모먼트를 제공. 한 번만 자동 표시.
    // 기본값 false → 첫 실행 시 표시. 본 후 true 저장.
    //
    // Settings → "사용 설명서" 에서 saveCoachmarkSeen(false) 호출로 다시 보기 트리거.

    fun saveCoachmarkSeen(ctx: Context, seen: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_COACHMARK_SEEN, seen).apply()
    }

    fun loadCoachmarkSeen(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_COACHMARK_SEEN, false)

    // ── Battery optimization dialog (한 번 물어보고 기록) ─────────────────────

    fun saveBatteryOptAsked(ctx: Context, asked: Boolean) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BATTERY_OPT_ASKED, asked).apply()
    }

    fun loadBatteryOptAsked(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_BATTERY_OPT_ASKED, false)

    // ── Conversion event dedupe (v1.2) ───────────────────────────────────────
    //
    // Firebase Analytics conversion 이벤트는 동일 디바이스에서 평생 1회만 발사해야
    // 정확한 funnel 측정이 됨. 아래 플래그로 dedupe.

    fun isFirstEjectLogged(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_FIRST_EJECT_LOGGED, false)

    fun markFirstEjectLogged(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FIRST_EJECT_LOGGED, true).apply()
    }

    fun isFirstScenarioLogged(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getBoolean(KEY_FIRST_SCENARIO_LOGGED, false)

    fun markFirstScenarioLogged(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_FIRST_SCENARIO_LOGGED, true).apply()
    }

    // ── Decoy launcher icon (v1.2.0) ─────────────────────────────────────────
    //
    // 사용자가 설정 → 위장 아이콘 에서 선택한 Decoy enum 의 name 을 저장.
    // DecoyManager.setActive() 가 PackageManager 토글 후 이 함수로 prefs 도 sync.

    fun saveDecoy(ctx: Context, decoy: DecoyManager.Decoy) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_DECOY, decoy.name).apply()
    }

    fun loadDecoy(ctx: Context): DecoyManager.Decoy {
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_DECOY, null)
        return DecoyManager.Decoy.fromName(raw)
    }

    // ── Geofence (v1.4.0~v1.5.7, REMOVED v1.6.9) ─────────────────────────────
    //
    // 지오펜스 기능은 v1.5.8 GPS 권한 제거 + v1.5.10 location dependency 제거
    // 이후 dead code 였음. v1.6.9 정리 작업에서 GeofenceTransitionReceiver +
    // SharedPreferences 관련 함수들 (saveGeofencePrevMode/load/has/clear,
    // saveGeofences/loadGeofences) 모두 제거. 향후 재도입 시 git history 참조.

    // ── Theme mode ───────────────────────────────────────────────────────────

    fun saveThemeMode(ctx: Context, mode: ThemeMode) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun loadThemeMode(ctx: Context): ThemeMode {
        // v1.5.2 — 사용자 결정: 디폴트 LIGHT (기존 SYSTEM → LIGHT).
        // 이미 SYSTEM 으로 저장된 사용자는 그 값 유지 (raw != null 이면 그대로 사용).
        // 신규 사용자만 LIGHT 시작.
        val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, ThemeMode.LIGHT.name) ?: ThemeMode.LIGHT.name
        return runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.LIGHT)
    }
}
