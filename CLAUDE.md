# CLAUDE.md

## Project overview

Dig Again is a GTNH 2.8.4 / Minecraft 1.7.10 Forge server-side QoL mod. It observes normal block breaking, lets the original GregTech/Forge harvest and drop pipeline finish unchanged, and later restores matching natural GregTech ores with configurable probability and delay.

## Build and validation

Use the GTNH Gradle wrapper:

```bash
./gradlew spotlessApply build
```

On this machine, external downloads may require the local proxy:

```bash
JAVA_OPTS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897" ./gradlew spotlessApply build
```

Expected production jar:

```text
build/libs/digagain-1.0.0.jar
```

## CI/CD

GitHub repository:

```text
https://github.com/Prymon/DigAgain
```

Workflows:

- `.github/workflows/build-and-test.yml` delegates to the official `GTNewHorizons/GTNH-Actions-Workflows` build workflow.
- `.github/workflows/release-tags.yml` delegates tagged releases to the official GTNH release workflow.
- `.github/workflows/manual-release.yml` provides a simple manual release path that builds with Java 17 and uploads `build/libs/*.jar`.

Before pushing CI changes, run:

```bash
JAVA_OPTS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897" ./gradlew spotlessApply build
```

After pushing, verify with:

```bash
gh run list --limit 5
gh run view <run-id> --log-failed
```

## Important implementation constraints

- Do not cancel `BlockEvent.BreakEvent`.
- Do not directly trigger or replace drop logic.
- Do not infer GT ore identity from drops, localized names, or broad OreDictionary matches.
- Preserve normal Fortune/Silk Touch/GregTech behavior by only observing the pre-break block and scheduling a later restore.
- Default behavior should remain narrow: Overworld + natural GregTech ores only.
- Client install is optional on dedicated servers; this is server-side logic and uses `acceptableRemoteVersions = "*"`.

## GTNH ore compatibility

The user's current GTNH version is 2.8.4. The server jar inspected during development was:

```text
D:/GTNH2026/server/mods/gregtech-5.09.51.482.jar
```

That jar uses:

- `gregtech.common.blocks.BlockOres`
- `gregtech.common.blocks.BlockOresAbstract`
- `gregtech.common.blocks.TileEntityOres`

The detector should keep supporting those classes. It also contains compatibility for newer GT5U master classes:

- `gregtech.common.blocks.GTBlockOre`
- `gregtech.common.ores.GTOreAdapter`

If updating GTNH versions, re-check the GregTech jar before changing ore detection.

Useful local inspection commands:

```bash
jar tf "D:/GTNH2026/server/mods/gregtech-5.09.51.482.jar" | grep -E "GTOreAdapter|GTBlockOre|BlockOres|TileEntityOres|OreInfo"
javap -classpath "D:/GTNH2026/server/mods/gregtech-5.09.51.482.jar" -public gregtech.common.blocks.BlockOresAbstract gregtech.common.blocks.BlockOres gregtech.common.blocks.TileEntityOres
```

## Runtime commands

```text
/digagain reload
/digagain chance <0.0-1.0>
/digagain delay <ticks>
/digagain maxPerTick <count>
/digagain natural <true|false>
/digagain status
/digagain probe
```

## Installation paths used in this workspace

Server:

```text
D:/GTNH2026/server/mods/digagain-1.0.0.jar
```

Client instance:

```text
D:/GTNH2026/PrismLauncher-Windows-MinGW-w64-Portable-11.0.2/instances/GT_New_Horizons_2.8.4_Java_17-25/.minecraft/mods/digagain-1.0.0.jar
```

For dedicated server testing, the client jar can be removed.
