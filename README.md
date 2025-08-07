# NuclearMusic
Noteblock music for Nukkit servers

## Features
- Supports NBS files of any version
- Full pitch range support
- Custom instruments
- Global playback
- Play music through note blocks
- Loop playback
- Sequential playback
- Allow control by players with different permission levels

## Supported Nukkit Branches
- [x] CloudBurstMC Nukkit
- [x] Nukkit-PM1E (Supports music playback, but console errors may occur, and playback speed (tempo) may be affected)
- [ ] PowerNukkitX
- [x] Nukkit-MOT
- [x] Netease Nukkit

## Usage
**Place your NBS music files in the `plugins/NuclearMusic/tracks` folder.  
You can modify plugin configurations via the `config.yml` file (configuration instructions are included in the file).  
Obtain the control tool using the command `/give <player> diamond_hoe:9999` or `/give <player> diamond_hoe 1 9999`. Short-press (or right-click) a note block with this tool to make it play your music.  
In non-global playback mode, multiple note blocks can play music simultaneously on the server. In global playback mode, only one note block can play music; operations on other note blocks will be invalid. New note blocks can only be set after the current music-playing note block is destroyed.  
To use custom instruments, use Note Block Studio to set the instrument name to the corresponding Bedrock Edition sound name.**

## Referenced Code
- [EaseCation NoteBlockAPI](https://github.com/EaseCation/NoteBlockAPI)
- [PetteriM1 NuclearMusic](https://github.com/PetteriM1/NuclearMusic)

## Additional Notes
**This plugin contains code partially written by AI**