use serde::de::DeserializeOwned;
use tauri::{
  plugin::{PluginApi, PluginHandle},
  AppHandle, Runtime,
};

use crate::models::*;

#[cfg(target_os = "android")]
const PLUGIN_IDENTIFIER: &str = "codes.dreaming.plugin.mobile_payments";

#[cfg(target_os = "ios")]
tauri::ios_plugin_binding!(init_plugin_mobile-payments);

// initializes the Kotlin or Swift plugin classes
pub fn init<R: Runtime, C: DeserializeOwned>(
  _app: &AppHandle<R>,
  api: PluginApi<R, C>,
) -> crate::Result<MobilePayments<R>> {
  #[cfg(target_os = "android")]
  let handle = api.register_android_plugin(PLUGIN_IDENTIFIER, "MobilePaymentsPlugin")?;
  #[cfg(target_os = "ios")]
  let handle = api.register_ios_plugin(init_plugin_mobile-payments)?;
  Ok(MobilePayments(handle))
}

/// Access to the mobile-payments APIs.
pub struct MobilePayments<R: Runtime>(PluginHandle<R>);

impl<R: Runtime> MobilePayments<R> {
  pub fn init(&self, payload: InitRequest) -> crate::Result<()> {
    self
      .0
      .run_mobile_plugin("init", payload)
      .map_err(Into::into)
  }
  
  pub fn destory(&self) -> crate::Result<()> {
    self
      .0
      .run_mobile_plugin("destory", ())
      .map_err(Into::into)
  }
}
