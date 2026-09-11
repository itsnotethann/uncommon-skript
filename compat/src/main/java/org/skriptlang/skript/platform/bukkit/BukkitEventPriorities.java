package org.skriptlang.skript.platform.bukkit;

import org.skriptlang.skript.lang.event.EventPriority;

public final class BukkitEventPriorities {

	private BukkitEventPriorities() {
	}

	public static org.bukkit.event.EventPriority toBukkit(EventPriority priority) {
		return org.bukkit.event.EventPriority.valueOf(priority.name());
	}

	public static EventPriority toPlatform(org.bukkit.event.EventPriority priority) {
		return EventPriority.valueOf(priority.name());
	}
}
