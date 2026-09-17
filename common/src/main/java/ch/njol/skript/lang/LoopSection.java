package ch.njol.skript.lang;

import com.google.common.collect.MapMaker;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.event.PlatformEvent;

import java.util.Iterator;
import java.util.Map;

/**
 * Represents a loop section.
 *
 * @see ch.njol.skript.sections.SecWhile
 * @see ch.njol.skript.sections.SecLoop
 */
public abstract class LoopSection extends Section implements SyntaxElement, Debuggable, SectionExitHandler {

	/**
	 * The per-event state of a running loop. Everything a loop tracks lives in one object so that
	 * an iteration costs a single map lookup rather than one per property.
	 */
	protected static final class LoopState {

		public long counter;
		public @Nullable Iterator<?> iterator;
		public @Nullable Object current;
		public boolean hasCurrent;
		public @Nullable Object previous;
		public @Nullable Object next;

	}

	protected final transient Map<PlatformEvent, LoopState> loopStates = new MapMaker()
		.weakKeys()
		.concurrencyLevel(8)
		.makeMap();

	/**
	 * @param event The event the loop is running for
	 * @return The loop's state for that event, creating it if the loop has not started yet
	 */
	protected LoopState loopState(PlatformEvent event) {
		LoopState state = loopStates.get(event);
		if (state == null) {
			state = new LoopState();
			loopStates.put(event, state);
		}
		return state;
	}

	/**
	 * @param event The event the loop is running for
	 * @return The loop's state for that event, or null if the loop is not running
	 */
	protected @Nullable LoopState currentLoopState(PlatformEvent event) {
		return loopStates.get(event);
	}

	/**
	 * @param event The event where the loop is used to return its loop iterations
	 * @return The loop iteration number
	 */
	public long getLoopCounter(PlatformEvent event) {
		LoopState state = loopStates.get(event);
		return state == null || state.counter == 0 ? 1L : state.counter;
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
		loopStates.remove(event);
	}

}
