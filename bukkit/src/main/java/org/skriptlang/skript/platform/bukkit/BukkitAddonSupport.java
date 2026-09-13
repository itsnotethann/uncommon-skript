package org.skriptlang.skript.platform.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.skriptlang.skript.platform.AddonRegistry;
import org.skriptlang.skript.platform.Platform;
import org.skriptlang.skript.platform.PlatformProvider;

public final class BukkitAddonSupport {

	private BukkitAddonSupport() {}

	public static AddonRegistry install() {
		installServer();
		return new BukkitAddonRegistry(Bukkit.getPluginManager());
	}

	public static AddonRegistry install(Server server) {
		installServer(server);
		return new BukkitAddonRegistry(Bukkit.getPluginManager());
	}

	static void installServer() {
		if (Bukkit.getServer() == null)
			installServer(new SpiServer(Platform.provider()));
	}

	private static synchronized void installServer(Server server) {
		if (Bukkit.getServer() != null)
			return;
		PlatformProvider provider = Platform.provider();
		Bukkit.setServer(server);
		Bukkit.setServerDirectory(provider.environment().serverDirectory());
		Bukkit.setPrimaryThreadCheck(() -> provider.environment().isPrimaryThread());
		Bukkit.setTicker(tick -> provider.scheduler().sync(tick, 1, 1));
		Bukkit.getScheduler();
	}
}
