# bukkit

Optional Bukkit addon support for uncommon-skript. `common` does not depend on this module and
never loads an `org.bukkit` class without it.

## What it gives you

The same addon support upstream skript-minestom has, on a core with no Bukkit in it.

That is a specific, limited thing. The shim — here and upstream — covers plugin loading,
configuration, scheduling, commands, permissions, services and Bukkit's event plumbing. It does not
contain Minecraft: no worlds, locations, blocks, items, inventories or entities. Addons built on
those do not work here or upstream. Addons that are logic and Java glue do.

## Using it

Put `bukkit.jar` on the host's classpath. It registers two services:

- `org.skriptlang.skript.platform.AddonRegistry` — loads jars from `addons/`
- `org.skriptlang.skript.platform.EventBus` — delivers Bukkit events addons fire or listen for

A host that looks up its addon registry through `ServiceLoader` needs no code change. Skript's core
picks up extra event buses on its own. A `PlatformProvider` must already be installed.

Use the shaded `bukkit.jar` from `:bukkit:shadowJar`. It bundles its own ASM, relocated to
`org.skriptlang.skript.platform.bukkit.asm`, because hosts ship whatever ASM their other dependencies
pull in — a typical Minestom host ships 9.7, which cannot read Java 25 class files, so every addon
compiled for a current Java failed to load. Nothing else is bundled: guava, snakeyaml, adventure and
logback must be the host's own copies, because their types cross into Skript.


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
| `Event.getEventName()` / `Event.isAsynchronous()` | statics in `BukkitAddons` |
| `org.bukkit.event.Cancellable` | the platform `Cancellable` — identical methods |
| `getEventPriority()` returning Bukkit's `EventPriority` | the core call, converted back to Bukkit's enum |
| `Skript.registerAddon(JavaPlugin)` | `BukkitAddons.registerAddon` — the core no longer has it |

The distinction that matters: a method overriding a **Skript** interface gets `PlatformEvent`; a
method implementing a **Bukkit shim** interface — skript-reflect's `EventExecutor.execute(Listener,
Event)` — keeps `Event`, or it stops implementing it. Superclasses and constructor calls are never
touched; addons subclass `Event`, and the shim's `Event` already implements `PlatformEvent`.

## Parity with upstream

Same skript-reflect 2.6.3, same script, upstream skript-minestom `b5096343` against this module on
uncommon-skript. 20 output lines each; 19 identical:

| Checked | Result |
|---|---|
| static calls, constructors, instance methods, static fields | identical |
| custom expressions, effects, conditions | identical |
| Java calls after `wait`, inside functions, in loops | identical |
| `event` from an addon expression | identical |
| Bukkit console sender, sending to it | identical |
| listening for a Bukkit event, firing it through `ServicesManager` | identical |
| skript-reflect custom events, defined and called | identical |
| `Bukkit.getName()` | differs: `uncommon-skript` vs `Skript-Minestom (<version>)` |

Not covered by that run, because it had no players connected, and known to differ:
`Bukkit.getOnlinePlayers()` and `getPlayer(UUID)` return nothing here, where upstream returns the
connected players as name-and-UUID records, and `getOnlineMode()` is always `false`. The SPI has no
player list to answer from. A host that needs those passes its own `Server` to
`BukkitAddonSupport.install(Server)`.

## Sizing an addon before trying it

List every Bukkit type the addon touches. Scan the whole jar, and include same-class calls — javap
prints those without an owner:

```bash
mkdir addon && cd addon && unzip -q ../SomeAddon.jar
javap -p -c -classpath . $(find . -name '*.class' | sed 's|^\./||; s|/|.|g; s|\.class$||') > all.txt
grep -oE "(Method|InterfaceMethod|Field) [^ ]*" all.txt | grep "org/bukkit/" | sort | uniq -c | sort -rn
```

Anything called on a Minecraft type will not work. Anything else must exist in the shim under
`src/main/java/org/bukkit/`.

## Performance

Not a reason to use this. Event dispatch was benchmarked against upstream on the same addon and
scripts: about 12 ns slower per event with no trigger, and within run-to-run noise with triggers.
