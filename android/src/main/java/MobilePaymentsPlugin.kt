package codes.dreaming.plugin.mobile_payments

import android.app.Activity
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Channel
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.JSObject
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@InvokeArg
class InitArgs {
    var alternative_billing_only: Boolean = false
}

@InvokeArg
class PurchaseArgs {
    lateinit var productId: String
    lateinit var isSub: String
    var obfuscatedAccountId: String? = null
}

@InvokeArg
class SetEventHandlerArgs {
    lateinit var handler: Channel
}

@InvokeArg
class ProductListArgs {
    lateinit var productId: String
    lateinit var sub: String
}

@InvokeArg
class UpdateSubscriptionArgs {
    lateinit var newProductId: String
    lateinit var oldPurchaseToken: String
    lateinit var replacementMode: String
    var obfuscatedAccountId: String? = null
}

@InvokeArg
class ActiveSubTokenArgs {
    lateinit var productId: String
}


@TauriPlugin
class MobilePaymentsPlugin(private val activity: Activity) : Plugin(activity) {
    private val implementation = MobilePayments(activity)

    @Command
    fun init(invoke: Invoke) {
        executeVoidCommand(invoke) {
            val args = invoke.parseArgs(InitArgs::class.java)
            implementation.init(
                args.alternative_billing_only
            )
        }
    }

    @Command
    fun setEventHandler(invoke: Invoke) {
        executeVoidCommand(invoke) {
            val args = invoke.parseArgs(SetEventHandlerArgs::class.java)
            implementation.setEventHandler(args.handler)
        }
    }

    @Command
    fun startConnection(invoke: Invoke) {
        executeSuspendingVoidCommand(invoke) {
            implementation.startConnection()
        }
    }

    @Command
    fun purchase(invoke: Invoke) {
        executeSuspendingVoidCommand(invoke) {
            val args = invoke.parseArgs(PurchaseArgs::class.java)
            // Ensure productType is correctly determined (SUBS or INAPP)
            val productType = if (args.isSub.toBoolean()) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP
            implementation.launchPurchaseFlow(
                productId = args.productId,
                productType = productType,
                obfuscatedAccountId = args.obfuscatedAccountId,
                updateParams = null // Explicitly null for initial purchase
            )
        }
    }

    @Command
    fun getActiveSubscriptionPurchaseToken(invoke: Invoke) {
        executeSuspendingCommand(invoke) {
            val args = invoke.parseArgs(ActiveSubTokenArgs::class.java)
            val token = implementation.getActiveSubscriptionPurchaseToken(args.productId)
            JSObject().apply { put("purchaseToken", token) }
        }
    }

    @Command
    fun updateSubscription(invoke: Invoke) { // Command for upgrades/downgrades
        executeSuspendingVoidCommand(invoke) {
            val args = invoke.parseArgs(UpdateSubscriptionArgs::class.java)

            // Map the string replacement mode to the BillingClient constant
            val replacementModeConstant = mapReplacementMode(args.replacementMode)

            val updateParams = BillingFlowParams.SubscriptionUpdateParams.newBuilder()
                .setOldPurchaseToken(args.oldPurchaseToken)
                .setSubscriptionReplacementMode(replacementModeConstant)
                .build()

            // Subscription updates are always for SUBS type
            implementation.launchPurchaseFlow(
                productId = args.newProductId,
                productType = BillingClient.ProductType.SUBS,
                obfuscatedAccountId = args.obfuscatedAccountId,
                updateParams = updateParams // Pass the update parameters
            )
        }
    }

    @Command
    fun getProductPrice(invoke: Invoke) {
       executeSuspendingCommand(invoke) {
            val args = invoke.parseArgs(ProductListArgs::class.java)
            // Pass the correct product type based on the 'sub' flag
            val productType = if (args.sub.toBoolean()) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP
            val productDetails = implementation.getProductDetails(args.productId, productType)

            // --- Refined Price Extraction ---
            val priceInfo = implementation.extractPriceInfo(productDetails)

            return@executeSuspendingCommand JSObject().apply {
                // Return more structured info if needed, e.g., base plan price, offer price
                put("formattedPrice", priceInfo.formattedPrice) // Keep simple for now
                put("currencyCode", priceInfo.currencyCode)
                put("priceAmountMicros", priceInfo.priceAmountMicros)
                // Potentially add offer details if relevant
            }
        }
    }

    private fun mapReplacementMode(mode: String): Int {
        // Use the constants available in BillingFlowParams.SubscriptionUpdateParams.ReplacementMode
        return when (mode.uppercase()) {
            // Map the incoming string "IMMEDIATE_WITH_TIME_PRORATION" to the constant WITH_TIME_PRORATION
            "IMMEDIATE_WITH_TIME_PRORATION" -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITH_TIME_PRORATION

            // Map "IMMEDIATE_AND_CHARGE_PRORATED_PRICE" to CHARGE_PRORATED_PRICE
            "IMMEDIATE_AND_CHARGE_PRORATED_PRICE" -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_PRORATED_PRICE

            // Map "IMMEDIATE_WITHOUT_PRORATION" to WITHOUT_PRORATION
            "IMMEDIATE_WITHOUT_PRORATION" -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.WITHOUT_PRORATION

            // Map "IMMEDIATE_AND_CHARGE_FULL_PRICE" to CHARGE_FULL_PRICE
            "IMMEDIATE_AND_CHARGE_FULL_PRICE" -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.CHARGE_FULL_PRICE

            // DEFERRED name matches the constant name
            "DEFERRED" -> BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED

            else -> {
                // Log an error or throw an exception for unsupported modes from the frontend
                System.err.println("Warning: Unsupported replacement mode string '$mode' received. Defaulting to DEFERRED.")
                // Default to a known valid constant
                BillingFlowParams.SubscriptionUpdateParams.ReplacementMode.DEFERRED
            }
        }
    }



    private inline fun executeCommand(invoke: Invoke, action: () -> JSObject) {
        try {
            invoke.resolve(action())
        } catch (e: IllegalStateException) {
            invoke.reject(e.message)
        }
    }

    private inline fun executeVoidCommand(invoke: Invoke, action: () -> Unit) {
        try {
            action()
        } catch (e: IllegalStateException) {
            invoke.reject(e.message)
            return
        }
        invoke.resolve()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private inline fun executeSuspendingCommand(invoke: Invoke, crossinline action: suspend () -> JSObject) {
        GlobalScope.launch(Dispatchers.Default) {
            try {
                invoke.resolve(action())
            } catch (e: Exception) {
                invoke.reject(e.message)
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private inline fun executeSuspendingVoidCommand(invoke: Invoke, crossinline action: suspend () -> Unit) {
        GlobalScope.launch(Dispatchers.Default) {
            try {
                action()
            } catch (e: Exception) {
                invoke.reject(e.message)
                return@launch
            }
            invoke.resolve()
        }
    }
}
