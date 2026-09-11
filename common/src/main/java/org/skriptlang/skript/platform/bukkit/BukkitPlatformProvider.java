package org.skriptlang.skript.platform.bukkit;

import ch.njol.skript.Skript;
import org.bukkit.Bukkit;
import org.skriptlang.skript.platform.AddonRegistry;
import org.skriptlang.skript.platform.EventBus;
import org.skriptlang.skript.platform.LogSink;
import org.skriptlang.skript.platform.PlatformEnvironment;
import org.skriptlang.skript.platform.PlatformProvider;
import org.skriptlang.skript.platform.PlatformScheduler;

public final class BukkitPlatformProvider implements PlatformProvider {

	private volatile PlatformScheduler scheduler;
	private volatile EventBus eventBus;
	private volatile AddonRegistry addonRegistry;

	@Override
	public PlatformEnvironment environment() {
		return new BukkitPlatformEnvironment();
	}

	@Override
	public LogSink logSink() {
		return LoggerLogSink.platformDefault();
	}

	@Override
	public PlatformScheduler scheduler() {
		PlatformScheduler current = scheduler;
		if (current == null)
			scheduler = current = new BukkitPlatformScheduler(Bukkit.getScheduler(), Skript.getInstance());
		return current;
	}

	@Override
	public EventBus eventBus() {
		EventBus current = eventBus;
		if (current == null)
			eventBus = current = new BukkitEventBus(Bukkit.getPluginManager(), Skript.getInstance());
		return current;
	}

	@Override
	public AddonRegistry addonRegistry() {
		AddonRegistry current = addonRegistry;
		if (current == null)
			addonRegistry = current = new BukkitAddonRegistry(Bukkit.getPluginManager());
		return current;
	}
}
