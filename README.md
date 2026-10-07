# Saturnic

This is the Saturnic client for Minecraft 1.21.11.
It does not currently receive automated updates.

### Join our discord - https://discord.gg/fkvKDz57Ku

**Saturnic** is a modular and lightweight anarchy client base built for PVE and automation.  

Most popular Minecraft clients are closed-source, paid, and obfuscated, making them difficult to audit or trust. Some may include backdoors or malicious code.

This project started as a clean, open-source alternative aiming to be transparent, secure, and easy to extend without relying on unsafe third-party clients.

---

## Screenshots

<details>
<summary>View screenshots</summary>

<img width="1920" height="1080" alt="ClickGUI" src="assets/clickgui.png" />
<img width="1920" height="1080" alt="HudEditor" src="assets/hudeditor.png" />
<img width="1920" height="1080" alt="Friends" src="assets/friends.png" />
<img width="1920" height="1080" alt="Friends" src="assets/configs.png" />

</details>

---

## FAQ

<details>
<summary>How to open ClickGUI?</summary>

Default keybind is `P`. The keybind must remain a keyboard key; clearing it or loading an old config with no key restores `P`. Type `?saturnic` in chat to see help regardless of the configured command prefix.

</details>

<details>
<summary>What is the command prefix?</summary>

The default command prefix is `-`.

</details>

---

## Requirements

- Java 21  
- Gradle 8+  
- Minecraft 1.21.11 
- Fabric loader, API

## Player location intel

Enable logging with `-intel start` and stop it with `-intel stop`. While enabled, the client records nearby player sightings and your own visited areas locally in `Saturnic/player-intel.json` under the Minecraft game directory, separated by server and dimension. Toggle the `IntelMinimap` HUD feature for a movable, configurable minimap; assign its `OpenMap` setting to a key to open the interactive fullscreen map. The full map supports mouse-wheel zoom, drag panning, and recentering. Heat shows recent visit density and fades with age; player sightings and saved bases use separate markers. View recent player coordinates with `-intel players`. Add a base marker at your current position with `-intel base <name>`, remove it with `-intel remove <name>`, and list saved markers with `-intel list`. Open the fullscreen map directly with `-intel map`. Replace `-` with the configured command prefix if it has been changed.

## AnarchyMod first-run setup

On first startup, Saturnic asks whether you want to download and install the third-party AnarchyMod 1.4.3 for 6b6t. Choosing Yes downloads the JAR from the configured HTTPS release URL, validates that it is a Fabric mod archive, and places it in the Minecraft `mods/` directory for the next launch. Choosing No (or closing the prompt) records that choice and prevents another first-run prompt. Run `-anarchymod` to open the prompt again. Only install third-party code if you trust its publisher; the archive check confirms its format, not that the code is safe. Replace `-` with the configured command prefix if it has been changed.


## How to Build

1. Clone the repository:

    ```bash
    git clone https://github.com/Unknown_Skittle/Saturnic.git  
    cd Saturnic
    ```

2. Build with Gradle:

    ```bash
    ./gradlew build
    ```


The client JAR includes the `saturnic-api` dependency.

---

## License

This project is licensed under the GNU General Public License v3.0 only. See [LICENSE](./LICENSE)
