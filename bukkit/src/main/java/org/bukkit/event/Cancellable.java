package org.bukkit.event;

public interface Cancellable extends org.skriptlang.skript.lang.event.Cancellable {
	public void setCancelled(boolean cancelled);
	public boolean isCancelled();
}
