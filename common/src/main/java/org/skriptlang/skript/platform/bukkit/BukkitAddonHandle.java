package org.skriptlang.skript.platform.bukkit;

import java.io.File;
import java.io.InputStream;

import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.platform.AddonHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BukkitAddonHandle implements AddonHandle {

	private final JavaPlugin plugin;

	public BukkitAddonHandle(JavaPlugin plugin) {
		this.plugin = plugin;
	}

	public JavaPlugin plugin() {
		return plugin;
	}

	@Override
	public String name() {
		return plugin.getName();
	}

	@Override
	public String version() {
		return plugin.getDescription().getVersion();
	}

	@Override
	public File dataFolder() {
		return plugin.getDataFolder();
	}

	@Override
	public File jarFile() {
		return plugin.getFile();
	}

	@Override
	public @Nullable InputStream resource(String path) {
		return plugin.getResource(path);
	}

	@Override
	public Class<?> source() {
		return plugin.getClass();
	}

	@Override
	public Logger logger() {
		return LoggerFactory.getLogger(plugin.getClass());
	}

	@Override
	public boolean isEnabled() {
		return plugin.isEnabled();
	}

	@Override
	public void setEnabled(boolean enabled) {
		plugin.setEnabled(enabled);
	}

	@Override
	public void onEnable() {
		plugin.onEnable();
	}

	@Override
	public void onDisable() {
		plugin.onDisable();
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof BukkitAddonHandle handle && handle.plugin == plugin;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(plugin);
	}
}
