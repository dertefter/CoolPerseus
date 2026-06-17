<p align="center">
  <img src="art/banner.png" alt="Banner">
</p>

# CoolPerseus

Magisk module for the **Xiaomi Mi Mix 3 (Perseus)** running **LineageOS**. 

### Features
- **Slider Settings App**: App to control slider settings
- **Official Sounds**: Restores the original Mi Mix 3 slider sound effects
- **Edges Glow**: Animation when slider opening or closing
- **Slider Customization**: 
  - Configure specific actions when the slider is opened
  - Configure specific actions when the slider is closed
- **AI Button Mapping**: Actions when AI Button pressed

### Tested on:
- Xiaomi Mi Mix 3 (Perseus)
- LineageOS 22.2

### Installation

Download the latest `CoolPerseus_Magisk.zip` from the [Releases](../../releases) section and install it with Magisk App.

### Building from Source

If you want to build the module yourself, you can use the provided PowerShell script.

1. Clone the repository:
   ```bash
   git clone https://github.com/dertefter/CoolPerseus.git
   cd CoolPerseus
   ```
2. Ensure you have the Android SDK and `JAVA_HOME` environment variable set.
3. Run the build script:
   ```powershell
   ./build_module.ps1
   ```
4. The flashable Magisk module will be generated in `build/magisk/CoolPerseus_Magisk.zip`.

### Credits
Official sound effects and images used in this project are the property of **Xiaomi Inc.**
