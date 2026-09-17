#!/usr/bin/env bash
# Boots the Minestom host once per addon with the bukkit layer on the classpath,
# runs `skript info` to list what loaded, and parses a probe script using that addon's syntax.
set -u
here=$(cd "$(dirname "$0")" && pwd)
root=$(cd "$here/../.." && pwd)
work=${ADDONTEST_WORK:-$here/work}
port=${ADDONTEST_PORT:-25700}

host=${HOST_JAR:-}
bridge=${BRIDGE_JAR:-}
bukkit=${BUKKIT_JAR:-$root/bukkit/build/libs/bukkit.jar}
if [ -z "$host" ] || [ -z "$bridge" ]; then
	echo "set HOST_JAR to a skript-minestom server jar built against this repo and BRIDGE_JAR to its minestom-bukkit jar" >&2
	exit 2
fi
for jar in "$host" "$bridge" "$bukkit"; do
	if [ ! -f "$jar" ]; then
		echo "missing jar: $jar" >&2
		exit 2
	fi
done

mkdir -p "$work/jars"
fetch() {
	local repo=$1 tag=$2 file=$3 sum=$4
	if [ ! -f "$work/jars/$file" ]; then
		curl -fsSL -o "$work/jars/$file" "https://github.com/skript-minestom/$repo/releases/download/$tag/$file" || return 1
	fi
	echo "$sum  $work/jars/$file" | sha256sum -c --quiet || { echo "checksum failed: $file" >&2; return 1; }
}

fetch SkCheese-minestom 1.8 SkCheese-1.8.jar 8499d0959a71837c6893e957fd8ddff72a34fc2026e9385793ea2a2767e1209d || exit 1
fetch skript-gui-minestom 1.3.2 skript-gui-minestom-1.3.2.jar 910324dadf2bdde4d8047bfe8d7a86136ff72e10ac03fa3dc52656b54ddefc60 || exit 1
fetch skript-combat 1.0.0 skript-combat-1.0.0.jar 9e850acc519f84da8343b986751e4fba8e2369d1e2166a791d91c3867d018ce6 || exit 1
fetch skript-nbs 1.0.1 skript-nbs-1.0.1.jar f3067a2679b5858524f590b6fc5710f6d473757d1422575348cdd83771f3981c || exit 1
fetch skript-bdengine 1.3.0 skript-bdengine-1.3.0-all.jar 56bd8f71bb0cde591b1b4705c1ee80cb39277f83e3fac520d92f812e569d4c75 || exit 1
fetch SKNoise 1.0.2 skNoise-1.0.2.jar 05380bc2b3925b49bf6394225f2896161a28c386e899d0c6e8d6162710db34a8 || exit 1

world=${ADDONTEST_WORLD:-}
if [ -n "$world" ] && [ ! -f "$world" ]; then
	echo "ADDONTEST_WORLD is set but $world does not exist" >&2
	exit 2
fi

failures=0
for probe in "$here"/probes/*.sk; do
	name=$(basename "$probe" .sk)
	jar="$work/jars/$name.jar"
	run="$work/run-$name"
	port=$((port + 1))
	rm -rf "$run"
	mkdir -p "$run/Skript/scripts" "$run/Skript/addons"
	cp "$host" "$run/server.jar"
	cp "$bukkit" "$run/bukkit.jar"
	cp "$bridge" "$run/bridge.jar"
	cp "$jar" "$run/Skript/addons/"
	cp "$probe" "$run/Skript/scripts/probe.sk"
	runtime="$here/runtime/$name.sk"
	if [ -n "$world" ] && [ -f "$runtime" ]; then
		mkdir -p "$run/worlds"
		cp "$world" "$run/worlds/world.polar"
		cp "$runtime" "$run/Skript/scripts/runtime.sk"
	fi
	printf 'online-mode=false\nserver-ip=127.0.0.1\nserver-port=%s\n' "$port" > "$run/server.properties"
	(
		cd "$run" || exit 1
		{ sleep 30; printf '\n'; sleep 2; printf 'skript info\n'; sleep 8; } \
			| timeout 60s java -cp "server.jar;bukkit.jar;bridge.jar" com.github.hapily04.skriptminestom.SkriptMinestom > log.txt 2>&1
		sed -i 's/\x1b\[[0-9;]*m//g' log.txt
	)
	listed=$(grep -cE '^.* - .*http' "$run/log.txt")
	clean=$(grep -cE "Failed to enable|ERROR\] .*Line [0-9]+:" "$run/log.txt")
	ran=""
	if [ -f "$run/Skript/scripts/runtime.sk" ]; then
		if grep -q "addontest runtime" "$run/log.txt"; then
			ran=" +runtime"
		else
			ran=" -runtime"
			clean=$((clean + 1))
		fi
	fi
	if [ "$listed" -ge 1 ] && [ "$clean" -eq 0 ]; then
		echo "PASS $name$ran"
	else
		echo "FAIL $name$ran (listed=$listed errors=$clean, see $run/log.txt)"
		failures=$((failures + 1))
	fi
done

echo "addon test: $failures failure(s)"
[ "$failures" -eq 0 ]
