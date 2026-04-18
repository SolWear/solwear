"""Prototype catalog and helpers for SolWear hardware variants."""

PROTOTYPES = {
    "prototype-a-rp2040-lcd169": {
        "name": "Prototype A (RP2040 + LCD 1.69)",
        "mcu": "rp2040",
        "display": "st7789-240x280",
        "flash": "uf2",
        "caps": {"status", "watch-control", "apps", "diagnostics", "uf2"},
    },
    "prototype-b-rp2040-lcd169": {
        "name": "Prototype B (RP2040 + LCD 1.69)",
        "mcu": "rp2040",
        "display": "st7789-240x280",
        "flash": "uf2",
        "caps": {"status", "watch-control", "apps", "diagnostics", "uf2"},
    },
    "prototype-c-rp2040-lcd169": {
        "name": "Prototype C (RP2040 + LCD 1.69)",
        "mcu": "rp2040",
        "display": "st7789-240x280",
        "flash": "uf2",
        "caps": {"status", "watch-control", "apps", "diagnostics", "uf2"},
    },
    "prototype-d-esp32v2-eink154": {
        "name": "Prototype D (ESP32 v2 + e-ink 1.54)",
        "mcu": "esp32",
        "display": "waveshare-eink-1.54",
        "flash": "serial",
        "caps": {"status", "watch-control", "apps"},
    },
    "prototype-e-esp32s3-lcd13": {
        "name": "Prototype 2 (ESP32-S3 + ST7789 240x240 color IPS)",
        "mcu": "esp32s3",
        "display": "st7789-240x240-color",
        "flash": "serial",
        "caps": {"status", "watch-control", "apps", "nfc", "battery", "charging"},
        "pinout": {
            "nfc_pn532": {"VCC": "3.3v", "GND": "GND", "SCL": "GP6", "SDA": "GP5"},
            "display_st7789": {
                "GND": "GND", "VCC": "3.3v", "SCL": "GP3", "SDA": "GP4",
                "RES": "GP7", "DC": "GP8", "BLK": "GP9",
            },
            "buttons": {"K1_UP": "GP13", "K2_DOWN": "GP12", "K3_HASH": "GP11", "K4_STAR": "GP10"},
            "battery_adc": {"PIN": "GP2", "DIVIDER": "100K/100K (ratio 2.0)"},
            "power_tp4056": {"OUT+": "5V", "OUT-": "GND", "B+": "LW 303040 3.7V 350mAh"},
        },
        "usb_vid_pid": [("303A", "4001"), ("303A", "1001")],
    },
    "prototype-2-esp32s3-lcd13": {
        "name": "Prototype 2 (ESP32-S3 + ST7789 240x240 color IPS)",
        "mcu": "esp32s3",
        "display": "st7789-240x240-color",
        "flash": "serial",
        "caps": {"status", "watch-control", "apps", "nfc", "battery", "charging"},
        "pinout": {
            "nfc_pn532": {"VCC": "3.3v", "GND": "GND", "SCL": "GP6", "SDA": "GP5"},
            "display_st7789": {
                "GND": "GND", "VCC": "3.3v", "SCL": "GP3", "SDA": "GP4",
                "RES": "GP7", "DC": "GP8", "BLK": "GP9",
            },
            "buttons": {"K1_UP": "GP13", "K2_DOWN": "GP12", "K3_HASH": "GP11", "K4_STAR": "GP10"},
            "battery_adc": {"PIN": "GP2", "DIVIDER": "100K/100K (ratio 2.0)"},
            "power_tp4056": {"OUT+": "5V", "OUT-": "GND", "B+": "LW 303040 3.7V 350mAh"},
        },
        "usb_vid_pid": [("303A", "4001"), ("303A", "1001")],
    },
}


GENERIC_BY_MCU = {
    "rp2040": {
        "name": "RP2040 prototype",
        "mcu": "rp2040",
        "display": "unknown",
        "flash": "uf2",
        "caps": {"status", "watch-control", "apps", "uf2"},
    },
    "esp32": {
        "name": "ESP32 prototype",
        "mcu": "esp32",
        "display": "unknown",
        "flash": "serial",
        "caps": {"status", "watch-control", "apps"},
    },
}


def normalize_proto_id(value: str) -> str:
    if not value:
        return ""
    return value.strip().lower()


def parse_caps(caps_value):
    if caps_value is None:
        return set()
    if isinstance(caps_value, (list, tuple, set)):
        return {str(v).strip().lower() for v in caps_value if str(v).strip()}
    parts = [p.strip().lower() for p in str(caps_value).split(",")]
    return {p for p in parts if p}


def resolve_profile(proto_id: str, mcu: str = "", display: str = "", caps=None):
    pid = normalize_proto_id(proto_id)
    mcu_norm = (mcu or "").strip().lower()
    display_norm = (display or "").strip().lower()
    cap_set = parse_caps(caps)

    if pid in PROTOTYPES:
        out = dict(PROTOTYPES[pid])
        out["id"] = pid
    elif mcu_norm in GENERIC_BY_MCU:
        out = dict(GENERIC_BY_MCU[mcu_norm])
        out["id"] = pid or f"generic-{mcu_norm}"
    else:
        out = {
            "id": pid or "unknown",
            "name": "Unknown prototype",
            "mcu": mcu_norm or "unknown",
            "display": display_norm or "unknown",
            "flash": "unknown",
            "caps": {"status", "watch-control"},
        }

    if mcu_norm:
        out["mcu"] = mcu_norm
    if display_norm:
        out["display"] = display_norm

    merged_caps = set(out.get("caps", set()))
    merged_caps.update(cap_set)
    out["caps"] = merged_caps
    return out
