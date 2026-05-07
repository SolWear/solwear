# SolWear

SolWear is a transparent crypto smartwatch for Solana. This public repository
is the judge-facing Frontier Hackathon bundle: firmware, Android companion app,
desktop service tooling, profile material, and the website live together so the
whole prototype can be reviewed from one place.

## What Is Included

| Directory | Purpose | Stack |
| --- | --- | --- |
| `firmware/` | SolWearOS embedded firmware for the watch prototype | ESP-IDF, C, ESP32-S3 |
| `mobile/` | Android companion app for NFC pairing and Solana wallet flows | Kotlin, Jetpack Compose |
| `service-tool/` | Desktop utility for serial inspection, flashing, and device service workflows | Python, Tkinter |
| `website/` | Marketing site and preorder/signup experience | Next.js, React, Tailwind |
| `profile/` | Project profile and supporting judge material | Markdown |

## Prototype Hardware

- MCU: Lolin ESP32-S3 Mini
- Display: ST7789 240 x 240 IPS over SPI
- NFC: PN532 over I2C
- Power: TP4056 charger with 350 mAh LiPo
- Input: four active-low tactile buttons
- Storage: NVS for wallet material, SPIFFS for receipts and game state

## Build SolWearOS

```bash
cd firmware
idf.py set-target esp32s3
idf.py build
idf.py -p COM_PORT flash monitor
```

Replace `COM_PORT` with your board serial port.

## Run The Mobile App

Open `mobile/` in Android Studio and let Gradle sync, or build from the command
line:

```bash
cd mobile
./gradlew assembleDebug
```

Use an NFC-capable Android phone for the watch pairing/signing demo.

## Run The Service Tool

```bash
cd service-tool
pip install -r requirements.txt
python main.py
```

## Run The Website

```bash
cd website
npm install
npm run dev
```

The website can also be run with Docker:

```bash
cd website
docker-compose up --build
```

## Hackathon Review Notes

The repository is organized around the working prototype rather than a single
framework. Reviewers can inspect the watch OS, mobile NFC flow, service tool,
and website independently, or use this bundle as a map for how the full SolWear
system fits together.

## Links

- Website: https://solwear.watch
- X: https://x.com/SolWear_
- Instagram: https://instagram.com/solwear.watch
- TikTok: https://tiktok.com/@solwear
