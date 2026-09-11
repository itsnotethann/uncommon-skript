package org.skriptlang.skript.platform;

public interface PlatformTask {
	int id();

	boolean isQueued();

	boolean isRunning();

	void cancel();
}
