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

## State at creation

Seeded from branch `sckript` @ `1659db9b`, which is the commit `re/DOMAIN.md`'s ~400 file:line
citations were verified against. That commit is **0 ahead / 82 behind `upstream/master`** — this
repo has no fork deltas at all today, and catching up those 82 commits is a decision to make
deliberately, because doing so invalidates the design doc's line citations.

## Consuming it

`C:\Dev\sckript` pulls `common` and `domain` from here via composite build
(`includeBuild("../uncommon-skript")`), so the two resolve from source and break at compile time
rather than at runtime. Coordinates are `com.github.itsnotethann.uncommonskript:{common,domain}`.

`sckript`'s own `common/` directory is now unbuilt and inert for the mirror-image reason
`minestom/` is inert here.
