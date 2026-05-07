# SolWear Mobile

SolWear Mobile is the Android companion app for the SolWear smartwatch. It
uses Jetpack Compose, NFC, and Solana RPC integrations to pair with the watch,
read wallet identity over NDEF, show balances, and prepare Solana transaction
flows for the prototype.

## What It Does

- Onboards a user into the SolWear mobile companion experience
- Reads SolWear watch data over NFC / NDEF Type 4
- Displays wallet balance, history, settings, and terminal-style screens
- Prepares send flows that coordinate with the watch signing surface
- Includes documentation for the NFC protocol, security model, and Solana
  transaction flow

## Stack

- Kotlin
- Android Gradle Plugin
- Jetpack Compose and Material 3
- Retrofit and OkHttp
- Kotlin serialization
- Bouncy Castle

## Repository Layout

| Path | Purpose |
| --- | --- |
| `app/src/main/java/com/example/solvare/` | Main Android app source |
| `app/src/main/java/com/example/solvare/nfc/` | NFC session and NDEF helpers |
| `app/src/main/java/com/example/solvare/data/solana/` | Solana RPC models and repository |
| `app/src/main/java/com/example/solvare/ui/` | Compose theme, components, and screens |
| `docs/` | Product, protocol, architecture, and test documentation |

## Build

Open the repository in Android Studio and let Gradle sync, or build from the
command line:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

## Run

Use an Android phone with NFC enabled. Install the debug build, open SolWear,
and tap the phone to the watch NFC antenna during the pairing or signing flow.

## Notes

Local Android Studio state, Gradle caches, APK/AAB outputs, and
`local.properties` are intentionally not committed.
