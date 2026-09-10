package ch.njol.skript.lang;

import com.google.common.collect.MapMaker;
import org.skriptlang.skript.lang.event.PlatformEvent;

import java.util.Map;

/**
 * Represents a loop section.
 *
 * @see ch.njol.skript.sections.SecWhile
 * @see ch.njol.skript.sections.SecLoop
 */
public abstract class LoopSection extends Section implements SyntaxElement, Debuggable, SectionExitHandler {

	protected final transient Map<PlatformEvent, Long> currentLoopCounter = new MapMaker()
		.weakKeys()
		.concurrencyLevel(8)
		.makeMap();

	/**
	 * @param event The event where the loop is used to return its loop iterations
	 * @return The loop iteration number
	 */
	public long getLoopCounter(PlatformEvent event) {
		return currentLoopCounter.getOrDefault(event, 1L);
	}

	/**
	 * @return The next {@link TriggerItem} after the loop
	 */
	public abstract TriggerItem getActualNext();

	/**
	 * Exit the loop, used to reset the loop properties such as iterations counter
	 * @param event The event where the loop is used to reset its relevant properties
	 */
	@Override
	public void exit(PlatformEvent event) {
		currentLoopCounter.remove(event);
	}

}