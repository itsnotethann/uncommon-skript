package org.skriptlang.skript.platform.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.skriptlang.skript.platform.EventBus;
import org.skriptlang.skript.platform.EventChannel;

public final class BukkitEventBusService implements EventBus {

	private volatile @Nullable EventBus delegate;

	private EventBus delegate() {
		EventBus current = delegate;
		if (current == null) {
			synchronized (this) {
				current = delegate;
				if (current == null) {
					BukkitAddonSupport.installServer();
					delegate = current = new BukkitEventBus(Bukkit.getPluginManager(), SkriptPluginOwner.get());
				}
			}
		}
		return current;
	}

	@Override
	public @Nullable EventChannel channelFor(Class<? extends PlatformEvent> eventClass) {
		if (!Event.class.isAssignableFrom(eventClass))
			return null;
		return delegate().channelFor(eventClass);
	}

	@Override
	public void fire(PlatformEvent event) {
		if (event instanceof Event)
			delegate().fire(event);
	}
}
