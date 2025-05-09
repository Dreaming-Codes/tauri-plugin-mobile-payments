use tauri::{AppHandle, command, Runtime};
use crate::{MobilePaymentsExt, ProductDetail, ProductPriceRequest, PurchaseRequest, UpdateSubscriptionRequest, ActiveSubTokenArgs, Result};

#[command]
pub(crate) async fn start_connection<R: Runtime>(app: AppHandle<R>) -> Result<()> {
    app.mobile_payments().start_connection().await
}

#[command]
pub(crate) async fn purchase<R: Runtime>(app: AppHandle<R>, args: PurchaseRequest) -> Result<()> {
    app.mobile_payments().purchase(args).await
}

#[command]
pub(crate) async fn get_product_price<R: Runtime>(app: AppHandle<R>, args: ProductPriceRequest) -> Result<ProductDetail> {
    app.mobile_payments().get_product_price(args).await
}

#[command]
pub(crate) async fn update_subscription<R: Runtime>(app: AppHandle<R>, args: UpdateSubscriptionRequest) -> Result<()> {
    app.mobile_payments().update_subscription(args).await
}

#[command]
pub(crate) async fn get_active_subscription_purchase_token<R: Runtime>(
    app: AppHandle<R>,
    args: ActiveSubTokenArgs,
) -> Result<Option<String>> {
    app.mobile_payments().get_active_subscription_purchase_token(args.product_id).await
}