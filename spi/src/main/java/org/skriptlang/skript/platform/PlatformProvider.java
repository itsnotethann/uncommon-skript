package org.skriptlang.skript.platform;

public interface PlatformProvider {
	PlatformEnvironment environment();

	LogSink logSink();

	PlatformScheduler scheduler();

	EventBus eventBus();

	AddonRegistry addonRegistry();
}
