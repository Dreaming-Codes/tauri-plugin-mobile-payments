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
import com.google.gson.Gson
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
    lateinit var productsId: List<String>
    lateinit var sub: String
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
            implementation.purchase(args.productId, if (args.isSub.toBoolean()) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP, args.obfuscatedAccountId)
        }
    }

    @Command
    fun getProductList(invoke: Invoke) {
        executeSuspendingCommand(invoke) {
            val args = invoke.parseArgs(ProductListArgs::class.java)
            val products = implementation.getProductList(args.productsId, if (args.sub.toBoolean()) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP);

            val jsonProducts = Gson().toJson(products)

            return@executeSuspendingCommand JSObject(jsonProducts)
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
            invoke.resolve()
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
                invoke.resolve()
            } catch (e: Exception) {
                invoke.reject(e.message)
                return@launch
            }
            invoke.resolve()
        }
    }
}
