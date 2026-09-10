package ch.njol.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptEventHandler;
import ch.njol.skript.events.custom.ScheduledEvent;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.util.Timespan;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.platform.PlatformTask;

public class EvtPeriodical extends SkriptEvent {

	static {
		Skript.registerEvent("*Periodical", EvtPeriodical.class, ScheduledEvent.class, "every %timespan%")
			.description("An event that is called periodically.")
			.examples(
				"every 2 seconds:",
				"every minecraft hour:",
				"every tick: # can cause lag depending on the code inside the event",
				"every minecraft days:"
			).since("1.0")
			.documentationID("eventperiodical");
	}

	@SuppressWarnings("NotNullFieldNotInitialized")
	private Timespan period;

	@SuppressWarnings("NotNullFieldNotInitialized")
	private PlatformTask task;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
		period = ((Literal<Timespan>) args[0]).getSingle();
		return true;
	}

	@Override
	public boolean postLoad() {
		long ticks = period.getAs(Timespan.TimePeriod.TICK);

		task = Skript.scheduler().sync(() -> {
			ScheduledEvent event = new ScheduledEvent();
			SkriptEventHandler.logEventStart(event);
			SkriptEventHandler.logTriggerStart(trigger);
			trigger.execute(event);
			SkriptEventHandler.logTriggerEnd(trigger);
			SkriptEventHandler.logEventEnd();
		}, ticks, ticks);

		return true;
	}

	@Override
	public void unload() {
		task.cancel();
	}

	@Override
	public boolean check(PlatformEvent event) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isEventPrioritySupported() {
		return false;
	}

	@Override
	public String toString(@Nullable PlatformEvent event, boolean debug) {
		return "every " + period;
	}

	private void execute() {

	}

}
