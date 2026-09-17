package org.skriptlang.skript.platform.bukkit;

import ch.njol.skript.LegacyAddonFactory;
import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.skriptlang.skript.lang.event.PlatformEvent;

public final class BukkitAddons {

	private BukkitAddons() {}

	public static SkriptAddon registerAddon(JavaPlugin plugin) {
		org.skriptlang.skript.addon.SkriptAddon addon =
			Skript.instance().registerAddon(plugin.getClass(), plugin.getName());
		return LegacyAddonFactory.create(plugin.getName(), plugin.getDescription().getVersion(),
			plugin.getClass(), plugin.getDataFolder(), plugin.getFile(), addon);
	}

	public static String eventName(PlatformEvent event) {
		return event.getClass().getSimpleName();
	}

	public static boolean isAsynchronous(PlatformEvent event) {
		return false;
	}

	public static void callEvent(PluginManager pluginManager, PlatformEvent event) {
		if (event instanceof Event bukkitEvent) {
			pluginManager.callEvent(bukkitEvent);
			return;
		}
		Skript.eventBus().fire(event);
	}
}
