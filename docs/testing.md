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
| Startup reconciliation | Removes missing or unpowered owners, corrects Single/Wide mismatches, preserves shared tickets, waits for required chunks without loading them synchronously, limits validation to 16 attempts per tick across dimensions, and clears pending work on dimension unload |
| Active limit and commands | Global counting across dimensions, overlapping owners, blocked activation and credit, mode changes, restored owners above the limit, command permissions and invalid arguments; console-set limits survive disk restarts |
| Admin listing and visits | Cross-dimension snapshots, ordering, pagination, permissions, expiration, and rejection of old links and inactive entries |

> We deliberately introduced ticket leaks, loading-only leaks, broken ticket restoration, an incorrect recipe-unlock ingredient, and missing saved credit. The tests caught each fault.

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

1. **Blocks and advancement display:** Check every block and variant in-world and in inventory, the four advancements, their tab background, and Wide's activation icon. The ordinary inventory item should look Off.
2. **Survival menus:** Obtain the unlock ingredients, check the nine crafting and six stonecutter recipes appear, and craft an output in each menu. Confirm previews and received items agree.
3. **Materials and shaders:** Check shapes in daylight and darkness with shaders off and with LabPBR alone. Shader appearance and GPU-specific behavior remain manual.
4. **Piston rendering:** Run Single and Wide with both same-tick power-removal orders in the lab. Check the piston retracts, the Off Ankerstone returns, and no ghost blocks remain before reloading.
5. **Single-player reopening:** Use the remote platform to enable Single, Wide, and Off. Save and return to title, reopen, then repeat after fully closing the client. Confirm active counters resume and Off stays stopped. This checks the integrated-server lifecycle.
6. **Mod Menu:** Use mod menu settings screen to verify slider ranges and tooltips,  `Done` vs `Escape`, persistence after relaunch, and uses settings without Mod Menu.
7. **Sound check:** Switch an Ankerstone from Off → Single → Wide → Single → Off. Listen for the matching cue at each transition.

### Admin Commands

Use the release JAR and a level-4 operator in a backup lab world. Have active Single and Wide Ankerstones in the Overworld, one active Ankerstone in the Nether, and one Off.

1. **List and read:** Run `/ankerstone list`. Check that the title and instructions are readable, coordinates and modes match the blocks, and the Off block is absent.
2. **Visit:** Click an entry in Creative. Confirm the Spectator message appears and you do not move. Switch to Spectator and click the same entry. Confirm you arrive just above the listed block and stay in Spectator.
3. **Visit another dimension:** From the Overworld, click the Nether entry. Confirm the destination dimension and coordinates.
4. **Reject an old link:** Run `/ankerstone list` again. Confirm a link from the previous list is rejected and the new link works.
5. **Check page two:** With at least 11 active Ankerstones, run `/ankerstone list`, then `/ankerstone list 2`. Confirm numbering starts at 11 and clicking that row visits the block it describes.
6. **Mod Menu:** Use mod menu settings screen to verify slider ranges and tooltips,  `Done` vs `Escape`, persistence after relaunch, and uses settings without Mod Menu.

## Recorded Checks

These results apply to the versions and arrangements tested. Repeat the affected checks after relevant changes, and smoke-test the exact JAR intended for release.

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
