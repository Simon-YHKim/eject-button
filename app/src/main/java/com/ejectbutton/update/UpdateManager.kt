package com.ejectbutton.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.ejectbutton.BuildConfig
import com.ejectbutton.analytics.EjectAnalytics
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.service.FakeCallOverlayService
import com.ejectbutton.service.ShakeDetectionService
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * v1.7.0 — In-App Update (Flexible flow) 통합.
 *
 * Repo 의 manager 패턴 (BillingManager / AdManager / ConsentManager) 과 동일한
 * 인터페이스로 In-App Update lifecycle 을 Activity 에서 분리.
 *
 * State machine ([UpdateState]):
 *   - Idle: 초기 상태. checkForUpdate() 호출 전 또는 metered network / sideload.
 *   - Downloading: startUpdateFlowForResult 호출 후 Play Store 가 백그라운드 다운로드.
 *   - Downloaded: InstallStateUpdatedListener 가 DOWNLOADED 받은 시점. UI 가 "재시작"
 *                 Snackbar 노출하기 시작 (단, emergency 가드는 호출부에서 polling).
 *   - Failed: 다운로드 / 설치가 FAILED. 사용자에게 retry 안내 가능.
 *
 * Activity lifecycle:
 *   - onCreate: [registerListener] 호출 → [checkForUpdate] 로 첫 트리거.
 *   - onResume: [refreshInstallStatus] 로 background-completed 다운로드 재평가.
 *   - onDestroy: [unregisterListener].
 *
 * 호출부는 Activity 가 [IntentSenderRequest] launcher 를 보유한 채로 [checkForUpdate]
 * 에 넘겨준다 (manager 가 Activity 의 ActivityResult API 를 직접 다루지 않도록).
 */
class UpdateManager(private val context: Context) {

    enum class UpdateState {
        Idle,
        Downloading,
        Downloaded,
        Failed,
    }

    private val appUpdateManager: AppUpdateManager =
        AppUpdateManagerFactory.create(context.applicationContext)

    private val _state = MutableStateFlow(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state

    @Volatile
    private var completing: Boolean = false

    private val installStateListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> _state.value = UpdateState.Downloading
            InstallStatus.DOWNLOADED -> {
                _state.value = UpdateState.Downloaded
                EjectAnalytics.logUpdateDownloaded()
            }
            InstallStatus.FAILED, InstallStatus.CANCELED -> {
                // v1.7.0 — 다운로드 / 설치 실패. UI 는 retry Snackbar 노출 가능.
                _state.value = UpdateState.Failed
            }
        }
    }

    fun registerListener() {
        appUpdateManager.registerListener(installStateListener)
    }

    fun unregisterListener() {
        runCatching { appUpdateManager.unregisterListener(installStateListener) }
    }

    /**
     * Wi-Fi / 이더넷 / 무제한 5G 일 때만 true. 모바일 데이터에서 silent 50~150MB
     * 다운로드 차단 (KR/IN/BR 데이터 절약 시장).
     */
    private fun isUnmeteredNetwork(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * 새 버전 확인 + Flexible 다운로드 시작. metered network / 이미 다운 중 / 사이드로드
     * 환경에서는 즉시 return.
     */
    fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        if (!isUnmeteredNetwork()) return

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                val status = info.installStatus()
                if (status == InstallStatus.DOWNLOADING ||
                    status == InstallStatus.DOWNLOADED ||
                    status == InstallStatus.INSTALLED
                ) {
                    // 이미 진행 중인 다운로드의 상태로 동기화.
                    if (status == InstallStatus.DOWNLOADING) _state.value = UpdateState.Downloading
                    if (status == InstallStatus.DOWNLOADED) _state.value = UpdateState.Downloaded
                    return@addOnSuccessListener
                }
                if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            info,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        )
                    }.onFailure { e ->
                        if (BuildConfig.DEBUG) {
                            android.util.Log.w("UpdateManager", "startUpdateFlow failed", e)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                // 디버그 빌드, 사이드로드 APK, Play Store 미설치 디바이스 등에서 발생.
                if (BuildConfig.DEBUG) {
                    android.util.Log.d("UpdateManager", "appUpdateInfo not available: ${e.message}")
                }
            }
    }

    /**
     * Activity onResume 마다 호출. background 에서 download 완료된 경우 state 갱신.
     */
    fun refreshInstallStatus() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when (info.installStatus()) {
                InstallStatus.DOWNLOADED -> _state.value = UpdateState.Downloaded
                InstallStatus.DOWNLOADING -> _state.value = UpdateState.Downloading
            }
        }
    }

    /**
     * "재시작" 액션. 다중 가드 — emergency 도중 절대 발화 X, 더블 콜 묵음, kill 전
     * forensic markers (logUpdateRestartClicked + Crashlytics update_in_progress +
     * markPostUpdateRelaunch) 발사 후 onDestroyExtras → completeUpdate().
     *
     * @param onDestroyExtras Activity 가 BillingManager.destroy / AdManager.destroy 같은
     *   추가 cleanup 을 process kill 직전에 수행할 hook. manager 자체는 이 의존성을
     *   직접 갖지 않음 (책임 분리).
     */
    fun completeUpdate(onDestroyExtras: () -> Unit = {}) {
        if (!shouldCompleteUpdate(
                emergencyActive = FakeCallOverlayService.isRunning || ShakeDetectionService.isRunning,
                alreadyCompleting = completing,
            )
        ) {
            return
        }
        completing = true

        // Kill 직전 forensic markers + funnel event. 모두 disk-persist.
        runCatching { EjectAnalytics.logUpdateRestartClicked() }
        runCatching { FirebaseCrashlytics.getInstance().setCustomKey("update_in_progress", true) }
        runCatching { EjectPrefs.markPostUpdateRelaunch(context) }

        runCatching { onDestroyExtras() }
        runCatching { appUpdateManager.completeUpdate() }
    }

    companion object {
        /**
         * completeUpdate() 가드 predicate. JUnit 으로 회귀 방지.
         *
         * false 를 반환하는 두 경우 모두 silent reject:
         * - emergencyActive=true: fake call / shake 진행 중 → process kill 금지 (위협 모델).
         * - alreadyCompleting=true: 더블 탭 등으로 이미 진행 중인 호출.
         */
        @JvmStatic
        fun shouldCompleteUpdate(
            emergencyActive: Boolean,
            alreadyCompleting: Boolean,
        ): Boolean = !emergencyActive && !alreadyCompleting
    }
}
