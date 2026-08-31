package com.ejectbutton.billing

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

internal enum class BillingProductKind {
    SUBSCRIPTION,
    ONE_TIME,
}

internal data class BillingRestoreGenerationSnapshot(
    val subscription: Int,
    val oneTime: Int,
) {
    fun generationFor(productKind: BillingProductKind): Int = when (productKind) {
        BillingProductKind.SUBSCRIPTION -> subscription
        BillingProductKind.ONE_TIME -> oneTime
    }
}

/** Keeps independent async-restore generations for SUBS and INAPP purchases. */
internal class BillingRestoreGenerationTracker(
    private val subscriptionProductId: String,
    private val oneTimeProductId: String,
) {
    private val subscriptionGeneration = AtomicInteger(0)
    private val oneTimeGeneration = AtomicInteger(0)

    fun beginRestore(): BillingRestoreGenerationSnapshot = BillingRestoreGenerationSnapshot(
        subscription = subscriptionGeneration.incrementAndGet(),
        oneTime = oneTimeGeneration.incrementAndGet(),
    )

    fun isCurrent(
        productKind: BillingProductKind,
        snapshot: BillingRestoreGenerationSnapshot,
    ): Boolean = generationFor(productKind) == snapshot.generationFor(productKind)

    private fun invalidate(productKind: BillingProductKind) {
        generationCounterFor(productKind).incrementAndGet()
    }

    /** Invalidates only the restore stream matching a newly purchased known product. */
    fun invalidatePurchasedProducts(products: List<String>): BillingProductKind? {
        val productKind = when {
            subscriptionProductId in products -> BillingProductKind.SUBSCRIPTION
            oneTimeProductId in products -> BillingProductKind.ONE_TIME
            else -> null
        }
        productKind?.let(::invalidate)
        return productKind
    }

    fun invalidateAll() {
        subscriptionGeneration.incrementAndGet()
        oneTimeGeneration.incrementAndGet()
    }

    private fun generationFor(productKind: BillingProductKind): Int =
        generationCounterFor(productKind).get()

    private fun generationCounterFor(productKind: BillingProductKind): AtomicInteger =
        when (productKind) {
            BillingProductKind.SUBSCRIPTION -> subscriptionGeneration
            BillingProductKind.ONE_TIME -> oneTimeGeneration
        }
}

/**
 * Tracks initial BillingClient setup separately from Billing 8+ auto service reconnection.
 * Auto reconnection remains responsible for a service loss after setup succeeds.
 */
internal class BillingConnectionState {
    private val connectionInProgress = AtomicBoolean(false)

    @Volatile
    private var ready = false

    @Volatile
    private var destroyed = false

    fun connectIfNeeded(startConnection: () -> Unit) {
        if (reserveConnectionIfNeeded()) startConnection()
    }

    fun onResume(startConnection: () -> Unit, refresh: () -> Unit) {
        when {
            destroyed -> Unit
            ready -> refresh()
            reserveConnectionIfNeeded() -> startConnection()
        }
    }

    /** Returns true only for a successful setup that is still valid for this manager. */
    fun onSetupFinished(success: Boolean): Boolean {
        connectionInProgress.set(false)
        if (destroyed) return false
        ready = success
        return success
    }

    fun destroy() {
        destroyed = true
        ready = false
        connectionInProgress.set(false)
    }

    private fun reserveConnectionIfNeeded(): Boolean {
        if (destroyed || ready) return false
        if (!connectionInProgress.compareAndSet(false, true)) return false
        if (destroyed || ready) {
            connectionInProgress.set(false)
            return false
        }
        return true
    }
}
