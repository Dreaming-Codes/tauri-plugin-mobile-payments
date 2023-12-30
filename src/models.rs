use serde::{Deserialize, Serialize};
use tauri::ipc::Channel;
use ts_rs::TS;

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct KotlinInitRequest {
  pub enable_pending_purchases: bool,
  pub enable_alternative_billing_only: bool,
  pub re_init: bool,
  pub channel: Channel
}

impl KotlinInitRequest {
  pub fn from_ts(ts: TsInitRequest, channel: Channel) -> Self {
    Self {
      enable_pending_purchases: ts.enable_pending_purchases,
      enable_alternative_billing_only: ts.enable_alternative_billing_only,
      re_init: ts.re_init,
      channel
    }
  }
}

#[derive(Deserialize, TS)]
#[serde(rename_all = "camelCase")]
#[ts(export)]
pub struct TsInitRequest {
  pub enable_pending_purchases: bool,
  pub enable_alternative_billing_only: bool,
  pub re_init: bool
}

impl From<KotlinInitRequest> for TsInitRequest {
  fn from(ts: KotlinInitRequest) -> Self {
    Self {
      enable_pending_purchases: ts.enable_pending_purchases,
      enable_alternative_billing_only: ts.enable_alternative_billing_only,
      re_init: ts.re_init
    }
  }
}

#[derive(Debug, Serialize, Deserialize, TS)]
#[ts(export)]
#[serde(rename_all = "camelCase")]
pub struct PurchaseRequest {
  pub product_id: String,
  pub is_sub: bool,
}
