use serde::{Deserialize, Serialize};
use ts_rs::TS;

#[derive(Debug, Serialize, Deserialize, TS)]
#[ts(export)]
#[serde(rename_all = "camelCase")]
pub struct InitRequest {
  pub enable_pending_purchases: bool,
  pub enable_alternative_billing_only: bool,
  pub re_init: bool,
}

impl Default for InitRequest {
  fn default() -> Self {
    Self {
      enable_pending_purchases: true,
      enable_alternative_billing_only: false,
      re_init: true,
    }
  }
}
