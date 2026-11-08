# Testing

## Run Tests

Run from the repository root:

| Windows Command | Runs |
|---|---|
| `.\gradlew.bat build` | Build, GameTests, and fresh-server restart checks |
| `.\gradlew.bat runGameTest` | GameTests only |
| `.\gradlew.bat restartTest` | Fresh-server restart checks only |


- Restart results are in `build/reports/restart-tests/summary.txt`; detailed logs stay with the disposable world under `build/restart-tests/`. 
- GitHub Actions retains test reports and logs with each run. Copy any local failure logs you want to keep before running `clean`.


## Automated Coverage

Test sources are in `src/gametest/java/io/github/omegaplex/ankerstone`.

| Area | Main Checks |
|---|---|
| Owners and tickets | Shared owners, mixed modes, mode changes, removal, negative coordinates, dimension-local storage, and registered load/unload callbacks |
| Saved data | Serialization, save flags, and copying mutable input positions before storing them |
| Redstone | Mode transitions, full-block conduction, Observer pulses, and comparator outputs of 0, 7, and 15 |
| Pistons | Off pushes and sticky pulls in all six directions; active blocks resisting movement; X/Z chunk boundaries, including negative coordinates; activation at the destination; same-tick power loss; interruption cleanup |
| Placer credit | Original-placer attribution, movement and replacement, powered placement, offline credit, cross-dimension delivery, and no repeat delivery; real disk restarts preserve ordinary/piston-moved placements, pending credit after block removal, and consumed credit |
| Mining and drops | Tool requirements and drop items/counts, including double slabs |
| Recipes | Checks Survival recipe-book discovery after inventory changes, core crafting, both families' variants, stonecutting, output counts, and invalid ingredients/layouts. |
| Advancements | Full-block acquisition milestones, activation credit, and the Wide advancement icon without changing the inventory item |
| Entity ticking | Measures actual entity ticks inside and outside the expected footprint. Single's center chunk, Wide's full 3x3 area, Off mode, and dimension isolation. |
| Chunk release | Single/Wide deactivation and removal, plus Wide-to-Single downgrade, in the Overworld, Nether, and End; shared and overlapping owners in the Overworld; chunk and entity-data unloading checked without loading them |
| Startup reconciliation | Removes missing or unpowered owners, corrects Single/Wide mismatches, preserves shared tickets, waits for required chunks without loading them synchronously, limits validation to 16 attempts per tick across dimensions, and clears pending work on dimension unload |
| Active limit and commands | Global counting across dimensions, overlapping owners, blocked activation and credit, mode changes, restored owners above the limit, command permissions and invalid arguments; console-set limits survive disk restarts |
| Admin listing and visits | Cross-dimension snapshots, ordering, pagination, permissions, expiration, and rejection of old links and inactive entries |

> We deliberately introduced ticket leaks, loading-only leaks, broken ticket restoration, an incorrect recipe-unlock ingredient, and missing saved credit. The tests caught each fault.

### Fresh-Server Restarts

The restart suite saves one disposable world across three separate Java 25 server processes:

1. **Prepare:** Confirm Ankerstones keep chunks ticking, leave inactive fixtures unloaded, and save ordinary and piston-moved placements plus pending offline credit.
2. **Restore:** Confirm ticking and credit survive restart, deliver pending credit through the player-join handler, then deactivate the Ankerstones and save.
3. **Released:** Confirm released chunks remain unloaded, consumed credit stays consumed, and deferred credit delivers once.

Restart checks cover Single and Wide in the Overworld, Nether, and End. They use normal redstone power and run long enough to detect ticking stopping when no players are present.

A deliberately stale owner is saved during Prepare. Restore confirms it survived disk loading, then checks that reconciliation removes its saved record and ticket and allows its chunks and entity data to unload. Released confirms the cleanup survives another restart.

Servers bind only to localhost on an assigned port, disable empty-server pausing, and allow no connected players. The task accepts the Minecraft server EULA for these disposable servers. Credit checks use server-side player stand-ins.

## Manual Checks

**Note:** Use the exact release JAR and the Ankerstone Lab world. 

1. **Blocks and advancement display:** Check every block and variant in-world and in inventory, the four advancements, their tab background, and Wide's activation icon. The ordinary inventory item should look Off.
2. **Survival menus:** Obtain the unlock ingredients, check the nine crafting and six stonecutter recipes appear, and craft an output in each menu. Confirm previews and received items agree.
3. **Materials and shaders:** Check shapes in daylight and darkness with shaders off and with LabPBR alone. Shader appearance and GPU-specific behavior remain manual.
4. **Piston rendering:** Run Single and Wide with both same-tick power-removal orders in the lab. Check the piston retracts, the Off Ankerstone returns, and no ghost blocks remain before reloading.
5. **Single-player reopening:** Use the remote platform to enable Single, Wide, and Off. Save and return to title, reopen, then repeat after fully closing the client. Confirm active counters resume and Off stays stopped. This checks the integrated-server lifecycle.
6. **Mod Menu:** Use mod menu settings screen to verify slider ranges and tooltips,  `Done` vs `Escape`, persistence after relaunch, and uses settings without Mod Menu.

### Admin Commands

Use the release JAR and a level-4 operator in a backup lab world. Have active Single and Wide Ankerstones in the Overworld, one active Ankerstone in the Nether, and one Off.

1. **List and read:** Run `/ankerstone list`. Check that the title and instructions are readable, coordinates and modes match the blocks, and the Off block is absent.
2. **Visit:** Click an entry in Creative. Confirm the Spectator message appears and you do not move. Switch to Spectator and click the same entry. Confirm you arrive just above the listed block and stay in Spectator.
3. **Visit another dimension:** From the Overworld, click the Nether entry. Confirm the destination dimension and coordinates.
4. **Reject an old link:** Run `/ankerstone list` again. Confirm a link from the previous list is rejected and the new link works.
5. **Check page two:** With at least 11 active Ankerstones, run `/ankerstone list`, then `/ankerstone list 2`. Confirm numbering starts at 11 and clicking that row visits the block it describes.

### Integration Checks

- Use the [manual multiplayer credit check](multiplayer-testing.md) once per new port and after multiplayer/login handling changes. 
- Use a backup lab world for upgrades when save formats or supported Minecraft versions change. When lab opens successfully, save a backup of the world and add it to the lab archive. We maintain an archive of upgraded labs with the original being from 26.1.2.

### Troubleshooting

- Restart cleanup: Validation now waits for block and entity data to be ready before removing stale owners. Ten consecutive restart runs passed after the change. If it recurs, keep the restart logs.

- Negative-boundary piston tests: If a test fails before the piston moves, check whether the fixture’s chunks and entity data were ready. Setup waits up to ten real-time seconds. The original cause is still unconfirmed, so keep the failure message and server log before rerunning.

## Release Verification

After automated and manual testing, the final release JARs were loaded into a testing instance for a smoke check. Final-JARs hashes ( SHA-256 ) are added to the each release note.

## Development (`main`, Minecraft 26.3)

- **2026-11-07:** All 187 GameTests and three restart phases passed locally and in CI. In-game checks confirmed that only active Ankerstones appeared, clickable visits required Spectator mode, old links were rejected, and a Nether visit worked.
- **2026-10-31:** All 171 GameTests and three restart phases passed in the full build. In-game limit checks passed: blocked activation, Single/Wide changes, lowering the limit without disabling active blocks, and restoring unlimited activation.
- **2026-10-31:** All 145 GameTests and three restart phases passed. Release fixtures run in separate environment batches, and both validation-budget tests establish readiness before measuring calls.
- **2026-10-30:** All 136 GameTests passed after updating asynchronous checks to use real-time deadlines. Console progress bars are suppressed; full logs and the final pass/fail summary remain available.
- **2026-10-29:** All 136 GameTests and all three restart phases passed, including startup reconciliation and stale-owner cleanup across disk restarts.  **Note**: Validation runs after restoration, not continuously during gameplay.
- **Nether Single Restart, 2026-10-29** Added Nether Single coverage; all three restart phases passed.

## 1.0.0 Prerelease Test Results
### Minecraft 26.3

- **Automated, 2026-10-28:** Full local build passed, as reported by the tester, after updating recipe-unlock advancements and both slab loot tables for the new data formats.
- **Test lab, 2026-10-28:** All five manual checks above passed using the packaged RC1 JAR, including crafting, advancements, piston rendering, textures, shaders/POM, and Off/Single/Wide counters after title-screen reopening and full client restarts.
- **Manual network credit, 2026-10-28:** Passed the [multiplayer credit check](multiplayer-testing.md). The activator received no placer credit; the returning placer received saved credit after anchor removal and a server restart. After revocation and another restart, consumed credit was not delivered again.

### Minecraft 26.2

- **Test lab, 2026-10-27:** All five manual checks above passed, including shader/POM appearance, both piston removal orders, and Off/Single/Wide counters after title-screen reopening and full client restarts. The lab save was upgraded from 26.1.2 to 26.2 and backed up after testing.
- **Automated, 2026-10-27:** Full local build passed all 125 GameTests and all three restart phases after removing client automation. 
- **Manual, 2026-10-23:** Installed-client block, variant, recipe, and advancement display; Off/Single/Wide full-client restart counters; POM pack icon/default state; non-shader Single/Wide appearance and compatible POM glow alignment passed.
- **Manual network credit, 2026-10-27:** Passed the [multiplayer credit check](multiplayer-testing.md) using sequential real-client connections as `AnkerPlacer` and `AnkerActivator` on a localhost-only, offline-mode dedicated server. The activator received no credit; the returning placer received credit after anchor removal and a server restart. After revocation and a second restart, consumed credit was not delivered again. Tested with Fabric Loader 0.20.1, Fabric API 0.161.0+26.2, and `ankerstone-1.0.0-rc1+mc26.2.jar`. 

### Minecraft 26.1.2

- **Automated, 2026-10-27:** Full local build passed all 125 GameTests and all three restart phases after removing client automation.
- **Manual:** Recipe-book, credit, restart, and piston checks were recorded before release; the latest Wide material check was on 2026-10-22. Installed-client checks on October 20 and 21 used RC1 builds whose contents changed under the same filename.
