import {invoke} from "@tauri-apps/api/primitives";

export async function execute() {
  await invoke('plugin:mobile-payments|execute')
}
