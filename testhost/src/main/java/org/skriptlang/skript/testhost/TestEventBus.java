package org.skriptlang.skript.testhost;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.event.EventPriority;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.skriptlang.skript.platform.EventBus;
import org.skriptlang.skript.platform.EventChannel;
import org.skriptlang.skript.platform.EventListener;
import org.skriptlang.skript.platform.Registration;

public final class TestEventBus implements EventBus {

	private final Map<Class<?>, Channel> channels = new ConcurrentHashMap<>();

	@Override
	public @Nullable EventChannel channelFor(Class<? extends PlatformEvent> eventClass) {
		if (!TestEvent.class.isAssignableFrom(eventClass))
			return null;
		return channels.computeIfAbsent(eventClass, Channel::new);
	}

	@Override
	public void fire(PlatformEvent event) {
		Channel channel = channels.get(event.getClass());
		if (channel == null)
			return;
		for (EventPriority priority : EventPriority.values())
			channel.call(priority, event);
	}

	private static final class Channel implements EventChannel {

		private final Map<EventPriority, List<EventListener>> listeners = new EnumMap<>(EventPriority.class);

		Channel(Class<?> eventClass) {
			for (EventPriority priority : EventPriority.values())
				listeners.put(priority, new CopyOnWriteArrayList<>());
		}

		void call(EventPriority priority, PlatformEvent event) {
			for (EventListener listener : listeners.get(priority))
				listener.execute(event);
		}

		@Override
		public Registration register(EventPriority priority, EventListener listener) {
			List<EventListener> list = listeners.get(priority);
			list.add(listener);
			return () -> list.remove(listener);
		}
	}
}
