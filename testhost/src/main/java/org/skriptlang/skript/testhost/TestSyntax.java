package org.skriptlang.skript.testhost;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.Variables;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.util.Kleenean;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.event.Cancellable;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.skriptlang.skript.lang.script.Script;

public final class TestSyntax {

	private TestSyntax() {}

	@SuppressWarnings("unchecked")
	static void register() {
		Skript.registerEvent("Test Event", EvtTest.class, new Class[] {TestEvent.class}, "test %string%");
		Skript.registerEvent("Sub Test Event", EvtTest.class, new Class[] {TestEvent.Sub.class}, "sub test %string%");
		Skript.registerEffect(EffMark.class, "mark %objects%");
		Skript.registerEffect(EffFire.class,
			"fire [cancelled:cancelled] [sub:sub] test event %string% [async:off the main thread]");
		Skript.registerEffect(EffCancel.class, "cancel the test event", "uncancel the test event");
		Skript.registerExpression(ExprThread.class, String.class, ExpressionType.SIMPLE, "[the] test thread");
		Skript.registerExpression(ExprPhase.class, Long.class, ExpressionType.SIMPLE, "[the] test phase");
		Skript.registerExpression(ExprTestName.class, String.class, ExpressionType.SIMPLE, "[the] test event name");
		Skript.registerExpression(ExprTick.class, Long.class, ExpressionType.SIMPLE, "[the] test tick");
		Skript.registerEffect(EffCopyLocals.class, "copy the test locals");
		Skript.registerExpression(ExprLocalsStorage.class, String.class, ExpressionType.SIMPLE, "[the] test locals storage");
	}

	static String scriptName(@Nullable Script script) {
		if (script == null)
			return "?";
		String name = script.getConfig().getFileName();
		return new File(name).getName();
	}

	public static final class EvtTest extends SkriptEvent {

		private String name;

		@Override
		public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
			name = (String) args[0].getSingle();
			return true;
		}

		@Override
		public boolean check(PlatformEvent event) {
			return event instanceof TestEvent test && test.name().equals(name);
		}

		@Override
		public String toString(@Nullable PlatformEvent event, boolean debug) {
			return "test " + name;
		}
	}

	public static final class EffMark extends Effect {

		private Expression<?> values;
		private String script;

		@Override
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			values = LiteralUtils.defendExpression(exprs[0]);
			script = scriptName(getParser().getCurrentScript());
			return LiteralUtils.canInitSafely(values);
		}

		@Override
		protected void execute(PlatformEvent event) {
			String text = Arrays.stream(values.getArray(event))
				.map(Classes::toString)
				.collect(Collectors.joining(" "));
			TestHost.recorder().mark(script, text);
		}

		@Override
		public String toString(@Nullable PlatformEvent event, boolean debug) {
			return "mark " + values.toString(event, debug);
		}
	}

	public static final class EffFire extends Effect {

		private Expression<String> name;
		private boolean cancelled;
		private boolean sub;
		private boolean async;

		@Override
		@SuppressWarnings("unchecked")
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			name = (Expression<String>) exprs[0];
			cancelled = parseResult.hasTag("cancelled");
			sub = parseResult.hasTag("sub");
			async = parseResult.hasTag("async");
			return true;
		}

		@Override
		protected void execute(PlatformEvent event) {
			String value = name.getSingle(event);
			if (value == null)
				return;
			TestEvent fired = sub ? new TestEvent.Sub(value) : new TestEvent(value);
			fired.setCancelled(cancelled);
			if (async)
				TestHost.loop().runAsync(() -> Skript.eventBus().fire(fired));
			else
				Skript.eventBus().fire(fired);
		}

		@Override
		public String toString(@Nullable PlatformEvent event, boolean debug) {
			return "fire test event " + name.toString(event, debug);
		}
	}

	public static final class EffCancel extends Effect {

		private boolean cancel;

		@Override
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			cancel = matchedPattern == 0;
			return true;
		}

		@Override
		protected void execute(PlatformEvent event) {
			if (event instanceof Cancellable cancellable)
				cancellable.setCancelled(cancel);
		}

		@Override
		public String toString(@Nullable PlatformEvent event, boolean debug) {
			return (cancel ? "cancel" : "uncancel") + " the test event";
		}
	}

	public static final class ExprThread extends Constant<String> {

		@Override
		protected String value(PlatformEvent event) {
			return Skript.scheduler().isPrimaryThread() ? "main" : "other";
		}

		@Override
		public Class<? extends String> getReturnType() {
			return String.class;
		}
	}

	public static final class ExprPhase extends Constant<Long> {

		@Override
		protected Long value(PlatformEvent event) {
			return (long) TestHost.phase();
		}

		@Override
		public Class<? extends Long> getReturnType() {
			return Long.class;
		}
	}

	public static final class ExprTick extends Constant<Long> {

		@Override
		protected Long value(PlatformEvent event) {
			return TestHost.loop().tick();
		}

		@Override
		public Class<? extends Long> getReturnType() {
			return Long.class;
		}
	}

	public static final class ExprTestName extends Constant<String> {

		@Override
		protected @Nullable String value(PlatformEvent event) {
			return event instanceof TestEvent test ? test.name() : null;
		}

		@Override
		public Class<? extends String> getReturnType() {
			return String.class;
		}
	}

	public static final class EffCopyLocals extends Effect {

		@Override
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			return true;
		}

		@Override
		protected void execute(PlatformEvent event) {
			Variables.copyLocalVariables(event);
		}

		@Override
		public String toString(@Nullable PlatformEvent event, boolean debug) {
			return "copy the test locals";
		}
	}

	public static final class ExprLocalsStorage extends Constant<String> {

		@Override
		protected String value(PlatformEvent event) {
			try {
				java.lang.reflect.Field field = Variables.class.getDeclaredField("localVariables");
				field.setAccessible(true);
				Object frame = ((java.util.Map<?, ?>) field.get(null)).get(event);
				if (frame == null)
					return "none";
				java.lang.reflect.Field table = frame.getClass().getDeclaredField("table");
				java.lang.reflect.Field values = frame.getClass().getDeclaredField("values");
				table.setAccessible(true);
				values.setAccessible(true);
				return table.get(frame) != null && values.get(frame) != null ? "slots" : "map";
			} catch (ReflectiveOperationException e) {
				return "error " + e.getClass().getSimpleName();
			}
		}

		@Override
		public Class<? extends String> getReturnType() {
			return String.class;
		}
	}

	public abstract static class Constant<T> extends SimpleExpression<T> {

		protected abstract @Nullable T value(PlatformEvent event);

		@Override
		public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
			return true;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected T @Nullable [] get(PlatformEvent event) {
			T value = value(event);
			if (value == null)
				return null;
			T[] array = (T[]) java.lang.reflect.Array.newInstance(getReturnType(), 1);
			array[0] = value;
			return array;
		}

		@Override
		public boolean isSingle() {
			return true;
		}

		@Override
		public String toString(@Nullable PlatformEvent event, boolean debug) {
			return getClass().getSimpleName();
		}
	}
}
