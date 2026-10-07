<img width="1583" height="365" alt="ankerstone_gen_logo-v1" src="https://github.com/user-attachments/assets/7b869887-6d9d-4985-bf1f-9474bd1d6fe2" />


# Ankerstone

A Fabric mod with redstone-controlled chunk anchors and two
decorative block families: Stillstone and Reinforced Stillstone.

## Current Behavior

| Mode | Redstone Input | Comparator Output | Piston Movable |
|---|---|---|---|
| Off | 0 | 0 | Yes |
| Single | 1-14 | 7 | No |
| Wide | 15 | 15 | No |

Active anchors keep chunks loaded and ticking. Saved ownership
restores their tickets when the dimension loads.

Both decorative families include blocks, slabs, stairs, and walls.
Textures include LabPBR material maps for compatible shaders.

All blocks require a pickaxe to drop items. Stillstone accepts any
pickaxe; Reinforced Stillstone and Ankerstone require iron or better.
Double slabs drop two slab items.

## Development

See [Testing](docs/testing.md) for automated tests and manual
restart checks.

## Lore & Planned Crafting

The crafting descriptions below are design notes.
Survival crafting recipes are not implemented yet.

---

## A History Written in Stone

No one civilization invented the Ankerstone. It is the end of a very long story — one that began with violence, passed through fear, and arrived at understanding only after centuries of lost knowledge and rediscovery.

The world has always contained a kind of energy that does not belong entirely to it. It bleeds through from somewhere else, or perhaps from the spaces between places. It resists being named. What it does, however, is observable: it destabilizes. It pushes through. It refuses to stay still.

Different peoples, in different ages, noticed this. Although they never spoke to each other, they all reached for the same thing.

---

### The Spawner Builders

They came first, and they came with iron.

Scattered across the world in crude dungeons and fortresses, these builders discovered something fundamental: dimensional energy could be caught. If you built the cage right — wrought iron with the correct geometry — you could force something through from the other side. Weaponized. Repeatable. Useful in the way that a trap is useful.

They did not understand what they were doing. They did not need to. It worked, and that was enough.

It is always easier to make a bomb than a power source. The spawner builders found the bomb first.

---

### The Ancient City Builders

They came later, and they came with something the dungeon builders never had: awareness that what they were dealing with was dangerous.

The sculk is not decoration. It is a sensing network — deliberate, architectural, vast. The ancient cities are built deep and dark and defensively, by a civilization that understood dimensional energy well enough to be afraid of it. They may have been responding, in their own way, to what the spawner builders had set loose.

They did not solve the problem. But they were the first to admit there was one.

---

### The Enchanting Tradition

Somewhere between the ancient cities and the present, someone stopped trying to contain the energy or survive it and started trying to *understand* it.

Lapis lazuli became the medium. Already used in enchanting as a catalyst — already carrying some affinity for focusing and transferring this kind of energy — it became the fingerprint of a new tradition. Academic. Patient. Interested in the phenomenon itself rather than its applications.

This tradition did not build weapons or cities. It built knowledge. And that knowledge, passed forward through time in fragments and half-understood methods, is what made stillstone possible.

---

## Field Notes — Materials

---

**stillstone**
*Crafted from deepslate and lapis lazuli*

Not invented. Discovered. A dense, lapis-altered deepslate that resists dimensional flux by nature — quiet, heavy, and inert. It does not generate in the world. No vein of it exists waiting to be found. Someone had to learn to make it, which means someone had to understand why lapis and deepslate together produce something that refuses to change.

The enchanting tradition's fingerprint is all over it. The lapis is not incidental.

*Primary property: passive resistance to dimensional flux.*
*It does not do anything. It simply refuses to become anything else.*

---

**reinforced stillstone**
*Crafted by surrounding stillstone with iron ingots*

The first act of intent. stillstone resists change on its own — reinforced stillstone is what happens when someone decided that resistance needs to last. Ancient ironwork, bound around the stillstone in a repeating lattice, turns a found material into a structural one. Load-bearing. Architectural. Permanent in a way that raw stillstone is not.

The iron does not add power. It adds commitment.

*This is where the material tradition becomes an engineering tradition.*
*stillstone is what was found. reinforced stillstone is what was built.*

---

**Ankerstone**
*Crafted from reinforced stillstone, redstone dust, and an ender pearl*

The realized mechanism. Everything the enchanting tradition learned about dimensional energy, expressed through stillstone's resistance, activated by redstone, and centered on an ender pearl that has been contained rather than thrown.

The pearl is not powering the device. The device is holding the pearl. The pearl wants to go somewhere. The Ankerstone simply refuses to let it.

A chunk anchor. A fixed point. The world remembers where it is because the Ankerstone will not let it forget.

The spawner builders pushed energy through a hole in the world. The Ankerstone holds the world still around that hole.

*Same physics. Opposite intent.*

---

## Planned Crafting Reference

### stillstone
`deepslate` + `lapis lazuli` = `stillstone x5`

| | | |
|---|---|---|
| deepslate | lapis lazuli | deepslate |
| lapis lazuli | deepslate | lapis lazuli |
| deepslate | lapis lazuli | deepslate |

---

### reinforced stillstone
`stillstone` + `iron ingots` = `reinforced stillstone x1`

| | | |
|---|---|---|
| iron ingot | iron ingot | iron ingot |
| iron ingot | stillstone | iron ingot |
| iron ingot | iron ingot | iron ingot |

---

### Ankerstone
`reinforced stillstone` + `redstone dust` + `ender pearl` = `Ankerstone x1`

| | | |
|---|---|---|
| reinforced stillstone | redstone dust | reinforced stillstone |
| redstone dust | ender pearl | redstone dust |
| reinforced stillstone | redstone dust | reinforced stillstone |

*Shaped recipe. Positional.*
*The redstone cardinal axes mirror the indicator gem on the physical block.*
*The reinforced stillstone anchors the corners.*
*The ender pearl sits at the intersection of all four activation lines.*
