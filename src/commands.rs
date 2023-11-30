use tauri::{AppHandle, command, Runtime};
#[cfg(mobile)]
use crate::MobilePaymentsExt;

use crate::{Error, InitRequest, Result};

#[command]
pub(crate) fn init<R: Runtime>(app: AppHandle<R>, args: InitRequest) -> Result<()> {
    #[cfg(mobile)]
    return app.mobile_payments().init(args);
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
