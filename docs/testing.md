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
- piston reactions; `OFF` piston pushing and sticky pulling, with `SINGLE` and `WIDE` resisting both pushes and sticky pulls
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
- Off pushes and sticky-piston round trips in all six directions preserve the placer and leave no active owners or Ankerstone tickets
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
- Observer pulses and connected lamp output on activation, upgrade to Wide, and deactivation
- real comparator circuits updating to 0, 7, and 15, including decreases
- Off pushes and sticky-piston round trips across actual X and Z chunk boundaries
- Single and Wide activation after crossing an X chunk boundary, with tickets centered on the destination and released on removal
- guaranteed negative-coordinate X/Z boundary pushes and sticky round trips, plus Single/Wide powered arrivals with destination ticket checks
- Single and Wide sticky retraction after both power sources are removed in the same tick, in either order; checks Off movement, owner/ticket cleanup, and retained offline credit
- delivered activation credit is not awarded again when JOIN handling runs after the advancement is revoked
- Single and Wide owners retain their tickets while another owner in the same chunk changes mode, turns Off, and is removed
- Single ticks entities in its center chunk, but not the surrounding 24 chunks checked
- Wide ticks entities throughout its 3×3 area, including corners, but not the 16-chunk outer ring
- Off leaves a tracked entity unticked while a separate control entity advances
- Wide's ticking footprint also works in the Nether
- A Wide Nether anchor ticks its Nether probe without ticking a tracked Overworld probe at matching coordinates

Off movement and exact ticket-count tests use separate batches to avoid interference from active fixtures. Adjustable test power uses daylight-detector states with automatic sunlight updates disabled.

Variant crafting tests check shifted and mirrored patterns, output items
and counts, mixed ingredients, and incorrect layouts.

Stonecutting tests check all six recipes, their outputs and counts, and rejection of the
other stone family. Recipe-book discovery is checked manually.

### Naming

Use short, descriptive class names ending in `Tests`. Omit the mod prefix and `Game` when the package and source folder already provide that context. Keep qualifiers that distinguish test groups, such as `PistonActivationTests` and `ActivationTests`. Shared helpers use names such as `PistonTestSupport`.

### Test Timing

Wait for observable readiness conditions when setup depends on asynchronous work. Use bounded timeouts that identify the condition that failed.

Piston movement tests wait for the completed block arrangement, then check mode, ownership, tickets, and attribution. Movement deadlines run the final assertions and cleanup even when movement never finishes. Callback guards prevent later ticks from checking an already-cleared fixture.

Negative-coordinate fixtures wait for both chunks to be entity-ticking and for their entity data to finish loading. Minecraft can report entity-ticking status before that data is ready, but moving piston block entities do not tick until it is loaded. A separate setup deadline leaves time for movement and cleanup.

Fixed delays remain for blocked movement, deliberate interruption during movement, and redstone pulse sampling.

A temporary blocked-push check confirmed that the push deadline preserves the original failure and clears the fixture, placer records, owners, and Ankerstone tickets.

Entity-ticking tests prepare terrain before measurement and wait for entity data to load. They measure armor stands' actual tick counters. Active fixtures receive no helper simulation tickets. The Off check uses a loading-only ticket to keep its probe present and a separate ticking control. Probes are removed during cleanup and are not saved.

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
