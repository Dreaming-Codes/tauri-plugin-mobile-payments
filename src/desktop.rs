use serde::de::DeserializeOwned;
use tauri::{plugin::PluginApi, AppHandle, Runtime};

use crate::models::*;

pub fn init<R: Runtime, C: DeserializeOwned>(
  app: &AppHandle<R>,
  _api: PluginApi<R, C>,
) -> crate::Result<MobilePayments<R>> {
  Ok(MobilePayments(app.clone()))
}

/// Access to the mobile-payments APIs.
pub struct MobilePayments<R: Runtime>(AppHandle<R>);

impl<R: Runtime> MobilePayments<R> {
}
