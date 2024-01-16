package codes.dreaming.plugin.mobile_payments

import android.app.Activity
import app.tauri.plugin.Channel
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.collections.ArrayList
import kotlin.coroutines.resume

@Suppress("unused")
class PurchasesUpdatedChannelMessage(var billingResult: BillingResult, var purchases: List<Purchase>)

class MobilePayments(private val activity: Activity) {
    private var billingClient: BillingClient? = null
    private var channel: Channel? = null

    fun init(enableAlternativeBillingOnly: Boolean) {
        if (billingClient != null) {
            throw IllegalStateException("BillingClient already initialized")
        }

        billingClient = BillingClient.newBuilder(activity).apply {
            setListener { billingResult, purchases ->
                PurchasesUpdatedChannelMessage(billingResult, purchases.orEmpty()).let {
                    channel?.sendObject(it)
                }
            }
            enablePendingPurchases()
            if (enableAlternativeBillingOnly) {
                enableAlternativeBillingOnly()
            }
        }.build()
    }

    fun setEventHandler(channel: Channel) {
        this.channel = channel
    }

    suspend fun startConnection() {
        if (billingClient == null) {
            throw IllegalStateException("BillingClient not initialized.")
        }

        suspendCancellableCoroutine<Unit> { continuation ->
            billingClient!!.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingResponseCode.OK) {
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

    suspend fun purchase(productId: String, productType: String, obfuscatedAccountId: String?): BillingResult {
        if (billingClient == null) {
            throw IllegalStateException("BillingClient not initialized.")
        }

        val productList = ArrayList<QueryProductDetailsParams.Product>()
        productList.add(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        val productsDetails = billingClient!!.queryProductDetails(params)

        if (productsDetails.billingResult.responseCode != BillingResponseCode.OK) {
            throw IllegalStateException("Billing response code: ${productsDetails.billingResult.responseCode}")
        }

        if (productsDetails.productDetailsList == null || productsDetails.productDetailsList!!.isEmpty()) {
            throw IllegalStateException("Product details list is empty.")
        }


        val productDetailsParamsList = productsDetails.productDetailsList!!.map { productDetails ->
            val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)

            if (productType == ProductType.SUBS) {
                productDetailsParams.setOfferToken(productDetails.subscriptionOfferDetails!![0]!!.offerToken)
            }

            productDetailsParams.build()
        }

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        return billingClient!!.launchBillingFlow(activity, billingFlowParams)
    }
}
