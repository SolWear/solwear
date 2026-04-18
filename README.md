# SolWear

> The first transparent crypto smartwatch. Your Solana wallet, on your wrist.

SolWear is a hardware + software ecosystem for carrying a self-custodial
Solana wallet on your body. This repository is the public bundle prepared
for the **Frontier Hackathon 2026** — four independent projects that ship
together.

---

## What's in the box

| Directory | What it is | Stack |
|---|---|---|
| [`firmware/`](./firmware) | SolWearOS — the embedded OS running on the watch (Prototype 2: ESP32-S3 Mini + ST7789 240×240 color IPS + PN532 NFC + TP4056 charger). | PlatformIO · Arduino · C++ |
| [`service-tool/`](./service-tool) | Desktop GUI for flashing firmware, inspecting device telemetry, and calibrating the battery. | Python · Tkinter · pyserial · esptool |
| [`mobile/`](./mobile) | Android companion app — scans the watch over NFC, resolves the Solana pubkey, sends signed transactions. | Kotlin · Jetpack Compose · Material 3 |
| [`website/`](./website) | [solwear.watch](https://solwear.watch) marketing site and pre-order signup. | Next.js 15 · React 19 · Framer Motion · Tailwind |

---

## The watch

**Prototype 2** — the first working hardware:

- **MCU:** Lolin ESP32-S3 Mini (240 MHz, 8 MB flash)
- **Display:** ST7789 1.3" 240×240 color IPS (SPI)
- **Auth:** PN532 NFC reader (I²C) — pair / sign over tap
- **Power:** TP4056 charger + LW 303040 3.7 V 350 mAh LiPo
- **Inputs:** 4 tactile buttons (K1 UP, K2 DOWN, K3 HASH, K4 STAR)

### Pinout (GPIO)

| Peripheral | Pins |
|---|---|
| Display SPI | SCL GP3 · SDA GP4 · RES GP7 · DC GP8 · BLK GP9 |
| NFC I²C | SCL GP6 · SDA GP5 |
| Buttons | K1 GP13 · K2 GP12 · K3 GP11 · K4 GP10 |
| Battery ADC | GP2 (100 kΩ / 100 kΩ divider from B+, ratio 2.0) |
| Power | TP4056 OUT+ → 5 V · OUT- → GND |

---

## Build & flash the firmware

```bash
cd firmware
pio run -e prototype_e_esp32s3_lcd13              # build
pio run -e prototype_e_esp32s3_lcd13 -t upload    # flash (USB CDC, 921600)
```

---

## Run the service tool

```bash
cd service-tool
pip install -r requirements.txt
python main.py
```

Use **⚡ Quick Connect** to auto-detect a SolWear board by USB VID:PID.

---

## Run the website

```bash
cd website
npm install
npm run dev
```

Or with Docker (SQLite email list persists in a named volume):

```bash
cd website
docker-compose up --build
```

---

## Run the mobile app

Open `mobile/` in Android Studio, let Gradle sync, and run on any
Android device with NFC. Tap the watch on the back of the phone to pair.

---

## License

MIT — see `firmware/LICENSE`, `service-tool/LICENSE`, `website/LICENSE`.

---

## The team

Built in Ukraine. Three engineers — hardware, firmware, and app — in days.

Follow the build: [@SolWear_](https://x.com/SolWear_) ·
[@solwear.watch](https://instagram.com/solwear.watch) ·
[@solwear](https://tiktok.com/@solwear)
