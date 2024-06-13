package codes.dreaming.plugin.mobile_payments

import android.app.Activity
import app.tauri.plugin.Channel
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume

@Suppress("unused")
data class PurchasesUpdatedChannelMessage(val billingResult: BillingResult, val purchases: List<Purchase>)

class MobilePayments(private val activity: Activity) {
    private var billingClient: BillingClient? = null
    private var channel: Channel? = null

    fun init(enableAlternativeBillingOnly: Boolean) {
        billingClient?.let {
            throw IllegalStateException("BillingClient already initialized")
        }

        billingClient = BillingClient.newBuilder(activity).apply {
            setListener { billingResult, purchases ->
                channel?.sendObject(PurchasesUpdatedChannelMessage(billingResult, purchases.orEmpty()))
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
        billingClient?.let { client ->
            suspendCancellableCoroutine<Unit> { continuation ->
                client.startConnection(object : BillingClientStateListener {
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
        } ?: throw IllegalStateException("BillingClient not initialized.")
    }

    suspend fun getProductList(productsId: List<String>, productType: String): BillingFlowParams.ProductDetailsParams {
        billingClient?.let { client ->
            val productList = productsId.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productsDetails = client.queryProductDetails(params)

            if (productsDetails.billingResult.responseCode != BillingResponseCode.OK) {
                throw IllegalStateException("Billing response code: ${productsDetails.billingResult.responseCode}")
            }

            val productDetails = productsDetails.productDetailsList?.firstOrNull()
                ?: throw IllegalStateException("Product details list is empty.")

            return BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        } ?: throw IllegalStateException("BillingClient not initialized.")
    }

    suspend fun purchase(productId: String, productType: String, obfuscatedAccountId: String?): BillingResult {
        billingClient?.let { client ->
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(productType)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(productList)
                .build()

            val productsDetails = client.queryProductDetails(params)

            if (productsDetails.billingResult.responseCode != BillingResponseCode.OK) {
                throw IllegalStateException("Billing response code: ${productsDetails.billingResult.responseCode}")
            }

            val productDetailsList = productsDetails.productDetailsList
                ?: throw IllegalStateException("Product details list is empty.")

            val productDetailsParamsList = productDetailsList.map { productDetails ->
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .apply {
                        if (productType == ProductType.SUBS) {
                            productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { setOfferToken(it) }
                        }
                    }
                    .build()
            }

            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .apply {
                    obfuscatedAccountId?.let { setObfuscatedAccountId(it) }
                }
                .build()

            return client.launchBillingFlow(activity, billingFlowParams)
        } ?: throw IllegalStateException("BillingClient not initialized.")
    }
}
