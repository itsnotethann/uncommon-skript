package ch.njol.skript.events.bukkit;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.Config;
import com.google.common.base.Preconditions;
import org.skriptlang.skript.lang.event.PlatformEvent;

import java.util.List;

/**
 * This event has no guarantee of being on the main thread.
 * Please do not use bukkit api before checking {@link Bukkit#isPrimaryThread()}
 * @deprecated Use {@link ScriptLoader.ScriptPreInitEvent}.
 */
@Deprecated(since = "2.10.0", forRemoval = true)
public class PreScriptLoadEvent implements PlatformEvent {

	private final List<Config> scripts;

	public PreScriptLoadEvent(List<Config> scripts) {
		super();
		Preconditions.checkNotNull(scripts);
		this.scripts = scripts;
	}

	public List<Config> getScripts() {
		return scripts;
	}

}