package org.skriptlang.skript.platform;

import org.skriptlang.skript.lang.event.PlatformEvent;

public interface EventListener {
	void execute(PlatformEvent event);
}
