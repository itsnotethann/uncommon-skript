package org.skriptlang.skript.platform;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.event.PlatformEvent;

public interface EventBus {
	@Nullable EventChannel channelFor(Class<? extends PlatformEvent> eventClass);

	void fire(PlatformEvent event);
}
