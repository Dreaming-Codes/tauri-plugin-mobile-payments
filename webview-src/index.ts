// @ts-ignore
import {invoke} from "@tauri-apps/api/core";
import {PurchaseRequest} from "./bindings";


export async function startConnection() {
    await invoke('plugin:mobile-payments|start_connection', {})
}

export async function purchase(args: PurchaseRequest) {
    await invoke('plugin:mobile-payments|purchase', {args})
}

export {PurchaseRequest}
