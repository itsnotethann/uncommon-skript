package org.skriptlang.skript.testhost;

import org.skriptlang.skript.lang.event.Cancellable;
import org.skriptlang.skript.lang.event.PlatformEvent;

public class TestEvent implements PlatformEvent, Cancellable {

	private final String name;
	private volatile boolean cancelled;

	public TestEvent(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public static class Sub extends TestEvent {

		public Sub(String name) {
			super(name);
		}
	}
}
