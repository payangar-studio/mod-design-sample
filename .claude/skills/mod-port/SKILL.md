---
name: mod-port
description: How to port this mod to another Minecraft version, after `mod port <version>` created its branch. Use when the user asks to port, update or move the mod to a Minecraft version, when the build breaks right after `mod port`, or to regenerate the screenshot references of a new version.
---

# Porting the mod to another Minecraft version

`mod port <version>` does the deterministic part: it creates the branch named
after the version from the current one, and writes the versions the socle pins
for it (Minecraft, loaders, Fabric API, Gradle plugins) in `.mod/socle.toml`.
It edits no code and commits nothing. This skill covers the rest.

## 1. Start the port

- Start from a clean working tree, on the branch of the version to port from.
- The socle must be the one of the installed `mc-cli`: `mod port` refuses an
  older one, since the versions it writes go with the build of its own socle.
  Run `mod update`, commit, then port.
- Run `git fetch` first: `mod port` refuses a branch of that version that
  exists locally or on a remote, and only sees the remote branches fetched.
- Run `mod port <version>`. A version it does not know means `mc-cli` has no
  versions pinned for it yet: tell the user to update `mc-cli`, and stop.
- Never edit the versions of `.mod/socle.toml` by hand: they come from `mc-cli`.

## 2. Fix the compile errors

1. Run `./gradlew build` and read every error before changing anything.
2. Fix the mod's own code only: `common/`, `fabric/`, `neoforge/`, outside the
   `vendor` packages. Look up each renamed or removed class or method in the
   new version's game and loader sources, never from memory.
3. Keep each fix to what the new version requires: no refactoring, no
   behaviour change. When the new version forces a behaviour change, say so to
   the user before making it.
4. An error in a file of the socle (`build-logic/`, the `vendor` packages,
   `.mod/`) is not fixed by hand: `mod update` would refuse or undo it. Tell
   the user that `mc-cli` needs a release that supports this version, and stop.

Repeat until the build passes.

## 3. Rerun every test layer

| Layer | Run locally |
|---|---|
| Build and unit tests | `./gradlew build` |
| Integration tests, server gametests, NeoForge's server self-test | `./gradlew check` |
| Client gametests and NeoForge's client self-test (needs Xvfb) | `./gradlew clientCheck` |

A test that fails after the port is a port bug until proven otherwise: fix the
code, not the test. Change a test only when the new version changed the
behaviour it checks, and say so to the user. Screenshot tests are the
exception, below.

## 4. Regenerate the screenshot references in CI

Renders depend on the game version, and references come from CI only, never
from a local GPU. Only the Fabric client gametests take screenshots.

1. Commit the port (`.mod/socle.toml` and the fixes together) and push the
   branch.
2. Wait for the `Tests` workflow. Its `client` job uploads the renders as the
   `screenshots` artifact, even when the screenshot tests fail.
3. Download it: `gh run download <run id> --name screenshots --dir <dir>`,
   with `<dir>` outside the repository.
4. Look at every render before promoting it: a render that is wrong is a bug
   to fix, not a new reference.
5. Run `./gradlew :fabric:promoteScreenshots -Pscreenshots=<dir>`, commit the
   new references, push, and check that the `Tests` workflow passes.

## 5. Finish

- The port is done when CI is green on the new branch.
- The default branch of the GitHub repository is the newest version: when the
  port is to a newer version than the current default, propose to the user to
  run `mod setup` once the new branch is pushed, which makes it the default.
  Never run it without their go.
