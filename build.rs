const COMMANDS: &[&str] = &["start_connection", "purchase", "get_product_price", "update_subscription"];

fn main() {
  tauri_plugin::Builder::new(COMMANDS)
      .android_path("android")
      .ios_path("ios")
      .build();
}
