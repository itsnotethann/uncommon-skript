# addontest

Checks that the addons published by the skript-minestom organisation load and parse on a Minestom
host built against this repo, with `bukkit.jar` on the classpath.

Each addon gets its own server directory holding the host jar, `bukkit.jar`, the host's
`minestom-bukkit` bridge and that one addon. The run boots, sends `skript info` so the addon list is
in the log, and loads a probe from `probes/` built from the addon's own documented syntax. An addon
passes when it is in the addon list and the log has no `Failed to enable` and no `Can't understand`.

This repo has no Minestom host, so you bring one:

```
./gradlew :bukkit:shadowJar
HOST_JAR=path/to/skript-minestom-all.jar BRIDGE_JAR=path/to/minestom-bukkit.jar tools/addontest/addontest.sh
```

The host has to consume this repo, for example through `includeBuild("../uncommon-skript")`. Upstream
skript-minestom's own jar bundles its own core and will not exercise this one.

Jars are downloaded once into `work/jars/` and checked against pinned sha256 sums. `ADDONTEST_WORK`
moves the working directory, `ADDONTEST_PORT` the port range (one port per addon) and `BUKKIT_JAR`
the layer jar.

Set `ADDONTEST_WORLD` to a `.polar` file to also run the scripts in `runtime/`, which load that world
and call into the addon for real. Those runs report `+runtime` and fail if the script never reaches
its broadcast. skript-gui and skript-nbs have no runtime probe because they need a connected player.
