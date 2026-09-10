package ch.njol.skript.lang;

import com.google.common.base.Preconditions;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

/**
 * Utility class to build syntax strings, primarily intended for use
 * in {@link Debuggable#toString(PlatformEvent, boolean)} implementations.
 * Spaces are automatically added between the provided objects.
 */
public class SyntaxStringBuilder {

	private final boolean debug;
	private final @Nullable PlatformEvent event;
	private final StringJoiner joiner = new StringJoiner(" ");

	/**
	 * Creates a new SyntaxStringBuilder.
	 *
	 * @param event The event to get information from. This is always null if debug == false.
	 * @param debug If true this should print more information, if false this should print what is shown to the end user
	 */
	public SyntaxStringBuilder(@Nullable PlatformEvent event, boolean debug) {
		this.event = event;
		this.debug = debug;
	}

	/**
	 * Adds an object to the string and returns the builder.
	 * Spaces are automatically added between the provided objects.
	 * If the object is a {@link Debuggable} it will be formatted using
	 * {@link Debuggable#toString(PlatformEvent, boolean)}.
	 *
	 * @param object The object to add.
	 * @return The builder.
	 */
	public SyntaxStringBuilder append(@NotNull Object object) {
		Preconditions.checkNotNull(object);
		if (object instanceof Debuggable debuggable) {
			joiner.add(debuggable.toString(event, debug));
		} else {
			joiner.add(object.toString());
		}
		return this;
	}

	/**
	 * Adds multiple objects to the string and returns the builder.
	 * Spaces are automatically added between the provided objects.
	 *
	 * @param objects The objects to add.
	 * @return The builder.
	 */
	public SyntaxStringBuilder append(@NotNull Object... objects) {
		for (Object object : objects) {
			append(object);
		}
		return this;
	}

	@Override
	public String toString() {
		return joiner.toString();
	}

}
