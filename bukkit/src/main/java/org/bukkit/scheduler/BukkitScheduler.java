package org.bukkit.scheduler;

import org.bukkit.plugin.Plugin;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

public interface BukkitScheduler {

	void tick();

	BukkitTask runTask(Plugin plugin, BukkitRunnable task);

	void runTask(Plugin plugin, Consumer<? super BukkitTask> task);

	BukkitTask runTask(Plugin plugin, Runnable task);

	BukkitTask runTaskLater(Plugin plugin, Runnable task, long delay);

	void runTaskLater(Plugin plugin, Consumer<? super BukkitTask> task, long delay);

	BukkitTask runTaskLater(Plugin plugin, BukkitRunnable task, long delay);

	BukkitTask runTaskTimer(Plugin plugin, Runnable task, long delay, long period);

	BukkitTask runTaskAsynchronously(Plugin plugin, Runnable runnable);

	BukkitTask runTaskLaterAsynchronously(Plugin plugin, Runnable runnable, long delay);

	int scheduleSyncDelayedTask(Plugin plugin, Runnable runnable, long delay);

	BukkitTask runTaskTimerAsynchronously(Plugin plugin, Runnable runnable, long delay, long duration);

	int scheduleSyncRepeatingTask(Plugin plugin, Runnable runnable, long delay, long duration);

	boolean isQueued(int taskID);

	boolean isCurrentlyRunning(int taskID);

	void cancelTask(int taskID);

	<T> Future<T> callSyncMethod(Plugin plugin, Callable<T> task);
}