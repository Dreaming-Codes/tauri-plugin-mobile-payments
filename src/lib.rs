#![cfg(mobile)]

use tauri::{
    plugin::{Builder, TauriPlugin},
    Manager, Runtime,
};
use tauri::async_runtime::spawn_blocking;
use tauri::plugin::PluginHandle;

pub use models::*;

mod commands;
mod error;
mod models;

pub use error::{Error, Result};

#[cfg(target_os = "android")]
const PLUGIN_IDENTIFIER: &str = "codes.dreaming.plugin.mobile_payments";

#[cfg(target_os = "ios")]
tauri::ios_plugin_binding!(init_plugin_mobile-payments);

/// Access to the mobile-payments APIs.
pub struct MobilePayments<R: Runtime>(PluginHandle<R>);

impl<R: Runtime> MobilePayments<R> {
    pub fn destroy(&self) -> crate::Result<()> {
        self
            .0
            .run_mobile_plugin("destroy", ())
            .map_err(Into::into)
    }

    pub async fn start_connection(&self) -> crate::Result<()> {
        spawn_blocking({
            let app = self.0.clone();
            move || {
                app
                    .run_mobile_plugin("startConnection", ())
                    .map_err(Into::into)
            }
        }).await.map_err(crate::Error::SpawnBlockingError)?
    }

    pub async fn purchase(&self, payload: PurchaseRequest) -> crate::Result<()> {
        spawn_blocking({
            let app = self.0.clone();
            move || {
                app
                    .run_mobile_plugin("purchase", payload)
                    .map_err(Into::into)
            }
        }).await.map_err(crate::Error::SpawnBlockingError)?
    }
}

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
pub fn init<R: Runtime>(args: InitRequest) -> TauriPlugin<R> {
    Builder::new("mobile-payments")
        .invoke_handler(tauri::generate_handler![commands::start_connection, commands::purchase])
        .setup(|app, api| {
            #[cfg(target_os = "android")]
                let handle = api.register_android_plugin(PLUGIN_IDENTIFIER, "MobilePaymentsPlugin")?;
            #[cfg(target_os = "ios")]
                let handle = api.register_ios_plugin(init_plugin_mobile - payments)?;

            handle
                .run_mobile_plugin::<()>("init", args)
                .expect("failed to initialize mobile-payments plugin");

            app.manage(MobilePayments(handle));

            Ok(())
        })
        .build()
}
