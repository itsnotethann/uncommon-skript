package org.bukkit.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.logging.Logger;

public interface Plugin extends TabExecutor {
	File getDataFolder();
	PluginDescriptionFile getDescription();
	void setEnabled(boolean enabled);
	boolean isEnabled();
	String getName();
	Logger getLogger();
	@NotNull FileConfiguration getConfig();
	void reloadConfig();
	void saveConfig();
	void saveDefaultConfig();
	void saveResource(@NotNull String resourcePath, boolean replace);

	default Server getServer() {
		return Bukkit.getServer();
	}

	void onEnable();
	void onDisable();
}
