# Saturnic

This is the Saturnic client for Minecraft 1.21.11.
It does not currently receive automated updates.

### Join our discord - https://discord.gg/auHTtNAqRq

<p>
  <a href="https://github.com/Unknown_Skittle/Saturnic/releases">
    <img src="https://img.shields.io/github/downloads/Unknown_Skittle/Saturnic/total?color=green&label=Total%20Downloads" alt="Total Downloads" />
  </a>
  <a href="https://github.com/Unknown_Skittle/Saturnic/commits">
  <img src="https://img.shields.io/github/commit-activity/m/Unknown_Skittle/Saturnic?label=Commits%20(last%20month)&color=yellow" alt="month" />
  </a>
  <a href="https://github.com/Unknown_Skittle/Saturnic/releases">
    <img src="https://img.shields.io/github/v/release/Unknown_Skittle/Saturnic?color=blue&label=Latest%20Release" alt="Latest Release" />
  </a>
  <a href="https://discord.gg/auHTtNAqRq">
    <img src="https://img.shields.io/discord/1298742596633497744?color=7289DB&label=Discord" alt="Discord" />
  </a>
</p>

![# badge](assets/readme/no-stops-no-regrets.svg)
![# badge](assets/readme/ensuring-code-integrity.svg)
![# badge](assets/readme/works-on-selfmerging.svg)


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

## Plugin development

See https://github.com/Unknown_Skittle/Saturnic for information

---

## FAQ

<details>
<summary>How to open ClickGUI?</summary>

Default keybind is: P  

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

## Platform compatibility

The client is Java code and does not bundle architecture-specific native libraries. Its runtime compatibility therefore depends on the Java, Minecraft launcher, and LWJGL/Fabric installation available on the target system:

- 64-bit x86 (x86_64) is the expected desktop target. 32-bit x86 is not supported by the Minecraft 1.21.11 runtime.
- AArch64/ARM64 can work when a compatible Java 21 and Minecraft/LWJGL runtime are available; it is not independently packaged or tested here.
- Linux with Xorg or Wayland relies on the display backend selected and supported by the installed LWJGL/GLFW runtime.
- BSD is not an officially verified target. It requires a compatible Java 21 runtime and a Minecraft/LWJGL distribution that supports the specific BSD system.

No source-level OS or CPU checks are used by the client itself. These notes describe runtime dependencies, not tested platform guarantees.

## Player location intel

Enable logging with `-intel start` and stop it with `-intel stop`. While enabled, the client records nearby player sightings and visit density locally in `Saturnic/player-intel.json` under the Minecraft game directory, separated by server and dimension. View recent player coordinates with `-intel players`. Add a base marker at your current position with `-intel base <name>`, remove it with `-intel remove <name>`, and list saved markers with `-intel list`. Open the top-down heatmap with `-intel map`. Replace `-` with the configured command prefix if it has been changed.

---

## How to Build

1. Clone the repository:

    ```bash
    git clone https://github.com/Unknown_Skittle/Saturnic.git  
    cd saturnic
    ```
2. In order to get saturnic-api dependency, you need to configure your PAT-token in your root .gradle/gradle.dependency

3. Build with Gradle:

    ```bash
    ./gradlew build
    ```

The compiled JAR will be located at:  
`build/libs/saturnic-<version>.jar`

saturnic-client is packaged with saturnic-api inside of it.

---

## License

This project is licensed under the MIT License. You are free to contribute, distribute, fork, or reuse any part.

---

## Special Thanks

- [cattyngmd](https://github.com/cattyngmd)

- [CatFormat](https://github.com/cattyngmd/CatFormat)
