import {Channel, invoke} from "@tauri-apps/api/core";
import {InitRequest} from "../bindings/InitRequest";
import {PurchaseRequest} from "../bindings/PurchaseRequest";

export async function init(args: InitRequest, channel: Channel) {
    await invoke('plugin:mobile-payments|init', {args, channel})
}

export async function destroy() {
    await invoke('plugin:mobile-payments|destroy', {})
}

export async function startConnection() {
    await invoke('plugin:mobile-payments|start_connection', {})
}

export async function purchase(args: PurchaseRequest) {
    await invoke('plugin:mobile-payments|purchase', {args})
}

export {InitRequest, PurchaseRequest}
