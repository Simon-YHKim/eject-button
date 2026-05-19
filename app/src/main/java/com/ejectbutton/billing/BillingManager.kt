package com.ejectbutton.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.ejectbutton.analytics.EjectAnalytics
import com.ejectbutton.data.EjectPrefs
import com.ejectbutton.data.strings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google Play Billing 통합.
 *
 * Round 12 — 일회성 INAPP 에서 **월 구독(SUBS)** 으로 전환.
 *  - 상품 ID: [PRODUCT_PREMIUM] = "eject_premium_monthly"
 *  - Play Console 에서 이 ID 로 구독 상품을 만들고, 1개의 base plan ("monthly")
 *    을 등록한 뒤 가격을 설정해야 한다 (예: ₩1,900 / $1.99).
 *
 * v1.3.0 — 광고 제거 일회성 (INAPP) 추가.
 *  - 상품 ID: [PRODUCT_REMOVE_ADS] = "eject_remove_ads_lifetime"
 *  - Play Console 에서 INAPP 상품 (구독 아님) 으로 등록, 가격 ₩3,300 권장.
 *  - 한 번 결제 + acknowledged 되면 영구 unlock. consume 하지 않음.
 *  - SUBS premium 구독자는 자동으로 광고 제거되므로 별도 unlock 불필요.
 *
 * 두 상품은 독립 — 사용자는 둘 다 살 수도, 하나만 살 수도, 안 살 수도 있다.
 * AdManager 는 (isPremium || isAdsRemoved) 둘 중 하나만 true 여도 광고 비활성.
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_PREMIUM     = "eject_premium_monthly"
        const val PRODUCT_REMOVE_ADS  = "eject_remove_ads_lifetime"
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    // SUBS premium 구독 상태
    private val _isPremium = MutableStateFlow(EjectPrefs.loadPremium(context))
    val isPremium: StateFlow<Boolean> = _isPremium

    // v1.3.0 — INAPP 광고 제거 일회성 unlock 상태 (SUBS 와 독립)
    private val _isAdsRemoved = MutableStateFlow(EjectPrefs.loadAdsRemoved(context))
    val isAdsRemoved: StateFlow<Boolean> = _isAdsRemoved

    private var subsDetails: ProductDetails? = null
    private var inappDetails: ProductDetails? = null

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    restorePurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                // 연결 끊기면 자동 재연결 시도
                try { billingClient.startConnection(this) } catch (_: Exception) {}
            }
        })
    }

    /**
     * SUBS + INAPP 두 상품을 모두 조회. 각각 별도 ProductDetails 캐시.
     */
    private fun queryProducts() {
        // SUBS — 월 구독 (eject_premium_monthly)
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PRODUCT_PREMIUM)
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    )
                )
                .build()
        ) { _, details -> subsDetails = details.firstOrNull() }

        // INAPP — 일회성 광고 제거 (eject_remove_ads_lifetime)
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(PRODUCT_REMOVE_ADS)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    )
                )
                .build()
        ) { _, details -> inappDetails = details.firstOrNull() }
    }

    /**
     * SUBS + INAPP 양쪽 구매 내역 복원. 각각 다른 type 으로 query.
     * 디바이스 변경 / 앱 재설치 시 자동 호출되어 unlock 복원.
     */
    fun restorePurchases() {
        // v1.5.12 — DEBUG mock: BuildConfig.DEBUG && EjectPrefs.is_premium=true 인 경우
        // Play Store sync 를 skip 하여 SharedPreferences 상태를 그대로 유지.
        // 이는 release 빌드에는 영향 없음 (BuildConfig.DEBUG=false 이라 분기 자체 안 탐).
        // 사용 목적: UI 검증 (구독 중 카드 노출) 시 Play Store 실제 구독 없이 강제 활성.
        if (com.ejectbutton.BuildConfig.DEBUG && EjectPrefs.loadPremium(context)) {
            android.util.Log.d("BillingManager", "DEBUG mock: skip Play Store sync, keeping is_premium=true")
            _isPremium.value = true
            // INAPP 광고 제거도 동일 mock 처리
            if (EjectPrefs.loadAdsRemoved(context)) {
                _isAdsRemoved.value = true
            }
            return
        }

        // SUBS premium 구독 복원
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { _, purchases ->
            val hasPremium = purchases.any {
                it.products.contains(PRODUCT_PREMIUM) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (hasPremium) {
                EjectPrefs.savePremium(context, true)
                _isPremium.value = true
                purchases.filter { !it.isAcknowledged }.forEach { ackIfNeeded(it, "premium-restore") }
            } else if (EjectPrefs.loadPremium(context)) {
                // 구독이 만료/취소되면 프리미엄 상태도 내린다.
                EjectPrefs.savePremium(context, false)
                _isPremium.value = false
            }
        }

        // v1.3.0 — INAPP 광고 제거 일회성 복원
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { _, purchases ->
            val hasAdsRemoved = purchases.any {
                it.products.contains(PRODUCT_REMOVE_ADS) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (hasAdsRemoved) {
                EjectPrefs.saveAdsRemoved(context, true)
                _isAdsRemoved.value = true
                purchases.filter { !it.isAcknowledged }.forEach { ackIfNeeded(it, "ads-removed-restore") }
            }
            // INAPP 일회성 unlock 은 만료 / 환불 케이스 거의 없음 (consume 안 하니).
            // 환불은 Play Console 에서 dev 가 직접 처리해야 isAdsRemoved=false 로 동기화 — 별도 follow-up.
        }
    }

    /**
     * SUBS premium 결제 시작 (월 구독). offerToken 필수.
     *
     * v1.7.1 — 이전엔 `subsDetails == null` 이면 silent return → 사용자에게는 팝업이
     * 그냥 닫히는 것 처럼 보임 (Play Store 결제 화면 안 뜸). 이제 Toast 로 안내 +
     * queryProducts() 재호출로 다음 시도가 성공할 수 있게 한다. 가능한 원인:
     *   - Play Console 에 PRODUCT_PREMIUM 구독 상품이 미등록 또는 inactive
     *   - 사용자의 Play 계정이 license testing list 에 없음
     *   - BillingClient connection 이 아직 안 됨 (cold start 직후)
     *   - 네트워크 오류로 queryProductDetailsAsync 실패
     */
    fun launchPurchase(activity: Activity) {
        val details = subsDetails
        if (details == null) {
            android.util.Log.w(
                "BillingManager",
                "launchPurchase: subsDetails null. " +
                    "Play Console product 'eject_premium_monthly' 가 미등록이거나 " +
                    "BillingClient 가 아직 product detail 못 받아옴. queryProducts() 재시도."
            )
            showBillingNotReadyToast(activity)
            queryProducts()
            return
        }
        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            android.util.Log.w(
                "BillingManager",
                "launchPurchase: offerToken null. Play Console 의 subscription base plan 누락."
            )
            showBillingNotReadyToast(activity)
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    /**
     * v1.3.0 — INAPP 광고 제거 일회성 결제 시작. offerToken 불필요 (구독이 아니므로).
     * v1.7.1 — null guard + Toast 안내 (launchPurchase 와 동일 패턴).
     */
    fun launchPurchaseRemoveAds(activity: Activity) {
        val details = inappDetails
        if (details == null) {
            android.util.Log.w(
                "BillingManager",
                "launchPurchaseRemoveAds: inappDetails null. " +
                    "Play Console product 'eject_remove_ads_lifetime' 미등록 가능."
            )
            showBillingNotReadyToast(activity)
            queryProducts()
            return
        }
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    /**
     * v1.7.1 — Toast 로 사용자에게 결제가 막힌 이유 안내. 로케일별 메시지는
     * AppStrings.billingNotReadyMsg 사용. Activity context 라 main thread.
     */
    private fun showBillingNotReadyToast(activity: Activity) {
        runCatching {
            val lang = com.ejectbutton.data.EjectPrefs.loadLanguage(activity)
            val msg = lang.strings().billingNotReadyMsg
            android.widget.Toast.makeText(activity, msg, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 구독의 경상 가격(월 요금) 을 반환. Play 에서 받아오는 `formattedPrice` 는
     * 로케일에 맞춰 통화 기호/숫자 포맷이 들어간 최종 문자열 ("₩1,900", "$1.99" 등).
     */
    fun getPriceText(): String? {
        val phases = subsDetails?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?: return null
        // free trial 이 있을 경우 가격이 0 인 phase 가 먼저 올 수 있으므로
        // 실제 청구 금액이 있는 첫 phase 를 고른다.
        return phases.firstOrNull { it.priceAmountMicros > 0 }?.formattedPrice
            ?: phases.firstOrNull()?.formattedPrice
    }

    /**
     * v1.3.0 — INAPP 광고 제거 일회성 가격 (예: "₩3,300", "$2.49"). 로케일 자동.
     * INAPP 은 oneTimePurchaseOfferDetails 에서 가격 추출.
     */
    fun getRemoveAdsPriceText(): String? =
        inappDetails?.oneTimePurchaseOfferDetails?.formattedPrice

    /**
     * 신규 구매 시 ack 또는 복원 ack 통합 헬퍼. 3 일 안에 ack 안 되면 Play 가
     * 자동 환불하므로 실패 시 다음 connect() → restorePurchases() 가 재시도.
     */
    private fun ackIfNeeded(purchase: Purchase, label: String) {
        if (purchase.isAcknowledged) return
        val ackParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(ackParams) { ackResult ->
            if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                android.util.Log.w(
                    "BillingManager",
                    "ackPurchase($label) failed: code=${ackResult.responseCode}"
                )
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        // v1.1.5 — 결제 응답 모든 분기 처리.
        // v1.3.0 — INAPP 광고 제거 상품 분기 추가. PRODUCT_PREMIUM / PRODUCT_REMOVE_ADS
        // 둘 다 같은 콜백으로 들어와 products list 로 구분.
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> handlePurchased(purchase)
                        Purchase.PurchaseState.PENDING -> {
                            // SEPA / 슬립 결제 등 비동기 결제. 일정 시간 후 PURCHASED 로 다시 콜백.
                            android.util.Log.i("BillingManager", "purchase pending: ${purchase.products}")
                        }
                        Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                            android.util.Log.w("BillingManager", "purchase state unspecified: ${purchase.products}")
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // 이미 구매한 상품을 다시 결제 시도. 자동 복원.
                android.util.Log.i("BillingManager", "ITEM_ALREADY_OWNED → restorePurchases() 자동 트리거")
                restorePurchases()
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                android.util.Log.d("BillingManager", "purchase canceled by user")
            }
            BillingClient.BillingResponseCode.NETWORK_ERROR,
            BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE -> {
                android.util.Log.w("BillingManager", "purchase failed (network): code=${result.responseCode}, msg=${result.debugMessage}")
            }
            else -> {
                android.util.Log.w("BillingManager", "purchase failed: code=${result.responseCode}, msg=${result.debugMessage}")
            }
        }
    }

    /**
     * PURCHASED 상태 처리 — 상품 ID 로 분기. premium / ads-removed 각각 처리.
     */
    private fun handlePurchased(purchase: Purchase) {
        when {
            purchase.products.contains(PRODUCT_PREMIUM) -> {
                EjectPrefs.savePremium(context, true)
                _isPremium.value = true
                EjectAnalytics.logPremiumPurchased(PRODUCT_PREMIUM)
                ackIfNeeded(purchase, "premium-new")
            }
            purchase.products.contains(PRODUCT_REMOVE_ADS) -> {
                EjectPrefs.saveAdsRemoved(context, true)
                _isAdsRemoved.value = true
                EjectAnalytics.logPremiumPurchased(PRODUCT_REMOVE_ADS)
                ackIfNeeded(purchase, "ads-removed-new")
            }
            else -> {
                android.util.Log.w("BillingManager", "unknown product purchased: ${purchase.products}")
            }
        }
    }

    fun destroy() {
        billingClient.endConnection()
    }
}
