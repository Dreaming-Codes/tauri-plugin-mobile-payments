import {invoke} from "@tauri-apps/api/primitives";
import {InitRequest} from "../bindings/InitRequest";

export async function init(args: InitRequest) {
  await invoke('plugin:mobile-payments|init', {args})
}

export {InitRequest}
