package org.skriptlang.skript.platform.bukkit;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.skriptlang.skript.platform.PlatformScheduler;
import org.skriptlang.skript.platform.PlatformTask;

public final class BukkitPlatformScheduler implements PlatformScheduler {

	private final BukkitScheduler scheduler;
	private final Plugin owner;

	public BukkitPlatformScheduler(BukkitScheduler scheduler, Plugin owner) {
		this.scheduler = scheduler;
		this.owner = owner;
	}

	@Override
	public PlatformTask sync(Runnable task, long delayTicks, long periodTicks) {
		var bukkitTask = periodTicks < 0
			? scheduler.runTaskLater(owner, task, delayTicks)
			: scheduler.runTaskTimer(owner, task, delayTicks, periodTicks);
		return new BukkitPlatformTask(scheduler, bukkitTask);
	}

	@Override
	public PlatformTask async(Runnable task, long delayTicks, long periodTicks) {
		var bukkitTask = periodTicks < 0
			? scheduler.runTaskLaterAsynchronously(owner, task, delayTicks)
			: scheduler.runTaskTimerAsynchronously(owner, task, delayTicks, periodTicks);
		return new BukkitPlatformTask(scheduler, bukkitTask);
	}

	@Override
	public <T> Future<T> callSync(Callable<T> task) {
		return scheduler.callSyncMethod(owner, task);
	}

	@Override
	public boolean isPrimaryThread() {
		return Bukkit.isPrimaryThread();
	}
}
