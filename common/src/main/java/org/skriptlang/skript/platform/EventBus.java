package org.skriptlang.skript.platform;

import org.skriptlang.skript.lang.event.PlatformEvent;

public interface EventBus {
	EventChannel channelFor(Class<? extends PlatformEvent> eventClass);

	void fire(PlatformEvent event);
}
