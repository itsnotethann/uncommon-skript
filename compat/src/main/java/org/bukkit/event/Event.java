package org.bukkit.event;

import org.jetbrains.annotations.NotNull;
import org.skriptlang.skript.lang.event.PlatformEvent;

public abstract class Event implements PlatformEvent {

	private String name;
	private final boolean async;

	/**
	 * The default constructor is defined for cleaner code. This constructor
	 * assumes the event is synchronous.
	 */
	public Event() {
		this(false);
	}

	/**
	 * This constructor is used to explicitly declare an event as synchronous
	 * or asynchronous.
	 *
	 * @param isAsync true indicates the event will fire asynchronously, false
	 *     by default from default constructor
	 */
	public Event(boolean isAsync) {
		this.async = isAsync;
	}

	public boolean callEvent() {
		org.bukkit.Bukkit.getPluginManager().callEvent(this);
		if (this instanceof Cancellable) {
			return !((Cancellable) this).isCancelled();
		} else {
			return true;
		}
	}

	/**
     * Convenience method for providing a user-friendly identifier. By
     * default, it is the event's class's {@linkplain Class#getSimpleName()
     * simple name}.
     *
     * @return name of this event
     */
    @NotNull
    public String getEventName() {
        if (name == null) {
            name = getClass().getSimpleName();
        }
        return name;
    }

	public final boolean isAsynchronous() {
		return async;
	}

	public abstract HandlerList getHandlers();

	public enum Result {
		ALLOW,
		DEFAULT,
		DENY
	}
}
