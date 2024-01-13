use tauri::{AppHandle, command, Runtime};
use crate::{MobilePaymentsExt};

use crate::{PurchaseRequest, Result};

#[command]
pub(crate) async fn start_connection<R: Runtime>(app: AppHandle<R>) -> Result<()> {
    return app.mobile_payments().start_connection().await;
}

#[command]
pub(crate) async fn purchase<R: Runtime>(app: AppHandle<R>, args: PurchaseRequest) -> Result<()> {
    return app.mobile_payments().purchase(args).await;
}
