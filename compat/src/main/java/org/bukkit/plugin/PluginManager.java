package org.bukkit.plugin;

import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

public interface PluginManager {
	List<Plugin> getPlugins();

	void registerEvent(
		Class<? extends Event> event,
		Listener listener,
		EventPriority priority,
		EventExecutor executor,
		Plugin plugin
	);

	void registerEvent(
		Class<? extends Event> event,
		Listener listener,
		EventPriority priority,
		EventExecutor executor,
		Plugin plugin,
		boolean ignoreCancelled
	);

	void registerEvents(Plugin plugin, Listener listener);

	void callEvent(Event event);

	JavaPlugin loadPlugin(File file);

	default void disablePlugin(Plugin plugin) {
		plugin.onDisable();
		plugin.setEnabled(false);
	}

	default Plugin getPlugin(String name) {
		for (Plugin plugin : getPlugins()) {
			if (plugin.getName().equalsIgnoreCase(name)) return plugin;
		}
		return null;
	}

	default boolean isPluginEnabled(String name) {
		Plugin plugin = getPlugin(name);
		if (plugin == null) return false;
		return plugin.isEnabled();
	}
}
