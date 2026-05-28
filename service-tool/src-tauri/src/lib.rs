#[tauri::command]
fn devices() -> Result<serde_json::Value, String> {
    solwear_st_core::detect_devices()
        .map(|devices| serde_json::to_value(devices).unwrap_or_default())
        .map_err(|err| err.to_string())
}

#[tauri::command]
fn parse_status(line: String) -> Option<solwear_st_core::StatusHeartbeat> {
    solwear_st_core::parse_status_line(&line)
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    tauri::Builder::default()
        .invoke_handler(tauri::generate_handler![devices, parse_status])
        .run(tauri::generate_context!())
        .expect("failed to run solwear_st app");
}
