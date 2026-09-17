package org.skriptlang.skript.testhost.bench;

import org.skriptlang.skript.lang.event.PlatformEvent;

public final class BenchEvent implements PlatformEvent {

	private final String name;

	public BenchEvent(String name) {
		this.name = name;
	}

	public String name() {
		return name;
	}
}
