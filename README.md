[![Telegram](art/badges/badge_tg.svg)](https://t.me/coolperseus)
[![Читать на русском](art/badges/badge_ru.svg)](README-RU.md)
[![Donate](art/badges/badge_dontate_en.svg)](https://dalink.to/dertefter)

<p align="center">
  <img src="art/banner.png" alt="Banner">
</p>

# CoolPerseus

An experimental Magisk module for **Xiaomi Mi Mix 3 (Perseus)** running **LineageOS**.

> [!NOTE]
> Tested on official LineageOS 22.2. Compatibility with other ROMs has not been tested or confirmed.

## Features

- An app for managing slider settings.
- Custom sounds for opening and closing the slider.
- Customizable animations when opening and closing the slider.
- Configurable actions triggered by opening and closing the slider.
- Remapping of the AI button actions.

## Installation

Download the latest `CoolPerseus_Magisk.zip` from [Releases](../../releases) and install it using the Magisk app.

## How to Create Your Own Sound Pack

A sound pack is a ZIP archive containing a collection of sounds and a preview image. You can find user-created sound packs [here](https://t.me/coolperseus).

##### Archive contents:

- Slider opening sound: the filename must contain `_open`.
- Slider closing sound: the filename must contain `_close`.
- Preview image: the filename must contain `image`.

> [!NOTE]
> You can add multiple opening and closing sounds!
> In this case, a random sound from the available ones will be played.

##### Example sound pack structure:

```text
soundpack.zip
├── image.png
├── sound_open_1.ogg
├── sound_open_2.ogg
├── sound_open_3.ogg
├── sound_close_1.ogg
└── sound_close_2.ogg
```

## Building from Source

If you want to build the module yourself, use the included PowerShell script.

1. Clone the repository:

   ```bash
   git clone https://github.com/dertefter/CoolPerseus.git
   cd CoolPerseus
   ```

2. Make sure Android SDK is installed and the `JAVA_HOME` environment variable is configured.

3. Run the build script:

   ```powershell
   ./build_module.ps1
   ```

4. The resulting Magisk module will be available at:

   `build/magisk/CoolPerseus_Magisk.zip`.

## 💎 Support My Work

[https://dalink.to/dertefter](https://dalink.to/dertefter)