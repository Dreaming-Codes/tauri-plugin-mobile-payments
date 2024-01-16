import {invoke} from "@tauri-apps/api/core";
import {PaymentEvent, PurchaseRequest} from "./bindings";
import {EventCallback, listen, Options, UnlistenFn} from "@tauri-apps/api/event";


export async function startConnection() {
    await invoke('plugin:mobile-payments|start_connection', {})
}

export async function purchase(args: PurchaseRequest) {
    await invoke('plugin:mobile-payments|purchase', {args})
}

export function listenForPurchases(handler: EventCallback<PaymentEvent>): Promise<UnlistenFn> {
    return listen("mobile-payments://event", handler);
}

export {PurchaseRequest}
