use tauri::{
    plugin::{Builder, TauriPlugin},
    Manager, Runtime,
};

pub use models::*;

#[cfg(desktop)]
mod desktop;
#[cfg(mobile)]
mod mobile;

mod commands;
mod error;
mod models;

pub use error::{Error, Result};

#[cfg(desktop)]
use desktop::MobilePayments;
#[cfg(mobile)]
use mobile::MobilePayments;

/// Extensions to [`tauri::App`], [`tauri::AppHandle`] and [`tauri::Window`] to access the mobile-payments APIs.
pub trait MobilePaymentsExt<R: Runtime> {
    fn mobile_payments(&self) -> &MobilePayments<R>;
}

impl<R: Runtime, T: Manager<R>> crate::MobilePaymentsExt<R> for T {
    fn mobile_payments(&self) -> &MobilePayments<R> {
        self.state::<MobilePayments<R>>().inner()
    }
}

/// Initializes the plugin.
pub fn init<R: Runtime>() -> TauriPlugin<R> {
    Builder::new("mobile-payments")
        .invoke_handler(tauri::generate_handler![commands::init])
        .setup(|app, api| {
            #[cfg(mobile)]
                let mobile_payments = mobile::init(app, api)?;
            #[cfg(desktop)]
                let mobile_payments = desktop::init(app, api)?;
            app.manage(mobile_payments);

            Ok(())
        })
        .build()
}
