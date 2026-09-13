package org.bukkit;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.util.Collection;
import java.util.UUID;
import java.util.List;
import java.util.Locale;
import org.jetbrains.annotations.Nullable;

public interface Server {

	default PluginManager getPluginManager() {
		return Bukkit.getPluginManager();
	}

	ConsoleCommandSender getConsoleSender();

	Collection<Player> getOnlinePlayers();

	boolean getOnlineMode();

	String getVersion();

	String getName();

	Player getPlayer(UUID uuid);

	default @Nullable Player getPlayer(String name) {
		Player exact = getPlayerExact(name);
		if (exact != null)
			return exact;
		String lower = name.toLowerCase(Locale.ROOT);
		Player found = null;
		int delta = Integer.MAX_VALUE;
		for (Player player : getOnlinePlayers()) {
			String candidate = player.getName();
			if (candidate != null && candidate.toLowerCase(Locale.ROOT).startsWith(lower)) {
				int length = candidate.length() - lower.length();
				if (length < delta) {
					found = player;
					delta = length;
				}
			}
		}
		return found;
	}

	default @Nullable Player getPlayerExact(String name) {
		for (Player player : getOnlinePlayers()) {
			if (name.equalsIgnoreCase(player.getName()))
				return player;
		}
		return null;
	}

	List<World> getWorlds();

	default @Nullable World getWorld(String name) {
		for (World world : getWorlds()) {
			if (world.getName().equalsIgnoreCase(name))
				return world;
		}
		return null;
	}

	default @Nullable World getWorld(UUID uid) {
		for (World world : getWorlds()) {
			if (world.getUID().equals(uid))
				return world;
		}
		return null;
	}

	File getServerDirectory();

	void setServerDirectory(File serverDirectory);

}
