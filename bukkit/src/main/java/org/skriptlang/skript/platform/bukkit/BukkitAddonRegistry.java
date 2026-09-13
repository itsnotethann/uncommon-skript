package org.skriptlang.skript.platform.bukkit;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.platform.AddonHandle;
import org.skriptlang.skript.platform.AddonRegistry;

public final class BukkitAddonRegistry implements AddonRegistry {

	private final PluginManager pluginManager;
	private final Map<Plugin, AddonHandle> handles = new ConcurrentHashMap<>();

	public BukkitAddonRegistry(PluginManager pluginManager) {
		this.pluginManager = pluginManager;
		SkriptPluginOwner owner = SkriptPluginOwner.get();
		owner.setEnabled(true);
		pluginManager.registerPlugin(owner);
	}

	@Override
	public @Nullable AddonHandle load(File jar) {
		JavaPlugin plugin = pluginManager.loadPlugin(jar);
		return plugin == null ? null : handleFor(plugin);
	}

	@Override
	public List<AddonHandle> addons() {
		return pluginManager.getPlugins().stream()
			.filter(JavaPlugin.class::isInstance)
			.filter(plugin -> !(plugin instanceof SkriptPluginOwner))
			.map(JavaPlugin.class::cast)
			.map(this::handleFor)
			.toList();
	}

	@Override
	public @Nullable AddonHandle providingAddon(Class<?> from) {
		JavaPlugin plugin = JavaPlugin.getProvidingPlugin(from);
		return plugin == null ? null : handleFor(plugin);
	}

	private AddonHandle handleFor(JavaPlugin plugin) {
		return handles.computeIfAbsent(plugin, key -> new BukkitAddonHandle((JavaPlugin) key));
	}
}
