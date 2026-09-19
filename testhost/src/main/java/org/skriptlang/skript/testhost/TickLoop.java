package org.skriptlang.skript.testhost;

import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.skriptlang.skript.platform.PlatformScheduler;
import org.skriptlang.skript.platform.PlatformTask;

public final class TickLoop implements PlatformScheduler {

	public static final long TICK_MILLIS = 50;

	private final PriorityQueue<Scheduled> queue = new PriorityQueue<>();
	private final AtomicInteger ids = new AtomicInteger();
	private final AtomicLong sequence = new AtomicLong();
	private final ExecutorService pool = Executors.newCachedThreadPool(runnable -> {
		Thread thread = new Thread(runnable, "TestHost-Async");
		thread.setDaemon(true);
		return thread;
	});
	private final Thread thread;
	private volatile long tick;
	private volatile boolean running = true;

	public TickLoop() {
		thread = new Thread(this::loop, "TestHost-Main");
		thread.setDaemon(true);
	}

	public void start() {
		thread.start();
	}

	public long tick() {
		return tick;
	}

	private void loop() {
		long next = System.nanoTime();
		while (running) {
			tick++;
			while (true) {
				Scheduled due;
				synchronized (queue) {
					due = queue.peek();
					if (due == null || due.dueTick > tick)
						break;
					queue.poll();
				}
				due.run();
			}
			next += TICK_MILLIS * 1_000_000;
			long sleep = next - System.nanoTime();
			if (sleep > 0) {
				try {
					Thread.sleep(sleep / 1_000_000, (int) (sleep % 1_000_000));
				} catch (InterruptedException e) {
					return;
				}
			} else {
				next = System.nanoTime();
			}
		}
	}

	public void drainPending(int maxTicks) {
		for (int i = 0; i < maxTicks; i++) {
			boolean pending = false;
			synchronized (queue) {
				for (Scheduled scheduled : queue) {
					if (scheduled.periodTicks < 0) {
						pending = true;
						break;
					}
				}
			}
			if (!pending)
				return;
			tick++;
			while (true) {
				Scheduled due;
				synchronized (queue) {
					due = queue.peek();
					if (due == null || due.dueTick > tick)
						break;
					queue.poll();
				}
				due.run();
			}
		}
	}

	private Scheduled enqueue(Runnable task, long delayTicks, long periodTicks) {
		Scheduled scheduled = new Scheduled(ids.incrementAndGet(), task, periodTicks);
		schedule(scheduled, delayTicks);
		return scheduled;
	}

	private void schedule(Scheduled scheduled, long delayTicks) {
		synchronized (queue) {
			scheduled.dueTick = tick + Math.max(1, delayTicks);
			scheduled.order = sequence.incrementAndGet();
			queue.add(scheduled);
		}
	}

	@Override
	public PlatformTask sync(Runnable task, long delayTicks, long periodTicks) {
		return enqueue(task, delayTicks, periodTicks);
	}

	@Override
	public PlatformTask async(Runnable task, long delayTicks, long periodTicks) {
		return enqueue(() -> pool.submit(task), delayTicks, periodTicks);
	}

	@Override
	public <T> Future<T> callSync(Callable<T> task) {
		CompletableFuture<T> future = new CompletableFuture<>();
		if (isPrimaryThread()) {
			complete(future, task);
			return future;
		}
		enqueue(() -> complete(future, task), 1, -1);
		return future;
	}

	private static <T> void complete(CompletableFuture<T> future, Callable<T> task) {
		try {
			future.complete(task.call());
		} catch (Throwable throwable) {
			future.completeExceptionally(throwable);
		}
	}

	@Override
	public boolean isPrimaryThread() {
		return Thread.currentThread() == thread;
	}

	public void runAsync(Runnable task) {
		pool.submit(task);
	}

	public void shutdown() {
		running = false;
		pool.shutdownNow();
	}

	private final class Scheduled implements PlatformTask, Comparable<Scheduled> {

		private final int id;
		private final Runnable task;
		private final long periodTicks;
		private long dueTick;
		private long order;
		private volatile boolean cancelled;
		private volatile boolean executing;

		Scheduled(int id, Runnable task, long periodTicks) {
			this.id = id;
			this.task = task;
			this.periodTicks = periodTicks;
		}

		void run() {
			if (cancelled)
				return;
			executing = true;
			try {
				task.run();
			} catch (Throwable throwable) {
				throwable.printStackTrace();
			} finally {
				executing = false;
			}
			if (periodTicks >= 0 && !cancelled)
				schedule(this, periodTicks);
		}

		@Override
		public int compareTo(Scheduled other) {
			int byTick = Long.compare(dueTick, other.dueTick);
			return byTick != 0 ? byTick : Long.compare(order, other.order);
		}

		@Override
		public int id() {
			return id;
		}

		@Override
		public boolean isQueued() {
			if (cancelled)
				return false;
			synchronized (queue) {
				return queue.contains(this);
			}
		}

		@Override
		public boolean isRunning() {
			return executing;
		}

		@Override
		public void cancel() {
			cancelled = true;
			synchronized (queue) {
				queue.remove(this);
			}
		}
	}
}
