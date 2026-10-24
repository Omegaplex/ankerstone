![Ankerstone logo above an active Ankerstone in an Ancient City](docs/images/ankerstone-banner.png)

# Ankerstone

A Fabric mod with redstone-controlled chunk anchors, plus Stillstone and Reinforced Stillstone blocks for building.

The Minecraft 26.2 port is a release candidate and still needs in-game testing.

## Chunk Anchoring

Place an Ankerstone and supply redstone power to keep chunks active without a player nearby.

| Mode | Redstone Input | Entity-Ticking Coverage | Comparator Output | Piston Movable |
|---|---|---|---|---|
| Off | 0 | None | 0 | Yes |
| Single | 1–14 | 1 chunk | 7 | No |
| Wide | 15 | 3×3 chunks | 15 | No |

Single keeps the chunk containing the Ankerstone entity-ticking. Wide keeps a 3×3 area entity-ticking, centered on that chunk, matching a portal chunk loader's entity-ticking coverage. Vanilla chunk loading also maintains supporting outer rings beyond these areas.

Both modes affect only the dimension where the Ankerstone is placed. Loading chunks in another dimension requires a separate anchor there.

Turning off the power releases the anchor. Active Ankerstones resume chunk loading automatically after a server restart.

Ankerstone has its own advancement tab. Activation credit goes to the player who placed the block, even if someone else powers it. If the placer is offline, they receive credit when they return.

## Building Blocks

Stillstone and Reinforced Stillstone each include full blocks, slabs, stairs, and walls.

Stillstone full blocks do not conduct redstone power. Reinforced Stillstone full blocks use normal solid-block conduction. Slabs, stairs, and walls follow ordinary Minecraft rules for their shapes.

All blocks require a pickaxe to drop items:
- Stillstone: any pickaxe.
- Reinforced Stillstone and Ankerstone: iron or better.
- Double slabs drop two slab items.

## Crafting

All recipes have recipe-book unlocks.

| Result | Ingredients and Arrangement | Output |
|---|---|---|
| Stillstone | Five cobbled deepslate and four lapis lazuli in a checkerboard, with deepslate in the corners and center | 5 |
| Reinforced Stillstone | One Stillstone surrounded by eight iron nuggets | 1 |
| Ankerstone | Reinforced Stillstone in the corners, redstone dust along the four sides, and an ender pearl in the center | 1 |

Both building-block families have crafting recipes for slabs, stairs, and walls. Their full blocks can also be cut into those shapes with a stonecutter.

## A Little Lore

Stillstone is deepslate altered with lapis to resist dimensional flux. Ironwork turns that resistance into a structure capable of containing the dimensional energies of an ender pearl. Redstone controls the finished Ankerstone.

Read the [lore](docs/lore.md) and [advancement notes](docs/advancements.md) for more.

## Requirements

- Minecraft Java Edition 26.2
- Fabric Loader 0.20.1 or later
- Fabric API 0.161.0 or later for Minecraft 26.2
- Java 25 or later

For multiplayer, install Ankerstone and Fabric API on both the client and server.

On dedicated servers, set `pause-when-empty-seconds=0` in `server.properties` if anchors should keep ticking with nobody connected. Ankerstone does not override whole-server pausing.

## Shaders

LabPBR material maps are included for compatible shaders. No additional PBR resource pack is needed for Ankerstone’s blocks. Shaders are optional.

For recessed surfaces, enable LabPBR support and Parallax Occlusion Mapping (POM) in your shader settings.

An optional **Ankerstone POM Compatibility** pack is available in Minecraft’s Resource Packs menu. It adjusts Wide’s glowing overlay so it does not float above the recessed surface. The pack starts disabled.

Disable the compatibility pack when playing without LabPBR shaders to restore Wide’s vanilla glow. 

## Development

See [Testing](docs/testing.md) for automated coverage and manual restart checks.

## License

- Code: [Mozilla Public License 2.0](https://www.mozilla.org/MPL/2.0/).
- Original textures and artwork: [Creative Commons Attribution-ShareAlike 4.0](https://creativecommons.org/licenses/by-sa/4.0/).
- See [NOTICE](NOTICE) for artwork credits and license scope.
