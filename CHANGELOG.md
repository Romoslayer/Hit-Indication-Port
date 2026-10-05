# Changelog

Versions follow the original mod's feature version. The Minecraft version is a build suffix
(`2.0.0+26.3`). All builds (Fabric, NeoForge and Forge, for 26.2 and 26.3) are released together.

## Unreleased

- Forge support: Forge builds for Minecraft 26.2 (Forge 65) and 26.3 (Forge 66), with the same
  features, config file (`hitindication-client.toml`) and keys as the NeoForge build. Forge has no
  built-in config screen, so the options are edited in the file (changes apply while the game runs)
  or with the toggle keys.

## 2.0.1

Fixes:

- "Heavy damage makes indicator larger" made heavy hits *smaller* (a 60% hit drew at half size).
  Hits of 30% of max health or more now grow with the damage (1.24x at 30%, 1.8x at 100%, at
  most 3x), and the blow that disables a shield draws its block indicator at twice the size, as
  the option and the README describe.
- Splash potions no longer show an indicator to players they did not affect (for example near
  the corners of the splash area, or when the debuff was too short to apply). Indicators now
  follow the effects and damage the game actually applied, one per potion per hit.
- Lingering potions no longer show an indicator the moment they shatter. The cloud shows one when
  it actually affects a player, including players who walk into it later.
- Instant Damage indicators now report the health actually lost (after the splash distance,
  Resistance, Protection and absorption) instead of an estimate that ignored distance and
  absorption.
- Lowering "Max Indicator Count" while indicators are on screen now applies straight away.
- Indicators no longer point the wrong way far from the world origin (float precision near the
  world border could put a nearby attacker a block to the side).
- Edge of Screen mode: indicators no longer stick partly off screen at diagonal angles.
- Turning hit indication, block indicators or crit/kill markers off now also hides those already
  on screen.
- A kill marker is no longer replaced by a crit marker sent for the same blow.
- Fabric: hits that kill the player (or are only survived thanks to a mod cancelling the death)
  now show their indicator and projectile marker, as on NeoForge.
- Fabric: a malformed `hitindication-client.json` is kept as `hitindication-client.json.broken-<time>`
  before being replaced with defaults, and the file is saved atomically, so an interrupted save no
  longer leaves it truncated.
- NeoForge: editing `hitindication-client.toml` by hand shortly after pressing a toggle key could be
  ignored when the edit restored a combination the key had just saved. Such edits now apply.
- Indicators and markers with unknown types or invalid coordinates from a mismatched server are
  ignored instead of being drawn as something else.

## 2.0.0 — first release of the port

- Port of Hit Indication 2.0 (Forge 1.18.2) to Minecraft 26.2 and 26.3 on Fabric and NeoForge.
  Same features, config options and defaults as the original. See [NOTICE.md](NOTICE.md) for the
  full list of changes.
- Players without the mod can join servers that have it.
- NeoForge: built-in config screen. Fabric: config screen via Mod Menu + Cloth Config (optional).
- NeoForge: pressing a toggle key (H/B/G) twice in quick succession no longer sometimes undoes the
  second press.
