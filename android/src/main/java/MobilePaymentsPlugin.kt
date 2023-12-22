package codes.dreaming.plugin.mobile_payments

import android.app.Activity
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke
import com.android.billingclient.api.BillingClient
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@InvokeArg
class InitArgs {
    var enablePendingPurchases: Boolean? = null
    var enableAlternativeBillingOnly: Boolean? = null
    var reInit: Boolean? = null
}

@InvokeArg
class PurchaseArgs {
    lateinit var productId: String
    var isSub: Boolean? = null
}

@TauriPlugin
class MobilePaymentsPlugin(private val activity: Activity) : Plugin(activity) {
    private val implementation = MobilePayments(activity)

    @Command
    fun init(invoke: Invoke) {
        executeCommand(invoke) {
            val args = invoke.parseArgs(InitArgs::class.java)
            implementation.init(
                args.enablePendingPurchases,
                args.enableAlternativeBillingOnly,
                args.reInit
            )
        }
    }

    @Command
    fun destroy(invoke: Invoke) {
        executeCommand(invoke) {
            implementation.destroy()
        }
    }

    @Command
    fun startConnection(invoke: Invoke) {
        executeSuspendingCommand(invoke) {
            implementation.startConnection()
        }
    }

    @Command
    fun purchase(invoke: Invoke) {
        executeSuspendingCommand(invoke) {
            val args = invoke.parseArgs(PurchaseArgs::class.java)
            implementation.purchase(args.productId, if (args.isSub) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP)
        }
    }

    private inline fun executeCommand(invoke: Invoke, action: () -> Unit) {
        try {
            action()
        } catch (e: IllegalStateException) {
            invoke.reject(e.message)
            return
        }
        invoke.resolve()
    }

    @OptIn(DelicateCoroutinesApi::class)
    private inline fun executeSuspendingCommand(invoke: Invoke, crossinline action: suspend () -> Unit) {
        GlobalScope.launch(Dispatchers.Default) {
            try {
                action()
                invoke.resolve()
            } catch (e: Exception) {
                invoke.reject(e.message)
            }
        }
    }
}
