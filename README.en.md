[Português (BR)](README.md) · **English**

# HY300 Launcher — prototype

A new lightweight launcher with a tech look for the **HY300** projector (Android 11, Allwinner H713). It keeps the functions of the original and aims to turn the projector into a **gaming projector**.

> **Status:** navigable design prototype, technical analysis of the original launcher, **launcher 0.4** and **HY300 Games app 0.2** (with its own emulation engine). Both **build and run on the projector**. Game performance has **not been measured yet**.
>
> Most documents in `docs/` are written in Brazilian Portuguese.

▶ **See how the project is going:** [online prototype](https://kelvin-rbs.github.io/Launcher-HY300-prototipo/)

## How this project is made
The idea is to use **AI** and **REA** from start to finish:
- **AI (Claude, by Anthropic):** analysis of the device data, documentation, prototype and the launcher code.
- **REA (Reverse Engineer Anything):** a reverse-engineering tool used to analyze the device apps. Its Android module (with JADX) is set up and was used to decompile `com.htc.htcsettings`: the Projection screen is already mapped ([`docs/10-analise-htcsettings.md`](docs/10-analise-htcsettings.md)).

The author sets the direction, tests on the device and decides what goes in.

## The idea
The HY300 already projects a big image on any wall. What is missing is a system to match: fast, good looking, easy to use with just the remote control and made for gaming. The vision:

- **Clean home screen**, with app shortcuts, a clock and quick access to network, Bluetooth and settings.
- **Retro 16-bit games** (starting with the Super Nintendo) in a **dedicated games app** with the launcher's look and its **own engine** (Snes9x core), no touch controls, compatible with Bluetooth controllers in general. During a game, **holding Start for 3 seconds** asks whether to close the game and go back to the list.
- **Consoles and PCs through HDMI**, one click away.
- **Bluetooth or USB controller** for playing (the projector's remote only has a D-pad and OK).
- **Reversible for now:** the launcher is installed as a regular app. The future plan is to **replace the firmware**.

> Goals, not promises: HDMI input latency and game performance on this hardware (32-bit, `low_ram`) **have not been measured yet**.

## Why replace the original launcher
It is heavy for the hardware, sends data to tracking servers (Baidu) and to a manufacturer store, and has screens the device does not use (battery). The original stays installed **for now**, so you can go back whenever you want (**for now**), because the future plan is to replace the firmware.

## What the prototype has
| Part | Description |
|---|---|
| **Editable shortcuts** | Up to 7 installed apps, on the left of the row |
| **Fixed shortcuts** | **Games** (Super Nintendo-style controller icon; opens the HY300 Games app), **Files** (internal storage and USB drive), **HDMI** and **Manage shortcuts** |
| **Top bar** | Network, Bluetooth and Settings (gear), all navigable, plus the clock |
| **Settings** | Network, Bluetooth, Language, **Apps** (all apps; OK opens, holding OK for 2 s uninstalls), Projection (zoom, method, keystone, brightness and color), Audio (volume and cursor beep), Date and time and, last, About the device |
| **Games** | Separate app with a list showing only the game names, a prompt to connect a Bluetooth controller and a pause menu |
| **Sound** | A tick when the cursor moves, can be turned off in Audio |
| **Background** | Lake at sunset (AI-generated) |

Part of Settings, such as Projection, only works on the device by opening the original system screen, because a regular app has no permission to change those settings. Details in [`docs/07-configuracoes.md`](docs/07-configuracoes.md).

## How to see the prototype
Open the [online prototype](https://kelvin-rbs.github.io/Launcher-HY300-prototipo/) or, on your computer, the file `prototype/index.html`.
- **Arrow keys:** navigate (with sound) · **Enter:** open · **Esc:** back · **P:** simulate a USB drive.
- **Up arrow** goes to the top icons (Network, Bluetooth, Settings).
- **Manage shortcuts** edits the 7 shortcuts on the left.
- On a phone, tap the cards.

## Repository structure
| Folder | Contents |
|---|---|
| `prototype/` | Navigable prototype (1280×720) |
| `docs/` | Device, extraction, analysis of the original, function map, design, roadmap, settings, games app and Snes9x EX+ analysis (in Portuguese) |
| `scripts/` | PowerShell (ADB) scripts to extract data, test screens, send games and generate `latest.json` |
| `jogos/` | Starting folder for games (contents kept out of Git) |
| `android/` | Android project (Kotlin): `app` module (launcher) and `games` module (HY300 Games app). Guide in `android/README.md` |

Main documents (Portuguese): [`docs/05-design.md`](docs/05-design.md) · [`docs/06-roadmap.md`](docs/06-roadmap.md) · [`docs/14-app-jogos.md`](docs/14-app-jogos.md) · [`docs/15-analise-snes9x-ex.md`](docs/15-analise-snes9x-ex.md) · [`docs/16-motor-proprio.md`](docs/16-motor-proprio.md) · [`docs/10-analise-htcsettings.md`](docs/10-analise-htcsettings.md) · [`docs/11-atualizacao-e-sobre.md`](docs/11-atualizacao-e-sobre.md) · [`docs/12-atualizacao-do-launcher-pelo-github.md`](docs/12-atualizacao-do-launcher-pelo-github.md) · [`docs/13-projeto-android.md`](docs/13-projeto-android.md) · [`CHANGELOG.md`](CHANGELOG.md)

## Summary of findings
- The original launcher (`com.htc.htclauncherhighenglishd08`, v1.4.5) is just an app grid. It runs as a system app and does **not** control keystone, focus or image.
- Those functions live in `com.htc.htcsettings`. The new launcher opens the original screens through intents.
- **HDMI** opens with `com.softwinner.awlivetv/.MainActivity` (tested).
- **Zoom, projection method and keystone** do not open directly (non-exported activities). The way is to open Projection and pick the card. **Brightness and color** opens directly.
- The device has **one** physical HDMI port, but the system declares three HDMI inputs and one AV. The launcher shows only "HDMI".
- **Local update** (USB drive) triggers a system service (`android.rockchip.update.service`); the **online** one is delegated to `com.htc.htcotaupdate`. The new launcher opens the original screens ([`docs/11-atualizacao-e-sobre.md`](docs/11-atualizacao-e-sobre.md)).
- The settings app itself (`htcsettings`) also has Baidu analytics; only a custom firmware removes it.
- A preinstalled app (Office Suite) ran in the background using about 143 MB of memory, and no analyzed system app depends on it. Apps like this can be removed for the user (reversibly).
- No battery: the low-battery screens are not included.
- Tracking in the original: Baidu analytics and a manufacturer store API. The new launcher includes none of it.

## Next steps
1. **Test v20 on the projector:** cursor tick, new games showing up in the list by themselves, and lower audio delay (turn on "Show fps and diagnostics" in the emulator settings).
2. Measure game performance and delay, and tune the sound ([`docs/16-motor-proprio.md`](docs/16-motor-proprio.md)).
3. Test NES (needs the `fceumm` core) and add other systems.
4. **Phone remote control:** the launcher runs a small server on the local Wi-Fi network and the phone controls the projector (d-pad, OK, Back, Home) through a web page, with a PIN shown on screen. Start with the launcher and the games; outside them it needs an accessibility service.
5. **Integrated live TV:** the `tv` module (TV AO VIVO) is already in the repository; study the projector's UniTV app with REA to reuse it or replicate live TV in the launcher, without bypassing logins or DRM.
6. Own Settings screens in the launcher, opening the original ones when a setting needs system permission ([`docs/07-configuracoes.md`](docs/07-configuracoes.md)).
7. Launcher updates through GitHub ([`docs/12`](docs/12-atualizacao-do-launcher-pelo-github.md)).
8. Future: custom firmware ([`docs/06-roadmap.md`](docs/06-roadmap.md)).

## Notices
- This repository does **not** include APKs, device dumps, games (ROMs) or the emulator core (`.so`). See `.gitignore`.
- For now, the new launcher does not change the firmware and needs no root. **Replacing the firmware is a future goal and can brick the device:** keep a copy of the original and have a recovery plan first.
- Use only games you have the right to use.
- Not affiliated with the HY300 manufacturer, Snes9x/EmuEx, Nintendo or Anthropic.

## Credits
- Background image generated by AI.
- Emulation core: **Snes9x** (through the libretro interface), downloaded from the libretro website and **not included** here. The Snes9x license is non-commercial.
- Analysis and prototype made with AI (Claude, by Anthropic) and REA.

## License
The code and documentation are under the MIT license (`LICENSE`). The background image was AI-generated and is not covered by that license.
