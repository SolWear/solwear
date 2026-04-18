<div align="center">

# SolWear

**A from-scratch smartwatch platform with a built-in Solana wallet.**

*Hardware. Firmware. Tooling. All open within the org.*

[![RP2040](https://img.shields.io/badge/MCU-RP2040-blueviolet)]()
[![Solana](https://img.shields.io/badge/chain-Solana-9945FF)]()
[![PlatformIO](https://img.shields.io/badge/build-PlatformIO-orange)]()
[![Python](https://img.shields.io/badge/tools-Python%203.9%2B-blue)]()

</div>

---

## What is SolWear?

SolWear is a **wearable Solana wallet** built on a Waveshare RP2040-Touch-LCD-1.69 board. It runs a custom OS — **SolWearOS** — with a square-icon app launcher, on-device wallet, NFC contract testing, step counting, and a fully animated touch UI. A companion **desktop service tool** pairs with it over USB for live debugging, settings editing, and one-click firmware flashing.

The whole platform — OS, apps, drivers, dev tools — is built and maintained inside this organization.

## Repositories

| Repo | Description | Stack |
|---|---|---|
| [**solwear_os**](https://github.com/SolWear/solwear_os) | Smartwatch firmware: HAL, core OS, UI, apps, Solana wallet, NFC | C++ · PlatformIO · Arduino |
| [**solwear_service_tool**](https://github.com/SolWear/solwear_service_tool) | Desktop debugger: live console, status dashboard, UF2 flasher | Python · Tkinter · pyserial |
| [**solwear**](https://github.com/SolWear/solwear) | This repo · org landing page, prototypes, design assets | — |

## Hardware

| Component | Part |
|---|---|
| MCU | **RP2040** (Waveshare board, 16 MB flash, 264 KB SRAM) |
| Display | **ST7789V2** 240×280 1.69" IPS touchscreen |
| Touch | **CST816S** capacitive controller |
| IMU | **QMI8658** 6-axis (steps, gestures) |
| NFC | **PN532 v3** (lazy-init — off by default for power & security) |
| Battery | **1200 mAh LiPo** |
| Audio | Passive piezo buzzer |

## Highlights

- **Square-icon app launcher** — Settings · Wallet · NFC · Health · Game · Alarm · Stats · Charging
- **Solana wallet** with NFC tag write/read for `solana:` URIs
- **NFC off by default** — PN532 only powers on when wallet/NFC app actually needs it
- **Animated charging screen** — auto-pushed on USB plug-in, auto-dismissed on removal
- **Live `[STATUS]` heartbeat** over USB CDC consumed by the service tool
- **Strip-mode display fallback** — device still boots even if 131 KB sprite alloc fails
- **Procedural icons & wallpapers** — drawn at runtime, near-zero flash cost
- **Three watch faces** — digital, analog, minimal
- **LittleFS** for persistent settings, wallet, and weekly step history

## Architecture

```
   ┌─────────────────────────┐         ┌─────────────────────────┐
   │       SolWearOS         │   USB   │  SolWear Service Tool   │
   │   (RP2040 firmware)     │◄───────►│   (desktop, Windows)    │
   │                         │   CDC   │                         │
   │  apps  →  ui  →  core   │         │  console · status       │
   │           ↓             │         │  settings · firmware    │
   │          hal            │         │                         │
   └────────────┬────────────┘         └─────────────────────────┘
                │
   ┌────────────▼────────────┐
   │     RP2040 hardware     │
   │  Display · Touch · IMU  │
   │  NFC · Battery · Buzzer │
   └─────────────────────────┘
```

- **Single core, 30 fps** event/render loop
- **Lock-free ring buffer** for the event system
- **Lazy hardware** — peripherals like NFC stay off until an app needs them
- **HAL → Core → UI → Apps** layering keeps each tier independently testable

## Roadmap

- [x] Core OS, app framework, screen manager, persistence
- [x] All eight apps wired up
- [x] Charging overlay + battery/system stats
- [x] Lazy NFC initialization (off by default)
- [x] Service tool with console, dashboard, flasher
- [ ] BLE companion sync (Android / iOS)
- [ ] Real Solana transaction signing (Ed25519 on-device)
- [ ] BIP-39 seed phrase setup wizard
- [ ] OTA firmware updates over BLE
- [ ] More watch faces, wallpapers, and games

## Getting Started

**Build the firmware**
```bash
git clone https://github.com/SolWear/solwear_os.git
cd solwear_os
pio run
```
Then flash `.pio/build/solwearos/firmware.uf2` by holding BOOTSEL and dragging onto the `RPI-RP2` drive.

**Run the service tool**
```bash
git clone https://github.com/SolWear/solwear_service_tool.git
cd solwear_service_tool
run.bat
```
Or build a single-file `.exe`:
```bash
build.bat
```

## License

All SolWear projects are private. © SolWear. All rights reserved.

---

<div align="center">
<sub>Built with care on the RP2040 · Powered by Solana</sub>
</div>
