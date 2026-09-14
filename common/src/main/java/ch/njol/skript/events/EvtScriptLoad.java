package ch.njol.skript.events;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.events.custom.ScriptLoadEvent;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleEvent;
import org.skriptlang.skript.lang.script.Script;

public class EvtScriptLoad extends SimpleEvent {
	static {
		Skript.registerEvent("Script Load", EvtScriptLoad.class, ScriptLoadEvent.class,
			"[script] (load|init|enable)", "server (load|start)");
	}

	private int pattern;

	@Override
	public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parser) {
		pattern = matchedPattern;
		return true;
	}

	@Override
	public boolean postLoad() {
		if (pattern == 1 && !Skript.isStarting()) return true;
		if (!ScriptLoader.isLoaderThread())
			trigger.execute(new ScriptLoadEvent());
		else
			Skript.scheduler().callSync(() -> {
				Script script = trigger.getScript();
				return (script == null || script.valid()) && trigger.execute(new ScriptLoadEvent());
			});
		return true;
	}
}
