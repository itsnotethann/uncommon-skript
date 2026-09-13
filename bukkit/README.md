# bukkit

Optional Bukkit addon support for uncommon-skript. `common` does not depend on this module and
never loads an `org.bukkit` class without it.

## What it gives you

Upstream skript-minestom's addon support, on a core with no Bukkit in it — plus a first set of
Minecraft types upstream does not have.

The shim covers plugin loading, configuration, scheduling, commands, permissions, services and
Bukkit's event plumbing, and now the core game types: `Entity`, `Damageable`, `LivingEntity`,
`HumanEntity`, `OfflinePlayer` and `Player` as interfaces, `World`, `Location`, `GameMode` and
`Permission`, plus `Bukkit.getWorlds()`, `getWorld(name|UUID)`, `getPlayer(name)` and
`getPlayerExact(name)`. Their shapes follow real Bukkit — interfaces where Bukkit has interfaces —
because addons are compiled against real Bukkit. Upstream's `Player` is a concrete class, so any
addon calling a method on a player fails there with `IncompatibleClassChangeError`.

Still not covered: blocks, items, materials, inventories, non-player entities, and every method on
the types above that is not declared here. An addon that calls one fails with a linkage error naming
it.

This module only defines the types. Something has to back them with a real server, and convert
between the platform's objects and Bukkit's:

- **`BukkitServerProvider`** is a `ServiceLoader` hook. A platform module registers one and returns a
  `Server` whose players and worlds are real. For Minestom that is `minestom-bukkit` in the Minestom
  host project, a separate jar.
- **`BukkitTypeBridge`** is the conversion hook. Addon code asking for a Bukkit type gets the
  platform object converted, whether it arrives through a cast, an array, an `instanceof` or a lambda;
  and the bridge registers Skript converters for the other direction, so an addon expression returning
  a Bukkit `Player` works in core syntax. Addon syntax typed `%player%` therefore receives a real Bukkit
  `Player`. Patterns naming types the platform does not register under Bukkit's name — `%location%`,
  `%world%` on Minestom — still do not parse.
- Without a provider, `SpiServer` is used: server questions are answered from the SPI, and there are
  no players or worlds.

## Using it

Put `bukkit.jar` on the host's classpath. It registers two services:

- `org.skriptlang.skript.platform.AddonRegistry` — loads jars from `addons/`
- `org.skriptlang.skript.platform.EventBus` — delivers Bukkit events addons fire or listen for

A host that looks up its addon registry through `ServiceLoader` needs no code change. Skript's core
picks up extra event buses on its own. A `PlatformProvider` must already be installed.

Use the shaded `bukkit.jar` from `:bukkit:shadowJar`. It bundles its own ASM, relocated to
`org.skriptlang.skript.platform.bukkit.asm`, because hosts ship whatever ASM their other dependencies
pull in — a typical Minestom host ships 9.7, which cannot read Java 25 class files, so every addon
compiled for a current Java failed to load. It also bundles two things addons expect to find inside
Skript: bStats' Bukkit `Metrics`, relocated to `ch.njol.skript.bstats` as upstream Skript ships it
(oopsk calls it), and JNA, which oopsk's bundled ByteBuddy needs. Nothing else is bundled: guava,
snakeyaml, adventure and logback must be the host's own copies, because their types cross into Skript.

Addons are found by `plugin.yml`, or `paper-plugin.yml` when there is none; a Paper
`dependencies.server` entry counts as `depend` when required and `softdepend` otherwise.


## Why it rewrites bytecode

uncommon-skript retypes `org.bukkit.event.Event` to `PlatformEvent` across Skript's syntax
interfaces. An addon compiled against upstream implements `getArray(org.bukkit.event.Event)`;
Skript calls `getArray(PlatformEvent)`. Different erasure, so the addon's method is not an override
and the call fails with `AbstractMethodError`. A shim cannot fix that, so `PluginClassLoader`
rewrites each addon class as it is defined:

| In the addon | Becomes |
|---|---|
| `Event` in the descriptor of a method that does **not** implement a shim type | `PlatformEvent`, and so do that method's frames, array creations and casts |
| `Event` in a call to or field of anything outside `org.bukkit` | `PlatformEvent` |
| a call into `org.bukkit` whose last argument is `Event`, inside a rewritten method | the same call, with a cast back to `Event` first |
| an `Event.class` or `Cancellable.class` literal | the platform type � oopsk looks up its generated constructors reflectively with it |
| `Event.getEventName()` / `Event.isAsynchronous()` | statics in `BukkitAddons` |
| `org.bukkit.event.Cancellable` | the platform `Cancellable` — identical methods |
| `getEventPriority()` returning Bukkit's `EventPriority` | the core call, converted back to Bukkit's enum |
| `Skript.registerAddon(JavaPlugin)` | `BukkitAddons.registerAddon` — the core no longer has it |
| a `checkcast` or `instanceof` against an `org.bukkit` type | preceded by `BukkitValues.adapt` / `isInstance`, which converts platform objects — a Minestom player becomes a Bukkit `Player` |
| a lambda or method reference with an `org.bukkit`-typed parameter | pointed at a synthetic bridge method that converts the arguments first, because the JVM-generated lambda class does its own cast |

The distinction that matters: a method overriding a **Skript** interface gets `PlatformEvent`; a
method implementing a **Bukkit shim** interface — skript-reflect's `EventExecutor.execute(Listener,
Event)` — keeps `Event`, or it stops implementing it. Superclasses and constructor calls are never
touched; addons subclass `Event`, and the shim's `Event` already implements `PlatformEvent`.

## Parity with upstream

Same skript-reflect 2.6.3, same script, upstream skript-minestom `b5096343` against this module on
uncommon-skript. 20 output lines each:

| Checked | Without a provider | With `minestom-bukkit` |
|---|---|---|
| static calls, constructors, instance methods, static fields | identical | identical |
| custom expressions, effects, conditions | identical | identical |
| Java calls after `wait`, inside functions, in loops | identical | identical |
| `event` from an addon expression | identical | identical |
| Bukkit console sender, sending to it | identical | identical |
| listening for a Bukkit event, firing it through `ServicesManager` | identical | identical |
| skript-reflect custom events, defined and called | identical | identical |
| `Bukkit.getName()` | `uncommon-skript` | identical |

With `minestom-bukkit`, a connected client also verified: `getPlayer(UUID)` and `getPlayerExact`
return equal wrappers, online and per-world player counts, `§` colour codes in `sendMessage`,
health, game mode read and set, location and world, cross-check against Minestom's own game mode,
teleport, and LuckPerms-backed `hasPermission`. `isOp()` is always `false` — Minestom has no
operators.

oopsk 1.0-beta2, with a template script covering instances, defaults, `const`, reset, copy, the
template condition and conversion to string: output identical to upstream, run alongside
skript-reflect with its 20 lines still identical. Upstream itself only starts oopsk with JNA added to
its classpath in the build tested here.

Minestom instances have no names, so a `World` reports its dimension name, or its UUID when more
than one instance shares that dimension.

## Sizing an addon before trying it

List every Bukkit type the addon touches. Scan the whole jar, and include same-class calls — javap
prints those without an owner:

```bash
mkdir addon && cd addon && unzip -q ../SomeAddon.jar
javap -p -c -classpath . $(find . -name '*.class' | sed 's|^\./||; s|/|.|g; s|\.class$||') > all.txt
grep -oE "(Method|InterfaceMethod|Field) [^ ]*" all.txt | grep "org/bukkit/" | sort | uniq -c | sort -rn
```

Every method listed must exist in the shim under `src/main/java/org/bukkit/` — and for game types,
be implemented by the platform's `BukkitServerProvider`. What is missing is the work.

## Performance

Not a reason to use this. Event dispatch was benchmarked against upstream on the same addon and
scripts: about 12 ns slower per event with no trigger, and within run-to-run noise with triggers.
