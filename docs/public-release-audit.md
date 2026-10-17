# Public Release Audit

Use this before changing the Ankerstone repository from private to public and again before the first stable release.

## Repository history and privacy

- [ ] Search the full Git history, not just the current files, for anything that should not become public.
- [ ] Check for passwords, API tokens, private paths, personal files, email addresses, temporary files, and other sensitive information.
- [ ] Check source and history for accidental Codex/OpenAI markers, generated-by comments, or unwanted co-author metadata.
- [ ] Review old issues and comments for anything written with the assumption that the repository would stay private.
- [ ] Review GitHub Actions logs and artifacts for anything sensitive that may have been printed or uploaded.
- [ ] Delete any workflow runs or artifacts that should not become public.

## Source code audit

- [ ] Review source for comments that only restate obvious code.
- [ ] Remove stale template comments and unused boilerplate.
- [ ] Look for unnecessary abstractions or over-engineered code.
- [ ] Check that names and terminology are consistent with the project: Off, Single, Wide, owners, tickets, etc.
- [ ] After the test work, go through the main and client code and shorten redundant class, method, and variable names. Keep useful distinctions and normal Java conventions. Update references and run the full build; leave registry IDs and saved-data keys unchanged.
- [ ] Check for dead code, abandoned experiments, and unused files.
- [ ] Flag important code that I do not understand well enough to explain before publishing.
- [ ] Keep working code simple rather than refactoring only to make it look more "professional."

## Documentation and issue audit

- [ ] Review the README for canned or overly formal wording.
- [ ] Review issues for specification-style language that does not sound natural for the project.
- [ ] Make sure documentation reflects the final 1.0 behavior and crafting progression.
- [ ] Clearly separate current features from future plans.
- [ ] Make sure installation requirements and supported Minecraft/Fabric versions are easy to find.
- [ ] Check that the license, source links, project description, and `fabric.mod.json` metadata are correct.
- [ ] Add an administrator note about placer credit: it uses world SavedData in the Overworld, keyed by dimension and position, not block-entity or item data. Explain piston movement, breaking and replacing blocks, and pending offline credit. Mark WorldEdit cloning as untested.

## Testing and release readiness

- [ ] Confirm the full Gradle build passes.
- [ ] Confirm all required GameTests pass.
- [ ] Run the documented manual restart/persistence checks.
- [ ] Test the actual built release jar in a normal Minecraft instance, not only the development environment.
- [ ] Check models, textures, particles, lighting, comparator behavior, pistons, recipes, advancements, and shader compatibility where applicable.
- [ ] Make sure important gameplay bugs have regression tests where practical.
- [ ] Review `docs/testing.md` and update it if test coverage or manual checks changed.

### Focused test follow-ups

- [x] Test mutable input positions in both saved and runtime owner maps. Changing the supplied position must not move the stored owner or prevent removal of its ticket. Protect both storage paths when fixing this.
- [x] Test an Observer watching actual Ankerstone mode changes. Let setup pulses finish before checking the transition. Keep the existing update flags unless a failing test demonstrates a problem.
- [x] Test a real comparator circuit updating to 0, 7, and 15, including decreasing power.
- [x] Cover Off piston pushes and sticky pulls in all six directions: east, west, north, south, up, and down. Check physical positions, mode, placer transfer, and absence of active ownership/tickets.
- [x ] Test movement across actual X and Z chunk boundaries, including negative coordinates. Check absolute positions rather than assuming local coordinate zero is a chunk boundary. Include a powered destination and check actual tickets as well as saved owners.
- [ ] Test a sticky piston retracting on the same tick Single or Wide Ankerstone loses power. Try both update orders and check block positions, chunk ownership/tickets, and placer records. Also check in-game for ghost blocks or client/server disagreement.
- [ ] Check actual entity ticking in Single and Wide coverage, including Wide's corners, positions just outside the footprint, Off mode, and another dimension. Keep player, spawn, and test-runner tickets from making the check pass accidentally.

### Other useful cases

- [ ] Test sticky retraction against Single and Wide blocks that remain powered. Active push blocking is covered, but active sticky-pull blocking is not directly tested.
- [ ] Test an anchor changing mode while another anchor shares its chunk. Check the actual ticket levels through upgrades, downgrades, and removal so one owner cannot release another's ticket.
- [ ] Automate the manual no-repeat-credit check: deliver pending credit, revoke the advancement, and invoke JOIN again. The consumed pending entry must not award it again.

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
