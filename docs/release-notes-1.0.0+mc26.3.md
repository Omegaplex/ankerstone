# Ankerstone 1.0.0 for Minecraft 26.3

The first stable release of Ankerstone.

Ankerstone keeps chunks active through redstone power:
- **Off:** no chunk loading; movable by pistons.
- **Single:** power levels 1–14 keep the anchor’s chunk entity-ticking.
- **Wide:** power level 15 keeps a centered 3×3 area entity-ticking.

Active anchors work in their own dimension and resume after a world or server restart. Comparator output is 0, 7, or 15 for Off, Single, and Wide.

Stillstone and Reinforced Stillstone include full blocks, slabs, stairs, and walls, with crafting and stonecutting recipes. Stillstone full blocks do not conduct redstone; Reinforced Stillstone full blocks do.

Includes an advancement tab. Activation credit goes to the original placer, even when another player activates the anchor. Offline credit is delivered when the placer returns.

## Requirements

- Minecraft Java Edition **26.3**
- Fabric Loader **0.20.1 or later**
- Fabric API **0.165.0 or later**, built for Minecraft 26.3
- Java **25 or later**

Install Ankerstone and Fabric API on both the client and server.

On dedicated servers, set `pause-when-empty-seconds=0` if anchors should keep ticking while nobody is connected.

## Optional Shader Support

LabPBR material maps are included. Compatible shaders can add normal mapping, emission, and POM depth.

Enable the bundled **Ankerstone POM Compatibility** resource pack when using LabPBR shaders with POM. Disable it when playing without compatible shaders to restore Wide’s vanilla glow.

Shaders are not required for gameplay.
