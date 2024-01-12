use serde::{Deserialize, Serialize};
use tauri::ipc::Channel;
use tsync::tsync;

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[tsync]
pub struct PurchaseRequest {
  pub product_id: String,
  pub is_sub: bool,
}

#[derive(Serialize)]
pub struct InitRequest {
  pub alternative_billing_only: bool
}
