use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
pub enum Command {
    StatusNow,
    Brightness(u8),
    ClockSync(u64),
    App(String),
    NavHome,
    NavBack,
    RebootBootsel,
    Raw(String),
}

impl Command {
    pub fn as_wire(&self) -> String {
        match self {
            Self::StatusNow => "status now".into(),
            Self::Brightness(value) => format!("bri {}", value.min(&100)),
            Self::ClockSync(epoch) => format!("clock sync {epoch}"),
            Self::App(name) => format!("app {name}"),
            Self::NavHome => "nav home".into(),
            Self::NavBack => "nav back".into(),
            Self::RebootBootsel => "reboot bootsel".into(),
            Self::Raw(raw) => raw.trim().into(),
        }
    }
}

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
pub struct StatusHeartbeat {
    pub battery_pct: u8,
    pub voltage: f32,
    pub heap_bytes: u32,
    pub steps: u32,
    pub uptime_sec: u64,
    pub charging: bool,
    pub temperature_c: Option<f32>,
    pub proto: String,
    pub mcu: String,
    pub display: String,
    pub caps: Vec<String>,
}

pub fn parse_status_line(text: &str) -> Option<StatusHeartbeat> {
    let payload = text.strip_prefix("[STATUS]")?.trim();
    let mut kv = std::collections::BTreeMap::new();
    for token in payload.split_whitespace() {
        if let Some((key, value)) = token.split_once('=') {
            kv.insert(key.trim().to_ascii_lowercase(), value.trim().to_string());
        }
    }
    Some(StatusHeartbeat {
        battery_pct: parse_u8(kv.get("batt").or_else(|| kv.get("battery")), 0),
        voltage: parse_f32(kv.get("volt").or_else(|| kv.get("voltage")), 0.0),
        heap_bytes: parse_u32(kv.get("heap").or_else(|| kv.get("freeheap")), 0),
        steps: parse_u32(kv.get("steps").or_else(|| kv.get("step")), 0),
        uptime_sec: parse_u64(kv.get("uptime").or_else(|| kv.get("up")), 0),
        charging: parse_bool(kv.get("charging")),
        temperature_c: kv
            .get("temp")
            .or_else(|| kv.get("tempc"))
            .and_then(|v| v.parse().ok()),
        proto: kv
            .get("proto")
            .or_else(|| kv.get("prototype"))
            .cloned()
            .unwrap_or_default(),
        mcu: kv.get("mcu").cloned().unwrap_or_default(),
        display: kv.get("display").cloned().unwrap_or_default(),
        caps: kv
            .get("caps")
            .map(|caps| {
                caps.split(',')
                    .map(|s| s.trim().to_ascii_lowercase())
                    .filter(|s| !s.is_empty())
                    .collect()
            })
            .unwrap_or_default(),
    })
}

fn parse_u8(value: Option<&String>, default: u8) -> u8 {
    value
        .and_then(|v| v.parse::<f32>().ok())
        .map(|v| v as u8)
        .unwrap_or(default)
}

fn parse_u32(value: Option<&String>, default: u32) -> u32 {
    value
        .and_then(|v| v.parse::<f32>().ok())
        .map(|v| v as u32)
        .unwrap_or(default)
}

fn parse_u64(value: Option<&String>, default: u64) -> u64 {
    value
        .and_then(|v| v.parse::<f64>().ok())
        .map(|v| v as u64)
        .unwrap_or(default)
}

fn parse_f32(value: Option<&String>, default: f32) -> f32 {
    value.and_then(|v| v.parse().ok()).unwrap_or(default)
}

fn parse_bool(value: Option<&String>) -> bool {
    matches!(value.map(|v| v.to_ascii_lowercase()), Some(v) if ["1", "true", "yes", "on"].contains(&v.as_str()))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn parses_heartbeat() {
        let status = parse_status_line("[STATUS] batt=87 volt=4.05 heap=180432 steps=10 uptime=22 charging=1 proto=prototype-2-esp32s3-lcd13 mcu=esp32s3 display=st7789-240x240-color caps=status,nfc").unwrap();
        assert_eq!(status.battery_pct, 87);
        assert!(status.charging);
        assert_eq!(status.caps, vec!["status", "nfc"]);
    }
}
