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

## Solana Mobile Integration

SolWear integrates with Solana Mobile through the Android companion app in
`mobile/`. The phone acts as a thin Solana Mobile client: it prepares the
transaction, requests signing from the air-gapped SolWear device over NFC, then
receives only the signed payload and broadcasts it through Solana RPC. The
private key remains on the wearable and never reaches the phone.

The integration is designed around Mobile Wallet Adapter-style signing
semantics: the mobile side creates a standardized signing request/response flow,
while the hardware wallet performs the final offline approval and Ed25519
signature. The app is implemented in Kotlin/Jetpack Compose for Android and is
the Saga/Seeker-facing surface for pairing, wallet preview, NFC signing, and
transaction submission.

Relevant implementation and documentation:

- `mobile/app/src/main/java/com/solwear/mobile/viewmodel/WalletViewModel.kt` -
  prepares Solana sends, fetches blockhashes, submits signed transactions, and
  checks confirmation status
- `mobile/app/src/main/java/com/solwear/mobile/nfc/NfcSessionManager.kt` -
  manages the phone-side NFC signing session
- `mobile/app/src/main/java/com/solwear/mobile/nfc/NdefProtocol.kt` - defines
  the wallet, sign request, and sign response payloads
- `mobile/docs/NFC_PROTOCOL_NDEF.md` - documents the NFC signing protocol
- `mobile/docs/SOLANA_TX_FLOW.md` - documents the Solana transaction lifecycle

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
