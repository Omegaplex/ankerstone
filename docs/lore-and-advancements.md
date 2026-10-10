# Ankerstone — Lore and Advancements

This document separates the mod's full fictional lore, material suitable for players and the project website, and the planned gameplay advancements.

**Status (2026-10-10):** Acquisition and activation advancements are implemented. Automated tests cover player attribution, offline credit, and removal during piston movement. Manual restart checks passed for ordinary and piston-moved placements. Offline pending credit across a restart still needs checking. Public-facing prose remains a draft.

## Full Lore

### Interpretation and central principle

This is Ankerstone's interpretation of the Minecraft world, **not official Minecraft canon**. The history connects familiar world elements into one proposed explanation; it does not claim that their creators or purposes are established by Mojang.

The world is subject to *dimensional flux*: a tendency for things and places to shift across the boundaries between places. This can be forced, detected, studied, resisted, or contained. Ankerstone's defining achievement is resistance, not a new source of energy.

**Dimensional resistance** is the mechanism. **Holding the world still** is the in-world description of what the completed device accomplishes: keeping nearby chunks loaded and ticking while powered. It does not freeze time, stop ordinary teleportation or portals, or create a new portal.

Earlier descriptions about the world's *memory* or chunks going to *sleep* are retired as literal explanations. They are not separate physics or capabilities.

### Earlier civilizations and traditions

**The spawner builders — application before understanding.** The builders of ancient dungeon spawners learned to use iron cages and particular structures to force creatures through from elsewhere. They found a repeatable use for dimensional phenomena without appreciating the danger of what they were manipulating. Their intent was to push something through an opening.

**The ancient city builders — recognition of danger.** A later civilization recognized the threat. In this interpretation, sculk sensing networks and defensive architecture reflect observation, caution, and efforts to detect or survive the forces involved. Whether these people knew of the spawner builders, or responded to their mistakes, is deliberately uncertain.

**The enchanting tradition — study and focus.** Other people began to investigate rather than force or fear the phenomenon. Lapis lazuli became a medium for focusing and understanding dimensional energy, consistent with its established role in enchanting. Fragments of this tradition's knowledge eventually allowed people to craft stillstone.

These are stages in accumulated knowledge, not a claim that every culture was part of a single uninterrupted organization.

### Materials and mechanism

**stillstone — passive resistance.** Cobbled deepslate altered with lapis becomes stillstone. The material is *discovered through deliberate crafting*, not mined as a naturally generated ore. Its unusual quality is that it passively resists dimensional flux. The lapis is a focusing medium, not a fuel. It is a usable building material, but not an activated machine. Its full block is redstone-inert; shaped blocks follow ordinary Minecraft slab, stair, and wall behavior where applicable.

Crafting: cobbled deepslate and lapis lazuli produce five stillstone blocks. Decorative slabs, stairs, and walls are available.

**reinforced stillstone — resistance engineered.** Ironwork built around stillstone turns a useful material into a deliberately constructed component. The iron binding reinforces and directs the structure; it does not provide electrical power. This is the transition from observing a property to building with it. The reinforced family also includes slabs, stairs, and walls. Unlike stillstone full blocks, reinforced full blocks use normal solid-block redstone conduction.

Crafting: one stillstone surrounded by eight iron nuggets produces one reinforced stillstone.

**Ankerstone — movement contained.** Reinforced stillstone, redstone, and an ender pearl make the finished mechanism. An ender pearl naturally tends toward movement between places. The device contains that tendency and holds it still. **The pearl is not a battery.** Redstone governs activation and the extent of the device's effect.

Crafting: four reinforced stillstone at the corners, four redstone dust at the cardinal sides, and an ender pearl in the center produce one Ankerstone. The construction surrounds the pearl with resistant material and a controllable redstone arrangement.

Ankerstone keeps its affected chunks loaded and ticking while active:

| Redstone input | Mode | Meaning |
|---|---|---|
| 0 | Off | No chunk anchoring |
| 1–14 | Single | Active, smaller coverage |
| 15 | Wide | Active, wider coverage |

The central contrast is deliberate: **spawners force something through a dimensional opening; Ankerstone holds the world still around one.** They use related phenomena for opposite purposes. The Ankerstone does not itself open a traversable portal.

### Terminology

Use lowercase **stillstone** and **reinforced stillstone** for the materials in lore prose, and capitalized **Ankerstone** for the completed mechanism. Item and advancement names use ordinary title capitalization where appropriate.

Avoid treating world memory, sleeping chunks, time freezing, or pearl-powered electricity as literal explanations.

## Player- and Site-Facing Lore

The text in this section is **draft copy**, not yet approved final website text. It is intentionally shorter and less explanatory than the full lore.

### Short description

Ankerstone uses redstone to hold nearby chunks active, built from stillstone, ironwork, and a contained ender pearl.

### Site / mod-page introduction

Long before the Ankerstone, others had found ways to work with the forces between places. Some forced things through. Others learned to fear what might emerge. Over time, study of lapis and deepslate revealed stillstone: a material that resists dimensional flux.

Reinforced with iron and built around an ender pearl, stillstone becomes something more. A little redstone, and the Ankerstone holds a piece of the world in place.

### Material notes (optional short descriptions)

- **stillstone:** Lapis-altered deepslate that resists dimensional flux.
- **reinforced stillstone:** A passive discovery made useful through ironwork.
- **Ankerstone:** A contained ender pearl and a redstone-controlled anchor.

Public copy can hint at the earlier civilizations without presenting the interpretation as established Minecraft canon. It should not suggest that the pearl supplies electrical power or that the block literally stops time.

## Advancements

### Progression and display

Add a **dedicated Ankerstone advancement tab**. These are visible progression milestones, **not** the existing recipe-book unlock advancements. Keep a single parent-child chain and do not add separate Single or Wide milestones just to fill the tree.

| Order | Name | Description (approved) | Criterion (intended) | Icon | Frame |
|---|---|---|---|---|---|
| 1 (root) | A Strange Stillness | Craft stillstone from deepslate and lapis. | Obtain a stillstone full-block item | Stillstone | Task |
| 2 | Iron Resolve | Strengthen stillstone with iron. | Obtain a reinforced stillstone full-block item | Reinforced Stillstone | Task |
| 3 | Pearl of an Idea | Give an ender pearl a reason to stay put. | Obtain an Ankerstone item | Ankerstone (ordinary Off item appearance) | Goal |
| 4 | Stay a While | Keep a piece of the world from slipping away. | First activation of an Ankerstone attributed to its placer | **Wide Ankerstone appearance** | Goal |

**Display decisions:**
- use normal vanilla visibility
- tab title `A Strange Stillness`; use stillstone as the advancement tab background and a stillstone icon for its root
- show all four advancements
- allow toast and chat announcement, and give no additional item or experience rewards.
- Stay a While uses an advancement-only item model selected through minecraft:item_model. The normal inventory item keeps its Off appearance.

### Material milestones

Use **obtaining**, rather than detecting the crafting action, for the first three criteria. This rewards discovery, sharing, trading, and obtaining items through other legitimate means. The words *Craft* and *Strengthen* in the approved descriptions convey the intended progression; they are not strict technical conditions.

For stillstone and reinforced stillstone, require the **full-block item**. Obtaining a decorative slab, stair, or wall alone should not substitute for the milestone.

Recipe-book unlocks continue to operate as already implemented and tested.

### Activation and player credit

**Approved player-facing rule:** Credit **the player who placed the Ankerstone**, not whoever supplies redstone power. Activation by a timer, automated circuit, or another player still belongs to the placer.

A qualifying activation is the block's first transition from **Off (0)** to **Single (1–14)** or **Wide (15)**. Immediate activation on placement into an already powered position must count. Neither Single-to-Wide nor Wide-to-Single requires a second advancement. As with other advancements, a player earns Stay a While only once.

Intended persistence and multiplayer behavior:

- Remember the placing player's identity even when the Ankerstone is Off, and across server restarts.
- If the block activates while its placer is offline, retain pending credit and award it when that player returns.
- Breaking and placing an Ankerstone again attributes the newly placed block to its new placer.
- Do not require the placer to stand nearby, activate a lever, or be online at the moment redstone changes.

Chunk-ticket ownership in `AnkerstoneSavedData` and `AnkerstoneChunkTickets` tracks block positions and modes, not players. `AnkerstoneAttributionData` separately stores placer UUIDs and pending advancement credit.

### Implementation Decisions

- Keep player attribution separate from chunk-ticket ownership.
- Creative placement by a player counts.
- Commands and older blocks without a known placer do not earn activation credit.
- Breaking and replacing a block assigns its new placer.
- Off Ankerstone keeps its original placer through intact piston movement.
- Earned offline credit survives removal of the block.
- Pending credit can be delivered regardless of the player's current dimension.
- Store only the positions, dimensions, and UUIDs needed for attribution.
- Placement records can be removed once activation credit is awarded or safely queued.
- Advancement parents organize the tree; they do not gate item acquisition.

Attribution storage and basic activation credit are implemented and tested. Still to do:

- check Creative placement and activation without a known placer
- check interrupted piston movement
- check saving and loading across an actual restart

Keep existing chunk tickets and recipe unlocks working as they are.