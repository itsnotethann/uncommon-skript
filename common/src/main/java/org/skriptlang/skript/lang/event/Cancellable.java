package org.skriptlang.skript.lang.event;

public interface Cancellable {
	boolean isCancelled();

	void setCancelled(boolean cancelled);
}
