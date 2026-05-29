use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "kebab-case")]
pub enum FlashMethod {
    Uf2,
    Serial,
    Unknown,
}

#[derive(Debug, Clone, Serialize)]
pub struct PrototypeProfile {
    pub id: &'static str,
    pub name: &'static str,
    pub mcu: &'static str,
    pub display: &'static str,
    pub flash: FlashMethod,
    pub caps: &'static [&'static str],
    pub vid_pid: &'static [(u16, u16)],
}

#[derive(Debug, Clone, Serialize)]
pub struct DeviceInfo {
    pub port: String,
    pub vid: Option<u16>,
    pub pid: Option<u16>,
    pub description: String,
    pub profile: PrototypeProfile,
}

pub const PROTOTYPES: &[PrototypeProfile] = &[
    PrototypeProfile {
        id: "prototype-a-rp2040-lcd169",
        name: "Prototype A (RP2040 + LCD 1.69)",
        mcu: "rp2040",
        display: "st7789-240x280",
        flash: FlashMethod::Uf2,
        caps: &["status", "watch-control", "apps", "diagnostics", "uf2"],
        vid_pid: &[(0x2e8a, 0x000a), (0x2e8a, 0x0003)],
    },
    PrototypeProfile {
        id: "prototype-b-rp2040-lcd169",
        name: "Prototype B (RP2040 + LCD 1.69)",
        mcu: "rp2040",
        display: "st7789-240x280",
        flash: FlashMethod::Uf2,
        caps: &["status", "watch-control", "apps", "diagnostics", "uf2"],
        vid_pid: &[(0x2e8a, 0x000a), (0x2e8a, 0x0003)],
    },
    PrototypeProfile {
        id: "prototype-c-rp2040-lcd169",
        name: "Prototype C (RP2040 + LCD 1.69)",
        mcu: "rp2040",
        display: "st7789-240x280",
        flash: FlashMethod::Uf2,
        caps: &["status", "watch-control", "apps", "diagnostics", "uf2"],
        vid_pid: &[(0x2e8a, 0x000a), (0x2e8a, 0x0003)],
    },
    PrototypeProfile {
        id: "prototype-d-esp32v2-eink154",
        name: "Prototype D (ESP32 v2 + e-ink 1.54)",
        mcu: "esp32",
        display: "waveshare-eink-1.54",
        flash: FlashMethod::Serial,
        caps: &["status", "watch-control", "apps"],
        vid_pid: &[],
    },
    PrototypeProfile {
        id: "prototype-2-esp32s3-lcd13",
        name: "Prototype 2 (ESP32-S3 + ST7789 240x240 color IPS)",
        mcu: "esp32s3",
        display: "st7789-240x240-color",
        flash: FlashMethod::Serial,
        caps: &[
            "status",
            "watch-control",
            "apps",
            "nfc",
            "battery",
            "charging",
        ],
        vid_pid: &[(0x303a, 0x4001), (0x303a, 0x1001)],
    },
];

pub fn resolve_profile(
    proto_id: &str,
    mcu: &str,
    display: &str,
    caps: &[String],
) -> PrototypeProfile {
    let proto_id = proto_id.trim().to_ascii_lowercase();
    if let Some(profile) = PROTOTYPES.iter().find(|p| p.id == proto_id) {
        return profile.clone();
    }
    let mcu = mcu.trim().to_ascii_lowercase();
    if mcu.contains("esp32s3") {
        return PROTOTYPES[4].clone();
    }
    if mcu == "esp32" {
        return PROTOTYPES[3].clone();
    }
    if mcu == "rp2040" || caps.iter().any(|cap| cap == "uf2") {
        return PROTOTYPES[0].clone();
    }
    if display.contains("st7789-240x240") {
        return PROTOTYPES[4].clone();
    }
    PrototypeProfile {
        id: "unknown",
        name: "Unknown prototype",
        mcu: "unknown",
        display: "unknown",
        flash: FlashMethod::Unknown,
        caps: &["status", "watch-control"],
        vid_pid: &[],
    }
}

pub fn detect_devices() -> anyhow::Result<Vec<DeviceInfo>> {
    let ports = serialport::available_ports()?;
    let mut devices = Vec::new();
    for port in ports {
        let (vid, pid) = match &port.port_type {
            serialport::SerialPortType::UsbPort(info) => (Some(info.vid), Some(info.pid)),
            _ => (None, None),
        };
        let profile = match (vid, pid) {
            (Some(v), Some(p)) => PROTOTYPES
                .iter()
                .find(|profile| profile.vid_pid.contains(&(v, p)))
                .cloned()
                .unwrap_or_else(|| resolve_profile("", "", "", &[])),
            _ => resolve_profile("", "", "", &[]),
        };
        devices.push(DeviceInfo {
            port: port.port_name,
            vid,
            pid,
            description: format!("{:?}", port.port_type),
            profile,
        });
    }
    devices.sort_by_key(|device| match device.profile.mcu {
        "esp32s3" => 0,
        "esp32" => 1,
        "rp2040" => 2,
        _ => 3,
    });
    Ok(devices)
}
