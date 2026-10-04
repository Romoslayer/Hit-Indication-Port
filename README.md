# Hit Indication (Port)

Hit indicators that show where damage came from, as in Call of Duty or Halo, ported to
**Minecraft 26.2 and 26.3** for **Fabric** and **NeoForge**.

This is an **unofficial** port of [Hit Indication](https://modrinth.com/mod/hit-indication) by
Hamester and Axovoxel ([source](https://github.com/TheHamester/HitIndicator)), licensed
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/). See [NOTICE.md](NOTICE.md) for
full credits and the list of changes.

## Features

Everything from the original's version 2.0:

- Red indicators pointing towards whatever damaged you, fading out over time.
- Blue indicators when you block with a shield, twice the size when the blow will disable it
  (with "Heavy damage makes indicator larger" on).
- Optional crit and kill markers at the crosshair.
- Optional indicators for non-damaging negative potions, and a circular indicator for damage
  with no direction.
- Edge of Screen mode, distance scaling, damage scaling, custom colours, opacity, scale, fade time.
- Keys to toggle hit indication (H), block indicators (B) and Edge of Screen mode (G).

The mod must be installed on the server (or in singleplayer) to send indicators; players without
it can still join.

## Configuration

- **NeoForge:** `config/hitindication-client.toml` (same file and keys as the original), or
  Mods → Hit Indication → Config. Edits to the file apply while the game is running.
- **Fabric:** `config/hitindication-client.json`, or the config button in
  [Mod Menu](https://modrinth.com/mod/modmenu) when [Cloth Config](https://modrinth.com/mod/cloth-config)
  is also installed. The file is read when the game starts, so edit it with the game closed (or use
  the Mod Menu screen). If it cannot be parsed, it is kept as `hitindication-client.json.broken-<time>`
  and replaced with defaults.

With distance scaling on (the default), indicators shrink beyond the cutoff distance and are not
drawn at all from 10 blocks past it, as in the original. Turn distance scaling off to always see
distant hits.

## Requirements

| | Minecraft 26.2 | Minecraft 26.3 |
|---|---|---|
| Fabric | Fabric Loader 0.19.5+, Fabric API | Fabric Loader 0.19.5+, Fabric API |
| NeoForge | NeoForge 26.2.x | NeoForge 26.3.x |

Java 25.

## Building

```bash
./gradlew buildAll            # Fabric + NeoForge for 26.3
./gradlew buildAll -Pmc=26.2  # Fabric + NeoForge for 26.2
```

Jars land in `fabric/build/libs/` and `neoforge/build/libs/`.

## License

CC BY-SA 4.0, the same as the original. See [LICENSE](LICENSE) and [NOTICE.md](NOTICE.md).
