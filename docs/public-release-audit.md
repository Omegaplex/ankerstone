# Public Release Audit

Use this before changing the Ankerstone repository from private to public and again before the first stable release.

## Repository history and privacy

- [ ] Search the full Git history, not just the current files, for anything that should not become public.
- [ ] Check for passwords, API tokens, private paths, personal files, email addresses, temporary files, and other sensitive information.
- [ ] Check source and history for accidental Codex/OpenAI markers, generated-by comments, or unwanted co-author metadata.
- [x] Review old issues and comments for anything written with the assumption that the repository would stay private.
- [ ] Review GitHub Actions logs and artifacts for anything sensitive that may have been printed or uploaded.
- [x] No workflow runs or artifacts identified for removal.

## Source code audit

- [x] Review source for comments that only restate obvious code.
- [x] Remove stale template comments and unused boilerplate.
- [x] Look for unnecessary abstractions or over-engineered code.
- [x] Check that names and terminology are consistent with the project: Off, Single, Wide, owners, tickets, etc.
- [x] After the test work, go through the main and client code and shorten redundant class, method, and variable names. Keep useful distinctions and normal Java conventions. Update references and run the full build; leave registry IDs and saved-data keys unchanged.
- [x] Check for dead code, abandoned experiments, and unused files.
- [ ] Flag important code that I do not understand well enough to explain before publishing.
- [x] Keep working code simple rather than refactoring only to make it look more "professional."

## Documentation and issue audit

- [x] Review the README for canned or overly formal wording.
- [ ] Review issues for specification-style language that does not sound natural for the project.
- [ ] Make sure documentation reflects the final 1.0 behavior and crafting progression.
- [ ] Clearly separate current features from future plans.
- [x] Make sure installation requirements and supported Minecraft/Fabric versions are easy to find.
- [x] Check that the license, source links, project description, and `fabric.mod.json` metadata are correct.
- [x] Add an administrator note about placer credit: it uses world SavedData in the Overworld, keyed by dimension and position, not block-entity or item data. Explain piston movement, breaking and replacing blocks, and pending offline credit. Mark WorldEdit cloning as untested.

## Testing and release readiness

- [ ] Confirm the full Gradle build passes.
- [ ] Confirm all required GameTests pass.
- [ ] Run the documented manual restart/persistence checks.
- [x] Test the actual built release jar in a normal Minecraft instance, not only the development environment.
- [ ] Check models, textures, particles, lighting, comparator behavior, pistons, recipes, advancements, and shader compatibility where applicable.
- [ ] Make sure important gameplay bugs have regression tests where practical.
- [x] Review `docs/testing.md` and update it if test coverage or manual checks changed.

### Test quality review

- [x] Review what each test proves and whether it would catch a real mod failure. Check that setup or unrelated Minecraft behavior cannot make it pass accidentally.
- [x] Remove tests that repeat coverage without protecting a different risk. Keep useful edge cases and readable repetition; do not add abstractions just to reduce the line count.
- [x] Check reliability, useful failure messages, cleanup on failure or timeout, and maintenance cost. Judge the suite by these checks, not test count or the test-to-mod line ratio.

### Focused test follow-ups

- [x] Test mutable input positions in both saved and runtime owner maps. Changing the supplied position must not move the stored owner or prevent removal of its ticket. Protect both storage paths when fixing this.
- [x] Test an Observer watching actual Ankerstone mode changes. Let setup pulses finish before checking the transition. Keep the existing update flags unless a failing test demonstrates a problem.
- [x] Test a real comparator circuit updating to 0, 7, and 15, including decreasing power.
- [x] Cover Off piston pushes and sticky pulls in all six directions: east, west, north, south, up, and down. Check physical positions, mode, placer transfer, and absence of active ownership/tickets.
- [x] Test movement across actual X and Z chunk boundaries, including negative coordinates. Check absolute positions rather than assuming local coordinate zero is a chunk boundary. Include a powered destination and check actual tickets as well as saved owners.
- [x] Test Single and Wide sticky retraction with both power-removal orders in the same tick, before the queued retraction runs. Check block positions, owners, tickets, placer records, and pending credit.
- [x] Check the same power-loss cases in-game for ghost blocks or client/server disagreement.
- [x] Check actual entity ticking in Single and Wide coverage, including Wide's corners, positions just outside the footprint, Off mode, and another dimension. Keep player, spawn, and test-runner tickets from making the check pass accidentally.
- [x] Directly check that a Nether anchor does not tick entities at matching Overworld coordinates. Nether footprint coverage alone does not prove dimension isolation.

### Other useful cases

- [x] Test sticky retraction against Single and Wide blocks that remain powered. The piston retracts while the block, owner mode, ticket, and earned offline credit remain intact.
- [x] Test an anchor changing mode while another anchor shares its chunk. Check the actual ticket levels through upgrades, downgrades, and removal so one owner cannot release another's ticket.
- [x] Automate the manual no-repeat-credit check: deliver pending credit, revoke the advancement, and invoke JOIN again. The consumed pending entry must not award it again.

### Second-opinion follow-up

- [x] Add a focused negative-coordinate ticket test that distinguishes floor division from truncation.
- [x] Test LOAD/UNLOAD registration and runtime clearing without letting surviving tickets mask a failure.
- [x] Test full-block conduction properties and real circuits with a vanilla stone control.
- [x] Check intermediate states between the two immediate power-removal calls.
- [x] Test attribution cleanup when Ankerstone is removed during sticky retraction.
- [x] Confirm targeted tests fail under deliberate production mistakes, then restore the code and pass the full build.
- [x] Trim repeated recipe shifts and mirroring checks. Retain the mining matrix, direction coverage, and Observer/comparator transitions after reviewing their maintenance cost.
- [x] Extend the manual restart check to a Wide corner chunk and record the result.
- [x] Repeat the outside review using current production code, tests, and all test resources.
- [x] Confirm existing ticking tests detect removal of each required ticket flag.
- [x] Test dimension-local saved ownership and ticket storage, including a deliberate shared-storage failure.
- [x] Remove the vanilla forced-chunk data fixer without changing the owner save format.
- [x] Replace the extension-interruption delay with bounded movement detection.
- [x] Check Wide loading after returning to the title screen and reopening the world without closing Minecraft.
- [x] Check Nether anchor persistence across a full Minecraft restart.

### Decisions for later

- [ ] Decide whether to repair stale owners left by external world edits that bypass removal hooks. If supported, test reconciliation timing before adding a chunk-load cleanup hook.
- [ ] Review retry behavior when an advancement resource is missing. Do not discard the placer or earned credit merely to clear the record; normal power cycling can retry after the resource is fixed.

## Version and release packaging

- [ ] Confirm the mod version is correct for the release candidate or stable release.
- [ ] Confirm the Minecraft target is included in the release/jar naming.
- [ ] Pin release tooling to stable versions where practical.
- [ ] Build from a clean checkout before the release.
- [ ] Install and test the exact jar that will be published.
- [ ] Confirm release notes accurately describe the supported Minecraft version and current feature set.

## Public repository checks

- [ ] Confirm the commit identity and noreply email are what I want visible publicly.
- [ ] Review repository description, topics, README front matter, and links.
- [ ] Confirm there are no branches, tags, or releases that should remain private.
- [ ] Review branch protection/rulesets after changing repository visibility.
- [ ] Make the existing repository public rather than creating a cleaned-up copy, unless a real privacy problem requires history rewriting.

## Final quality check

The goal is not to hide the use of coding tools. The goal is to publish code that is understandable, tested, internally consistent, and reflects deliberate project decisions.

Before publishing, I should be able to explain the important behavior in my own words even if I could not reproduce every implementation from memory.
