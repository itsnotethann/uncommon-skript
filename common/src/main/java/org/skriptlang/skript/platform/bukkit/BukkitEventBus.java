package org.skriptlang.skript.platform.bukkit;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.skriptlang.skript.platform.EventBus;
import org.skriptlang.skript.platform.EventChannel;

public final class BukkitEventBus implements EventBus {

	private final PluginManager pluginManager;
	private final Plugin owner;
	private final Map<Class<?>, EventChannel> channelsByClass = new ConcurrentHashMap<>();
	private final Map<HandlerList, BukkitEventChannel> channelsByHandlerList = new IdentityHashMap<>();

	public BukkitEventBus(PluginManager pluginManager, Plugin owner) {
		this.pluginManager = pluginManager;
		this.owner = owner;
	}

	@Override
	public EventChannel channelFor(Class<? extends PlatformEvent> eventClass) {
		return channelsByClass.computeIfAbsent(eventClass, key -> {
			if (!Event.class.isAssignableFrom(key))
				throw new IllegalArgumentException("Not a Bukkit-shim event: " + key);
			Class<? extends Event> bukkitClass = key.asSubclass(Event.class);
			HandlerList handlerList;
			try {
				handlerList = SimplePluginManager.getHandlerList(bukkitClass);
			} catch (ReflectiveOperationException exception) {
				throw new IllegalArgumentException("No getHandlerList() on " + bukkitClass, exception);
			}
			synchronized (channelsByHandlerList) {
				return channelsByHandlerList.computeIfAbsent(handlerList,
					list -> new BukkitEventChannel(list, owner));
			}
		});
	}

	@Override
	public void fire(PlatformEvent event) {
		if (!(event instanceof Event bukkitEvent))
			throw new IllegalArgumentException("Not a Bukkit-shim event: " + event);
		pluginManager.callEvent(bukkitEvent);
	}
}
