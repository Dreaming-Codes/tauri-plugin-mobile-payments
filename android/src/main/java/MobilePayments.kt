package codes.dreaming.plugin.mobile_payments

import android.app.Activity
import app.tauri.plugin.Channel
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.Purchase
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import kotlin.coroutines.resume

@Suppress("unused")
data class PurchasesUpdatedChannelMessage(val billingResult: BillingResult, val purchases: List<Purchase>)

data class PriceInfo(
    val formattedPrice: String?,
    val currencyCode: String?,
    val priceAmountMicros: Long?
)

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
            enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
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


    suspend fun getActiveSubscriptionPurchaseToken(productId: String): String? {
        val client = billingClient ?: throw IllegalStateException("BillingClient not initialized.")

        return suspendCancellableCoroutine { continuation ->
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            ) { billingResult, purchasesList ->
                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    continuation.resumeWith(Result.failure(IllegalStateException("Failed to query purchases: ${billingResult.debugMessage}")))
                    return@queryPurchasesAsync
                }
                // Find the purchase for the given productId
                val token = purchasesList.firstOrNull { it.products.contains(productId) }?.purchaseToken
                continuation.resume(token)
            }
        }
    }



    suspend fun getProductDetails(productId: String, productType: String): ProductDetails {
        val client = billingClient ?: throw IllegalStateException("BillingClient not initialized.")

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(productType)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

        val productDetailsResult = client.queryProductDetails(params)

        if (productDetailsResult.billingResult.responseCode != BillingResponseCode.OK) {
            throw IllegalStateException("Failed to query product details: ${productDetailsResult.billingResult.debugMessage} (Code: ${productDetailsResult.billingResult.responseCode})")
        }

        return productDetailsResult.productDetailsList?.firstOrNull { it.productId == productId }
            ?: throw IllegalStateException("Product details not found for ID: $productId")
    }

    fun extractPriceInfo(productDetails: ProductDetails): PriceInfo {
         // For Subscriptions: Prioritize finding the base plan's price or a specific offer's price
        productDetails.subscriptionOfferDetails?.firstOrNull()?.let { offer ->
            // Often the first pricing phase is the relevant one for display
            offer.pricingPhases.pricingPhaseList.firstOrNull()?.let { phase ->
                return PriceInfo(phase.formattedPrice, phase.priceCurrencyCode, phase.priceAmountMicros)
            }
        }

        // For One-Time Purchases or fallback for Subs
        productDetails.oneTimePurchaseOfferDetails?.let { offer ->
             return PriceInfo(offer.formattedPrice, offer.priceCurrencyCode, offer.priceAmountMicros)
        }

        // Fallback if no price found (should ideally not happen for valid products)
        return PriceInfo(null, null, null)
    }


    // --- Consolidate purchase flow logic ---
    suspend fun launchPurchaseFlow(
        productId: String,
        productType: String,
        obfuscatedAccountId: String?,
        updateParams: BillingFlowParams.SubscriptionUpdateParams? // Null for new purchase
    ): BillingResult {
        val client = billingClient ?: throw IllegalStateException("BillingClient not initialized.")

        // 1. Get ProductDetails for the product being purchased/updated TO
        val productDetails = getProductDetails(productId, productType) // Use the refactored method

        // 2. Build ProductDetailsParams (including offer token for SUBS)
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails)
            .apply {
                // Ensure we get the offer token for the *target* subscription plan
                if (productType == ProductType.SUBS) {
                    // Find the appropriate offer token. Often the first one or base plan.
                    // You might need more logic here if you have multiple offers per subscription.
                    productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { offerToken ->
                        setOfferToken(offerToken)
                    } ?: run {
                         // Handle case where subscription details exist but no offer token found (might be an issue)
                         System.err.println("Warning: No offer token found for subscription product $productId")
                    }
                }
            }
            .build()

        // 3. Build BillingFlowParams
        val billingFlowParamsBuilder = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .apply {
                obfuscatedAccountId?.let { setObfuscatedAccountId(it) }
                // Add update parameters ONLY if this is an upgrade/downgrade
                updateParams?.let { setSubscriptionUpdateParams(it) }
            }

        val billingFlowParams = billingFlowParamsBuilder.build()

        // 4. Launch Billing Flow
        return client.launchBillingFlow(activity, billingFlowParams)
    }

    suspend fun purchase(productId: String, productType: String, obfuscatedAccountId: String?): BillingResult {
        billingClient?.let { client ->
            val productList = listOf(
                QueryProductDetailsParams.Product.newBuilder().setProductId(productId).setProductType(productType)
                    .build()
            )

            val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()

            val productsDetails = client.queryProductDetails(params)

            if (productsDetails.billingResult.responseCode != BillingResponseCode.OK) {
                throw IllegalStateException("Billing response code: ${productsDetails.billingResult.responseCode}")
            }

            val productDetailsList =
                productsDetails.productDetailsList ?: throw IllegalStateException("Product details list is empty.")

            val productDetailsParamsList = productDetailsList.map { productDetails ->
                BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetails).apply {
                    if (productType == ProductType.SUBS) {
                        productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { setOfferToken(it) }
                    }
                }.build()
            }

            val billingFlowParams =
                BillingFlowParams.newBuilder().setProductDetailsParamsList(productDetailsParamsList).apply {
                    obfuscatedAccountId?.let { setObfuscatedAccountId(it) }
                }.build()

            return client.launchBillingFlow(activity, billingFlowParams)
        } ?: throw IllegalStateException("BillingClient not initialized.")
    }
}
