package org.skriptlang.skript.domain;

public interface DomainScheduler {
	void scheduleNextTick(Runnable task);
}
