package com.ejectbutton.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase

internal data class BillingPurchaseSnapshot(
    val products: List<String>,
    val purchaseState: Int,
    val isAcknowledged: Boolean,
    val isSuspended: Boolean,
    val purchaseToken: String,
)

internal enum class BillingEntitlementAction {
    GRANT,
    REVOKE,
    PRESERVE,
}

internal data class BillingRestoreDecision(
    val entitlementAction: BillingEntitlementAction,
    val acknowledgementTokens: List<String>,
)

internal enum class BillingLaunchAction {
    ACCEPTED,
    CANCELED,
    RESTORE,
    RETRY,
}

/** Small, deterministic decisions kept outside BillingClient callbacks for regression tests. */
internal object BillingPurchasePolicy {
    fun ownsProduct(snapshot: BillingPurchaseSnapshot, productId: String): Boolean =
        productId in snapshot.products &&
            snapshot.purchaseState == Purchase.PurchaseState.PURCHASED &&
            !snapshot.isSuspended

    fun shouldAcknowledge(snapshot: BillingPurchaseSnapshot, productId: String): Boolean =
        ownsProduct(snapshot, productId) && !snapshot.isAcknowledged

    fun restoreDecision(
        responseCode: Int,
        purchases: List<BillingPurchaseSnapshot>,
        productId: String,
    ): BillingRestoreDecision {
        if (responseCode != BillingClient.BillingResponseCode.OK) {
            return BillingRestoreDecision(BillingEntitlementAction.PRESERVE, emptyList())
        }

        val owned = purchases.filter { ownsProduct(it, productId) }
        return BillingRestoreDecision(
            entitlementAction = if (owned.isEmpty()) {
                BillingEntitlementAction.REVOKE
            } else {
                BillingEntitlementAction.GRANT
            },
            acknowledgementTokens = owned
                .filter { shouldAcknowledge(it, productId) }
                .map { it.purchaseToken },
        )
    }

    fun launchAction(responseCode: Int): BillingLaunchAction = when (responseCode) {
        BillingClient.BillingResponseCode.OK -> BillingLaunchAction.ACCEPTED
        BillingClient.BillingResponseCode.USER_CANCELED -> BillingLaunchAction.CANCELED
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> BillingLaunchAction.RESTORE
        else -> BillingLaunchAction.RETRY
    }
}

internal fun Purchase.toBillingSnapshot(): BillingPurchaseSnapshot = BillingPurchaseSnapshot(
    products = products,
    purchaseState = purchaseState,
    isAcknowledged = isAcknowledged,
    isSuspended = isSuspended,
    purchaseToken = purchaseToken,
)
