package org.skriptlang.skript.platform.bukkit;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.skriptlang.skript.platform.Registration;

final class BukkitRegistration implements Registration {

	private final HandlerList handlerList;
	private final Listener token;

	BukkitRegistration(HandlerList handlerList, Listener token) {
		this.handlerList = handlerList;
		this.token = token;
	}

	@Override
	public void unregister() {
		handlerList.unregister(token);
	}
}
