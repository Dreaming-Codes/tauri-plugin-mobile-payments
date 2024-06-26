use serde::{Deserialize, Serialize};
use tauri::ipc::Channel;
use tsync::tsync;

#[derive(Serialize)]
pub(super) struct SetEventHandlerArgs {
  pub handler: Channel
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[tsync]
pub struct PurchaseRequest {
  pub product_id: String,
  pub is_sub: bool,
  pub obfuscated_account_id: Option<String>
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[tsync]
pub struct ProductPriceRequest {
  pub product_id: String,
  pub sub: bool
}

#[derive(Serialize)]
pub struct InitRequest {
  pub alternative_billing_only: bool
}


#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[tsync]
struct AccountIdentifiers {
  pub obfuscated_account_id: String,
  pub obfuscated_profile_id: String,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[tsync]
pub struct ProductDetail {
  pub price: String
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[tsync]
struct Purchase {
  pub account_identifiers: AccountIdentifiers,
  pub acknowledged: bool,
  pub auto_renewing: bool,
  pub developer_payload: String,
  pub order_id: String,
  pub original_json: String,
  pub package_name: String,
  pub products: Vec<String>,
  pub purchase_state: i64,
  pub purchase_time: i64,
  pub purchase_token: String,
  pub quantity: i64,
  pub signature: String,
  pub skus: Vec<String>,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[tsync]
struct BillingResult {
  pub debug_message: String,
  pub response_code: i64,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
#[tsync]
struct PaymentEvent {
  pub billing_result: BillingResult,
  pub purchases: Vec<Purchase>,
}
