package com.ejectbutton.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingRuntimeStateTest {
    private fun generationTracker() = BillingRestoreGenerationTracker(
        subscriptionProductId = BillingManager.PRODUCT_PREMIUM,
        oneTimeProductId = BillingManager.PRODUCT_REMOVE_ADS,
    )

    @Test
    fun `subscription purchase does not invalidate in-flight one-time restore`() {
        val tracker = generationTracker()
        val inFlight = tracker.beginRestore()

        assertEquals(
            BillingProductKind.SUBSCRIPTION,
            tracker.invalidatePurchasedProducts(listOf(BillingManager.PRODUCT_PREMIUM)),
        )

        assertFalse(tracker.isCurrent(BillingProductKind.SUBSCRIPTION, inFlight))
        assertTrue(tracker.isCurrent(BillingProductKind.ONE_TIME, inFlight))
    }

    @Test
    fun `one-time purchase does not invalidate in-flight subscription restore`() {
        val tracker = generationTracker()
        val inFlight = tracker.beginRestore()

        assertEquals(
            BillingProductKind.ONE_TIME,
            tracker.invalidatePurchasedProducts(listOf(BillingManager.PRODUCT_REMOVE_ADS)),
        )

        assertTrue(tracker.isCurrent(BillingProductKind.SUBSCRIPTION, inFlight))
        assertFalse(tracker.isCurrent(BillingProductKind.ONE_TIME, inFlight))
    }

    @Test
    fun `unknown purchase invalidates neither restore stream`() {
        val tracker = generationTracker()
        val inFlight = tracker.beginRestore()

        assertNull(tracker.invalidatePurchasedProducts(listOf("unknown_product")))

        assertTrue(tracker.isCurrent(BillingProductKind.SUBSCRIPTION, inFlight))
        assertTrue(tracker.isCurrent(BillingProductKind.ONE_TIME, inFlight))
    }

    @Test
    fun `new restore invalidates both older restore callbacks`() {
        val tracker = generationTracker()
        val older = tracker.beginRestore()

        tracker.beginRestore()

        assertFalse(tracker.isCurrent(BillingProductKind.SUBSCRIPTION, older))
        assertFalse(tracker.isCurrent(BillingProductKind.ONE_TIME, older))
    }

    @Test
    fun `destroy invalidates both in-flight restore callbacks`() {
        val tracker = generationTracker()
        val inFlight = tracker.beginRestore()

        tracker.invalidateAll()

        assertFalse(tracker.isCurrent(BillingProductKind.SUBSCRIPTION, inFlight))
        assertFalse(tracker.isCurrent(BillingProductKind.ONE_TIME, inFlight))
    }

    @Test
    fun `failed initial setup lets next resume reconnect without duplicate attempts`() {
        val state = BillingConnectionState()
        var connectionAttempts = 0
        var refreshes = 0
        val startConnection = { connectionAttempts += 1 }
        val refresh = { refreshes += 1 }

        state.connectIfNeeded(startConnection)
        state.onResume(startConnection, refresh)
        assertEquals(1, connectionAttempts)
        assertEquals(0, refreshes)
        assertFalse(state.onSetupFinished(success = false))

        state.onResume(startConnection, refresh)
        state.onResume(startConnection, refresh)
        state.onResume(startConnection, refresh)
        assertEquals(2, connectionAttempts)
        assertEquals(0, refreshes)
    }

    @Test
    fun `successful setup refreshes on resume and does not start another connection`() {
        val state = BillingConnectionState()
        var connectionAttempts = 0
        var refreshes = 0
        val startConnection = { connectionAttempts += 1 }
        val refresh = { refreshes += 1 }

        state.connectIfNeeded(startConnection)
        assertTrue(state.onSetupFinished(success = true))
        state.onResume(startConnection, refresh)
        state.connectIfNeeded(startConnection)

        assertEquals(1, connectionAttempts)
        assertEquals(1, refreshes)
    }

    @Test
    fun `destroy prevents reconnect and ignores late setup success`() {
        val state = BillingConnectionState()
        var connectionAttempts = 0
        var refreshes = 0
        val startConnection = { connectionAttempts += 1 }
        val refresh = { refreshes += 1 }
        state.connectIfNeeded(startConnection)

        state.destroy()

        assertFalse(state.onSetupFinished(success = true))
        state.onResume(startConnection, refresh)
        state.connectIfNeeded(startConnection)
        assertEquals(1, connectionAttempts)
        assertEquals(0, refreshes)
    }
}
