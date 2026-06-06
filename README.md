# Dig Again

[![Build and test](https://github.com/Prymon/DigAgain/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/Prymon/DigAgain/actions/workflows/build-and-test.yml)
[![Release tagged build](https://github.com/Prymon/DigAgain/actions/workflows/release-tags.yml/badge.svg)](https://github.com/Prymon/DigAgain/actions/workflows/release-tags.yml)

Dig Again is a small GTNH / Minecraft 1.7.10 Forge mod that lets normal ore harvesting finish first, then probabilistically restores matching natural GregTech ores after a configurable delay.

## Target environment

- Modpack: GregTech New Horizons 2.8.4
- Minecraft: 1.7.10
- Forge: 10.13.4.1614
- Build system: GTNH ExampleMod / RetroFuturaGradle convention plugin

## Behavior

When a player breaks a supported ore block:

1. The original Forge/GregTech break event continues normally.
2. The mod does not cancel the event.
3. The mod does not change drops, Fortune, Silk Touch, or GregTech ore drop logic.
4. If the block matches the configured scope and passes the restore chance roll, it is queued.
5. After the configured delay, the original block state is restored if the target position is still air or replaceable.

By default, the mod targets only GTNH/GregTech natural ores in the Overworld.

## GTNH ore detection

GTNH 2.8.4 uses the older GregTech ore block/tile-entity system in `gregtech-5.09.51.482.jar`:

- `gregtech.common.blocks.BlockOres`
- `gregtech.common.blocks.BlockOresAbstract`
- `gregtech.common.blocks.TileEntityOres`

Dig Again detects those classes at runtime and checks the natural ore flag from tile-entity NBT when available.

The detector also keeps compatibility with newer GT5U source layouts that use:

- `gregtech.common.blocks.GTBlockOre`
- `gregtech.common.ores.GTOreAdapter`

Fallback block whitelist and OreDictionary detection exist, but are disabled by default to avoid false positives.

## Installation

### Dedicated server

Install this jar on the server:

```text
build/libs/digagain-1.0.0.jar
```

The mod is server-side logic and sets `acceptableRemoteVersions = "*"`, so clients do not need to install it when connecting to a dedicated server.

### Single player

Install the jar in the client instance, because single player runs an integrated server.

## Build

Use the included Gradle wrapper:

```bash
./gradlew build
```

The production jar is generated at:

```text
build/libs/digagain-1.0.0.jar
```

If network access needs the local proxy on this Windows machine:

```bash
JAVA_OPTS="-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7897 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7897" ./gradlew build
```

## CI/CD

This repository uses GitHub Actions with Java 25, matching the current GTNH Gradle plugin requirement:

- `Build and test`: runs `./gradlew spotlessCheck build --no-daemon` on pushes and pull requests to `master`/`main`, then uploads jar artifacts.
- `Release tagged build`: runs when a tag is pushed and publishes `build/libs/digagain-*.jar` to a GitHub release.
- `Manual release`: can be started from the Actions tab with a tag input; it builds and uploads `build/libs/*.jar` to a GitHub release.

Typical release flow:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Configuration

Forge config file:

```text
config/digagain.cfg
```

Important defaults:

```properties
general {
  D:restoreChance=0.30
  I:delayTicks=20
  I:maxRestoresPerTick=256
  I:maxQueuedRestores=8192
  B:requireAirOrReplaceable=true
  B:deferUnloadedChunks=false
}

dimensions {
  I:whitelist <
    0
  >
}

detection {
  B:enableGtOreDetector=true
  B:requireNaturalGtOre=true
  B:enableFallbackBlockWhitelist=false
  B:enableFallbackOreDictionary=false
}
```

## Commands

Requires permission level 2:

```text
/digagain reload
/digagain chance <0.0-1.0>
/digagain delay <ticks>
/digagain maxPerTick <count>
/digagain natural <true|false>
/digagain status
/digagain probe
```

Recommended first test:

```text
/digagain chance 1.0
/digagain delay 20
/digagain probe
```

Then break a natural GT ore in the Overworld. It should drop normally and restore after the delay.

## Safety notes

- The mod does not overwrite non-replaceable blocks at the restore location.
- Restore entries are in memory only and are not persisted across server restarts.
- Unloaded chunks are skipped by default.
- Queue size is bounded by `maxQueuedRestores`.
