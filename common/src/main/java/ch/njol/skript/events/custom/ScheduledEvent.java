package ch.njol.skript.events.custom;

import org.bukkit.event.Event;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.bukkit.event.HandlerList;

/**
 * @author Peter Güttinger
 */
public class ScheduledEvent extends Event {

	// Bukkit stuff
	private final static HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
