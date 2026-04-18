![Hearthstone Mod](docs/images/banner.png)

A lightweight mod inspired by *World of Warcraft* that adds a craftable Hearthstone. This branch is now set up as a Minecraft `1.21.1` multiloader workspace targeting both NeoForge and Fabric, with the gameplay port still to come.

## Workspace Layout

- `common`: shared Minecraft code, shared resources, shared config/network/content scaffolding
- `neoforge`: NeoForge bootstrap, registries, events, payload registration, config-screen hookup
- `fabric`: Fabric bootstrap, registries, events, networking bridge, mixin scaffold, Mod Menu hookup

The old `1.20.1-Forge` implementation is kept as the porting reference and is no longer part of the active build path in this workspace.

Reference sources:

- Local backup: `B:\Minecraft Modding\Hearthstone_Mod backup`
- GitHub branch: `https://github.com/Sythiex/Hearthstone_Mod/tree/1.20.1-Forge`

## Requirements

- Java `21`

## Build Commands

Windows:

```powershell
.\gradlew.bat :common:compileJava
.\gradlew.bat build
.\gradlew.bat buildFabric
.\gradlew.bat buildNeoForge
```

macOS / Linux:

```bash
./gradlew :common:compileJava
./gradlew build
./gradlew buildFabric
./gradlew buildNeoForge
```

## Run Commands

Windows:

```powershell
.\gradlew.bat runFabricClient
.\gradlew.bat runNeoForgeClient
```

macOS / Linux:

```bash
./gradlew runFabricClient
./gradlew runNeoForgeClient
```

## Notes

- Shared assets, lang files, recipes, models, sounds, and other common resources live under `common/src/main/resources`.
- Loader-specific metadata stays local to each loader module:
  - Fabric: `fabric.mod.json`, mixin config
  - NeoForge: `META-INF/neoforge.mods.toml`
- `common` is a build module, not a separately published mod artifact.
