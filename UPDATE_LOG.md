# Update Log

## 2026-05-20 - Prototype V2 OS and Mobile NFC Update

This update keeps SolWear focused on Prototype V2 hardware. Prototype V3 hardware work is intentionally paused, so the firmware and mobile companion now lean harder into what the current device can do well.

### SolWearOS

- Reimagined and refactored the watch UI around a Pebble-inspired monochrome visual language: high-contrast black and white surfaces, sharper primitive icons, simpler panels, and calmer motion.
- Added more detailed watchfaces, including dedicated GM and GN faces, plus richer digital, analog, wallet, and minimal faces.
- Animated the GM/GN faces: GN now has a drifting moon phase with twinkling stars, and GM now has a monochrome sunrise scene with pulsing rays.
- Reduced visual lag by raising the ST7789 SPI display path to 40 MHz, sending larger flush stripes, and pacing redraws so static screens are not rendered every frame.
- Updated wallet, receive, transaction review, settings, stats, onboarding, and game surfaces to feel more coherent on the 240 x 240 display.
- Expanded the signing review screen so SolWear can show sender, recipient, amount, network, fee, and a final trusted-sign confirmation step.
- Moved NFC on/off to a deliberate 5-second K1 hold and wallet lock to a deliberate 5-second K4 hold to reduce accidental security-state changes.

### SolWear Mobile

- Reworked the Prototype V2 NFC flow around the current hardware reality: the phone reads SolWear as a tag, with clear guidance to hold the phone antenna within roughly 3 cm of the watch coil.
- Updated signing UX for the two-tap flow: write the request, review/sign on SolWear, then tap again to read the signature.
- Added NFC signing session IDs and request metadata so the watch and phone can match responses to the right signing attempt.
- Updated send flow copy to make the watch the explicit review/signing surface before the phone broadcasts anything.
- Added focused tests for the NDEF signing payload and Type 4 tag APDU helpers.

### Prototype V2 Hardware Note

Current NFC behavior still reflects the Prototype V2 layout: tag mode is the practical phone-facing path, while reader mode remains too short-range for reliable phone interaction. The product direction for this update is therefore to make SolWear feel like a stronger trusted approval device and hardware wallet on the existing board, instead of depending on new hardware changes.
