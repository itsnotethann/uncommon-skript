package org.skriptlang.skript.platform.bukkit;

import org.bukkit.Bukkit;
import org.skriptlang.skript.platform.PlatformEnvironment;

public final class BukkitPlatformEnvironment implements PlatformEnvironment {

	@Override
	public String platformVersion() {
		return Bukkit.getVersion();
	}

	@Override
	public boolean isPrimaryThread() {
		return Bukkit.isPrimaryThread();
	}
}
