package org.skriptlang.skript.platform.bukkit;

import java.io.File;

import org.bukkit.Bukkit;
import org.slf4j.Logger;
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

	@Override
	public File serverDirectory() {
		return Bukkit.getServerDirectory();
	}

	@Override
	public Logger logger() {
		return Bukkit.getBetterLogger();
	}
}
