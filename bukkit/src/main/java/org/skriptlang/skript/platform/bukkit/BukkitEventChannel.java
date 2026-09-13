package org.skriptlang.skript.platform.bukkit;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.RegisteredListener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.skriptlang.skript.lang.event.EventPriority;
import org.skriptlang.skript.platform.EventChannel;
import org.skriptlang.skript.platform.EventListener;
import org.skriptlang.skript.platform.Registration;

final class BukkitEventChannel implements EventChannel {

	private final HandlerList handlerList;
	private final Plugin owner;

	BukkitEventChannel(HandlerList handlerList, Plugin owner) {
		this.handlerList = handlerList;
		this.owner = owner;
	}

	@Override
	public Registration register(EventPriority priority, EventListener listener) {
		Listener token = new Listener() {
		};
		EventExecutor executor = (ignoredListener, event) -> listener.execute((org.bukkit.event.Event) event);
		handlerList.register(new RegisteredListener(owner, executor,
			BukkitEventPriorities.toBukkit(priority), false, token));
		return new BukkitRegistration(handlerList, token);
	}
}
