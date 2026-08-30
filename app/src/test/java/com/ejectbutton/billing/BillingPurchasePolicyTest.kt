package com.ejectbutton.billing

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.Purchase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingPurchasePolicyTest {
    private fun purchase(
        products: List<String> = listOf(BillingManager.PRODUCT_PREMIUM),
        state: Int = Purchase.PurchaseState.PURCHASED,
        acknowledged: Boolean = false,
        suspended: Boolean = false,
        token: String = "target-token",
    ) = BillingPurchaseSnapshot(products, state, acknowledged, suspended, token)

    @Test
    fun `purchased target grants entitlement and requires acknowledgement`() {
        val purchase = purchase()

        assertTrue(BillingPurchasePolicy.ownsProduct(purchase, BillingManager.PRODUCT_PREMIUM))
        assertTrue(BillingPurchasePolicy.shouldAcknowledge(purchase, BillingManager.PRODUCT_PREMIUM))
    }

    @Test
    fun `pending purchase grants nothing and is not acknowledged`() {
        val pending = purchase(state = Purchase.PurchaseState.PENDING)

        assertFalse(BillingPurchasePolicy.ownsProduct(pending, BillingManager.PRODUCT_PREMIUM))
        assertFalse(BillingPurchasePolicy.shouldAcknowledge(pending, BillingManager.PRODUCT_PREMIUM))
    }

    @Test
    fun `suspended subscription grants nothing`() {
        assertFalse(
            BillingPurchasePolicy.ownsProduct(
                purchase(suspended = true),
                BillingManager.PRODUCT_PREMIUM,
            )
        )
    }

    @Test
    fun `unknown product is never granted or acknowledged`() {
        val unrelated = purchase(products = listOf("unrelated_product"))

        assertFalse(BillingPurchasePolicy.ownsProduct(unrelated, BillingManager.PRODUCT_PREMIUM))
        assertFalse(BillingPurchasePolicy.shouldAcknowledge(unrelated, BillingManager.PRODUCT_PREMIUM))
    }

    @Test
    fun `acknowledged purchase remains owned without another acknowledgement`() {
        val acknowledged = purchase(acknowledged = true)

        assertTrue(BillingPurchasePolicy.ownsProduct(acknowledged, BillingManager.PRODUCT_PREMIUM))
        assertFalse(BillingPurchasePolicy.shouldAcknowledge(acknowledged, BillingManager.PRODUCT_PREMIUM))
    }

    @Test
    fun `successful empty restore revokes entitlement`() {
        val decision = BillingPurchasePolicy.restoreDecision(
            responseCode = BillingClient.BillingResponseCode.OK,
            purchases = emptyList(),
            productId = BillingManager.PRODUCT_REMOVE_ADS,
        )

        assertEquals(BillingEntitlementAction.REVOKE, decision.entitlementAction)
        assertTrue(decision.acknowledgementTokens.isEmpty())
    }

    @Test
    fun `failed empty restore preserves last known entitlement`() {
        val decision = BillingPurchasePolicy.restoreDecision(
            responseCode = BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            purchases = emptyList(),
            productId = BillingManager.PRODUCT_REMOVE_ADS,
        )

        assertEquals(BillingEntitlementAction.PRESERVE, decision.entitlementAction)
        assertTrue(decision.acknowledgementTokens.isEmpty())
    }

    @Test
    fun `restore grants and acknowledges only purchased target`() {
        val target = purchase(token = "premium-purchased")
        val alreadyAcknowledged = purchase(acknowledged = true, token = "premium-acknowledged")
        val pending = purchase(state = Purchase.PurchaseState.PENDING, token = "premium-pending")
        val unrelated = purchase(products = listOf("unrelated_product"), token = "unrelated")

        val decision = BillingPurchasePolicy.restoreDecision(
            responseCode = BillingClient.BillingResponseCode.OK,
            purchases = listOf(target, alreadyAcknowledged, pending, unrelated),
            productId = BillingManager.PRODUCT_PREMIUM,
        )

        assertEquals(BillingEntitlementAction.GRANT, decision.entitlementAction)
        assertEquals(listOf("premium-purchased"), decision.acknowledgementTokens)
    }

    @Test
    fun `launch response preserves accepted canceled restore and retry paths`() {
        assertEquals(
            BillingLaunchAction.ACCEPTED,
            BillingPurchasePolicy.launchAction(BillingClient.BillingResponseCode.OK),
        )
        assertEquals(
            BillingLaunchAction.CANCELED,
            BillingPurchasePolicy.launchAction(BillingClient.BillingResponseCode.USER_CANCELED),
        )
        assertEquals(
            BillingLaunchAction.RESTORE,
            BillingPurchasePolicy.launchAction(BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED),
        )
        assertEquals(
            BillingLaunchAction.RETRY,
            BillingPurchasePolicy.launchAction(BillingClient.BillingResponseCode.DEVELOPER_ERROR),
        )
    }
}
