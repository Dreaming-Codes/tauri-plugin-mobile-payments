package codes.dreaming.plugin.mobile_payments

import android.app.Activity
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Plugin
import app.tauri.plugin.Invoke

@InvokeArg
class InitArgs {
    var enablePendingPurchases: Boolean = false
    var enableAlternativeBillingOnly: Boolean = false
    var reInit: Boolean = false
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

    private inline fun executeCommand(invoke: Invoke, action: () -> Unit) {
        try {
            action()
        } catch (e: IllegalStateException) {
            invoke.reject(e.message)
            return
        }
        invoke.resolve()
    }
}
