package ch.njol.skript.util;

import ch.njol.skript.Skript;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.variables.Variables;
import org.skriptlang.skript.lang.event.PlatformEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Effects that extend this class are ran asynchronously. Next trigger item will be ran
 * in main server thread, as if there had been a delay before.
 * <p>
 * Majority of Skript and Minecraft APIs are not thread-safe, so be careful.
 *
 * Make sure to add set {@link ch.njol.skript.ScriptLoader#hasDelayBefore} to
 * {@link ch.njol.util.Kleenean#TRUE} in the {@code init} method.
 */
public abstract class AsyncEffect extends Effect {

	@Override
	@Nullable
	protected TriggerItem walk(PlatformEvent e) {
		debug(e, true);

		Object localVars = Variables.removeLocals(e); // Back up local variables

		if (!Skript.getInstance().isEnabled()) // See https://github.com/SkriptLang/Skript/issues/3702
			return null;

		Skript.scheduler().async(() -> {
			Delay.addDelayedEvent(e); // Mark this event as delayed
			// Re-set local variables
			if (localVars != null)
				Variables.setLocalVariables(e, localVars);

			execute(e); // Execute this effect

			if (getNext() != null) {
				Skript.scheduler().sync(() -> { // Walk to next item synchronously

					TriggerItem.walk(getNext(), e);

					Variables.removeLocals(e); // Clean up local vars, we may be exiting now
				}, 0, -1);
			} else {
				Variables.removeLocals(e);
			}
		}, 0, -1);
		return null;
	}
}