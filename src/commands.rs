use tauri::{AppHandle, command, Runtime};

use crate::{Error, InitRequest, Result};

#[command]
pub(crate) fn init<R: Runtime>(app: AppHandle<R>, args: InitRequest) -> Result<()> {
    #[cfg(mobile)]
    return app.mobile_payments().init(args);
    Err(Error::UnsupportedPlatform)
}
