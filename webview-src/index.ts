import {invoke} from "@tauri-apps/api/primitives";
import {InitRequest} from "../bindings/InitRequest";

export async function init(args: InitRequest) {
    await invoke('plugin:mobile-payments|init', {args})
}

export async function destroy() {
    await invoke('plugin:mobile-payments|destroy', {})
}

export async function startConnection() {
    await invoke('plugin:mobile-payments|start_connection', {})
}

export {InitRequest}
