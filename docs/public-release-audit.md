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

## Testing and release readiness

- [ ] Confirm the full Gradle build passes.
- [ ] Confirm all required GameTests pass.
- [ ] Run the documented manual restart/persistence checks.
- [ ] Test the actual built release jar in a normal Minecraft instance, not only the development environment.
- [ ] Check models, textures, particles, lighting, comparator behavior, pistons, recipes, advancements, and shader compatibility where applicable.
- [ ] Make sure important gameplay bugs have regression tests where practical.
- [ ] Review `docs/testing.md` and update it if test coverage or manual checks changed.

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
