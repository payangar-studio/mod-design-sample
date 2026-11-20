<!-- mod:begin -->
## Mod socle

This repository is a Minecraft mod managed with `mc-cli` (`mod` binary). Each Minecraft version has its own branch, named after it; the default branch is the newest.

### What is where

- `mod.toml`: the mod's identity and contract (id, package, side, loaders, license, dependencies). The build fills the loader metadata and the publication from it: declare a dependency there, once.
- `page/`: the project page on the platforms; `page/page.toml` holds the platform ids.
- `changelogs/`: the release notes, one file per version, written by hand.
- `common/`, `fabric/`, `neoforge/`: the mod's code, one subproject per loader plus the shared code; a single-loader mod has only its loader's subproject.
- The socle, rewritten by `mod update`: `.mod/`, `build-logic/`, the Gradle wrapper and `settings.gradle.kts`, the socle's skills, its workflows (`mod-` prefix) and this block.

### Use the CLI

| To | Run |
|---|---|
| Bring the socle of this repository to the CLI's version | `mod update` |
| Port the mod to another Minecraft version, on a new branch; then follow the `mod-port` skill | `mod port <version>` |
| Configure the GitHub repository once the version branch is pushed: ruleset of the release tags, vault variables, CODEOWNERS, default branch, design page | `mod setup [--no-github]` |
| Create a new mod repository, elsewhere | `mod init [dir]` |

Every command runs without a terminal: a missing value is an error that names the argument to pass. Never pass `--force` without the user's go: it overwrites what they changed.

### Tests

| Layer | Run locally |
|---|---|
| Build and unit tests | `./gradlew build` |

### What counts as proof

- A bug fix: a test that fails before the fix and passes after it.
- A feature: a test that exercises it in the game, not only its compilation.
- A screen: a screenshot test.
- Done means these pass locally and CI is green on the pushed branch.

### Sides

- The side is set once, as `side` in `mod.toml`: `client`, `server`, `both-required` or `both-optional`. The loader metadata, the platform sides, the config scopes and the test matrix follow from it; change it there only.
- Client code (rendering, screens, key bindings, anything under `net.minecraft.client`) lives in the client source set and is reached only from the client entrypoint: a dedicated server has none of these classes.
- A client-only mod has client settings only.
- Fabric refuses no client that lacks a mod required on both sides: a `both-required` mod checks the other side itself.
- NeoForge disconnects a client that lacks a required network channel: a `both-optional` mod declares its channels optional.

### Never

- Edit a file of the socle: `build-logic/` (including the loader metadata templates of `build-logic/templates/`), the `vendor` packages, `.mod/`, the socle's skills and workflows. `mod update` rewrites them and refuses to overwrite a hand edit.
- Push a `v*` tag without the user's go: a tag publishes the mod.
- Print, ask for or store a secret. Publication runs in CI only, with the vault's tokens.

### Agent instructions

- Follow the `editing-agents-md` skill before changing any AGENTS.md or CLAUDE.md; CI runs its check.
- After a finding that could recur beyond the current work (a review, a validation, a human report), ask what in the harness would have prevented it: a check first; otherwise a rule in the closest AGENTS.md; otherwise nothing.
- When a change removes the cause behind such a rule (a check now covers it, the code changed), update or remove the rule.
<!-- mod:end -->
