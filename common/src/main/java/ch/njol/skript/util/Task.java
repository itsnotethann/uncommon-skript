/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.util.Closeable;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.platform.PlatformScheduler;
import org.skriptlang.skript.platform.PlatformTask;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Peter Güttinger
 */
public abstract class Task implements Runnable, Closeable {

	private final PlatformScheduler scheduler;
	private final boolean async;
	private long period = -1;

	@Nullable
	private PlatformTask task;

	public Task(final long delay, final long period) {
		this(delay, period, false);
	}

	public Task(final long delay, final long period, final boolean async) {
		this.scheduler = Skript.scheduler();
		this.period = period;
		this.async = async;
		schedule(delay);
	}

	public Task(final long delay) {
		this(delay, false);
	}

	public Task(final long delay, final boolean async) {
		this.scheduler = Skript.scheduler();
		this.async = async;
		schedule(delay);
	}

	/**
	 * Only call this if the task is not alive.
	 *
	 * @param delay
	 */
	private void schedule(final long delay) {
		assert !isAlive();
		if (!Skript.getInstance().isEnabled())
			return;

		task = async ? scheduler.async(this, delay, period) : scheduler.sync(this, delay, period);
		assert task != null;
	}

	/**
	 * @return Whether this task is still running, i.e. whether it will run later or is currently running.
	 */
	public final boolean isAlive() {
		final PlatformTask t = task;
		if (t == null)
			return false;
		return t.isQueued() || t.isRunning();
	}

	/**
	 * Cancels this task.
	 */
	public final void cancel() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}
	
	@Override
	public void close() {
		cancel();
	}
	
	/**
	 * Re-schedules the task to run next after the given delay. If this task was repeating it will continue so using the same period as before.
	 * 
	 * @param delay
	 */
	public void setNextExecution(final long delay) {
		assert delay >= 0;
		cancel();
		schedule(delay);
	}
	
	/**
	 * Sets the period of this task. This will re-schedule the task to be run next after the given period if the task is still running.
	 * 
	 * @param period Period in ticks or -1 to cancel the task and make it non-repeating
	 */
	public void setPeriod(final long period) {
		assert period == -1 || period > 0;
		if (period == this.period)
			return;
		this.period = period;
		if (isAlive()) {
			cancel();
			if (period != -1)
				schedule(period);
		}
	}
	
	/**
	 * Equivalent to <tt>{@link #callSync(Callable, PlatformScheduler) callSync}(c, {@link Skript#scheduler()})</tt>
	 */
	@Nullable
	public static <T> T callSync(final Callable<T> c) {
		return callSync(c, Skript.scheduler());
	}

	@Nullable
	public static <T> T callSync(final Callable<T> c, final PlatformScheduler scheduler) {
		if (Skript.ENVIRONMENT.isPrimaryThread()) {
			try {
				return c.call();
			} catch (final Exception e) {
				Skript.exception(e);
			}
		}
		return await(scheduler.callSync(c));
	}

	@Nullable
	private static <T> T await(final Future<T> f) {
		try {
			while (true) {
				try {
					return f.get();
				} catch (final InterruptedException e) {}
			}
		} catch (final ExecutionException e) {
			Skript.exception(e);
		} catch (final CancellationException e) {}
		return null;
	}
	
	/**
	 * Calls a method on the server's main thread.
	 * <p>
	 * Hint: Use a Callable&lt;Void&gt; to make a task which blocks your current thread until it is completed.
	 * 
	 * @param c The method
	 * @param p The plugin that owns the task. Must be enabled.
	 * @return What the method returned or null if it threw an error or was stopped (usually due to the server shutting down)
	 */
	
}
