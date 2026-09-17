package org.skriptlang.skript.testhost.bench;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.event.PlatformEvent;

public final class BenchSyntax {

	private static final Set<String> names = new LinkedHashSet<>();

	private BenchSyntax() {}

	static void register() {
		Skript.registerEvent("Bench", EvtBench.class, new Class[] {BenchEvent.class}, "bench %string%");
	}

	static Collection<String> names() {
		return names;
	}

	public static final class EvtBench extends SkriptEvent {

		private String name;

		@Override
		public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
			name = (String) args[0].getSingle();
			if (name == null)
				return false;
			synchronized (names) {
				names.add(name);
			}
			return true;
		}

		@Override
		public boolean check(PlatformEvent event) {
			return event instanceof BenchEvent bench && bench.name().equals(name);
		}

		@Override
		public String toString(@Nullable PlatformEvent event, boolean debug) {
			return "bench " + name;
		}
	}
}
