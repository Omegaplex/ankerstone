# Testing

## Automated Tests

Run from the repository root on Windows:

```powershell
.\gradlew.bat runGameTest
.\gradlew.bat build
```

`build` includes the GameTests. GitHub Actions runs `./gradlew build`
on pushes and pull requests.

Coverage includes:

- shared and mixed-mode tickets
- owner serialization and protection from mutable input positions
- ticket removal after the caller changes its input position
- ticket restoration
- piston reactions; `OFF` piston pushing & sticky-piston pulling, `SINGLE` & `WIDE` resisting pushes
- ownership at powered destinations, `OFF` pushes, and blocked pushes
- active placement, power transitions and comparator readouts
- tool eligibility
- loot item/count checks
- recipe matching and invalid ingredient/layout rejection
- crafting output item/count checks
- advancement acquisition for the three full-block items
- decorative variants excluded from material milestones
- obtaining Ankerstone does not grant activation credit
- Wide model selection for the activation advancement icon
- placer UUID serialization, dimension separation, and mutable-position copying
- shared attribution storage across dimensions
- pending credit serialization, clearing, and retention after placement-record removal
- save flags for placement and pending-credit changes
- player placement records the placer; ordinary removal clears the record
- activation without a known placer does not credit the player supplying power
- Single and Wide activation grant placer credit, including placement into existing power
- another player supplying power still credits the original placer
- replacing a block assigns credit to its new placer
- offline credit survives block removal; join delivery works in the Overworld and Nether
- piston pushes and sticky pulls preserve the original placer
- adjacent moves preserve each block's placer; blocked moves leave attribution unchanged
- Single and Wide activation after piston movement queue credit for the original placer
- removing Ankerstone during piston movement clears its attribution
- moved attribution requires saving and survives serialization at its destination
- - Observer pulses and connected lamp output on activation, upgrade to Wide, and deactivation

Variant crafting tests check shifted and mirrored patterns, output items
and counts, mixed ingredients, and incorrect layouts.

Stonecutting tests check all six recipes, their outputs and counts, and rejection of the
other stone family. Recipe-book discovery is checked manually.

## Manual Restart Check

Use a new Creative superflat world with commands enabled and no other
chunk loaders. Create the remote fixture:

```mcfunction
/scoreboard objectives add anchor_ticks dummy
/scoreboard objectives setdisplay sidebar anchor_ticks
/tp @s 10008 100 10008
/fill 10004 98 10004 10011 98 10011 minecraft:stone
/give @s minecraft:repeating_command_block
/setblock 10010 98 10008 minecraft:redstone_block
/setblock 10010 99 10008 ankerstone:ankerstone
```

Place the command block on the platform. Set it to Repeat, Unconditional,
and Always Active, using `scoreboard players add Anchor anchor_ticks 1`.

Start with Wide. For Single, return to the platform and run:

```mcfunction
/setblock 10010 98 10008 minecraft:stone
/setblock 10007 99 10008 minecraft:redstone_block
/setblock 10008 99 10008 minecraft:redstone_wire
/setblock 10009 99 10008 minecraft:redstone_wire
```

For Off, remove that source with
`/setblock 10007 99 10008 minecraft:air`.

For each mode:

1. Confirm the mode with F3.
2. Run `/tp @s 0 100 0`, wait 60 seconds unpaused, then observe.
3. Single and Wide should advance the counter; Off should stop it.
4. Save and Quit, close Minecraft completely, relaunch, and reopen.
5. Stay remote and unpaused for 20 seconds. Expect the same behavior.
6. Return using `/tp @s 10008 100 10008` before changing modes.

This checks persistence and ticking in the anchor's own chunk.
It does not verify the full surrounding coverage of Wide mode.

Manual Off, Single, and Wide restart checks passed on 2026-10-06
using Minecraft 26.1.2.

## Manual Crafting Check

- Survival crafting passed for all three core recipes with outputs of
  five Stillstone, one Reinforced Stillstone, and one Ankerstone.

- Recipe-book discovery passed in a fresh world: lapis unlocked Stillstone, Stillstone unlocked Reinforced Stillstone, and Reinforced Stillstone unlocked Ankerstone. _Checked on 2026-10-07 using Minecraft 26.1.2._

- Both families' variant recipes passed in-game checks on 2026-10-09 using Minecraft 26.1.2. Obtaining each base block unlocked its crafting recipes in a fresh Survival world. Crafting produced six slabs, four stairs, or six walls; stonecutting produced two slabs, one stair, or one wall.

## Manual Advancement Check

Checked on 2026-10-10 using Minecraft 26.1.2.

- Obtaining each full-block item earned its corresponding advancement.
- The tab, descriptions, icons, and tiled Stillstone background displayed correctly.
- Stay a While used the Wide appearance; the inventory item stayed Off.
- Ankerstone placed while Off retained its placer across a full restart and awarded Stay a While when activated.
- Ankerstone moved by a piston while Off retained its placer across a full restart and awarded Stay a While at its destination.
- Placement beside existing power awarded Stay a While immediately.

Restart checks used fixed dev usernames, AnkerstoneTester and
AnkerstoneHelper, with Minecraft closed completely between runs.
Offline activation credited the original placer on return, even after
the block was removed. After revoking the advancement and restarting
again, it was not awarded a second time.
