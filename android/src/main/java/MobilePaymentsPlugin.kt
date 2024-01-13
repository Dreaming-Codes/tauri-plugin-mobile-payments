package codes.dreaming.plugin.mobile_payments

import android.app.Activity
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Channel
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke
import com.android.billingclient.api.BillingClient
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
}

@InvokeArg
class SetEventHandlerArgs {
    lateinit var handler: Channel
}

@TauriPlugin
class MobilePaymentsPlugin(private val activity: Activity) : Plugin(activity) {
    private val implementation = MobilePayments(activity)

    @Command
    fun init(invoke: Invoke) {
        executeCommand(invoke) {
            val args = invoke.parseArgs(InitArgs::class.java)
            implementation.init(
                args.alternative_billing_only
            )
        }
    }

    @Command
    fun setEventHandler(invoke: Invoke) {
        executeCommand(invoke) {
            val args = invoke.parseArgs(SetEventHandlerArgs::class.java)
            implementation.setEventHandler(args.handler)
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
            implementation.purchase(args.productId, if (args.isSub.toBoolean()) BillingClient.ProductType.SUBS else BillingClient.ProductType.INAPP)
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
