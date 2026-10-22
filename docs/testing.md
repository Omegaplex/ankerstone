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
- Single and Wide sticky retraction with both immediate power-removal orders; checks the intermediate mode, owner, ticket, and extended piston before removing the second source, then verifies Off movement, cleanup, and retained offline credit. Both sources are removed in the same tick before queued retraction runs.
- delivered activation credit is not awarded again when JOIN handling runs after the advancement is revoked
- Single and Wide owners retain their tickets while another owner in the same chunk changes mode, turns Off, and is removed
- Single ticks entities in its center chunk, but not the surrounding 24 chunks checked
- Wide ticks entities throughout its 3×3 area, including corners, but not the 16-chunk outer ring
- Off leaves a tracked entity unticked while a separate control entity advances
- Wide's ticking footprint also works in the Nether
- Wide's Nether footprint continues ticking for at least 320 entity ticks, beyond the idle-dimension grace period, without another dimension-keeping ticket present at setup
- owner additions, mode changes, and removals require saving; unchanged or absent entries do not
- Placement, mode changes, and the Stay a While advancement worked.

Off movement and exact ticket-count tests use separate batches to reduce interference. These batches share a server; they are not separate worlds. The `ticket_ownership` and `restoration` tests also rely on running synchronously. Revisit their isolation before adding delayed callbacks.

Adjustable test power uses daylight-detector states with automatic sunlight updates disabled.

Variant crafting tests check each family's slab, stair, and wall recipes,
output items and counts, mixed ingredients, and incorrect layouts.

Stonecutting tests check all six recipes, their outputs and counts, and rejection of the
other stone family. Recipe-book discovery is checked manually.

### Outside Review Follow-up

Additional checks cover:

- negative owner coordinates where floor division and ordinary integer division produce different chunks
- registered LOAD and UNLOAD callbacks rebuilding tickets while preserving saved owners
- Stillstone and Reinforced Stillstone full-block conduction properties, plus real circuits with a vanilla stone control
- intermediate states between the two immediate power-removal calls
- removing an Ankerstone during sticky retraction, after attribution has reached the pull destination
- Nether saved ownership and tickets stay separate from Overworld storage at the same coordinates

Temporary production changes confirmed that the tests detect:

- ordinary division replacing floor division
- missing LOAD registration
- missing UNLOAD registration or runtime clearing
- Stillstone becoming conductive
- Reinforced Stillstone losing conduction
- moving-block cleanup handling extension but ignoring retraction
- missing keep-dimension-active flag, detected by Nether probes stopping before 320 ticks
- missing simulation flag, detected by loaded chunks not becoming entity-ticking
- missing loading flag, detected by fixture readiness failing
- Nether ownership incorrectly using Overworld saved-data storage

All temporary changes were reverted before the final passing build.

Owner saved data no longer selects vanilla's forced-chunk data fixer. Its saved-data ID and existing position/mode list format are unchanged.

Recipe checks retain one valid pattern per variant recipe, plus output and rejection checks. Explicit row-shifting and mirroring checks were removed. Mining checks still cover every block variant and the existing tool matrix.

Lifecycle callback checks do not simulate a disk restart or a fresh chunk manager. Piston interruption checks verify immediate removal and attribution cleanup, not client rendering or later recovery from every possible interruption.

### Naming

Use short, descriptive class names ending in `Tests`. Omit the mod prefix and `Game` when the package and source folder already provide that context. Keep qualifiers that distinguish test groups, such as `PistonActivationTests` and `ActivationTests`. Shared helpers use names such as `PistonTestSupport`.

### Test Timing

Wait for observable readiness conditions when setup depends on asynchronous work. Use bounded timeouts that identify the condition that failed.

Piston movement tests wait for the completed block arrangement, then check mode, ownership, tickets, and attribution. Movement deadlines run the final assertions and cleanup even when movement never finishes. Callback guards prevent later ticks from checking an already-cleared fixture.

Negative-coordinate fixtures wait for both chunks to be entity-ticking and for their entity data to finish loading. Minecraft can report entity-ticking status before that data is ready, but moving piston block entities do not tick until it is loaded. A separate setup deadline leaves time for movement and cleanup.

Fixed delays remain for blocked movement and redstone pulse sampling. Both interruption tests wait to observe a moving piston at the destination before removing it.

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

## Additional Manual Checks

Passed on 2026-10-19 using Minecraft 26.1.2:

- Wide continued ticking after returning to the title screen and
  reopening the world without closing Minecraft.
- An item entity in an Overworld Wide corner chunk increased in
  age before and after a full Minecraft restart, with the player remote.
- An item entity in a Nether Wide corner chunk increased in age
  before and after a full restart, with the player in the Overworld.
- Single and Wide sticky retraction passed with both power-removal
  orders. An impulse/chain command-block pair removed both sources
  in the same tick. After movement settled, the visible blocks
  matched server block checks before touching blocks or reloading.

These checks cover the tested arrangements, not every possible
piston interruption or client desynchronization.

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

## Installed JAR Check

Checked on 2026-10-20 in Prism using Fabulously Optimized for Minecraft 26.1.2 and `ankerstone-1.0.0-rc1+mc26.1.2.jar`.

- Blocks and variants displayed correctly.
- Acquisition and activation advancements worked.
- Survival crafting, stonecutting, and recipe-book checks passed.
- Wide kept the remote command-block counter advancing after a full Minecraft restart.
- Visual checks passed with shaders off and on. Overlay/PBR effects remained difficult to perceive and need a separate review.

The restart check measured block ticking in the anchor's own chunk, not the full entity-ticking footprint.

A separate minimal instance containing only Fabric Loader, Fabric API, and Ankerstone also passed on 2026-10-20. Placement and activation credit worked, and the remote counter continued advancing after a full Minecraft restart.

## Dedicated Server Check

Checked on 2026-10-20 using the release JAR on a minimal Fabric dedicated server.

- The remote counter continued advancing with no players connected.
- After a clean server shutdown and restart, the counter resumed advancing before any player joined.
- `pause-when-empty-seconds=0` was set for these checks.

This verifies center-chunk block ticking and restoration. Ankerstone does not override Minecraft's whole-server empty-server pausing.

## Material and Shader Review

Checked on 2026-10-21 in the Gradle development client using Complementary Reimagined r5.8.1.

- Refreshed Ankerstone and Reinforced Stillstone normal maps using deliberately authored height maps. Stillstone maps were reviewed and retained.
- Reviewed Ankerstone in daylight and darkness, with shaders disabled and with LabPBR enabled. Checked the revised normals with POM on and off.
- Kept Off’s core recessed and made Single’s active core flush with the outer surface.
- Added an optional, disabled-by-default POM compatibility pack for Wide. Its overlay-free model allows shader emission to follow the recessed ring.
- Confirmed the bundled pack works with the standalone test pack disabled. Disabling the bundled pack restores the vanilla overlay.
- Reviewed Reinforced Stillstone and its variants after correcting the normal-map export.

The updated RC1 JAR passed an installed check in Prism. Vanilla glow worked with shaders off and the compatibility pack disabled. With LabPBR and POM enabled, the bundled pack displayed Wide’s recessed ring correctly. Reinforced Stillstone’s refreshed appearance also passed.