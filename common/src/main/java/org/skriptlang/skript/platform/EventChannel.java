package org.skriptlang.skript.platform;

import org.skriptlang.skript.lang.event.EventPriority;

public interface EventChannel {
	Registration register(EventPriority priority, EventListener listener);
}
