use tauri::{AppHandle, command, Runtime};
use tauri::ipc::Channel;
#[cfg(mobile)]
use crate::{MobilePaymentsExt, KotlinInitRequest};

use crate::{Error, PurchaseRequest, Result, TsInitRequest};

#[command]
pub(crate) fn init<R: Runtime>(app: AppHandle<R>, args: TsInitRequest, channel: Channel) -> Result<()> {
    #[cfg(mobile)]
    return app.mobile_payments().init(KotlinInitRequest::from_ts(args, channel));
    Err(Error::UnsupportedPlatform)
}

#[command]
pub(crate) fn destroy<R: Runtime>(app: AppHandle<R>) -> Result<()> {
    #[cfg(mobile)]
    return app.mobile_payments().destroy();
    Err(Error::UnsupportedPlatform)
}

#[command]
pub(crate) async fn start_connection<R: Runtime>(app: AppHandle<R>) -> Result<()> {
    #[cfg(mobile)]
    return app.mobile_payments().start_connection().await;
    Err(Error::UnsupportedPlatform)
}

#[command]
pub(crate) async fn purchase<R: Runtime>(app: AppHandle<R>, args: PurchaseRequest) -> Result<()> {
    #[cfg(mobile)]
    return app.mobile_payments().purchase(args).await;
    Err(Error::UnsupportedPlatform)
}
