package org.skriptlang.skript.platform;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.event.PlatformEvent;

public final class CompositeEventBus implements EventBus {

	private final List<EventBus> buses;

	private CompositeEventBus(List<EventBus> buses) {
		this.buses = buses;
	}

	public static EventBus withExtensions(EventBus primary) {
		List<EventBus> buses = new ArrayList<>();
		buses.add(primary);
		ServiceLoader.load(EventBus.class, Platform.class.getClassLoader()).forEach(buses::add);
		return buses.size() == 1 ? primary : new CompositeEventBus(List.copyOf(buses));
	}

	@Override
	public @Nullable EventChannel channelFor(Class<? extends PlatformEvent> eventClass) {
		for (EventBus bus : buses) {
			EventChannel channel = bus.channelFor(eventClass);
			if (channel != null)
				return channel;
		}
		return null;
	}

	@Override
	public void fire(PlatformEvent event) {
		for (EventBus bus : buses)
			bus.fire(event);
	}
}
