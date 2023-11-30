package codes.dreaming.plugin.mobile_payments

import android.app.Activity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

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
}
