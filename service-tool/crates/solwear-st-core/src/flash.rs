use std::{path::Path, process::Command};

use crate::device::{FlashMethod, PrototypeProfile};

pub fn flash_firmware(
    profile: &PrototypeProfile,
    port: &str,
    firmware: &Path,
) -> anyhow::Result<()> {
    match profile.flash {
        FlashMethod::Serial => flash_serial(profile, port, firmware),
        FlashMethod::Uf2 => {
            anyhow::bail!("UF2 flashing is handled by copying the .uf2 to an RPI-RP2 drive")
        }
        FlashMethod::Unknown => anyhow::bail!("unknown flash method for {}", profile.name),
    }
}

fn flash_serial(profile: &PrototypeProfile, port: &str, firmware: &Path) -> anyhow::Result<()> {
    let chip = if profile.mcu == "esp32s3" {
        "esp32s3"
    } else {
        "esp32"
    };
    let status = Command::new("python")
        .args([
            "-m",
            "esptool",
            "--chip",
            chip,
            "--port",
            port,
            "--baud",
            "921600",
            "--before",
            "default_reset",
            "--after",
            "hard_reset",
            "write_flash",
            "-z",
            "--flash_mode",
            "dio",
            "--flash_freq",
            "80m",
            "0x10000",
        ])
        .arg(firmware)
        .status()?;
    if !status.success() {
        anyhow::bail!("esptool failed with status {status}");
    }
    Ok(())
}
