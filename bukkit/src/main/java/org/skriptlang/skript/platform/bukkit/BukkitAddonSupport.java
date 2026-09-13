package org.skriptlang.skript.platform.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.skriptlang.skript.platform.AddonRegistry;
import org.skriptlang.skript.platform.Platform;
import org.skriptlang.skript.platform.PlatformProvider;

public final class BukkitAddonSupport {

	private BukkitAddonSupport() {}

	public static AddonRegistry install() {
		return install(new SpiServer(Platform.provider().environment()));
	}

	public static AddonRegistry install(Server server) {
		if (Bukkit.getServer() == null) {
			PlatformProvider provider = Platform.provider();
			Bukkit.setServer(server);
			Bukkit.setServerDirectory(provider.environment().serverDirectory());
			Bukkit.setPrimaryThreadCheck(() -> provider.environment().isPrimaryThread());
			Bukkit.setTicker(tick -> provider.scheduler().sync(tick, 1, 1));
			Bukkit.getScheduler();
		}
		return new BukkitAddonRegistry(Bukkit.getPluginManager());
	}
}
