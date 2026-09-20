package ch.njol.skript.lang;

import com.google.common.collect.MapMaker;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.event.PlatformEvent;

import java.lang.ref.WeakReference;
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

		/**
		 * Set when the loop exits, so that a memo held by another thread cannot hand out a state
		 * the loop has already finished with. A trigger can change threads across a delay, which
		 * means the thread that exits the loop is not always the one that entered it.
		 */
		volatile boolean exited;

	}

	protected final transient Map<PlatformEvent, LoopState> loopStates = new MapMaker()
		.weakKeys()
		.concurrencyLevel(8)
		.makeMap();

	/**
	 * The last state looked up on this thread. Iterations of one loop run back to back for the same
	 * event, so this answers nearly every lookup without touching the map, whose weak keys make a
	 * read far more expensive than a plain hash lookup. The event is held weakly so that a loop
	 * abandoned by an exception cannot pin it.
	 */
	private static final ThreadLocal<Memo> MEMO = ThreadLocal.withInitial(Memo::new);

	private static final class Memo {

		private @Nullable LoopSection section;
		private @Nullable WeakReference<PlatformEvent> event;
		private @Nullable LoopState state;

		private @Nullable LoopState get(LoopSection section, PlatformEvent event) {
			if (this.section != section)
				return null;
			WeakReference<PlatformEvent> reference = this.event;
			if (reference == null || reference.get() != event)
				return null;
			LoopState state = this.state;
			return state != null && !state.exited ? state : null;
		}

		private void set(LoopSection section, PlatformEvent event, LoopState state) {
			this.section = section;
			this.event = new WeakReference<>(event);
			this.state = state;
		}

		private void clear(LoopSection section, PlatformEvent event) {
			if (this.section != section)
				return;
			WeakReference<PlatformEvent> reference = this.event;
			if (reference != null && reference.get() != event)
				return;
			this.section = null;
			event = null;
			state = null;
		}

	}

	/**
	 * @param event The event the loop is running for
	 * @return The loop's state for that event, creating it if the loop has not started yet
	 */
	protected LoopState loopState(PlatformEvent event) {
		Memo memo = MEMO.get();
		LoopState state = memo.get(this, event);
		if (state != null)
			return state;
		state = loopStates.get(event);
		if (state == null) {
			state = new LoopState();
			loopStates.put(event, state);
		}
		memo.set(this, event, state);
		return state;
	}

	/**
	 * @param event The event the loop is running for
	 * @return The loop's state for that event, or null if the loop is not running
	 */
	protected @Nullable LoopState currentLoopState(PlatformEvent event) {
		LoopState state = MEMO.get().get(this, event);
		return state != null ? state : loopStates.get(event);
	}

	/**
	 * @param event The event where the loop is used to return its loop iterations
	 * @return The loop iteration number
	 */
	public long getLoopCounter(PlatformEvent event) {
		LoopState state = currentLoopState(event);
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
		LoopState state = loopStates.remove(event);
		if (state != null)
			state.exited = true;
		MEMO.get().clear(this, event);
	}

}
