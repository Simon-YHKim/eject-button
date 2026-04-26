package com.ejectbutton.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.ejectbutton.analytics.EjectAnalytics
import com.ejectbutton.data.EjectPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google Play Billing 통합.
 *
 * Round 12 — 일회성 INAPP 에서 **월 구독(SUBS)** 으로 전환.
 *  - 상품 ID: [PRODUCT_PREMIUM] = "eject_premium_monthly"
 *  - Play Console 에서 이 ID 로 구독 상품을 만들고, 1개의 base plan ("monthly")
 *    을 등록한 뒤 가격을 설정해야 한다 (예: ₩1,900 / $1.99).
 *  - 사용자가 결제하면 [onPurchasesUpdated] → EjectPrefs.savePremium(true).
 *  - 복원 / acknowledge 는 SUBS ProductType 으로 재쿼리.
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_PREMIUM = "eject_premium_monthly"
    }

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private val _isPremium = MutableStateFlow(EjectPrefs.loadPremium(context))
    val isPremium: StateFlow<Boolean> = _isPremium

    private var productDetails: ProductDetails? = null

    fun connect() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProduct()
                    restorePurchases()
                }
            }
            override fun onBillingServiceDisconnected() {
                // 연결 끊기면 자동 재연결 시도
                try { billingClient.startConnection(this) } catch (_: Exception) {}
            }
        })
    }

    private fun queryProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_PREMIUM)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { _, details ->
            productDetails = details.firstOrNull()
        }
    }

    fun restorePurchases() {
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
                purchases.filter { !it.isAcknowledged }.forEach { purchase ->
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    // v1.0.9 — 콜백의 BillingResult 검증.
                    // ack 실패 시 다음 connect() → restorePurchases() 가 다시 시도하므로
                    // 자동 회복이 가능하지만, Play 서버는 3일 안에 ack 안 된 구매를
                    // 자동 환불한다. 그 안에 connect 가 한 번이라도 성공하면 회복.
                    billingClient.acknowledgePurchase(ackParams) { ackResult ->
                        if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                            android.util.Log.w(
                                "BillingManager",
                                "ackPurchase(restore) failed: code=${ackResult.responseCode}"
                            )
                        }
                    }
                }
            } else {
                // 구독이 만료/취소되면 프리미엄 상태도 내린다.
                if (EjectPrefs.loadPremium(context)) {
                    EjectPrefs.savePremium(context, false)
                    _isPremium.value = false
                }
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails ?: return
        // 구독은 base plan 의 offerToken 을 지정해야 한다.
        val offerToken = details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: return
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
     * 구독의 경상 가격(월 요금) 을 반환. Play 에서 받아오는 `formattedPrice` 는
     * 로케일에 맞춰 통화 기호/숫자 포맷이 들어간 최종 문자열 ("₩1,900", "$1.99" 등).
     */
    fun getPriceText(): String? {
        val phases = productDetails?.subscriptionOfferDetails
            ?.firstOrNull()
            ?.pricingPhases
            ?.pricingPhaseList
            ?: return null
        // free trial 이 있을 경우 가격이 0 인 phase 가 먼저 올 수 있으므로
        // 실제 청구 금액이 있는 첫 phase 를 고른다.
        return phases.firstOrNull { it.priceAmountMicros > 0 }?.formattedPrice
            ?: phases.firstOrNull()?.formattedPrice
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        // v1.1.5 — 결제 응답 모든 분기 처리 (이전엔 OK + PURCHASED 만 처리, 나머지 묵음 → 환불 요청 risk).
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> {
                            EjectPrefs.savePremium(context, true)
                            _isPremium.value = true
                            EjectAnalytics.logPremiumPurchased(PRODUCT_PREMIUM)
                            if (!purchase.isAcknowledged) {
                                val ackParams = AcknowledgePurchaseParams.newBuilder()
                                    .setPurchaseToken(purchase.purchaseToken)
                                    .build()
                                // v1.0.9 — BillingResult OK 검증. 실패해도 premium 은 이미 저장되어
                                // 있고 다음 실행 시 restorePurchases() 가 ack 를 재시도.
                                billingClient.acknowledgePurchase(ackParams) { ackResult ->
                                    if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                                        android.util.Log.w(
                                            "BillingManager",
                                            "ackPurchase(new) failed: code=${ackResult.responseCode}"
                                        )
                                    }
                                }
                            }
                        }
                        Purchase.PurchaseState.PENDING -> {
                            // SEPA / 슬립 결제 등 비동기 결제. 일정 시간 후 PURCHASED 로 다시 콜백 옴.
                            // 사용자에게는 "결제 진행 중" 토스트가 도움 되지만 BillingManager 는 Service 컨텍스트가 아니라
                            // 토스트는 호출부 (MainActivity) 에서 처리. 여기선 로깅만.
                            android.util.Log.i("BillingManager", "purchase pending: ${purchase.products}")
                        }
                        Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                            android.util.Log.w("BillingManager", "purchase state unspecified: ${purchase.products}")
                        }
                    }
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // 사용자가 이미 구매한 상품을 다시 결제 시도 (디바이스 변경, 재설치 등). 자동 복원.
                android.util.Log.i("BillingManager", "ITEM_ALREADY_OWNED → restorePurchases() 자동 트리거")
                restorePurchases()
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // 사용자가 결제 다이얼로그를 닫음 — 일반적이고 예상된 흐름. 로깅만.
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

    fun destroy() {
        billingClient.endConnection()
    }
}
