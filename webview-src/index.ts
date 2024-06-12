import {invoke} from "@tauri-apps/api/core";
import {PaymentEvent, ProductListRequest, PurchaseRequest} from "./bindings";
import {EventCallback, listen, UnlistenFn} from "@tauri-apps/api/event";


export async function startConnection() {
    await invoke('plugin:mobile-payments|start_connection', {})
}

export async function purchase(args: PurchaseRequest) {
    await invoke('plugin:mobile-payments|purchase', {args})
}

export async function getProductList(args: ProductListRequest) {
    return await invoke<object>('plugin:mobile-payments|get_product_list', {args})
}

export function listenForPurchases(handler: EventCallback<PaymentEvent>): Promise<UnlistenFn> {
    return listen("mobile-payments://event", handler);
}

export {PurchaseRequest}
