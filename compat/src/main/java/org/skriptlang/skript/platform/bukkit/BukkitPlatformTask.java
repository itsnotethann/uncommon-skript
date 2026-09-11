package org.skriptlang.skript.platform.bukkit;

import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.skriptlang.skript.platform.PlatformTask;

final class BukkitPlatformTask implements PlatformTask {

	private final BukkitScheduler scheduler;
	private final BukkitTask task;

	BukkitPlatformTask(BukkitScheduler scheduler, BukkitTask task) {
		this.scheduler = scheduler;
		this.task = task;
	}

	@Override
	public int id() {
		return task.getTaskId();
	}

	@Override
	public boolean isQueued() {
		return scheduler.isQueued(task.getTaskId());
	}

	@Override
	public boolean isRunning() {
		return scheduler.isCurrentlyRunning(task.getTaskId());
	}

	@Override
	public void cancel() {
		task.cancel();
	}
}
