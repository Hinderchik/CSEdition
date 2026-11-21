# CS Edition

Counter-Strike style shooter mod for **Minecraft 1.20.1** (Forge).

CS Edition brings a CS:GO-inspired combat layer to Minecraft: a dedicated HUD with four weapon slots and a weapon assignment system, so you can switch between primary, secondary, knife and grenade the way you would in Counter-Strike.

## Features

- **CS:GO-style HUD** — health, armor and ammo laid out like a classic tactical shooter
- **4 weapon slots** — primary, secondary, melee and grenade
- **Weapon assignment** — bind any weapon to a slot and switch between them with hotkeys
- Built on top of **TACZ** (Timeless and Classics Zero) for gun mechanics

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.3.0+ |
| Java | 17 |
| TACZ (Timeless and Classics Zero) | required |

## Installation

1. Install Minecraft Forge 47.3.0 or newer for 1.20.1.
2. Drop the TACZ mod into your `mods` folder.
3. Drop the CS Edition jar into the same `mods` folder.
4. Launch the game with the Forge profile.

## Building from source

```bash
git clone https://github.com/Hinderchik/CSEdition.git
cd CSEdition
./gradlew build
```

The compiled jar appears in `build/libs/`.

Requirements: JDK 17 and Gradle (wrapper included).

## Project structure

```
CSEdition/
├── src/main/          # mod sources and resources
├── build.gradle       # ForgeGradle 6.0.16 setup
├── gradle.properties  # mod metadata, MC/Forge versions
└── icon.png           # mod icon
```

## Version

Current version: **1.2.56**

## License

MIT

## Author

Hinderchik
