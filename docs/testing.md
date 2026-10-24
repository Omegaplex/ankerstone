# Testing

## Run the Tests

From the repository root on Windows, run:

```powershell
.\gradlew.bat build
```

This builds the mod and runs the required Fabric GameTests. A required test failure fails the build. GitHub Actions runs the same build on pushes and pull requests.

To run just the GameTests:

```powershell
.\gradlew.bat runGameTest
```

On Linux or macOS, use `./gradlew` instead of `.\gradlew.bat`.

These are server-side GameTests. They can check blocks, circuits, saved data, and entity ticking, but they do not replace the client and restart checks below.

## Minecraft 26.2 Port

The current build targets Minecraft 26.2 with Java 25, Fabric Loader 0.20.1, Fabric API 0.161.0+26.2, Loom 1.17.21, and Gradle 9.5.1. The bundled POM pack uses resource-pack format 88.0.

On 2026-10-23, the full local build and three repeat runs passed all 113 required GameTests without Java source changes. The packaged version, dependency requirements, and POM pack metadata were checked, and the JAR contains no test-only code. CI and the client, installed-JAR, restart, and multiplayer checks have not yet been repeated for 26.2. The earlier 26.1.2 results below do not verify this port.

Test `ankerstone-1.0.0-rc1+mc26.2.jar` in a separate 26.2 instance. Use disposable worlds for the manual checks. If checking an upgrade from 26.1.2, use a backup copy of the world, not the original.

## What the Tests Cover

The test classes are in `src/gametest/java/io/github/omegaplex/ankerstone`.

| Area | Main Checks |
|---|---|
| Owners and tickets | Shared owners, mixed modes, mode changes, removal, negative coordinates, dimension-local storage, and registered load/unload callbacks |
| Saved data | Serialization, save flags, and copying mutable input positions before storing them |
| Redstone | Mode transitions, full-block conduction, Observer pulses, and comparator outputs of 0, 7, and 15 |
| Pistons | Off pushes and sticky pulls in all six directions; active blocks resisting movement; X/Z chunk boundaries, including negative coordinates; activation at the destination; same-tick power loss; interruption cleanup |
| Placer credit | Original-placer attribution, movement and replacement, powered placement, offline credit, cross-dimension join delivery, and no repeat delivery after credit is consumed |
| Mining and drops | Tool requirements and drop items/counts, including double slabs |
| Recipes | Core crafting, both families' variants, stonecutting, output counts, and invalid ingredients or layouts |
| Advancements | Full-block acquisition milestones, activation credit, and the Wide advancement icon without changing the inventory item |
| Entity ticking | Single's center chunk, Wide's full 3x3 area, unticked chunks outside those footprints, Off mode, and dimension isolation |

Footprint tests measure armor stands' actual tick counters. Single checks the surrounding 24 chunks remain unticked; Wide checks the 16-chunk ring outside its 3x3 area. Nether coverage includes at least 320 entity ticks to check continued ticking beyond the idle-dimension grace period.

Lifecycle callback tests do not simulate a disk restart or a fresh chunk manager. Recipe-book discovery and client appearance are checked manually.

## Intermittent CI Readiness Failure

A CI run on 2026-10-21 failed in `stickyRoundTripCrossesNegativeZChunkBoundary`: a fixture chunk was not entity-ticking when the tick-160 watchdog ran. The cause has not been confirmed.

The readiness assertion now reports entity-ticking state, entity-data readiness, temporary ticket levels, and chunk-loading details. Setup watchdogs also stop checking once movement starts. These changes passed ten consecutive local runs and a fresh CI run with all 113 required tests passing on 2026-10-22. That does not establish that the original problem is fixed.

A second CI failure on 2026-10-23 occurred in `negativeBoundaryArrivalActivatesWide`, before movement began. The fixture ticket was present at level 31 and the chunk reported `ENTITY_TICKING`, but entity data was not loaded at tick 160. The diagnostic changes did not resolve the readiness problem.

Negative-boundary setup now waits for entity data before starting the movement sequence. It processes completed entity loads using vanilla's entity-wait mechanism, with a ten-second real-time limit because GameTest ticks run without normal tick pacing. The entity-ticking checks and movement deadlines remain unchanged. The [first CI run with the bounded wait](https://github.com/Omegaplex/ankerstone/actions/runs/39994180957) passed on 2026-10-23.

In controlled local checks, a temporary five-second entity-load delay made the old setup fail at tick 160; the new setup passed all 113 tests with the same delay. A fifteen-second delay hit the new setup timeout and failed without hanging. After removing the temporary delay hook, five consecutive runs passed all 113 tests, including the full build. These checks reproduce a timing failure, but do not establish the exact cause of the CI failures.

Keep the failure message and job log before rerunning. Relevant runs: [original failure](https://github.com/Omegaplex/ankerstone/actions/runs/39688317977), [diagnostic changes passing](https://github.com/Omegaplex/ankerstone/actions/runs/39857154833), [entity-data readiness failure](https://github.com/Omegaplex/ankerstone/actions/runs/39989892912).

## Manual Checks

Use a disposable world. For chunk-loading checks, use a new Creative superflat world with commands enabled and no other chunk loaders.

### Center-Chunk Restart Check

First set up the scoreboard and teleport to the remote test site:

```mcfunction
/scoreboard objectives add anchor_ticks dummy
/scoreboard objectives setdisplay sidebar anchor_ticks
/tp @s 10008 100 10008
```

Wait for the terrain to load, then run:

```mcfunction
/fill 10004 98 10004 10011 98 10011 minecraft:stone
/give @s minecraft:repeating_command_block
/setblock 10010 98 10008 minecraft:redstone_block
/setblock 10010 99 10008 ankerstone:ankerstone
```

Place the command block on the platform. Set it to Repeat, Unconditional, and Always Active, with this command:

```mcfunction
scoreboard players add Anchor anchor_ticks 1
```

The setup starts in Wide mode. To change to Single, run:

```mcfunction
/setblock 10010 98 10008 minecraft:stone
/setblock 10007 99 10008 minecraft:redstone_block
/setblock 10008 99 10008 minecraft:redstone_wire
/setblock 10009 99 10008 minecraft:redstone_wire
```

For Off, remove the source:

```mcfunction
/setblock 10007 99 10008 minecraft:air
```

For each mode:

1. Confirm the mode with F3, then teleport away using `/tp @s 0 100 0`.
2. Leave the game unpaused for 20 seconds to let unrelated loading settle. Note the counter, wait another 60 seconds, and compare. Single and Wide should advance it; Off should not.
3. Save and quit, close Minecraft completely, then relaunch and reopen the world. Stay remote and unpaused and repeat the counter check.
4. Return with `/tp @s 10008 100 10008` before changing modes.

This checks block ticking in the anchor's own chunk. It does not prove Wide's full entity-ticking footprint.

### Other Restart and Multiplayer Checks

- Reopen a world from the title screen without closing Minecraft and check that an active anchor resumes ticking.
- Check an item entity's Age in a Wide corner chunk before and after a full restart, with the player remote. Repeat in the Nether, restarting with the player in the Overworld.
- Check placer credit across a restart for an Off placement and a piston-moved placement, then activate the block.
- With two players or test clients, activate a block while its placer is offline. Remove the block, restart, and check that the placer receives credit on return. Revoke the advancement and restart again to check that consumed credit is not delivered twice.
- On a dedicated server, set `pause-when-empty-seconds=0`. Check that the remote counter advances after everyone disconnects and after a server restart before anyone joins. Ankerstone does not override whole-server pausing.

### Client and Installed-JAR Checks

Test the built mod JAR, not the sources JAR, in a separate Minecraft instance.

- Check all blocks and variants, the four advancements, the tab background, and the Wide activation icon. The ordinary inventory item should remain Off.
- In Survival, check crafting and stonecutting outputs. In a fresh world, check that lapis unlocks Stillstone, Stillstone unlocks Reinforced Stillstone, and Reinforced Stillstone unlocks Ankerstone. Base blocks should unlock their variant recipes.
- Check Single and Wide sticky retraction with anchor power removed first and piston power removed first in the same tick. Compare the visible blocks with server block checks before touching the fixture or reloading.
- Check materials in daylight and darkness with shaders off, then with LabPBR and POM on and off. Include slabs, stairs, walls, and views from different angles.
- Check the bundled POM compatibility pack with the standalone test pack disabled. It should start disabled. With LabPBR shaders, enabling it should keep Wide's recessed glow aligned. Disable it when checking vanilla glow.

## Recorded Checks

These results apply to the versions and arrangements tested. Repeat the affected checks after relevant changes, and smoke-test the exact JAR intended for release.

The manual results recorded below are from Minecraft 26.1.2.

| Date | Passed Checks |
|---|---|
| 2026-10-06 | Off, Single, and Wide center-chunk restart checks on Minecraft 26.1.2 |
| 2026-10-07 to 2026-10-09 | Survival crafting, stonecutting, output counts, and fresh-world recipe-book unlocks |
| 2026-10-10 | Advancement display and credit; ordinary and piston-moved placements across restarts; offline credit after block removal; no repeat delivery |
| 2026-10-19 | Same-JVM world reopen; Overworld and Nether Wide corner entity checks across full restarts; both same-tick power-removal orders for Single and Wide |
| 2026-10-20 | RC1 installed in Prism with Fabulously Optimized and separately with minimal Fabric; placement, activation credit, and restart counter checks; crafting and advancement checks in the Fabulously Optimized instance |
| 2026-10-20 | Minimal Fabric dedicated server: counter continued with no players and resumed after restart before anyone joined |
| 2026-10-21 | Refreshed Ankerstone and Reinforced Stillstone maps and retained Stillstone maps; visual checks in the dev client with Complementary Reimagined r5.8.1; updated RC1 visual and bundled-pack checks in Prism |
| 2026-10-22 | Negative-boundary diagnostic changes: ten consecutive local passes and a fresh CI pass, all 113 required tests passing |
| 2026-10-22 | Revised Wide's color, normal, and emission maps; checked the appearance in the development client with shaders on and off, including nighttime rain |

The installed artifact tested on October 20 and 21 was `ankerstone-1.0.0-rc1+mc26.1.2.jar`. The material refresh changed that JAR's contents without changing its RC1 filename; the October 21 checks used the updated build.
