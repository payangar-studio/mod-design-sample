---
name: mod-design
description: Build or update the design page of this mod, `docs/design/index.html` on the `design` branch, from the design documents next to it (`docs/design/*.md`), as one self-contained HTML file that people read without Claude, published by GitHub Pages. Use when the design documents changed and the page must follow, when asked to build, refresh or rework the design page, or when someone finds the page unclear ("too much text", "I cannot see how the sides talk"). Not for writing the design itself, which happens in the Markdown documents.
---

# Design page of the mod

The `design` branch holds the design of the mod twice:

- the Markdown documents of `docs/design/`, for agents. They are the source;
- `docs/design/index.html`, the same design for people. GitHub Pages serves
  it at `https://<owner>.github.io/<repository>/design/`.

The page shows what the documents say, nothing more. When they disagree, the
page is wrong. The branch does not follow the code: it says what the mod is
meant to do.

## Where to work

The branch is checked out apart from the code. Find a worktree of it with
`git worktree list`; if there is none, add one next to the repository:
`git worktree add ../<repository>-design design`. Work there, and never merge
`design` into a version branch or the other way round.

## What the page is made of

A page is looked at, not read. Someone opens it between two things and must
see what the mod does at a glance, then find the detail where they look for
it.

Pick the form from what is described, not by habit:

| What the documents describe | Form on the page |
|---|---|
| A mechanic whose parts depend on each other (steps, triggers, a state that spreads) | An inline SVG diagram |
| Comparable things: blocks, items, entities, config keys, commands | A table, one short line per row and why it exists |
| What only words can carry | A short paragraph |

A paragraph that explains what a diagram would show means the diagram is
missing.

The sections follow the documents. A mod usually has these:

- **Overview**: what the mod adds to the game in two sentences, and what the
  player does with it.
- **Mechanics**: one diagram per mechanic, then a numbered legend that refers
  to the marks on the diagram. Show where a state is born (a tick, an event,
  a player action) and what it changes. A concrete example ("a zombie hit by
  the blade at night loses 4 hearts") shows a rule better than its wording.
- **Blocks and items**: a table with id, what it does, how it is obtained
  (recipe, loot, trade), and its notable properties. A crafting recipe is
  drawn as the 3x3 grid of the game. A texture of the mod may appear as a
  `data:` PNG with `image-rendering: pixelated`.
- **Config**: a table with key, default, range, side (client or server) and
  effect.
- **Sides**: a diagram of the client and the server, with what runs on each
  and what crosses between them (packets, synced values). `mod.toml` says
  where the mod runs (`side`); the page uses the same words.
- **Open questions**: each one where the problem is understood, with what
  each answer would change, not in a list at the end.

Also:

- A glossary first when the mod brings its own words, each word then used in
  one sense only. A name not chosen yet is shown as missing ("???"), never
  replaced by a working word.
- A statement says what it is: a fact, a decision or a question. Options are
  shown at the same level of detail, without a recommendation.
- A diagram is understood without someone pointing at it. What links the
  parts shows more than the parts.
- The principle, not the how: no build steps, no code beyond the few lines
  that make a point.

## The page is one self-contained file

It must read offline, from Pages, or from a copy sent by mail:

- styles in one `<style>`, scripts (if any) in `<script>`, diagrams as inline
  SVG, images as `data:` URIs;
- no CDN, no web font, no remote image, no `fetch`, no other file next to it,
  and nothing that needs Claude to render (no artifact runtime);
- links people follow (`<a href>`) are fine: they load nothing;
- colors as CSS variables on `:root`, redefined under
  `@media (prefers-color-scheme: dark)`, with an explicit `body` background;
- readable on a phone: a 16px side gutter, no horizontal scroll, wide tables
  in their own scrolling box.

Check it, from the worktree of `design`, with the script next to this skill:
`sh <this skill's directory>/check-page.sh docs/design/index.html`. It lists
everything the page loads; fix each one until it says the page is
self-contained.

## Steps

1. Read every document of `docs/design/`, and the current page if it is not
   the starter one.
2. Write the page. When it exists, keep what still holds and rework around
   it; a diagram that is no longer true goes, or changes with the decision
   behind it.
3. Run `check-page.sh` until it passes.
4. Look at the rendered page when a browser is available: desktop and phone
   width, light and dark theme.
5. For a first version or a rework, have a fresh agent read the page cold,
   with only the file path and who the reader is: what it understood first,
   then what is unclear, too long or badly displayed. It changes nothing;
   you decide what to fix.
6. Commit on `design` and push it. Pages publishes the page within minutes.
   When `origin` has no `design` branch yet, do not push it: on an empty
   repository it would become the default branch. `mod setup` pushes it once
   the version branch is there, and sets Pages.
