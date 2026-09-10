/**
 *   This file is part of Skript.
 *
 *  Skript is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Skript is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Skript.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright Peter Güttinger, SkriptLang team and contributors
 */
package ch.njol.skript;

import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.util.Task;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.bukkit.event.Event;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.skriptlang.skript.lang.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.skriptlang.skript.lang.event.Cancellable;
import org.eclipse.jdt.annotation.Nullable;
import org.skriptlang.skript.platform.Registration;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public final class SkriptEventHandler {

	private SkriptEventHandler() { }

	/**
	 * One {@link Registration} per (Event class, priority) pair currently listened to.
	 */
	private static final Map<Class<? extends PlatformEvent>, Map<EventPriority, Registration>> registrations = new HashMap<>();

	/**
	 * A Multimap tracking what Triggers are paired with what Events.
	 * Each Event effectively maps to an ArrayList of Triggers.
	 */
	private static final Multimap<Class<? extends PlatformEvent>, Trigger> triggers = ArrayListMultimap.create();

	/**
	 * A utility method to get all Triggers registered under the provided Event class.
	 * @param event The event to find pairs from.
	 * @return A List containing all Triggers registered under the provided Event class.
	 */
	private static List<Trigger> getTriggers(Class<? extends PlatformEvent> event) {
		HandlerList eventHandlerList = getHandlerList(event);
		assert eventHandlerList != null; // It had one at some point so this should remain true
		return triggers.asMap().entrySet().stream()
			.filter(entry -> entry.getKey().isAssignableFrom(event) && getHandlerList(entry.getKey()) == eventHandlerList)
			.flatMap(entry -> entry.getValue().stream())
			.collect(Collectors.toList()); // forces evaluation now and prevents us from having to call getTriggers again if very high logging is enabled
	}

	/**
	 * This method is used for validating that the provided Event may be handled by Skript.
	 * If validation is successful, all Triggers associated with the provided Event are executed.
	 * A Trigger will only be executed if its priority matches the provided EventPriority.
	 * @param event The Event to check.
	 * @param priority The priority of the Event.
	 */
	private static void check(PlatformEvent event, EventPriority priority) {
		// get all triggers for this event, return if none
		List<Trigger> triggers = getTriggers(event.getClass());
		if (triggers.isEmpty())
			return;

		// Check if this event should be treated as cancelled
		boolean isCancelled = isCancelled(event);

		// This logs events even if there isn't a trigger that's going to run at that priority.
		// However, there should only be a priority listener IF there's a trigger at that priority.
		// So the time will be logged even if no triggers pass check(), which is still useful information.
		logEventStart(event, priority);

		for (Trigger trigger : triggers) {
			SkriptEvent triggerEvent = trigger.getEvent();

			// check if the trigger is at the right priority
			if (triggerEvent.getEventPriority() != priority)
				continue;

			// check if the cancel state of the event is correct
			if (!triggerEvent.getListeningBehavior().matches(isCancelled))
				continue;

			// execute the trigger
			execute(trigger, event);
		}

		logEventEnd();
	}

	/**
	 * Helper method to check if we should treat the provided Event as cancelled.
	 *
	 * @param event The event to check.
	 * @return Whether the event should be treated as cancelled.
	 */
	private static boolean isCancelled(PlatformEvent event) {
		return event instanceof Cancellable &&
			(((Cancellable) event).isCancelled()) &&
			// TODO: listenCancelled is deprecated and should be removed in 2.10
			!listenCancelled.contains(event.getClass());
	}

	/**
	 * Executes the provided Trigger with the provided Event as context.
	 *
	 * @param trigger The Trigger to execute.
	 * @param event The Event to execute the Trigger with.
	 */
	private static void execute(Trigger trigger, PlatformEvent event) {
		// these methods need to be run on whatever thread the trigger is
		Runnable execute = () -> {
			logTriggerStart(trigger);
			trigger.execute(event);
			logTriggerEnd(trigger);
		};

		if (trigger.getEvent().canExecuteAsynchronously()) {
			if (trigger.getEvent().check(event)) {
				execute.run();
			}
		} else { // Ensure main thread
			Task.callSync(() -> {
				if (trigger.getEvent().check(event))
					execute.run();
				return null; // we don't care about a return value
			});
		}
	}


	private static long startEvent;

	/**
	 * Logs that the provided Event has started.
	 * Requires {@link Skript#logVeryHigh()} to be true to log anything.
	 * @param event The Event that started.
	 */
	public static void logEventStart(PlatformEvent event) {
		logEventStart(event, null);
	}

	/**
	 * Logs that the provided Event has started with a priority.
	 * Requires {@link Skript#logVeryHigh()} to be true to log anything.
	 * @param event The Event that started.
	 * @param priority The priority of the Event.
	 */
	public static void logEventStart(PlatformEvent event, @Nullable EventPriority priority) {
		startEvent = System.nanoTime();
		if (!Skript.logVeryHigh())
			return;
		Skript.info("");

		String message = "== " + event.getClass().getName();

		if (priority != null)
			message += " with priority " + priority;

		if (event instanceof Cancellable && ((Cancellable) event).isCancelled())
			message += " (cancelled)";

		Skript.info(message + " ==");
	}

	/**
	 * Logs that the last logged Event start has ended.
	 * Includes the number of milliseconds execution took.
	 * Requires {@link Skript#logVeryHigh()} to be true to log anything.
	 */
	public static void logEventEnd() {
		if (!Skript.logVeryHigh())
			return;
		Skript.info("== took " + 1. * (System.nanoTime() - startEvent) / 1000000. + " milliseconds ==");
	}

	private static long startTrigger;

	/**
	 * Logs that the provided Trigger has begun execution.
	 * Requires {@link Skript#logVeryHigh()} to be true.
	 * @param trigger The Trigger that execution has begun for.
	 */
	public static void logTriggerStart(Trigger trigger) {
		startTrigger = System.nanoTime();
		if (!Skript.logVeryHigh())
			return;
		Skript.info("# " + trigger.getName());
	}

	/**
	 * Logs that the last logged Trigger execution has ended.
	 * Includes the number of milliseconds execution took.
	 * Requires {@link Skript#logVeryHigh()} to be true to log anything.
	 */
	public static void logTriggerEnd(Trigger t) {
		if (!Skript.logVeryHigh())
			return;
		Skript.info("# " + t.getName() + " took " + 1. * (System.nanoTime() - startTrigger) / 1000000. + " milliseconds");
	}

	/**
	 * @deprecated This method no longer does anything as self registered Triggers
	 * 	are unloaded when the {@link ch.njol.skript.lang.SkriptEvent} is unloaded (no need to keep tracking them here).
	 */
	@Deprecated
	public static void addSelfRegisteringTrigger(Trigger t) { }

	/**
	 * A utility method that calls {@link #registerBukkitEvent(Trigger, Class)} for each Event class provided.
	 * For specific details of the process, see the javadoc of that method.
	 * @param trigger The Trigger to run when the Event occurs.
	 * @param events The Event to listen for.
	 * @see #registerBukkitEvent(Trigger, Class)
	 * @see #unregisterBukkitEvents(Trigger)
	 */
	public static void registerBukkitEvents(Trigger trigger, Class<? extends PlatformEvent>[] events) {
		for (Class<? extends PlatformEvent> event : events)
			registerBukkitEvent(trigger, event);
	}

	/**
	 * Registers a listener on the {@link org.skriptlang.skript.platform.EventBus} for the provided Event.
	 * Marks that the provided Trigger should be executed when the provided Event occurs.
	 * @param trigger The Trigger to run when the Event occurs.
	 * @param event The Event to listen for.
	 * @see #registerBukkitEvents(Trigger, Class[])
	 * @see #unregisterBukkitEvents(Trigger)
	 */
	public static void registerBukkitEvent(Trigger trigger, Class<? extends PlatformEvent> event) {
		HandlerList handlerList = getHandlerList(event);
		if (handlerList == null)
			return;

		triggers.put(event, trigger);

		EventPriority priority = trigger.getEvent().getEventPriority();

		Map<EventPriority, Registration> byPriority = registrations.computeIfAbsent(event, key -> new EnumMap<>(EventPriority.class));
		if (!byPriority.containsKey(priority)) { // Check if event is registered
			Registration registration = Skript.eventBus().channelFor(event)
				.register(priority, platformEvent -> check(platformEvent, priority));
			byPriority.put(priority, registration);
		}
	}

	/**
	 * Unregisters all events tied to the provided Trigger.
	 * @param trigger The Trigger to unregister events for.
	 */
	public static void unregisterBukkitEvents(Trigger trigger) {
		Iterator<Entry<Class<? extends PlatformEvent>, Trigger>> entryIterator = triggers.entries().iterator();
		entryLoop: while (entryIterator.hasNext()) {
			Entry<Class<? extends PlatformEvent>, Trigger> entry = entryIterator.next();
			if (entry.getValue() != trigger)
				continue;
			Class<? extends PlatformEvent> event = entry.getKey();

			// Remove the trigger from the map
			entryIterator.remove();

			// check if we can unregister the listener
			EventPriority priority = trigger.getEvent().getEventPriority();
			for (Trigger eventTrigger : triggers.get(event)) {
				if (eventTrigger.getEvent().getEventPriority() == priority)
					continue entryLoop;
			}

			// We can attempt to unregister this listener
			Map<EventPriority, Registration> byPriority = registrations.get(event);
			if (byPriority == null)
				continue;
			Registration registration = byPriority.remove(priority);
			if (registration != null)
				registration.unregister();
			if (byPriority.isEmpty())
				registrations.remove(event);
		}
	}

	/**
	 * Events which are listened even if they are cancelled. This should no longer be used.
	 * @deprecated Users should specify the listening behavior in the event declaration. "on any %event%:", "on cancelled %event%:".
	 */
	@Deprecated
	public static final Set<Class<? extends PlatformEvent>> listenCancelled = new HashSet<>();

	/**
	 * A cache for the getHandlerList methods of Event classes.
	 */
	private static final Map<Class<? extends PlatformEvent>, Method> handlerListMethods = new HashMap<>();

	/**
	 * A cache for obtained HandlerLists.
	 */
	private static final Map<Method, WeakReference<HandlerList>> handlerListCache = new HashMap<>();

	@Nullable
	private static HandlerList getHandlerList(Class<? extends PlatformEvent> eventClass) {
		try {
			Method method = getHandlerListMethod(eventClass);

			WeakReference<HandlerList> handlerListReference = handlerListCache.get(method);
			HandlerList handlerList = handlerListReference != null ? handlerListReference.get() : null;
			if (handlerList == null) {
				method.setAccessible(true);
				handlerList = (HandlerList) method.invoke(null);
				handlerListCache.put(method, new WeakReference<>(handlerList));
			}

			return handlerList;
		} catch (Exception ex) {
			//noinspection ThrowableNotThrown
			Skript.exception(ex, "Failed to get HandlerList for event " + eventClass.getName());
			return null;
		}
	}

	private static Method getHandlerListMethod(Class<? extends PlatformEvent> eventClass) {
		Method method;

		synchronized (handlerListMethods) {
			method = handlerListMethods.get(eventClass);
			if (method == null) {
				method = getHandlerListMethod_i(eventClass);
				if (method != null)
					method.setAccessible(true);
				handlerListMethods.put(eventClass, method);
			}
		}

		if (method == null)
			throw new RuntimeException("No getHandlerList method found");

		return method;
	}

	@Nullable
	private static Method getHandlerListMethod_i(Class<? extends PlatformEvent> eventClass) {
		try {
			return eventClass.getDeclaredMethod("getHandlerList");
		} catch (NoSuchMethodException e) {
			if (
				eventClass.getSuperclass() != null
					&& !eventClass.getSuperclass().equals(Event.class)
					&& Event.class.isAssignableFrom(eventClass.getSuperclass())
			) {
				return getHandlerListMethod(eventClass.getSuperclass().asSubclass(PlatformEvent.class));
			} else {
				return null;
			}
		}
	}

}