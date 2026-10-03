# Attribution and license notice

This mod is an **unofficial, modified port** of *Hit Indication*. It is not affiliated with or
endorsed by the original authors.

## Original work

| | |
|---|---|
| **Title** | Hit Indication |
| **Authors** | Hamester ([TheHamester](https://github.com/TheHamester)) and Axovoxel |
| **Source** | <https://github.com/TheHamester/HitIndicator> (branch `1.18.2`, version 2.0, commit `22874ec`) |
| **Mod page** | <https://modrinth.com/mod/hit-indication> |
| **License** | [Creative Commons Attribution-ShareAlike 4.0 International (CC BY-SA 4.0)](https://creativecommons.org/licenses/by-sa/4.0/) |

Localizations included with the original, kept unchanged here except for one renamed key:

- Simplified and Traditional Chinese: HaHaWTH
- Swedish and toki pona: Emik
- Norwegian: Enitoni
- German: Lily
- Pirate Speak: Axovoxel
- Dutch and Russian: from the original repository

The textures, the mod icon and the translations come from the original repository and are used
under the same license.

## This port

Port of version 2.0 to Minecraft 26.2 and 26.3, for Fabric and NeoForge, by Romoslayer.

**License:** this port, as an adaptation of a CC BY-SA 4.0 work, is itself licensed under
**CC BY-SA 4.0**. The full license text is in [`LICENSE`](LICENSE) and is included in every jar.
No additional terms or technical restrictions are applied. Anyone may share and adapt it under the
same license, provided they give credit to the original authors and to this port, and indicate
their own changes.

The work is provided as-is, without warranties of any kind, as set out in section 5 of the license.

### Changes from the original

Gameplay behaviour and every config option are kept from version 2.0. Changed:

- **Mod loaders and Minecraft version.** Rewritten from Forge 1.18.2 to Minecraft 26.2/26.3 on both
  Fabric and NeoForge. The code is split into a shared `common` module and thin `fabric` and
  `neoforge` modules.
- **Rendering.** Ported from `PoseStack`/`RenderSystem` shader-colour calls to 26.x's
  `GuiGraphicsExtractor` with a 2D pose stack and per-draw ARGB tint. Indicators are drawn as the
  last HUD layer, as before.
- **Networking.** Forge `SimpleChannel` packets replaced with `CustomPacketPayload` records and
  stream codecs. The channel is optional, so players without the mod can still join a server
  running it.
- **Damage detection.** Forge's `LivingAttackEvent`/`LivingDamageEvent` replaced with each
  loader's post-damage hook (NeoForge `LivingDamageEvent.Post`, Fabric `AFTER_DAMAGE`). Shield
  blocking is read from the game's own blocked-damage result instead of a copy of the old
  `canBlockDamageSource` check, since 26.x reworked shields into the `BlocksAttacks` component;
  likewise "shield about to break" now uses the attacker's `disableBlockingForSeconds` weapon
  property.
- **Critical hits and thrown potions on Fabric.** Fabric has no events for these, so two small
  mixins (`ServerPlayer#crit`, `AbstractThrownPotion#onHit`) stand in for Forge's
  `CriticalHitEvent` and `ProjectileImpactEvent`. NeoForge still uses those events.
- **Instant Damage prediction.** The copy of the vanilla potion damage calculation no longer awards
  "damage resisted" statistics, since it only predicts damage and would otherwise count it twice.
- **Configuration.** NeoForge keeps `hitindication-client.toml` with the original section and keys,
  and gains NeoForge's built-in config screen. The three toggle keys no longer read their setting
  straight back from the config after saving: a background reload of the file could occasionally
  undo a quick second key press, so the NeoForge build now keeps its own copy of those three values. Fabric stores the same options in
  `hitindication-client.json`, with an optional Mod Menu + Cloth Config screen.
- **Translations.** The key-binding category key was renamed to the 26.x format
  (`key.category.hitindication.hitindication`); English config screen titles and tooltips were
  added, taken from the original config comments.
- **Mod name.** Shown as "Hit Indication (Unofficial Port)" so it is not mistaken for an official
  release.
