import {invoke} from "@tauri-apps/api/core";
import {PaymentEvent, ProductDetail, PurchaseRequest, ProductPriceRequest, UpdateSubscriptionRequest, ActiveSubTokenArgs } from "./bindings";
import {EventCallback, listen, UnlistenFn} from "@tauri-apps/api/event";

export const enum SubscriptionReplacementMode {
    /** The replacement takes effect immediately, and the remaining time will be prorated and credited to the user. */
    IMMEDIATE_WITH_TIME_PRORATION = "IMMEDIATE_WITH_TIME_PRORATION",
    /** The replacement takes effect immediately, and the billing cycle remains the same. The price for the remaining period will be charged. */
    IMMEDIATE_AND_CHARGE_PRORATED_PRICE = "IMMEDIATE_AND_CHARGE_PRORATED_PRICE",
    /** The replacement takes effect immediately, and the new price will be charged on next recurrence time. The remaining time is lost. */
    IMMEDIATE_WITHOUT_PRORATION = "IMMEDIATE_WITHOUT_PRORATION",
    /** The replacement takes effect immediately, and the user is charged full price of new plan. The remaining time is lost. */
    IMMEDIATE_AND_CHARGE_FULL_PRICE = "IMMEDIATE_AND_CHARGE_FULL_PRICE",
    /** The replacement takes effect when the old plan expires. */
    DEFERRED = "DEFERRED",
}


export async function startConnection() {
    await invoke('plugin:mobile-payments|start_connection', {})
}

export async function purchase(args: PurchaseRequest) {
    await invoke('plugin:mobile-payments|purchase', {args})
}

export async function getProductPrice(args: ProductPriceRequest) {
    return await invoke<ProductDetail>('plugin:mobile-payments|get_product_price', {args})
}

export function listenForPurchases(handler: EventCallback<PaymentEvent>): Promise<UnlistenFn> {
    return listen("mobile-payments://event", handler);
}

export async function updateSubscription(args: {
    newProductId: string;
    oldPurchaseToken: string;
    replacementMode?: SubscriptionReplacementMode; // Use the enum/constants
    obfuscatedAccountId?: string;
}): Promise<void> {
    // Map the enum/constant to the string expected by Rust/Kotlin
    const requestArgs: UpdateSubscriptionRequest = {
        newProductId: args.newProductId,
        oldPurchaseToken: args.oldPurchaseToken,
        // Provide a default if desired, or let Rust handle it
        replacementMode: args.replacementMode ?? SubscriptionReplacementMode.DEFERRED,
        obfuscatedAccountId: args.obfuscatedAccountId,
    };
    await invoke("plugin:mobile-payments|update_subscription", { args: requestArgs });
}

export async function getActiveSubscriptionPurchaseToken(productId: string): Promise<string | null> {
    const result = await invoke<{ purchaseToken: string | null }>(
        'plugin:mobile-payments|get_active_subscription_purchase_token',
        { args: { productId } }
    );
    return result.purchaseToken ?? null;
}

export {PurchaseRequest}