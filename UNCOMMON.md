# uncommon-skript

The platform-free half of the Skript-on-Minestom stack. Design doc: `re/DOMAIN.md` in the
`sckript` worktree (§6.4 for why this repo exists, §6.5 for the build order).

## Layout

| Path | Built here? | Owner |
|---|---|---|
| `common/` | yes | a **tracking fork** of `skript-minestom/skript-minestom`'s `common/`. Merge from `upstream`; keep fork deltas small and enumerable. |
| `domain/` | yes | fork-owned. The `§2` domain interfaces + `DomainClasses`. Package root `org.skriptlang.skript.domain`. Zero upstream occupant. |
| `minestom/` | **no** | inert. Present only so `git merge upstream` never hits modify/delete conflicts. **`C:\Dev\sckript` is authoritative for this module** — do not edit it here. |

`settings.gradle.kts` includes `common` and `domain` only, which is what makes `minestom/` inert.

## Remotes

- `upstream` → `skript-minestom/skript-minestom` — merge from this.
- `fork` → `itsnotethann/skript-minestom` — the existing fork this repo was seeded from.
- no `origin` yet.

## State

Seeded from branch `sckript` @ `1659db9b` (0 ahead / 82 behind `upstream/master`), the commit
`re/DOMAIN.md` was originally verified against.

**Caught up to `upstream/master` @ `68fb2a85`** — all 82 commits merged, one conflict
(`build.gradle.kts` group/version, resolved in favour of this fork's coordinates). `sckript`
fast-forwarded to the same commit. The design doc was re-audited against the result: of its 48
file:line citations, 35 were unchanged, 10 renumbered, 1 relocated, and 2 turned out to have been
wrong before the merge. See `re/DOMAIN.md` §6.4.6 for the full audit, including the counts that
drifted (in-scope classes 261 → 300) and the one section it partly refuted (§2.15).

Fork deltas against upstream are deliberately kept small and enumerable: the root
`build.gradle.kts` coordinates, `settings.gradle.kts`, this file, and the `domain/` module.

## Consuming it

`C:\Dev\sckript` pulls `common` and `domain` from here via composite build
(`includeBuild("../uncommon-skript")`), so the two resolve from source and break at compile time
rather than at runtime. Coordinates are `com.github.itsnotethann.uncommonskript:{common,domain}`.

`sckript`'s own `common/` directory is now unbuilt and inert for the mirror-image reason
`minestom/` is inert here.
