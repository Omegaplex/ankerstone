# Testing

## Run Tests

Run from the repository root:

| Windows Command | Runs |
|---|---|
| `.\gradlew.bat build` | Build, GameTests, and fresh-server restart checks |
| `.\gradlew.bat runGameTest` | GameTests only |
| `.\gradlew.bat restartTest` | Fresh-server restart checks only |

On Linux or macOS, use `./gradlew`. The normal build runs both automated server suites, including in GitHub Actions. Client checks are manual; no graphics-based test harness is maintained.

Restart results are in `build/reports/restart-tests/summary.txt`; detailed logs stay with the disposable world under `build/restart-tests/`. CI uploads test reports and server logs. `clean` removes generated test worlds.

Automated tests use disposable worlds, never the hand-built lab or personal saves.

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
| Recipes | Core crafting, both families' variants, stonecutting, output counts, invalid ingredients/layouts, and Survival recipe-book discovery for all 15 recipes, including negative unlock checks |
| Advancements | Full-block acquisition milestones, activation credit, and the Wide advancement icon without changing the inventory item |
| Entity ticking | Single's center chunk, Wide's full 3x3 area, unticked chunks outside those footprints, Off mode, dimension isolation, and Single/Wide restart fixtures in the End |
| Chunk release | Single/Wide deactivation and removal; Wide-to-Single entity-ticking downgrade; shared and overlapping owners; Nether release; chunk and entity-data unloading without loading them through the checks |

Footprint tests measure actual entity ticks and check that chunks outside the footprint stay unticked. Release checks query chunk and entity-data unloading without loading those chunks. Recipe discovery checks a Survival player's recipe book after inventory changes.

### Fresh-Server Restarts

The restart suite saves one disposable world across three separate Java 25 server processes:

1. **Prepare:** Prove anchors tick, leave inactive fixtures unloaded, and save ordinary/piston-moved placements and pending offline credit.
2. **Restore:** Confirm ticking and credit survive restart, deliver credit through the join handler, then release the active anchors and save.
3. **Released:** Confirm released chunks remain unloaded, consumed credit stays consumed, and deferred credit delivers once.

Fixtures cover Single and Wide in the Overworld and End, plus Wide in the Nether. They use ordinary redstone power and observe 1,200 game ticks after settling, at 200 ticks per second, exceeding the idle-dimension grace period.

Servers bind only to localhost on an assigned port, disable empty-server pausing, and allow no connected players. The task accepts the Minecraft server EULA for these disposable servers. Credit checks use server-side player stand-ins, not real network connections.

## Manual Checks

Use the exact release JAR in a separate Minecraft instance and disposable worlds. Run this checklist before releasing a new Minecraft port; after smaller changes, repeat affected checks. The server mechanics listed under **Automated Coverage** do not need a full manual rerun.

1. **Blocks and advancement display:** Check every block and variant in-world and in inventory, the four advancements, their tab background, and Wide's activation icon. The ordinary inventory item should look Off.
2. **Survival menus:** Obtain the unlock ingredients, check the nine crafting and six stonecutter recipes appear, and craft an output in each menu. Confirm previews and received items agree. Recipe definitions, counts, and unlock rules are already automated; this checks the installed client's menus.
3. **Materials and shaders:** Check shapes in daylight and darkness with shaders off, with LabPBR alone, and with LabPBR plus POM. Confirm the bundled pack's icon and that enabling it aligns Wide's recessed glow; disable it again and check the non-shader glow. Shader appearance and GPU-specific behavior remain manual.
4. **Piston rendering:** Run Single and Wide with both same-tick power-removal orders in the lab. Check the piston retracts, the Off anchor returns, and no ghost blocks remain before reloading. Server movement and ticket behavior are already automated.
5. **Single-player reopening:** Leave Single/Wide/Off counter fixtures outside the player's ticking area. Save and return to title, reopen, then repeat after fully closing the client. Confirm active counters resume and Off stays stopped. Disk persistence and server restart behavior are already automated; this checks the integrated-server lifecycle.

### Optional Integration Checks

Use the [manual multiplayer credit check](multiplayer-testing.md) once per new port and after multiplayer/login handling changes. One client can be relaunched under two local identities: the placer disconnects, someone else activates, and the placer returns after a server restart. Confirm only the placer receives credit, once. Record it as untested until completed. Attribution, offline credit, dimensions, and saved credit across restarts are also covered by automated server tests.

Use a backup world for upgrades when save formats or supported Minecraft versions change. Extra counter runs, repeated build loops, and fixture diagnostics are troubleshooting, not routine release checks.

## Known Issues

Negative-boundary piston tests have intermittently failed during fixture setup, before movement, reporting that chunk or entity data was not ready. Setup now waits up to ten real-time seconds for entity data; movement assertions remain unchanged.

The bounded wait passed [CI](https://github.com/Omegaplex/ankerstone/actions/runs/39994180957) and controlled-delay checks, but the original cause remains unconfirmed. If it recurs, keep the failure message and server log before rerunning. Diagnostics include entity-ticking state, entity-data readiness, ticket levels, and chunk-loading details. See the [entity-data readiness failure](https://github.com/Omegaplex/ankerstone/actions/runs/39989892912).

## Latest Results

Results apply only to the version and artifact tested. Repeat affected checks after changes and smoke-test the exact release JAR.

### Minecraft 26.3

- **Automated, 2026-10-28:** Full local build passed, as reported by the tester, after updating recipe-unlock advancements and both slab loot tables for the new data formats.
- **Test lab, 2026-10-28:** All five manual checks above passed using the packaged RC1 JAR, including crafting, advancements, piston rendering, textures, shaders/POM, and Off/Single/Wide counters after title-screen reopening and full client restarts.
- **Manual network credit, 2026-10-28:** Passed the [multiplayer credit check](multiplayer-testing.md). The activator received no placer credit; the returning placer received saved credit after anchor removal and a server restart. After revocation and another restart, consumed credit was not delivered again.
- **Artifact:** `ankerstone-1.0.0-rc1+mc26.3.jar`. These results do not yet constitute a final-release JAR smoke test or a CI pass.

### Minecraft 26.2

- **Test lab, 2026-10-27:** All five manual checks above passed, including shader/POM appearance, both piston removal orders, and Off/Single/Wide counters after title-screen reopening and full client restarts. The lab save was upgraded from 26.1.2 to 26.2 and backed up after testing.
- **Automated, 2026-10-27:** Full local build passed all 125 GameTests and all three restart phases after removing client automation. CI passed the earlier 113-test suite on 2026-10-23; the expanded suite still needs CI confirmation.
- **Manual, 2026-10-23:** Installed-client block, variant, recipe, and advancement display; Off/Single/Wide full-client restart counters; POM pack icon/default state; non-shader Single/Wide appearance and compatible POM glow alignment passed.
- **Manual network credit, 2026-10-27:** Passed the [multiplayer credit check](multiplayer-testing.md) using sequential real-client connections as AnkerPlacer and AnkerActivator on a localhost-only, offline-mode dedicated server. The activator received no credit; the returning placer received credit after anchor removal and a server restart. After revocation and a second restart, consumed credit was not delivered again. Tested with Fabric Loader 0.20.1, Fabric API 0.161.0+26.2, and `ankerstone-1.0.0-rc1+mc26.2.jar`. This does not test Microsoft authentication or internet routing.
- **Artifact:** `ankerstone-1.0.0-rc1+mc26.2.jar` remains byte-for-byte identical to the installed JAR tested above and contains no test-only code.

### Minecraft 26.1.2

- **Automated, 2026-10-27:** Full local build passed all 125 GameTests and all three restart phases after removing client automation.
- **Manual:** Recipe-book, credit, restart, and piston checks were recorded before release; the latest Wide material check was on 2026-10-22. Installed-client checks on October 20 and 21 used RC1 builds whose contents changed under the same filename.
- **Artifact:** `ankerstone-1.0.0+mc26.1.2.jar` remains byte-for-byte identical to the published 1.0.0 JAR and contains no test-only code. The RC1 manual records are not a separate installed-client pass for this stable artifact.

Controlled faults confirmed that ticket leaks, loading-only leaks, disabled ticket restoration, an incorrect unlock ingredient, and omitted saved credit fail the relevant server tests. Release artifacts contain no test-only code.

The experimental real-client automation passed locally on 2026-10-26, but was retired on 2026-10-27 because its version-specific maintenance outweighed the benefit. Those development-runtime runs do not replace the installed-client checklist above.
