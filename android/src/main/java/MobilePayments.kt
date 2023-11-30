package codes.dreaming.plugin.mobile_payments

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume

class MobilePayments(private val activity: Activity) {
    private var billingClient: BillingClient? = null

    private var purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        // To be implemented in a later section.
    }

    fun init(enablePendingPurchases: Boolean, enableAlternativeBillingOnly: Boolean, reInit: Boolean) {
        if (billingClient != null) {
            if (reInit) {
                destroy()
            } else {
                throw IllegalStateException("BillingClient already initialized")
            }
        }

        billingClient = BillingClient.newBuilder(activity).apply {
            setListener(purchasesUpdatedListener)

            if (enablePendingPurchases) {
                enablePendingPurchases()
            }
            if (enableAlternativeBillingOnly) {
                enableAlternativeBillingOnly()
            }
        }.build()
    }

    fun destroy() {
        if (billingClient == null) {
            throw IllegalStateException("BillingClient not initialized")
        }

        billingClient?.run {
            endConnection()
            billingClient = null
        }
    }

    suspend fun startConnection() {
        if (billingClient == null) {
            throw IllegalStateException("BillingClient not initialized.")
        }

        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient?.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode ==  BillingResponseCode.OK) {
                        continuation.resume(Unit)
                    } else {
                        continuation.cancel(CancellationException("Billing setup failed with response code: ${billingResult.responseCode}"))
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // TODO: Implement retry logic or notify the rust side that we are disconnected.
                }
            })
        }
    }
}
