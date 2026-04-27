package com.ejectbutton.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

/**
 * v1.4.0 — GPS 지오펜스 기반 시나리오 자동 전환.
 *
 * 사용자가 설정한 위치 (예: 직장, 집, 위험 장소) 의 [radiusMeters] 범위 안에 들어오면
 * `GEOFENCE_TRANSITION_ENTER` 가 발생해 sidebtn 모드를 자동 ON.
 * 범위를 벗어나면 `GEOFENCE_TRANSITION_EXIT` 가 발생해 원래 모드로 원복.
 *
 * GeofencingClient API 의 장점:
 *  - OS 가 배터리 효율적으로 관리 (자체 위치 polling 안 함)
 *  - 백그라운드에서도 작동 (FOREGROUND_SERVICE 불필요)
 *  - 디바이스가 sleep 상태여도 wake up 해서 BroadcastReceiver 호출
 *
 * 권한 요구사항:
 *  - ACCESS_FINE_LOCATION (진입 후 정확한 위치)
 *  - ACCESS_BACKGROUND_LOCATION (폰이 백그라운드/sleep 일 때 이벤트 수신)
 *  - 둘 다 manifest 에 선언 + 사용자 런타임 동의 필수.
 *
 * 가정폭력 use case 시너지:
 *  - "집 도착 시 자동 활성화" — 위험 장소 진입 시 즉시 EJECT 준비 상태.
 *  - "직장 도착 시 비활성화" — 안전한 환경에선 모드 OFF (오발 방지).
 *
 * 사용 예시:
 * ```
 * GeofenceManager.register(context, "workplace", 37.5665, 126.9780, 100f)
 * GeofenceManager.unregister(context, "workplace")
 * ```
 */
object GeofenceManager {

    /**
     * 단일 지오펜스 등록. [requestId] 가 같으면 자동으로 덮어쓴다 (Android 동작).
     *
     * @param requestId 사용자 정의 ID. 예: "workplace", "home", "ex_residence".
     *                  GeofenceTransitionReceiver 에서 어느 위치인지 식별.
     * @param latitude  중심 위도 (소수점)
     * @param longitude 중심 경도 (소수점)
     * @param radiusMeters 반경 (미터). 100~500m 권장.
     * @return true 면 등록 성공 / false 면 권한 없거나 실패.
     */
    fun register(
        context: Context,
        requestId: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 100f,
    ): Boolean {
        if (!hasRequiredPermissions(context)) return false

        val geofence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(latitude, longitude, radiusMeters)
            // 만료 안 함 — 사용자가 명시적으로 unregister 할 때까지 유지.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_EXIT
            )
            // INITIAL_TRIGGER_ENTER — 등록 시점에 이미 안에 있으면 즉시 ENTER 이벤트 발사.
            // 사용자 경험상 "등록했는데 모드 안 바뀜" 혼란 방지.
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        return runCatching {
            getClient(context).addGeofences(request, getPendingIntent(context))
            true
        }.getOrDefault(false)
    }

    /**
     * [requestId] 의 지오펜스 해제. 사용자가 위치 등록을 삭제했을 때 호출.
     */
    fun unregister(context: Context, requestId: String): Boolean =
        runCatching {
            getClient(context).removeGeofences(listOf(requestId))
            true
        }.getOrDefault(false)

    /**
     * 모든 지오펜스 해제. 사용자가 ACCESS_BACKGROUND_LOCATION 권한을 철회했거나
     * 앱이 unregister 일괄 처리할 때 사용.
     */
    fun unregisterAll(context: Context): Boolean =
        runCatching {
            getClient(context).removeGeofences(getPendingIntent(context))
            true
        }.getOrDefault(false)

    /**
     * ACCESS_FINE_LOCATION + ACCESS_BACKGROUND_LOCATION (Android Q+) 둘 다 granted 인지.
     */
    fun hasRequiredPermissions(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // API 28 이하는 ACCESS_FINE_LOCATION 만으로 충분 (background 권한 분리 전).
            true
        }
        return fine && background
    }

    private fun getClient(context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context.applicationContext)

    /**
     * GeofencingClient 가 transition 이벤트 발생 시 호출할 PendingIntent.
     * 같은 PendingIntent 를 register / unregister 양쪽에 사용해야 OS 가 동일한
     * subscription 으로 인식.
     */
    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context.applicationContext, GeofenceTransitionReceiver::class.java)
        // FLAG_MUTABLE — Android 12+ 에서 PendingIntent 가 OS 가 추가하는 extras
        // (transition type, triggering geofences 등) 를 받기 위해 필수.
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getBroadcast(
            context.applicationContext,
            GEOFENCE_REQUEST_CODE,
            intent,
            flags,
        )
    }

    private const val GEOFENCE_REQUEST_CODE = 0x4711
}
