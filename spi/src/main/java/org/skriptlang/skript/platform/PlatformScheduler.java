package org.skriptlang.skript.platform;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public interface PlatformScheduler {
	PlatformTask sync(Runnable task, long delayTicks, long periodTicks);

	PlatformTask async(Runnable task, long delayTicks, long periodTicks);

	<T> Future<T> callSync(Callable<T> task);

	boolean isPrimaryThread();
}
