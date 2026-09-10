package org.skriptlang.skript.platform.bukkit;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;
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
	public @Nullable EventChannel channelFor(Class<? extends PlatformEvent> eventClass) {
		EventChannel cached = channelsByClass.get(eventClass);
		if (cached != null)
			return cached;
		if (!Event.class.isAssignableFrom(eventClass))
			return null;
		HandlerList handlerList;
		try {
			handlerList = SimplePluginManager.getHandlerList(eventClass.asSubclass(Event.class));
		} catch (ReflectiveOperationException exception) {
			return null;
		}
		if (handlerList == null)
			return null;
		BukkitEventChannel channel;
		synchronized (channelsByHandlerList) {
			channel = channelsByHandlerList.computeIfAbsent(handlerList,
				list -> new BukkitEventChannel(list, owner));
		}
		EventChannel raced = channelsByClass.putIfAbsent(eventClass, channel);
		return raced != null ? raced : channel;
	}

	@Override
	public void fire(PlatformEvent event) {
		if (!(event instanceof Event bukkitEvent))
			throw new IllegalArgumentException("Not a Bukkit-shim event: " + event);
		pluginManager.callEvent(bukkitEvent);
	}
}
