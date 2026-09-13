package org.bukkit.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;

public class Task implements BukkitTask {
	private static final AtomicInteger COUNTER = new AtomicInteger();
	public boolean async;
	private final Plugin owner;
	public final Runnable runnable;
	public long delay;
	public @Nullable Long duration;
	public long ticksLeft;
	public int id;
	private boolean cancelled = false;

	public Task(Plugin owner, Runnable runnable, boolean async, long delay, @Nullable Long duration) {
		this.owner = owner;
		this.delay = delay;
		this.duration = duration;
		this.ticksLeft = delay;
		this.id = COUNTER.getAndIncrement();
		this.runnable = runnable;
		this.async = async;
	}

	public boolean isRepeating() {
		return duration != null;
	}

	@Override
	public int getTaskId() {
		return id;
	}

	@Override
	public @NotNull Plugin getOwner() {
		return owner;
	}

	@Override
	public boolean isSync() {
		return !async;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public void cancel() {
		Bukkit.getScheduler().cancelTask(getTaskId());
	}

}
